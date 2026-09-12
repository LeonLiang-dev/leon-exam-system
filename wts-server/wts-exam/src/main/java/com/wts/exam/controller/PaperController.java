package com.wts.exam.controller;

import com.wts.auth.enums.Permission;
import com.wts.auth.service.PermissionService;
import com.wts.common.exception.BizException;
import com.wts.common.result.R;
import com.wts.common.security.CurrentUser;
import com.wts.common.security.CurrentUserProvider;
import com.wts.exam.dto.BatchIdsDTO;
import com.wts.exam.dto.PaperDTO;
import com.wts.exam.service.PaperService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/papers")
@RequiredArgsConstructor
public class PaperController {
    private final PaperService service;
    private final CurrentUserProvider currentUserProvider;
    private final PermissionService permissionService;

    @GetMapping
    public R<?> list(@RequestParam(defaultValue = "1") int page,
                     @RequestParam(defaultValue = "20") int size,
                     @RequestParam(required = false) String keyword) {
        CurrentUser user = currentUserProvider.require();
        permissionService.require(user, Permission.SUBJECT_MANAGE.name());
        return R.ok(service.list(page, size, keyword, permissionService.visibleOwnerIds(user)));
    }

    @GetMapping("/{id}")
    public R<?> detail(@PathVariable String id) {
        CurrentUser user = currentUserProvider.require();
        permissionService.require(user, Permission.SUBJECT_MANAGE.name());
        var paper = service.getDetail(id);
        requireOwns(user, paper.getCuser());
        return R.ok(paper);
    }

    @PostMapping
    public R<?> create(@RequestBody PaperDTO dto) {
        CurrentUser user = currentUserProvider.require();
        permissionService.require(user, Permission.SUBJECT_MANAGE.name());
        return R.ok(service.create(dto, user.id(), user.displayName()));
    }

    @PutMapping("/{id}")
    public R<?> update(@PathVariable String id, @RequestBody PaperDTO dto) {
        CurrentUser user = currentUserProvider.require();
        permissionService.require(user, Permission.SUBJECT_MANAGE.name());
        requireOwnsPaper(user, id);
        service.update(id, dto, user.id());
        return R.ok();
    }

    @PostMapping("/{id}/subjects")
    public R<?> addSubject(@PathVariable String id,
                           @RequestParam String subjectId,
                           @RequestParam(required = false) String versionId,
                           @RequestParam(required = false) String chapterId,
                           @RequestParam(required = false) Integer sort,
                           @RequestParam(required = false) Integer point) {
        CurrentUser user = currentUserProvider.require();
        permissionService.require(user, Permission.SUBJECT_MANAGE.name());
        requireOwnsPaper(user, id);
        service.addSubject(id, subjectId, versionId, chapterId, sort, point);
        return R.ok();
    }

    @GetMapping("/{id}/chapters")
    public R<?> getChapters(@PathVariable String id) {
        CurrentUser user = currentUserProvider.require();
        permissionService.require(user, Permission.SUBJECT_MANAGE.name());
        requireOwnsPaper(user, id);
        return R.ok(service.getChapters(id));
    }

    @GetMapping("/{id}/paper-subjects")
    public R<?> getPaperSubjects(@PathVariable String id) {
        CurrentUser user = currentUserProvider.require();
        permissionService.require(user, Permission.SUBJECT_MANAGE.name());
        requireOwnsPaper(user, id);
        return R.ok(service.getPaperSubjects(id));
    }

    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable String id) {
        CurrentUser user = currentUserProvider.require();
        permissionService.require(user, Permission.SUBJECT_MANAGE.name());
        requireOwnsPaper(user, id);
        service.delete(id, user.id());
        return R.ok();
    }

    @PostMapping("/batch-delete")
    public R<?> batchDelete(@RequestBody BatchIdsDTO dto) {
        CurrentUser user = currentUserProvider.require();
        permissionService.require(user, Permission.SUBJECT_MANAGE.name());
        List<String> ids = dto.normalizedIds();
        service.deleteBatch(ids, user.id());
        return R.ok();
    }

    private void requireOwnsPaper(CurrentUser user, String paperId) {
        var paper = service.getDetail(paperId);
        requireOwns(user, paper.getCuser());
    }

    private void requireOwns(CurrentUser user, String ownerId) {
        if (user.isPlatformAdmin()) {
            return;
        }
        List<String> scope = permissionService.visibleOwnerIds(user);
        if (scope == null) {
            return;
        }
        if (ownerId == null || !scope.contains(ownerId)) {
            throw BizException.forbidden("只能操作自己创建的试卷");
        }
    }
}