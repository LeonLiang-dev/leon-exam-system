package com.wts.exam.controller;

import com.wts.auth.enums.Permission;
import com.wts.auth.service.PermissionService;
import com.wts.common.exception.BizException;
import com.wts.common.result.R;
import com.wts.common.security.CurrentUser;
import com.wts.common.security.CurrentUserProvider;
import com.wts.exam.dto.BatchIdsDTO;
import com.wts.exam.entity.ExamSubjectType;
import com.wts.exam.mapper.ExamSubjectTypeMapper;
import com.wts.exam.service.SubjectTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/subject-types")
@RequiredArgsConstructor
public class SubjectTypeController {
    private final SubjectTypeService service;
    private final CurrentUserProvider currentUserProvider;
    private final PermissionService permissionService;
    private final ExamSubjectTypeMapper typeMapper;

    @GetMapping("/tree")
    public R<?> tree() {
        CurrentUser user = currentUserProvider.require();
        // 题目全院共用，分类树同步全院可见；创建/编辑/删除仍按创建者范围校验
        return R.ok(service.getTree(null));
    }

    @PostMapping
    public R<?> create(@RequestBody ExamSubjectType entity) {
        CurrentUser user = currentUserProvider.require();
        permissionService.require(user, Permission.SUBJECT_MANAGE.name());
        service.create(entity, user.id());
        return R.ok();
    }

    @PutMapping("/{id}")
    public R<?> update(@PathVariable String id, @RequestBody ExamSubjectType entity) {
        CurrentUser user = currentUserProvider.require();
        permissionService.require(user, Permission.SUBJECT_MANAGE.name());
        requireOwnsType(user, id);
        service.update(id, entity, user.id());
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable String id) {
        CurrentUser user = currentUserProvider.require();
        permissionService.require(user, Permission.SUBJECT_MANAGE.name());
        requireOwnsType(user, id);
        service.delete(id, user.id());
        return R.ok();
    }

    @PostMapping("/batch-delete")
    public R<?> batchDelete(@RequestBody BatchIdsDTO dto) {
        CurrentUser user = currentUserProvider.require();
        permissionService.require(user, Permission.SUBJECT_MANAGE.name());
        for (String id : dto.normalizedIds()) {
            requireOwnsType(user, id);
        }
        service.deleteBatch(dto.normalizedIds(), user.id());
        return R.ok();
    }

    private void requireOwnsType(CurrentUser user, String id) {
        ExamSubjectType type = typeMapper.selectById(id);
        if (type == null) {
            throw BizException.notFound("题目分类");
        }
        if (user.isPlatformAdmin()) {
            return;
        }
        List<String> scope = permissionService.visibleOwnerIds(user);
        if (scope == null) {
            return;
        }
        if (type.getCuser() == null || !scope.contains(type.getCuser())) {
            throw BizException.forbidden("只能操作自己可见范围内的题目分类");
        }
    }
}
