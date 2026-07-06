package com.wts.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wts.auth.entity.SysOrganization;
import com.wts.auth.mapper.SysOrganizationMapper;
import com.wts.common.exception.BizException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final SysOrganizationMapper organizationMapper;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String ROOT_PARENT_ID = "NONE";

    /**
     * 获取组织树
     */
    public List<OrgTreeNode> getOrgTree() {
        List<SysOrganization> all = organizationMapper.selectList(
                new LambdaQueryWrapper<SysOrganization>()
                        .eq(SysOrganization::getState, "1")
                        .orderByAsc(SysOrganization::getSort)
        );
        return buildTree(all, ROOT_PARENT_ID);
    }

    /**
     * 创建组织
     */
    public SysOrganization createOrganization(SysOrganization org, String operatorId) {
        String now = LocalDateTime.now().format(FMT);
        String id = UUID.randomUUID().toString().replace("-", "");
        org.setId(id);
        org.setCtime(now);
        org.setUtime(now);
        org.setCuser(operatorId);
        org.setMuser(operatorId);
        org.setState("1");
        org.setUuid(id);

        String parentId = normalizeParentId(org.getParentid());
        org.setParentid(parentId);
        if (org.getType() == null || org.getType().isBlank()) {
            org.setType("0");
        }
        if (org.getSort() == null) {
            org.setSort(1);
        }

        // 生成 treecode
        if (ROOT_PARENT_ID.equals(parentId)) {
            org.setTreecode(id);
        } else {
            SysOrganization parent = organizationMapper.selectById(parentId);
            if (parent == null) {
                throw BizException.notFound("父组织不存在");
            }
            String parentTreecode = parent.getTreecode() != null ? parent.getTreecode() : parent.getId();
            org.setTreecode(parentTreecode + id);
        }

        organizationMapper.insert(org);
        return org;
    }

    /**
     * 更新组织
     */
    public SysOrganization updateOrganization(String id, SysOrganization org, String operatorId) {
        SysOrganization existing = organizationMapper.selectById(id);
        if (existing == null) {
            throw BizException.notFound("组织不存在");
        }

        String now = LocalDateTime.now().format(FMT);
        if (org.getName() != null) existing.setName(org.getName());
        if (org.getComments() != null) existing.setComments(org.getComments());
        if (org.getType() != null) existing.setType(org.getType());
        if (org.getSort() != null) existing.setSort(org.getSort());
        existing.setUtime(now);
        existing.setMuser(operatorId);

        organizationMapper.updateById(existing);
        return existing;
    }

    /**
     * 删除组织 (逻辑删除)
     */
    public void deleteOrganization(String id, String operatorId) {
        SysOrganization org = organizationMapper.selectById(id);
        if (org == null) {
            throw BizException.notFound("组织不存在");
        }

        // 检查是否有子组织
        Long childCount = organizationMapper.selectCount(
                new LambdaQueryWrapper<SysOrganization>()
                        .eq(SysOrganization::getParentid, id)
                        .eq(SysOrganization::getState, "1")
        );
        if (childCount > 0) {
            throw BizException.fail("该组织下有子组织，不能删除");
        }

        String now = LocalDateTime.now().format(FMT);
        org.setState("0");
        org.setUtime(now);
        org.setMuser(operatorId);
        organizationMapper.updateById(org);
    }

    private List<OrgTreeNode> buildTree(List<SysOrganization> all, String parentId) {
        return all.stream()
                .filter(org -> parentId.equals(normalizeParentId(org.getParentid())))
                .map(org -> {
                    OrgTreeNode node = new OrgTreeNode();
                    node.setId(org.getId());
                    node.setName(org.getName());
                    node.setType(org.getType());
                    node.setSort(org.getSort());
                    node.setComments(org.getComments());
                    node.setParentid(normalizeParentId(org.getParentid()));
                    node.setChildren(buildTree(all, org.getId()));
                    if (node.getChildren().isEmpty()) {
                        node.setChildren(null);
                    }
                    return node;
                })
                .collect(Collectors.toList());
    }

    private String normalizeParentId(String parentId) {
        if (parentId == null || parentId.isBlank()) {
            return ROOT_PARENT_ID;
        }
        return parentId;
    }

    @Data
    public static class OrgTreeNode {
        private String id;
        private String name;
        private String type;
        private Integer sort;
        private String comments;
        private String parentid;
        private List<OrgTreeNode> children;
    }
}
