package com.wts.exam.controller;

import com.wts.common.exception.BizException;
import com.wts.common.result.R;
import com.wts.common.security.CurrentUser;
import com.wts.common.security.CurrentUserProvider;
import com.wts.exam.dto.BatchIdsDTO;
import com.wts.exam.dto.CardSubmitDTO;
import com.wts.exam.dto.JudgeDTO;
import com.wts.exam.entity.ExamCard;
import com.wts.exam.service.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/cards")
@RequiredArgsConstructor
public class CardController {
    private final CardService service;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping("/enter")
    public R<?> enterRoom(@RequestParam String roomId) {
        CurrentUser user = currentUserProvider.require();
        return R.ok(service.enterRoom(roomId, user.id(), user.displayName(), user.isAdmin()));
    }

    @GetMapping("/{id}")
    public R<?> detail(@PathVariable String id) {
        CurrentUser user = currentUserProvider.require();
        ExamCard card = service.getResult(id, user.id(), user.isAdmin());
        var answers = service.getCardAnswers(id);
        var points = service.getCardPoints(id);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("card", card);
        result.put("answers", answers);
        result.put("points", points);
        return R.ok(result);
    }

    @PostMapping("/{id}/save")
    public R<?> save(@PathVariable String id, @RequestBody CardSubmitDTO dto) {
        CurrentUser user = currentUserProvider.require();
        service.saveAnswers(id, dto, user.id());
        return R.ok();
    }

    @PostMapping("/{id}/submit")
    public R<?> submit(@PathVariable String id, @RequestBody CardSubmitDTO dto) {
        CurrentUser user = currentUserProvider.require();
        service.submit(id, dto, user.id());
        return R.ok();
    }

    @PostMapping("/{id}/judge")
    public R<?> judge(@PathVariable String id,
                      @RequestBody(required = false) JudgeDTO dto) {
        CurrentUser user = currentUserProvider.require();
        requireAdmin(user);
        service.judge(id, dto, user.id(), user.displayName());
        return R.ok();
    }

    @PostMapping("/batch-judge")
    public R<?> batchJudge(@RequestBody BatchIdsDTO dto) {
        CurrentUser user = currentUserProvider.require();
        requireAdmin(user);
        service.judgeBatch(dto.normalizedIds(), user.id(), user.displayName());
        return R.ok();
    }

    @GetMapping("/{id}/paper")
    public R<?> getExamPaper(@PathVariable String id) {
        CurrentUser user = currentUserProvider.require();
        return R.ok(service.getExamPaper(id, user.id()));
    }

    @GetMapping("/{id}/paper-review")
    public R<?> getExamPaperForReview(@PathVariable String id) {
        CurrentUser user = currentUserProvider.require();
        requireAdmin(user);
        return R.ok(service.getExamPaperForReview(id));
    }

    @GetMapping("/rooms/{roomId}/cards")
    public R<?> getRoomCards(@PathVariable String roomId,
                             @RequestParam(defaultValue = "1") int page,
                             @RequestParam(defaultValue = "20") int size) {
        CurrentUser user = currentUserProvider.require();
        requireAdmin(user);
        return R.ok(service.getRoomCards(roomId, page, size));
    }

    private void requireAdmin(CurrentUser user) {
        if (!user.isAdmin()) {
            throw BizException.forbidden("无权操作");
        }
    }
}
