package com.wts.auth.controller;

import com.wts.auth.dto.StudentImportResult;
import com.wts.auth.dto.BatchIdsDTO;
import com.wts.auth.dto.UserDTO;
import com.wts.auth.entity.SysUser;
import com.wts.auth.enums.Permission;
import com.wts.auth.enums.UserPost;
import com.wts.auth.service.PermissionService;
import com.wts.auth.service.UserService;
import com.wts.common.exception.BizException;
import com.wts.common.result.PageResult;
import com.wts.common.result.R;
import com.wts.common.security.CurrentUser;
import com.wts.common.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final PermissionService permissionService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    public R<PageResult<SysUser>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String post,
            @RequestParam(required = false) String className,
            @RequestParam(required = false) String orgId) {
        CurrentUser user = currentUserProvider.require();
        // 用户管理（主任/管理员）或可导入班级（所有教师）均可进入用户列表，各自范围由 visibleUserIds 限定
        requireManageOrImport(user);
        // 学生全院共用：查询学生时不做范围限制，任何教职工可见全院学生；教职工列表仍按用户管理范围过滤
        List<String> scope = permissionService.visibleUserIds(user);
        if (StringUtils.hasText(post) && UserPost.STUDENT.code().equals(post.trim())) {
            scope = null;
        }
        PageResult<SysUser> result = userService.listUsers(
                page, size, keyword, state, post, className, orgId, scope);
        return R.ok(result);
    }

    @PostMapping
    public R<SysUser> create(@RequestBody UserDTO dto) {
        CurrentUser user = currentUserProvider.require();
        requireCreateAllowed(user, dto.getPost());
        ensurePostAllowed(user, dto.getPost());
        ensureOrgAllowed(user, dto.getOrgId());
        SysUser created = userService.createUser(dto, user.id());
        return R.ok(created);
    }

    @PostMapping("/import-students")
    public R<StudentImportResult> importStudents(@RequestParam("file") MultipartFile file) {
        CurrentUser user = currentUserProvider.require();
        permissionService.require(user, Permission.CLASS_IMPORT.name());
        if (file == null || file.isEmpty()) {
            throw BizException.fail("请上传学生帐号 Excel 文件");
        }
        try {
            return R.ok(userService.importStudents(file.getInputStream(), user.id()));
        } catch (IOException e) {
            throw BizException.fail("读取上传文件失败: " + e.getMessage());
        }
    }

    @GetMapping("/import-template")
    public void downloadTemplate(HttpServletResponse response) {
        CurrentUser user = currentUserProvider.require();
        permissionService.require(user, Permission.CLASS_IMPORT.name());
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition",
                    "attachment; filename=" + URLEncoder.encode("学生导入模板.xlsx", StandardCharsets.UTF_8));
            userService.downloadTemplate(response.getOutputStream());
            response.getOutputStream().flush();
        } catch (Exception e) {
            throw BizException.fail("下载模板失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public R<SysUser> update(@PathVariable String id, @RequestBody UserDTO dto) {
        CurrentUser user = currentUserProvider.require();
        requireUserWriteAllowed(user, dto, List.of(id));
        ensurePostAllowed(user, dto.getPost());
        ensureOrgAllowed(user, dto.getOrgId());
        SysUser updated = userService.updateUser(id, dto, user.id());
        return R.ok(updated);
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable String id) {
        CurrentUser user = currentUserProvider.require();
        requireUserWriteAllowed(user, null, List.of(id));
        userService.deleteUser(id, user.id());
        return R.ok();
    }

    @PostMapping("/batch-disable")
    public R<Void> batchDisable(@RequestBody BatchIdsDTO dto) {
        CurrentUser user = currentUserProvider.require();
        requireUserWriteAllowed(user, null, dto.getIds());
        userService.disableUsers(dto.getIds(), user.id());
        return R.ok();
    }

    @DeleteMapping("/{id}/hard-delete")
    public R<Void> hardDelete(@PathVariable String id) {
        CurrentUser user = currentUserProvider.require();
        requireUserWriteAllowed(user, null, List.of(id));
        userService.hardDeleteUser(id, user.id());
        return R.ok();
    }

    @PostMapping("/batch-hard-delete")
    public R<Void> batchHardDelete(@RequestBody BatchIdsDTO dto) {
        CurrentUser user = currentUserProvider.require();
        requireUserWriteAllowed(user, null, dto.getIds());
        userService.hardDeleteUsers(dto.getIds(), user.id());
        return R.ok();
    }

    @PostMapping("/{id}/reset-password")
    public R<Void> resetPassword(@PathVariable String id) {
        CurrentUser user = currentUserProvider.require();
        requireUserWriteAllowed(user, null, List.of(id));
        userService.resetPassword(id, user.id());
        return R.ok();
    }

    @PostMapping("/change-password")
    public R<Void> changePassword(@RequestBody Map<String, String> body) {
        CurrentUser user = currentUserProvider.require();
        userService.changePassword(user.id(), body.get("oldPassword"), body.get("newPassword"));
        return R.ok();
    }

    /** 新建用户权限：主任/管理员可建学生与教职工；普通教师仅能建学生（学生全院共用） */
    private void requireCreateAllowed(CurrentUser user, String post) {
        if (user.isPlatformAdmin() || user.hasPerm(Permission.USER_MANAGE.name())) {
            return;
        }
        if (!UserPost.STUDENT.code().equals(post)) {
            throw BizException.forbidden("只能创建学生账号");
        }
    }

    /**
     * 用户写操作权限（更新/删除/禁用/重置密码）。
     * 目标全部为学生时：学生全院共用，任何教职工可管理任意学生，且不受组织范围限制；
     * 涉及教职工的操作：仍需 USER_MANAGE 权限 + 组织范围校验。
     */
    private void requireUserWriteAllowed(CurrentUser user, UserDTO dto, List<String> targetIds) {
        List<SysUser> loaded = (targetIds == null || targetIds.isEmpty())
                ? List.of()
                : userService.listByIds(targetIds);
        List<SysUser> targets = loaded == null ? List.of() : loaded;
        boolean allStudents = !targets.isEmpty()
                && targets.stream().allMatch(u -> UserPost.STUDENT.code().equals(u.getPost()));
        if (allStudents) {
            if (!user.isStaff()) {
                throw BizException.forbidden("无权操作");
            }
            if (dto != null && StringUtils.hasText(dto.getPost())
                    && !UserPost.STUDENT.code().equals(dto.getPost())) {
                throw BizException.forbidden("不能将学生账号变更为其他职位");
            }
            return;
        }
        permissionService.require(user, Permission.USER_MANAGE.name());
        permissionService.ensureTargetsInScope(user, targetIds);
    }

    /** 职位调整范围校验：非平台管理员只能设 学生/教师/副主任，不能设 主任/平台管理员 */
    private void ensurePostAllowed(CurrentUser user, String post) {
        if (user.isPlatformAdmin() || post == null || post.isBlank()) {
            return;
        }
        if (UserPost.DIRECTOR.code().equals(post)) {
            throw BizException.forbidden("只有平台管理员可以设置主任");
        }
        if (UserPost.PLATFORM_ADMIN.code().equals(post)) {
            throw BizException.forbidden("无权设置平台管理员");
        }
    }

    /** 组织归属校验：非平台管理员只能将用户分配到自己可见的组织节点下 */
    private void ensureOrgAllowed(CurrentUser user, String orgId) {
        if (orgId == null || orgId.isBlank() || user.isPlatformAdmin()) {
            return;
        }
        List<String> visibleOrgs = permissionService.visibleOrgIds(user);
        if (visibleOrgs == null) {
            return;
        }
        Set<String> allowed = Set.copyOf(visibleOrgs);
        if (!allowed.contains(orgId)) {
            throw BizException.forbidden("目标组织不在你的管理范围内");
        }
    }

    /** 用户管理（主任/管理员）或班级导入（所有教师）任一权限即可进入用户列表 */
    private void requireManageOrImport(CurrentUser user) {
        if (!user.hasPerm(Permission.USER_MANAGE.name()) && !user.hasPerm(Permission.CLASS_IMPORT.name())) {
            throw BizException.forbidden("无权操作");
        }
    }
}