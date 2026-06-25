package com.wts.exam.controller;

import com.wts.common.result.R;
import com.wts.common.security.CurrentUser;
import com.wts.common.security.CurrentUserProvider;
import com.wts.exam.dto.BatchIdsDTO;
import com.wts.exam.entity.ExamSubjectType;
import com.wts.exam.service.SubjectTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/subject-types")
@RequiredArgsConstructor
public class SubjectTypeController {
    private final SubjectTypeService service;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/tree")
    public R<?> tree() {
        return R.ok(service.getTree());
    }

    @PostMapping
    public R<?> create(@RequestBody ExamSubjectType entity) {
        CurrentUser user = currentUserProvider.require();
        requireAdmin(user);
        service.create(entity, user.id());
        return R.ok();
    }

    @PutMapping("/{id}")
    public R<?> update(@PathVariable String id, @RequestBody ExamSubjectType entity) {
        CurrentUser user = currentUserProvider.require();
        requireAdmin(user);
        service.update(id, entity, user.id());
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable String id) {
        CurrentUser user = currentUserProvider.require();
        requireAdmin(user);
        service.delete(id, user.id());
        return R.ok();
    }

    @PostMapping("/batch-delete")
    public R<?> batchDelete(@RequestBody BatchIdsDTO dto) {
        CurrentUser user = currentUserProvider.require();
        requireAdmin(user);
        service.deleteBatch(dto.normalizedIds(), user.id());
        return R.ok();
    }

    private void requireAdmin(CurrentUser user) {
        if (!user.isAdmin()) {
            throw com.wts.common.exception.BizException.fail("无权限操作");
        }
    }
}
