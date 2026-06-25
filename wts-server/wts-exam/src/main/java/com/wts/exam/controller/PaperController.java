package com.wts.exam.controller;

import com.wts.common.result.R;
import com.wts.common.security.CurrentUser;
import com.wts.common.security.CurrentUserProvider;
import com.wts.exam.dto.BatchIdsDTO;
import com.wts.exam.dto.PaperDTO;
import com.wts.exam.service.PaperService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/papers")
@RequiredArgsConstructor
public class PaperController {
    private final PaperService service;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    public R<?> list(@RequestParam(defaultValue = "1") int page,
                     @RequestParam(defaultValue = "20") int size,
                     @RequestParam(required = false) String keyword) {
        return R.ok(service.list(page, size, keyword));
    }

    @GetMapping("/{id}")
    public R<?> detail(@PathVariable String id) {
        return R.ok(service.getDetail(id));
    }

    @PostMapping
    public R<?> create(@RequestBody PaperDTO dto) {
        CurrentUser user = currentUserProvider.require();
        requireAdmin(user);
        return R.ok(service.create(dto, user.id(), user.displayName()));
    }

    @PutMapping("/{id}")
    public R<?> update(@PathVariable String id, @RequestBody PaperDTO dto) {
        CurrentUser user = currentUserProvider.require();
        requireAdmin(user);
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
        requireAdmin(user);
        service.addSubject(id, subjectId, versionId, chapterId, sort, point);
        return R.ok();
    }

    @GetMapping("/{id}/chapters")
    public R<?> getChapters(@PathVariable String id) {
        return R.ok(service.getChapters(id));
    }

    @GetMapping("/{id}/paper-subjects")
    public R<?> getPaperSubjects(@PathVariable String id) {
        return R.ok(service.getPaperSubjects(id));
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
