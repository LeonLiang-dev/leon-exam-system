package com.wts.exam.controller;

import com.wts.common.result.R;
import com.wts.common.security.CurrentUser;
import com.wts.common.security.CurrentUserProvider;
import com.wts.exam.dto.BatchIdsDTO;
import com.wts.exam.dto.RandomItemDTO;
import com.wts.exam.service.RandomService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RandomController {
    private final RandomService service;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/random-items")
    public R<?> listItems() {
        return R.ok(service.listItems());
    }

    @PostMapping("/random-items")
    public R<?> createItem(@RequestBody RandomItemDTO dto) {
        CurrentUser user = currentUserProvider.require();
        requireAdmin(user);
        return R.ok(service.createItem(dto, user.id()));
    }

    @PutMapping("/random-items/{id}")
    public R<?> updateItem(@PathVariable String id, @RequestBody RandomItemDTO dto) {
        CurrentUser user = currentUserProvider.require();
        requireAdmin(user);
        return R.ok(service.updateItem(id, dto));
    }

    @DeleteMapping("/random-items/{id}")
    public R<?> deleteItem(@PathVariable String id) {
        CurrentUser user = currentUserProvider.require();
        requireAdmin(user);
        service.deleteItem(id);
        return R.ok();
    }

    @PostMapping("/random-items/batch-delete")
    public R<?> batchDeleteItems(@RequestBody BatchIdsDTO dto) {
        CurrentUser user = currentUserProvider.require();
        requireAdmin(user);
        service.deleteItemsBatch(dto.normalizedIds());
        return R.ok();
    }

    @GetMapping("/random-items/{itemId}/steps")
    public R<?> getSteps(@PathVariable String itemId) {
        return R.ok(service.getSteps(itemId));
    }

    @PostMapping("/random-items/{itemId}/steps")
    public R<?> addStep(@PathVariable String itemId, @RequestBody RandomItemDTO.RandomStepDTO dto) {
        CurrentUser user = currentUserProvider.require();
        requireAdmin(user);
        return R.ok(service.addStep(itemId, dto));
    }

    @PutMapping("/random-steps/{id}")
    public R<?> updateStep(@PathVariable String id, @RequestBody RandomItemDTO.RandomStepDTO dto) {
        CurrentUser user = currentUserProvider.require();
        requireAdmin(user);
        return R.ok(service.updateStep(id, dto));
    }

    @DeleteMapping("/random-steps/{id}")
    public R<?> deleteStep(@PathVariable String id) {
        CurrentUser user = currentUserProvider.require();
        requireAdmin(user);
        service.deleteStep(id);
        return R.ok();
    }

    @PostMapping("/random-steps/batch-delete")
    public R<?> batchDeleteSteps(@RequestBody BatchIdsDTO dto) {
        CurrentUser user = currentUserProvider.require();
        requireAdmin(user);
        service.deleteStepsBatch(dto.normalizedIds());
        return R.ok();
    }

    @PostMapping("/random-items/{itemId}/generate")
    public R<?> generate(@PathVariable String itemId,
                         @RequestParam(defaultValue = "1") int count) {
        CurrentUser user = currentUserProvider.require();
        requireAdmin(user);
        return R.ok(service.generatePapers(itemId, count, user.id()));
    }

    private void requireAdmin(CurrentUser user) {
        if (!user.isAdmin()) {
            throw com.wts.common.exception.BizException.fail("无权限操作");
        }
    }
}
