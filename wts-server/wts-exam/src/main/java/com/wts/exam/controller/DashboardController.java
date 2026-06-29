package com.wts.exam.controller;

import com.wts.common.result.R;
import com.wts.common.security.CurrentUser;
import com.wts.common.security.CurrentUserProvider;
import com.wts.exam.entity.ExamPaper;
import com.wts.exam.entity.ExamRoom;
import com.wts.exam.entity.ExamSubject;
import com.wts.auth.entity.SysUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final com.wts.exam.mapper.ExamSubjectMapper subjectMapper;
    private final com.wts.exam.mapper.ExamPaperMapper paperMapper;
    private final com.wts.exam.mapper.ExamRoomMapper roomMapper;
    private final com.wts.exam.mapper.ExamCardMapper cardMapper;
    private final com.wts.auth.mapper.SysUserMapper userMapper;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/stats")
    public R<?> stats() {
        CurrentUser user = currentUserProvider.require();
        if (!user.isAdmin()) {
            throw com.wts.common.exception.BizException.fail("无权限访问");
        }
        long subjectCount = subjectMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ExamSubject>()
                        .eq(ExamSubject::getPstate, "1"));
        long paperCount = paperMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ExamPaper>()
                        .eq(ExamPaper::getPstate, "1"));
        long roomCount = roomMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ExamRoom>()
                        .eq(ExamRoom::getPstate, "21"));
        long cardCount = cardMapper.countFinishedCardsWithExistingRoom();
        long userCount = userMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getState, "1"));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("subjectCount", subjectCount);
        data.put("paperCount", paperCount);
        data.put("roomCount", roomCount);
        data.put("cardCount", cardCount);
        data.put("userCount", userCount);
        return R.ok(data);
    }
}
