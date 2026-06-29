package com.wts.exam.service.impl;

import com.wts.common.exception.BizException;
import com.wts.common.result.PageResult;
import com.wts.exam.dto.RoomDTO;
import com.wts.exam.entity.ExamCard;
import com.wts.exam.entity.ExamRoom;
import com.wts.exam.entity.ExamRoomUser;
import com.wts.exam.mapper.ExamCardMapper;
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

    private RoomServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RoomServiceImpl(
                roomMapper,
                roomPaperMapper,
                roomUserMapper,
                cardMapper,
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
    void createAllowsManualRoomState() {
        RoomDTO dto = roomDto("21");

        ExamRoom room = service.create(dto, "teacher-1", "Teacher One");

        assertEquals("21", room.getPstate());
        assertEquals("room-name", room.getName());
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
    void updateAllowsManualRoomStateChange() {
        ExamRoom room = room("room-1", "21", "1");
        when(roomMapper.selectById("room-1")).thenReturn(room);
        RoomDTO dto = roomDto("31");

        ExamRoom updated = service.update("room-1", dto, "teacher-1");

        assertEquals(room, updated);
        assertEquals("31", room.getPstate());
        verify(roomMapper).updateById(room);
    }

    @Test
    void updateKeepsCurrentRoomStateWhenDtoOmitsState() {
        ExamRoom room = room("room-1", "21", "1");
        when(roomMapper.selectById("room-1")).thenReturn(room);
        RoomDTO dto = roomDto(null);

        service.update("room-1", dto, "teacher-1");

        assertEquals("21", room.getPstate());
        verify(roomMapper).updateById(room);
    }

    @Test
    void createRejectsInvalidRoomState() {
        RoomDTO dto = roomDto("99");

        BizException error = assertThrows(BizException.class,
                () -> service.create(dto, "teacher-1", "Teacher One"));

        assertEquals("答题室状态无效", error.getMessage());
        verify(roomMapper, never()).insert(any(ExamRoom.class));
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
