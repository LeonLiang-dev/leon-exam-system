package com.wts.exam.service.impl;

import com.wts.common.exception.BizException;
import com.wts.common.result.PageResult;
import com.wts.exam.dto.RoomDTO;
import com.wts.exam.entity.ExamCard;
import com.wts.exam.entity.ExamRoom;
import com.wts.exam.entity.ExamRoomPaper;
import com.wts.exam.entity.ExamRoomUser;
import com.wts.exam.mapper.ExamCardMapper;
import com.wts.exam.mapper.ExamCardAnswerMapper;
import com.wts.exam.mapper.ExamCardPointMapper;
import com.wts.exam.mapper.ExamRoomMapper;
import com.wts.exam.mapper.ExamRoomPaperMapper;
import com.wts.exam.mapper.ExamRoomUserMapper;
import com.wts.exam.util.ExamTimeUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomServiceImplParticipationTest {

    @Mock
    private ExamRoomMapper roomMapper;
    @Mock
    private ExamRoomPaperMapper roomPaperMapper;
    @Mock
    private ExamRoomUserMapper roomUserMapper;
    @Mock
    private ExamCardMapper cardMapper;
    @Mock
    private ExamCardAnswerMapper cardAnswerMapper;
    @Mock
    private ExamCardPointMapper cardPointMapper;

    private RoomServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RoomServiceImpl(
                roomMapper,
                roomPaperMapper,
                roomUserMapper,
                cardMapper,
                cardAnswerMapper,
                cardPointMapper,
                new RoomParticipationPolicy(roomUserMapper)
        );
    }

    @Test
    void listMyRoomsReturnsOnlyVisibleRoomsForStudent() {
        when(roomUserMapper.selectList(any())).thenReturn(List.of(roomUser("assigned-room", "user-1")));
        when(cardMapper.selectList(any())).thenReturn(List.of(
                card("finished-card-1", "finished-room", "user-1", "16"),
                card("finished-card-2", "finished-closed-room", "user-1", "16")
        ));
        when(roomMapper.selectList(any())).thenReturn(List.of(
                room("public-room", "21", "1"),
                room("assigned-room", "21", "2"),
                room("unassigned-room", "21", "2"),
                room("finished-room", "21", "1"),
                room("finished-closed-room", "31", "2"),
                room("closed-without-result-room", "31", "1")
        ));

        PageResult<ExamRoom> page = service.listMyRooms(1, 10, "user-1", null, "21,31");

        assertEquals(4, page.getTotal());
        assertEquals(List.of("public-room", "assigned-room", "finished-room", "finished-closed-room"),
                page.getRecords().stream().map(ExamRoom::getId).toList());
        assertEquals("finished-card-1", page.getRecords().get(2).getMyCardId());
        assertEquals("finished-card-2", page.getRecords().get(3).getMyCardId());
    }

    @Test
    void listMyRoomsPaginatesAfterVisibilityFiltering() {
        when(roomUserMapper.selectList(any())).thenReturn(List.of(roomUser("assigned-room", "user-1")));
        when(cardMapper.selectList(any())).thenReturn(List.of(card("finished-card", "finished-closed-room", "user-1", "16")));
        when(roomMapper.selectList(any())).thenReturn(List.of(
                room("public-room", "21", "1"),
                room("assigned-room", "21", "2"),
                room("another-public-room", "21", "1"),
                room("finished-closed-room", "31", "2")
        ));

        PageResult<ExamRoom> page = service.listMyRooms(2, 2, "user-1", null, "21,31");

        assertEquals(4, page.getTotal());
        assertEquals(2, page.getCurrent());
        assertEquals(2, page.getSize());
        assertEquals(List.of("another-public-room", "finished-closed-room"),
                page.getRecords().stream().map(ExamRoom::getId).toList());
    }

    @Test
    void assignUsersReplacesExistingAssignmentsAndDeduplicatesUserIds() {
        when(roomMapper.selectById("room-1")).thenReturn(room("room-1", "11", "2"));

        service.assignUsers("room-1", List.of("user-1", "", "user-1", "user-2"));

        verify(roomUserMapper).delete(any());
        ArgumentCaptor<ExamRoomUser> userCaptor = ArgumentCaptor.forClass(ExamRoomUser.class);
        verify(roomUserMapper, times(2)).insert(userCaptor.capture());
        assertEquals(List.of("user-1", "user-2"),
                userCaptor.getAllValues().stream().map(ExamRoomUser::getUserid).toList());
    }

    @Test
    void getAssignedUsersReturnsRoomAssignments() {
        ExamRoomUser userOne = roomUser("room-1", "user-1");
        ExamRoomUser userTwo = roomUser("room-1", "user-2");
        when(roomMapper.selectById("room-1")).thenReturn(room("room-1", "11", "2"));
        when(roomUserMapper.selectList(any())).thenReturn(List.of(userOne, userTwo));

        List<ExamRoomUser> users = service.getAssignedUsers("room-1");

        assertEquals(List.of(userOne, userTwo), users);
    }

    @Test
    void getAssignedUsersRejectsMissingRoom() {
        when(roomMapper.selectById("missing-room")).thenReturn(null);

        BizException error = assertThrows(BizException.class,
                () -> service.getAssignedUsers("missing-room"));

        assertEquals(404, error.getCode());
    }

    @Test
    void deleteRemovesRoomCardsAndCardChildren() {
        when(roomMapper.selectById("room-1")).thenReturn(room("room-1", "31", "1"));
        when(cardMapper.selectList(any())).thenReturn(List.of(
                card("card-1", "room-1", "user-1", "16"),
                card("card-2", "room-1", "user-2", "21")
        ));

        service.delete("room-1", "teacher-1");

        verify(cardAnswerMapper).delete(any());
        verify(cardPointMapper).delete(any());
        verify(cardMapper).delete(any());
        verify(roomPaperMapper).delete(any());
        verify(roomUserMapper).delete(any());
        verify(roomMapper).deleteById("room-1");
    }

    @Test
    void deleteRejectsPublishedRoom() {
        when(roomMapper.selectById("room-1")).thenReturn(room("room-1", "21", "1"));

        BizException error = assertThrows(BizException.class,
                () -> service.delete("room-1", "teacher-1"));

        assertEquals("已发布答题室请先关闭后再删除", error.getMessage());
        verify(cardMapper, never()).selectList(any());
        verify(roomMapper, never()).deleteById("room-1");
    }

    @Test
    void publishRejectsRoomWithoutPaper() {
        ExamRoom room = room("room-1", "11", "1");
        room.setStarttime(ExamTimeUtils.format(LocalDateTime.now().minusMinutes(5)));
        room.setEndtime(ExamTimeUtils.format(LocalDateTime.now().plusHours(1)));
        when(roomMapper.selectById("room-1")).thenReturn(room);
        when(roomPaperMapper.selectCount(any())).thenReturn(0L);

        BizException error = assertThrows(BizException.class,
                () -> service.publish("room-1", "teacher-1"));

        assertEquals("请先添加试卷再发布", error.getMessage());
        verify(roomMapper, never()).updateById(any(ExamRoom.class));
    }

    @Test
    void publishRejectsNonDraftRoom() {
        when(roomMapper.selectById("room-1")).thenReturn(room("room-1", "21", "1"));

        BizException error = assertThrows(BizException.class,
                () -> service.publish("room-1", "teacher-1"));

        assertEquals("只有草稿状态的答题室允许发布", error.getMessage());
        verify(roomPaperMapper, never()).selectCount(any());
        verify(roomMapper, never()).updateById(any(ExamRoom.class));
    }

    @Test
    void publishRejectsMissingStartTime() {
        ExamRoom room = room("room-1", "11", "1");
        room.setEndtime(ExamTimeUtils.format(LocalDateTime.now().plusHours(1)));
        when(roomMapper.selectById("room-1")).thenReturn(room);
        when(roomPaperMapper.selectCount(any())).thenReturn(1L);

        BizException error = assertThrows(BizException.class,
                () -> service.publish("room-1", "teacher-1"));

        assertEquals("请设置答题室开始时间", error.getMessage());
        verify(roomMapper, never()).updateById(any(ExamRoom.class));
    }

    @Test
    void publishRejectsExpiredEndTime() {
        ExamRoom room = room("room-1", "11", "1");
        room.setStarttime(ExamTimeUtils.format(LocalDateTime.now().minusHours(2)));
        room.setEndtime(ExamTimeUtils.format(LocalDateTime.now().minusHours(1)));
        when(roomMapper.selectById("room-1")).thenReturn(room);
        when(roomPaperMapper.selectCount(any())).thenReturn(1L);

        BizException error = assertThrows(BizException.class,
                () -> service.publish("room-1", "teacher-1"));

        assertEquals("结束时间已过，无法发布", error.getMessage());
        verify(roomMapper, never()).updateById(any(ExamRoom.class));
    }

    @Test
    void publishAcceptsValidTimeWindow() {
        ExamRoom room = room("room-1", "11", "1");
        room.setStarttime(ExamTimeUtils.format(LocalDateTime.now().minusMinutes(5)));
        room.setEndtime(ExamTimeUtils.format(LocalDateTime.now().plusHours(1)));
        when(roomMapper.selectById("room-1")).thenReturn(room);
        when(roomPaperMapper.selectCount(any())).thenReturn(1L);

        service.publish("room-1", "teacher-1");

        assertEquals("21", room.getPstate());
        verify(roomMapper).updateById(room);
    }

    @Test
    void closeRejectsDraftRoom() {
        ExamRoom room = room("room-1", "11", "1");
        when(roomMapper.selectById("room-1")).thenReturn(room);

        BizException error = assertThrows(BizException.class,
                () -> service.close("room-1", "teacher-1"));

        assertEquals("只有已发布的答题室允许关闭", error.getMessage());
        verify(roomMapper, never()).updateById(any(ExamRoom.class));
    }

    @Test
    void closeAcceptsPublishedRoom() {
        ExamRoom room = room("room-1", "21", "1");
        when(roomMapper.selectById("room-1")).thenReturn(room);

        service.close("room-1", "teacher-1");

        assertEquals("31", room.getPstate());
        verify(roomMapper).updateById(room);
    }

    @Test
    void createForcesRoomStateToDraft() {
        RoomDTO dto = roomDto("21");

        ExamRoom room = service.create(dto, "teacher-1", "Teacher One");

        assertEquals("11", room.getPstate());
        assertEquals("room-name", room.getName());
        assertEquals("teacher-1", room.getCuser());
        assertEquals("Teacher One", room.getCusername());
        assertEquals("teacher-1", room.getEuser());
        assertEquals("Teacher One", room.getEusername());
        assertEquals("", room.getDuser());
        assertEquals("", room.getDusername());
        assertEquals("", room.getDtime());
        assertEquals("", room.getPcontent());
        assertEquals("", room.getImgid());
        assertEquals("1", room.getType());
        assertEquals("0", room.getStatistical());
        assertEquals("0", room.getTypemodel());
        assertEquals("0", room.getPapervmodel());
        verify(roomMapper).insert(room);
    }

    @Test
    void createDefaultsRoomStateToDraft() {
        RoomDTO dto = roomDto(null);

        ExamRoom room = service.create(dto, "teacher-1", "Teacher One");

        assertEquals("11", room.getPstate());
        verify(roomMapper).insert(room);
    }

    @Test
    void updateRejectsPublishedRoom() {
        ExamRoom room = room("room-1", "21", "1");
        when(roomMapper.selectById("room-1")).thenReturn(room);
        RoomDTO dto = roomDto("11");

        BizException error = assertThrows(BizException.class,
                () -> service.update("room-1", dto, "teacher-1"));

        assertEquals("只有草稿状态的答题室允许修改", error.getMessage());
        verify(roomMapper, never()).updateById(any(ExamRoom.class));
    }

    @Test
    void updateForcesRoomStateToDraft() {
        ExamRoom room = room("room-1", "11", "1");
        when(roomMapper.selectById("room-1")).thenReturn(room);
        RoomDTO dto = roomDto("21");

        service.update("room-1", dto, "teacher-1");

        assertEquals("11", room.getPstate());
        verify(roomMapper).updateById(room);
    }

    @Test
    void createIgnoresInvalidRoomStateAndForcesDraft() {
        RoomDTO dto = roomDto("99");

        ExamRoom room = service.create(dto, "teacher-1", "Teacher One");

        assertEquals("11", room.getPstate());
        verify(roomMapper).insert(room);
    }

    @Test
    void addPaperRejectsPublishedRoom() {
        when(roomMapper.selectById("room-1")).thenReturn(room("room-1", "21", "1"));

        BizException error = assertThrows(BizException.class,
                () -> service.addPaper("room-1", "paper-1", "paper", 60F));

        assertEquals("只有草稿状态的答题室允许绑定试卷", error.getMessage());
        verify(roomPaperMapper, never()).insert(any(ExamRoomPaper.class));
    }

    @Test
    void assignUsersRejectsPublishedRoom() {
        when(roomMapper.selectById("room-1")).thenReturn(room("room-1", "21", "2"));

        BizException error = assertThrows(BizException.class,
                () -> service.assignUsers("room-1", List.of("user-1")));

        assertEquals("只有草稿状态的答题室允许分配人员", error.getMessage());
        verify(roomUserMapper, never()).delete(any());
    }

    private static ExamRoom room(String id, String pstate, String publictype) {
        ExamRoom room = new ExamRoom();
        room.setId(id);
        room.setPstate(pstate);
        room.setPublictype(publictype);
        return room;
    }

    private static ExamRoomUser roomUser(String roomId, String userId) {
        ExamRoomUser roomUser = new ExamRoomUser();
        roomUser.setId(roomId + "-" + userId);
        roomUser.setRoomid(roomId);
        roomUser.setUserid(userId);
        return roomUser;
    }

    private static ExamCard card(String id, String roomId, String userId, String pstate) {
        ExamCard card = new ExamCard();
        card.setId(id);
        card.setRoomid(roomId);
        card.setUserid(userId);
        card.setPstate(pstate);
        return card;
    }

    private static RoomDTO roomDto(String pstate) {
        RoomDTO dto = new RoomDTO();
        dto.setName("room-name");
        dto.setPstate(pstate);
        dto.setPublictype("1");
        dto.setStarttime(ExamTimeUtils.format(LocalDateTime.now().minusMinutes(5)));
        dto.setEndtime(ExamTimeUtils.format(LocalDateTime.now().plusHours(1)));
        dto.setTimelen(60);
        return dto;
    }
}
