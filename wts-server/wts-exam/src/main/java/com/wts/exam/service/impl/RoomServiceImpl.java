package com.wts.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wts.common.exception.BizException;
import com.wts.common.result.PageResult;
import com.wts.exam.dto.RoomDTO;
import com.wts.exam.entity.*;
import com.wts.exam.mapper.*;
import com.wts.exam.service.RoomService;
import com.wts.exam.util.ExamTimeUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {
    private final ExamRoomMapper roomMapper;
    private final ExamRoomPaperMapper roomPaperMapper;
    private final ExamRoomUserMapper roomUserMapper;
    private final ExamCardMapper cardMapper;
    private final RoomParticipationPolicy roomParticipationPolicy;
    private static final String ROOM_PUBLISHED = "21";
    private static final String CARD_SUBMITTED = "16";
    private static final String CARD_JUDGED = "21";

    @Override
    public PageResult<ExamRoom> list(int page, int size, String keyword, String pstate) {
        LambdaQueryWrapper<ExamRoom> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(ExamRoom::getName, keyword);
        }
        if (pstate != null && !pstate.isEmpty()) {
            String[] states = pstate.split(",");
            if (states.length == 1) {
                wrapper.eq(ExamRoom::getPstate, states[0]);
            } else {
                wrapper.in(ExamRoom::getPstate, Arrays.asList(states));
            }
        }
        wrapper.orderByDesc(ExamRoom::getCtime);
        return PageResult.of(roomMapper.selectPage(new Page<>(page, size), wrapper));
    }

    @Override
    public PageResult<ExamRoom> listMyRooms(int page, int size, String userId, String keyword, String pstate) {
        LambdaQueryWrapper<ExamRoom> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(ExamRoom::getName, keyword);
        }
        if (pstate != null && !pstate.isEmpty()) {
            String[] states = pstate.split(",");
            if (states.length == 1) {
                wrapper.eq(ExamRoom::getPstate, states[0]);
            } else {
                wrapper.in(ExamRoom::getPstate, Arrays.asList(states));
            }
        } else {
            wrapper.eq(ExamRoom::getPstate, ROOM_PUBLISHED);
        }
        wrapper.orderByDesc(ExamRoom::getCtime);

        Set<String> assignedRoomIds = roomParticipationPolicy.getAssignedRoomIds(userId);
        Map<String, ExamCard> finishedCardByRoomId = toFinishedCardByRoomId(cardMapper.selectList(new LambdaQueryWrapper<ExamCard>()
                        .eq(ExamCard::getUserid, userId)
                        .in(ExamCard::getPstate, CARD_SUBMITTED, CARD_JUDGED)));

        List<ExamRoom> visibleRooms = roomMapper.selectList(wrapper)
                .stream()
                .filter(room -> isVisibleToStudent(room, assignedRoomIds, finishedCardByRoomId))
                .collect(Collectors.toList());
        visibleRooms.forEach(room -> attachStudentCardInfo(room, finishedCardByRoomId.get(room.getId())));

        int current = Math.max(page, 1);
        int pageSize = Math.max(size, 1);
        int fromIndex = Math.min((current - 1) * pageSize, visibleRooms.size());
        int toIndex = Math.min(fromIndex + pageSize, visibleRooms.size());
        List<ExamRoom> pageRecords = fromIndex < toIndex
                ? visibleRooms.subList(fromIndex, toIndex)
                : Collections.emptyList();
        return PageResult.of(pageRecords, visibleRooms.size(), current, pageSize);
    }

    @Override
    public ExamRoom getDetail(String id) {
        ExamRoom room = roomMapper.selectById(id);
        if (room == null) throw BizException.notFound("答题室");
        return room;
    }

    @Override
    @Transactional
    public ExamRoom create(RoomDTO dto, String operatorId, String operatorName) {
        ExamRoom room = new ExamRoom();
        String starttime = ExamTimeUtils.normalizeNullable(dto.getStarttime(), "开始时间");
        String endtime = ExamTimeUtils.normalizeNullable(dto.getEndtime(), "结束时间");
        validateTimeOrder(starttime, endtime);

        room.setId(UUID.randomUUID().toString().replace("-", ""));
        room.setUuid(room.getId());
        room.setName(dto.getName());
        room.setRoomnote(dto.getRoomnote());
        room.setExamtypeid(dto.getExamtypeid());
        room.setTimelen(dto.getTimelen() != null ? dto.getTimelen() : 60);
        room.setTimetype(dto.getTimetype() != null ? dto.getTimetype() : "1");
        room.setWritetype(dto.getWritetype() != null ? dto.getWritetype() : "0");
        room.setCounttype(dto.getCounttype() != null ? dto.getCounttype() : "2");
        room.setResultstype(dto.getResultstype() != null ? dto.getResultstype() : "1");
        room.setPublictype(dto.getPublictype() != null ? dto.getPublictype() : "1");
        room.setAdjudgetype(dto.getAdjudgetype() != null ? dto.getAdjudgetype() : "1");
        room.setPicktype(dto.getPicktype() != null ? dto.getPicktype() : "2");
        room.setRestarttype(dto.getRestarttype() != null ? dto.getRestarttype() : "99");
        room.setSsorttype(dto.getSsorttype() != null ? dto.getSsorttype() : "1");
        room.setOsorttype(dto.getOsorttype() != null ? dto.getOsorttype() : "1");
        room.setPshowtype(dto.getPshowtype() != null ? dto.getPshowtype() : "1");
        room.setStarttime(starttime);
        room.setEndtime(endtime);
        room.setPstate("11"); // Draft state
        String now = ExamTimeUtils.nowCompact();
        room.setCtime(now);
        room.setEtime(now);
        roomMapper.insert(room);
        return room;
    }

    @Override
    @Transactional
    public ExamRoom update(String id, RoomDTO dto, String operatorId) {
        ExamRoom room = roomMapper.selectById(id);
        if (room == null) throw BizException.notFound("答题室");
        String starttime = ExamTimeUtils.normalizeNullable(dto.getStarttime(), "开始时间");
        String endtime = ExamTimeUtils.normalizeNullable(dto.getEndtime(), "结束时间");
        validateTimeOrder(starttime, endtime);

        room.setName(dto.getName());
        room.setRoomnote(dto.getRoomnote());
        room.setExamtypeid(dto.getExamtypeid());
        room.setTimelen(dto.getTimelen());
        room.setTimetype(dto.getTimetype());
        room.setWritetype(dto.getWritetype());
        room.setCounttype(dto.getCounttype());
        room.setResultstype(dto.getResultstype());
        room.setPublictype(dto.getPublictype());
        room.setAdjudgetype(dto.getAdjudgetype());
        room.setPicktype(dto.getPicktype());
        room.setRestarttype(dto.getRestarttype());
        room.setSsorttype(dto.getSsorttype());
        room.setOsorttype(dto.getOsorttype());
        room.setPshowtype(dto.getPshowtype());
        room.setStarttime(starttime);
        room.setEndtime(endtime);
        roomMapper.updateById(room);
        return room;
    }

    @Override
    @Transactional
    public void delete(String id, String operatorId) {
        roomPaperMapper.delete(new LambdaQueryWrapper<ExamRoomPaper>().eq(ExamRoomPaper::getRoomid, id));
        roomUserMapper.delete(new LambdaQueryWrapper<ExamRoomUser>().eq(ExamRoomUser::getRoomid, id));
        roomMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void deleteBatch(List<String> ids, String operatorId) {
        if (ids == null || ids.isEmpty()) {
            throw BizException.fail("请选择要删除的答题室");
        }
        for (String id : ids) {
            delete(id, operatorId);
        }
    }

    @Override
    @Transactional
    public void publish(String id, String operatorId) {
        ExamRoom room = roomMapper.selectById(id);
        if (room == null) throw BizException.notFound("答题室");
        // Check room has at least one paper
        Long paperCount = roomPaperMapper.selectCount(
                new LambdaQueryWrapper<ExamRoomPaper>().eq(ExamRoomPaper::getRoomid, id));
        if (paperCount == 0) throw BizException.fail("请先添加试卷再发布");
        validatePublishTimeWindow(room);
        room.setPstate("21"); // Published
        roomMapper.updateById(room);
    }

    @Override
    @Transactional
    public void publishBatch(List<String> ids, String operatorId) {
        if (ids == null || ids.isEmpty()) {
            throw BizException.fail("请选择要发布的答题室");
        }
        for (String id : ids) {
            publish(id, operatorId);
        }
    }

    @Override
    @Transactional
    public void close(String id, String operatorId) {
        ExamRoom room = roomMapper.selectById(id);
        if (room == null) throw BizException.notFound("答题室");
        room.setPstate("31"); // Closed
        roomMapper.updateById(room);
    }

    @Override
    @Transactional
    public void closeBatch(List<String> ids, String operatorId) {
        if (ids == null || ids.isEmpty()) {
            throw BizException.fail("请选择要关闭的答题室");
        }
        for (String id : ids) {
            close(id, operatorId);
        }
    }

    @Override
    @Transactional
    public void addPaper(String roomId, String paperId, String name, Float passPoint) {
        ExamRoomPaper rp = new ExamRoomPaper();
        rp.setId(UUID.randomUUID().toString().replace("-", ""));
        rp.setRoomid(roomId);
        rp.setPaperid(paperId);
        rp.setName(name);
        rp.setPasspoint(passPoint != null ? passPoint : 60f);
        roomPaperMapper.insert(rp);
    }

    @Override
    @Transactional
    public void removePaper(String roomId, String paperId) {
        roomPaperMapper.delete(new LambdaQueryWrapper<ExamRoomPaper>()
                .eq(ExamRoomPaper::getRoomid, roomId)
                .eq(ExamRoomPaper::getPaperid, paperId));
    }

    @Override
    public List<ExamRoomPaper> getRoomPapers(String roomId) {
        return roomPaperMapper.selectList(
                new LambdaQueryWrapper<ExamRoomPaper>().eq(ExamRoomPaper::getRoomid, roomId));
    }

    @Override
    public List<ExamRoomUser> getAssignedUsers(String roomId) {
        ExamRoom room = roomMapper.selectById(roomId);
        if (room == null) throw BizException.notFound("答题室");
        return roomUserMapper.selectList(new LambdaQueryWrapper<ExamRoomUser>()
                .eq(ExamRoomUser::getRoomid, roomId)
                .orderByAsc(ExamRoomUser::getUserid));
    }

    @Override
    @Transactional
    public void assignUsers(String roomId, List<String> userIds) {
        ExamRoom room = roomMapper.selectById(roomId);
        if (room == null) throw BizException.notFound("答题室");

        roomUserMapper.delete(new LambdaQueryWrapper<ExamRoomUser>().eq(ExamRoomUser::getRoomid, roomId));
        if (userIds == null || userIds.isEmpty()) {
            return;
        }

        for (String userId : userIds.stream()
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .collect(Collectors.toList())) {
            ExamRoomUser ru = new ExamRoomUser();
            ru.setId(UUID.randomUUID().toString().replace("-", ""));
            ru.setRoomid(roomId);
            ru.setUserid(userId);
            roomUserMapper.insert(ru);
        }
    }

    private boolean isVisibleToStudent(ExamRoom room, Set<String> assignedRoomIds, Map<String, ExamCard> finishedCardByRoomId) {
        if (room == null) {
            return false;
        }
        if (finishedCardByRoomId.containsKey(room.getId())) {
            return true;
        }
        return ROOM_PUBLISHED.equals(room.getPstate())
                && roomParticipationPolicy.canParticipate(room, assignedRoomIds);
    }

    private Map<String, ExamCard> toFinishedCardByRoomId(List<ExamCard> cards) {
        Map<String, ExamCard> result = new HashMap<>();
        for (ExamCard card : cards) {
            if (card.getRoomid() != null) {
                result.putIfAbsent(card.getRoomid(), card);
            }
        }
        return result;
    }

    private void attachStudentCardInfo(ExamRoom room, ExamCard card) {
        if (card == null) {
            return;
        }
        room.setMyCardId(card.getId());
        room.setMyCardPstate(card.getPstate());

        String unavailableReason = getResultUnavailableReason(room, card);
        room.setResultAvailable(unavailableReason == null);
        room.setResultUnavailableReason(unavailableReason);
    }

    private String getResultUnavailableReason(ExamRoom room, ExamCard card) {
        if (!CARD_SUBMITTED.equals(card.getPstate()) && !CARD_JUDGED.equals(card.getPstate())) {
            return "答卷尚未提交";
        }

        String resultstype = room.getResultstype() != null ? room.getResultstype() : "1";
        if ("3".equals(resultstype)) {
            return "该答题室不显示成绩";
        }

        LocalDateTime end = ExamTimeUtils.parseNullable(room.getEndtime());
        if (end != null && LocalDateTime.now().isBefore(end)) {
            return "考试结束后可查看成绩";
        }

        if ("2".equals(resultstype) && !CARD_JUDGED.equals(card.getPstate())) {
            return "阅卷后可查看成绩";
        }

        return null;
    }

    private void validateTimeOrder(String starttime, String endtime) {
        LocalDateTime start = ExamTimeUtils.parseNullable(starttime);
        LocalDateTime end = ExamTimeUtils.parseNullable(endtime);
        if (start != null && end != null && !start.isBefore(end)) {
            throw BizException.fail("开始时间必须早于结束时间");
        }
    }

    private void validatePublishTimeWindow(ExamRoom room) {
        String starttime = ExamTimeUtils.normalizeNullable(room.getStarttime(), "开始时间");
        String endtime = ExamTimeUtils.normalizeNullable(room.getEndtime(), "结束时间");
        LocalDateTime start = ExamTimeUtils.parseRequired(starttime, "答题室开始时间");
        LocalDateTime end = ExamTimeUtils.parseRequired(endtime, "答题室结束时间");
        if (!start.isBefore(end)) {
            throw BizException.fail("开始时间必须早于结束时间");
        }
        if (!end.isAfter(LocalDateTime.now())) {
            throw BizException.fail("结束时间已过，无法发布");
        }
        room.setStarttime(starttime);
        room.setEndtime(endtime);
    }
}
