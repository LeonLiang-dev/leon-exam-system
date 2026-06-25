package com.wts.exam.controller;

import com.wts.common.exception.BizException;
import com.wts.common.result.R;
import com.wts.common.security.CurrentUser;
import com.wts.common.security.CurrentUserProvider;
import com.wts.exam.dto.BatchIdsDTO;
import com.wts.exam.dto.RoomDTO;
import com.wts.exam.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
public class RoomController {
    private final RoomService service;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    public R<?> list(@RequestParam(defaultValue = "1") int page,
                     @RequestParam(defaultValue = "20") int size,
                     @RequestParam(required = false) String keyword,
                     @RequestParam(required = false) String pstate) {
        CurrentUser user = currentUserProvider.require();
        requireAdmin(user);
        return R.ok(service.list(page, size, keyword, pstate));
    }

    @GetMapping("/my")
    public R<?> myRooms(@RequestParam(defaultValue = "1") int page,
                        @RequestParam(defaultValue = "20") int size,
                        @RequestParam(required = false) String keyword,
                        @RequestParam(required = false) String pstate) {
        CurrentUser user = currentUserProvider.require();
        return R.ok(service.listMyRooms(page, size, user.id(), keyword, pstate));
    }

    @GetMapping("/{id}")
    public R<?> detail(@PathVariable String id) {
        CurrentUser user = currentUserProvider.require();
        requireAdmin(user);
        return R.ok(service.getDetail(id));
    }

    @PostMapping
    public R<?> create(@RequestBody RoomDTO dto) {
        CurrentUser user = currentUserProvider.require();
        requireAdmin(user);
        return R.ok(service.create(dto, user.id(), user.displayName()));
    }

    @PutMapping("/{id}")
    public R<?> update(@PathVariable String id, @RequestBody RoomDTO dto) {
        CurrentUser user = currentUserProvider.require();
        requireAdmin(user);
        service.update(id, dto, user.id());
        return R.ok();
    }

    @PostMapping("/{id}/publish")
    public R<?> publish(@PathVariable String id) {
        CurrentUser user = currentUserProvider.require();
        requireAdmin(user);
        service.publish(id, user.id());
        return R.ok();
    }

    @PostMapping("/batch-publish")
    public R<?> batchPublish(@RequestBody BatchIdsDTO dto) {
        CurrentUser user = currentUserProvider.require();
        requireAdmin(user);
        service.publishBatch(dto.normalizedIds(), user.id());
        return R.ok();
    }

    @PostMapping("/{id}/close")
    public R<?> close(@PathVariable String id) {
        CurrentUser user = currentUserProvider.require();
        requireAdmin(user);
        service.close(id, user.id());
        return R.ok();
    }

    @PostMapping("/batch-close")
    public R<?> batchClose(@RequestBody BatchIdsDTO dto) {
        CurrentUser user = currentUserProvider.require();
        requireAdmin(user);
        service.closeBatch(dto.normalizedIds(), user.id());
        return R.ok();
    }

    @PostMapping("/{id}/papers")
    public R<?> addPaper(@PathVariable String id,
                         @RequestParam String paperId,
                         @RequestParam(required = false) String name,
                         @RequestParam(required = false) Float passPoint) {
        CurrentUser user = currentUserProvider.require();
        requireAdmin(user);
        service.addPaper(id, paperId, name, passPoint);
        return R.ok();
    }

    @GetMapping("/{id}/papers")
    public R<?> getRoomPapers(@PathVariable String id) {
        CurrentUser user = currentUserProvider.require();
        requireAdmin(user);
        return R.ok(service.getRoomPapers(id));
    }

    @GetMapping("/{id}/users")
    public R<?> getAssignedUsers(@PathVariable String id) {
        CurrentUser user = currentUserProvider.require();
        requireAdmin(user);
        return R.ok(service.getAssignedUsers(id));
    }

    @PostMapping("/{id}/users")
    public R<?> assignUsers(@PathVariable String id, @RequestBody List<String> userIds) {
        CurrentUser user = currentUserProvider.require();
        requireAdmin(user);
        service.assignUsers(id, userIds);
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
            throw BizException.forbidden("无权操作");
        }
    }
}
