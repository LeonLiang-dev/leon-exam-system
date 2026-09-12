package com.wts.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wts.auth.entity.SysOrganization;
import com.wts.auth.entity.SysUser;
import com.wts.auth.entity.SysUserorg;
import com.wts.auth.enums.Permission;
import com.wts.auth.enums.UserPost;
import com.wts.auth.mapper.SysOrganizationMapper;
import com.wts.auth.mapper.SysUserMapper;
import com.wts.auth.mapper.SysUserorgMapper;
import com.wts.common.exception.BizException;
import com.wts.common.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 职位与权限统一判定、数据可见范围计算。
 * 权限点记录在 alone_auth_user.PERMS（逗号分隔）；platform_admin 的 PERMS 为空 = 全部权限。
 */
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final SysUserMapper userMapper;
    private final SysUserorgMapper userorgMapper;
    private final SysOrganizationMapper organizationMapper;

    /** 用户身份快照（供登录链路/过滤器/用户管理使用） */
    public record UserIdentity(String post, Set<String> perms, List<String> orgIds) {
    }

    /**
     * 加载用户身份快照：职位、权限点集合、所属组织节点 id 列表。
     */
    public UserIdentity loadIdentity(String userId) {
        SysUser user = userId != null ? userMapper.selectById(userId) : null;
        if (user == null) {
            return null;
        }
        String post = normalizePost(user);
        Set<String> perms = parsePerms(user.getPerms());
        List<String> orgIds = userorgMapper.selectList(
                        new LambdaQueryWrapper<SysUserorg>().eq(SysUserorg::getUserid, userId))
                .stream().map(SysUserorg::getOrganizationid).filter(StringUtils::hasText).distinct().toList();
        return new UserIdentity(post, perms, orgIds);
    }

    /** 职位缺失时按旧 type 推导 */
    public String normalizePost(SysUser user) {
        if (StringUtils.hasText(user.getPost())) {
            return user.getPost();
        }
        if ("2".equals(user.getType())) {
            return UserPost.STUDENT.code();
        }
        if ("3".equals(user.getType())) {
            return UserPost.PLATFORM_ADMIN.code();
        }
        return UserPost.TEACHER.code();
    }

    /** 按职位返回默认权限点集合；platform_admin 与 student 都返回空集（空 = 全部权限，仅对 platform_admin 生效） */
    public static Set<String> defaultPerms(String post) {
        if (post == null || UserPost.PLATFORM_ADMIN.code().equals(post) || UserPost.STUDENT.code().equals(post)) {
            return new HashSet<>();
        }
        Set<String> perms = new HashSet<>(Permission.TEACHER_DEFAULT_PERMS);
        if (UserPost.isDeptManager(post)) {
            perms.addAll(Permission.DEPT_MANAGER_EXTRA_PERMS);
        }
        return perms;
    }

    private Set<String> parsePerms(String perms) {
        if (!StringUtils.hasText(perms)) {
            return new HashSet<>();
        }
        return Set.of(perms.split(","));
    }

    /** 校验当前操作是否有指定权限，无则抛 403 */
    public void require(CurrentUser user, String permission) {
        if (user == null || !user.hasPerm(permission)) {
            throw BizException.forbidden("无权操作");
        }
    }

    /**
     * 当前用户可管理的用户 id 集合（用户管理模块的数据范围）。
     * platform_admin = null（不做限制）；teacher = 仅自己；director/deputy = 本教研室（含其下组织）用户。
     */
    public List<String> visibleUserIds(CurrentUser user) {
        if (user == null || user.isPlatformAdmin()) {
            return null;
        }
        if (user.isDeptManager()) {
            return findUserIdsInDept(user, null);
        }
        return List.of(user.id());
    }

    /**
     * 题目/试卷/答题室的创建者（cuser）可见范围。
     * platform_admin = null（不限）；teacher = 仅自己；director/deputy = 本教研室全体教师/负责人。
     */
    public List<String> visibleOwnerIds(CurrentUser user) {
        if (user == null || user.isPlatformAdmin()) {
            return null;
        }
        if (user.isDeptManager()) {
            return findUserIdsInDept(user, List.of(
                    UserPost.TEACHER.code(), UserPost.DIRECTOR.code(), UserPost.DEPUTY.code()));
        }
        return List.of(user.id());
    }

    /**
     * 当前用户可分配/操作的组织节点 id 集合（用于创建/更新用户时的组织归属校验）。
     * platform_admin = null（不限）；director/deputy = 其教研室子树；其他 = 空。
     */
    public List<String> visibleOrgIds(CurrentUser user) {
        if (user == null || user.isPlatformAdmin()) {
            return null;
        }
        if (user.isDeptManager()) {
            return resolveDeptOrgIds(user);
        }
        return List.of();
    }

    /**
     * 校验目标用户是否都在当前用户的可管理范围内（platform_admin 恒通过）。
     */
    public void ensureTargetsInScope(CurrentUser user, List<String> targetIds) {
        if (user == null || targetIds == null || targetIds.isEmpty() || user.isPlatformAdmin()) {
            return;
        }
        List<String> visible = visibleUserIds(user);
        if (visible == null) {
            return;
        }
        Set<String> visibleSet = new HashSet<>(visible);
        List<String> outOfScope = targetIds.stream()
                .filter(id -> !visibleSet.contains(id))
                .distinct()
                .toList();
        if (!outOfScope.isEmpty()) {
            throw BizException.forbidden("部分目标用户不在你的管理范围内");
        }
    }

    /**
     * 查询当前主任/副主任所在教研室（用户所属组织节点及其全部子孙组织）下的用户。
     *
     * @param postFilter 用户职位过滤，null 表示不限
     */
    private List<String> findUserIdsInDept(CurrentUser user, List<String> postFilter) {
        List<String> deptOrgIds = resolveDeptOrgIds(user);
        if (deptOrgIds.isEmpty()) {
            // 无组织归属的主任/副主任降级为仅自己
            return List.of(user.id());
        }

        List<SysUserorg> userOrgs = userorgMapper.selectList(
                new LambdaQueryWrapper<SysUserorg>().in(SysUserorg::getOrganizationid, deptOrgIds));
        if (userOrgs.isEmpty()) {
            return List.of(user.id());
        }
        List<String> userIds = userOrgs.stream().map(SysUserorg::getUserid).distinct().collect(Collectors.toList());
        if (postFilter == null) {
            return userIds;
        }
        if (userIds.isEmpty()) {
            return List.of();
        }
        return userMapper.selectList(new LambdaQueryWrapper<SysUser>().in(SysUser::getId, userIds))
                .stream()
                .filter(u -> postFilter.contains(normalizePost(u)))
                .map(SysUser::getId)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 解析主任/副主任所属教研室的组织节点集合（含组织节点的全部子孙节点）。
     * 取用户所属组织节点为起点向下展开；若用户挂的是「教师」等子节点，向上回退到其教研室层级。
     */
    private List<String> resolveDeptOrgIds(CurrentUser user) {
        List<String> orgIds = user.orgIds() != null ? user.orgIds() : List.of();
        if (orgIds.isEmpty()) {
            return List.of();
        }

        // 全量组织，构建 parent -> children 映射
        List<SysOrganization> allOrgs = organizationMapper.selectList(null);
        Map<String, List<SysOrganization>> childrenByParent = new HashMap<>();
        Map<String, SysOrganization> byId = new HashMap<>();
        for (SysOrganization org : allOrgs) {
            byId.put(org.getId(), org);
            childrenByParent.computeIfAbsent(org.getParentid(), k -> new ArrayList<>()).add(org);
        }

        Set<String> result = new HashSet<>();
        for (String orgId : orgIds) {
            String rootId = findDeptRoot(orgId, byId);
            if (rootId != null) {
                collectSubtree(rootId, byId, childrenByParent, result);
            }
        }
        return new ArrayList<>(result);
    }

    /**
     * 从节点向上找到「教研室」层级：父节点为学院领导或根组织的最高一级组织节点。
     * 约定组织结构: 默认组织(根 NONE) → 学院领导 → 教研室 → 教师分组。
     */
    private String findDeptRoot(String orgId, Map<String, SysOrganization> byId) {
        SysOrganization node = byId.get(orgId);
        if (node == null) {
            return null;
        }
        String current = node.getId();
        while (true) {
            SysOrganization cur = byId.get(current);
            if (cur == null) {
                return null;
            }
            String parentId = cur.getParentid();
            if (parentId == null || "NONE".equals(parentId)) {
                return current;
            }
            SysOrganization parent = byId.get(parentId);
            if (parent == null) {
                return null;
            }
            // 父节点是根或学院领导（根/学院领导的父再向上是 NONE 或根），当前节点即为教研室层级
            String grandParent = parent.getParentid();
            boolean parentIsRootOrCollege = grandParent == null
                    || "NONE".equals(grandParent)
                    || "NONE".equals(parentId)
                    || (byId.get(grandParent) != null && "NONE".equals(byId.get(grandParent).getParentid()));
            if (parentIsRootOrCollege) {
                return current;
            }
            current = parentId;
        }
    }

    private void collectSubtree(String nodeId, Map<String, SysOrganization> byId,
                                Map<String, List<SysOrganization>> childrenByParent, Set<String> out) {
        if (!out.add(nodeId)) {
            return;
        }
        List<SysOrganization> children = childrenByParent.getOrDefault(nodeId, List.of());
        for (SysOrganization child : children) {
            collectSubtree(child.getId(), byId, childrenByParent, out);
        }
    }
}