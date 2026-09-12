package com.wts.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wts.auth.entity.SysOrganization;
import com.wts.auth.entity.SysUserorg;
import com.wts.auth.mapper.SysOrganizationMapper;
import com.wts.auth.mapper.SysUserorgMapper;
import com.wts.common.exception.BizException;
import com.wts.exam.entity.ExamSubjectType;
import com.wts.exam.mapper.ExamSubjectTypeMapper;
import com.wts.exam.service.SubjectTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubjectTypeServiceImpl implements SubjectTypeService {

    private final ExamSubjectTypeMapper typeMapper;
    private final SysUserorgMapper userorgMapper;
    private final SysOrganizationMapper organizationMapper;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String ROOT_PARENT_ID = "NONE";

    @Override
    public List<ExamSubjectType> getTree() {
        return getTree(null);
    }

    @Override
    public List<ExamSubjectType> getTree(List<String> ownerIds) {
        List<ExamSubjectType> all = typeMapper.selectList(
                new LambdaQueryWrapper<ExamSubjectType>()
                        .eq(ExamSubjectType::getState, "1")
                        .orderByAsc(ExamSubjectType::getSort)
        );
        List<ExamSubjectType> visible = ownerIds == null ? all : filterByOwners(all, ownerIds);
        fillOrgNames(visible);
        return buildTree(visible, ROOT_PARENT_ID);
    }

    /**
     * 保留可见节点及其完整祖先链（父链节点仍参与建树，但仅作为路径展示）。
     */
    private List<ExamSubjectType> filterByOwners(List<ExamSubjectType> all, List<String> ownerIds) {
        Set<String> owners = new HashSet<>(ownerIds);
        Set<String> keep = new HashSet<>();
        Map<String, ExamSubjectType> byId = all.stream()
                .collect(Collectors.toMap(ExamSubjectType::getId, t -> t, (a, b) -> a));
        for (ExamSubjectType type : all) {
            if (!owners.contains(type.getCuser())) {
                continue;
            }
            String current = type.getId();
            while (current != null) {
                keep.add(current);
                ExamSubjectType node = byId.get(current);
                if (node == null) {
                    break;
                }
                String parentId = node.getParentid();
                if (StringUtils.hasText(parentId) && !ROOT_PARENT_ID.equals(parentId)) {
                    current = parentId;
                } else {
                    break;
                }
            }
        }
        return all.stream().filter(t -> keep.contains(t.getId())).collect(Collectors.toList());
    }

    /** 批量填充分类创建人所属教研室名称 */
    private void fillOrgNames(List<ExamSubjectType> types) {
        if (types == null || types.isEmpty()) {
            return;
        }
        Set<String> cusers = types.stream()
                .map(ExamSubjectType::getCuser)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        if (cusers.isEmpty()) {
            return;
        }
        Map<String, String> orgByUser = userorgMapper.selectList(
                        new LambdaQueryWrapper<SysUserorg>()
                                .in(SysUserorg::getUserid, cusers))
                .stream()
                .collect(Collectors.toMap(SysUserorg::getUserid, SysUserorg::getOrganizationid, (a, b) -> a));
        if (orgByUser.isEmpty()) {
            return;
        }
        Map<String, String> nameByOrg = organizationMapper.selectList(
                        new LambdaQueryWrapper<SysOrganization>()
                                .in(SysOrganization::getId, new HashSet<>(orgByUser.values())))
                .stream()
                .collect(Collectors.toMap(SysOrganization::getId, SysOrganization::getName, (a, b) -> a));
        types.forEach(t -> {
            String orgId = t.getCuser() != null ? orgByUser.get(t.getCuser()) : null;
            t.setOrgName(orgId != null ? nameByOrg.get(orgId) : null);
        });
    }

    private List<ExamSubjectType> buildTree(List<ExamSubjectType> all, String parentId) {
        return all.stream()
                .filter(t -> parentId.equals(t.getParentid() == null ? "NONE" : t.getParentid()))
                .map(t -> {
                    List<ExamSubjectType> children = buildTree(all, t.getId());
                    if (!children.isEmpty()) {
                        t.setChildren(children);
                    }
                    return t;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ExamSubjectType create(ExamSubjectType type, String operatorId) {
        String now = LocalDateTime.now().format(FMT);
        type.setId(UUID.randomUUID().toString().replace("-", ""));
        type.setState("1");
        type.setCtime(now);
        type.setUtime(now);
        type.setCuser(operatorId);
        type.setMuser(operatorId);
        if (type.getReadpop() == null || type.getReadpop().isBlank()) type.setReadpop("1");
        if (type.getWritepop() == null || type.getWritepop().isBlank()) type.setWritepop("1");
        if (type.getSort() == null) type.setSort(1);
        if (type.getParentid() == null || type.getParentid().isEmpty()) {
            type.setParentid("NONE");
        }
        // Generate treecode: parent's treecode + own id
        String parentTreecode = "";
        if (!"NONE".equals(type.getParentid())) {
            ExamSubjectType parent = typeMapper.selectById(type.getParentid());
            if (parent != null && parent.getTreecode() != null) {
                parentTreecode = parent.getTreecode();
            }
        }
        type.setTreecode(parentTreecode.isEmpty() ? type.getId() : parentTreecode + "." + type.getId());
        typeMapper.insert(type);
        return type;
    }

    @Override
    @Transactional
    public ExamSubjectType update(String id, ExamSubjectType type, String operatorId) {
        ExamSubjectType existing = typeMapper.selectById(id);
        if (existing == null) throw BizException.notFound("题目分类");
        String now = LocalDateTime.now().format(FMT);
        if (type.getName() != null) existing.setName(type.getName());
        if (type.getComments() != null) existing.setComments(type.getComments());
        if (type.getSort() != null) existing.setSort(type.getSort());
        if (type.getReadpop() != null) existing.setReadpop(type.getReadpop());
        if (type.getWritepop() != null) existing.setWritepop(type.getWritepop());
        existing.setUtime(now);
        existing.setMuser(operatorId);
        typeMapper.updateById(existing);
        return existing;
    }

    @Override
    @Transactional
    public void delete(String id, String operatorId) {
        ExamSubjectType existing = typeMapper.selectById(id);
        if (existing == null) throw BizException.notFound("题目分类");
        String now = LocalDateTime.now().format(FMT);
        existing.setState("0");
        existing.setUtime(now);
        existing.setMuser(operatorId);
        typeMapper.updateById(existing);
    }

    @Override
    @Transactional
    public void deleteBatch(List<String> ids, String operatorId) {
        if (ids == null || ids.isEmpty()) {
            throw BizException.fail("请选择要删除的题目分类");
        }
        for (String id : ids) {
            delete(id, operatorId);
        }
    }
}
