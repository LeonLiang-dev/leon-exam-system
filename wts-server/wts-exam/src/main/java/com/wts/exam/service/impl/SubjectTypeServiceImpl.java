package com.wts.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wts.common.exception.BizException;
import com.wts.exam.entity.ExamSubjectType;
import com.wts.exam.mapper.ExamSubjectTypeMapper;
import com.wts.exam.service.SubjectTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubjectTypeServiceImpl implements SubjectTypeService {

    private final ExamSubjectTypeMapper typeMapper;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Override
    public List<ExamSubjectType> getTree() {
        List<ExamSubjectType> all = typeMapper.selectList(
                new LambdaQueryWrapper<ExamSubjectType>()
                        .eq(ExamSubjectType::getState, "1")
                        .orderByAsc(ExamSubjectType::getSort)
        );
        return buildTree(all, "NONE");
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
