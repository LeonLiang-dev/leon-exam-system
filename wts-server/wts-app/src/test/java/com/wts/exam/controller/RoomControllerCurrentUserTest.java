package com.wts.exam.controller;

import com.wts.auth.mapper.SysOrganizationMapper;
import com.wts.auth.mapper.SysUserMapper;
import com.wts.auth.mapper.SysUserorgMapper;
import com.wts.auth.service.PermissionService;
import com.wts.common.security.CurrentUserProvider;
import com.wts.common.security.LoginUserDetails;
import com.wts.exam.entity.ExamRoom;
import com.wts.exam.service.RoomService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomControllerCurrentUserTest {

    @Mock
    private RoomService service;
    @Mock
    private SysUserMapper userMapper;
    @Mock
    private SysUserorgMapper userorgMapper;
    @Mock
    private SysOrganizationMapper organizationMapper;

    private RoomController controller;

    @BeforeEach
    void setUp() {
        PermissionService permissionService =
                new PermissionService(userMapper, userorgMapper, organizationMapper);
        controller = new RoomController(service, new CurrentUserProvider(), permissionService);

        LoginUserDetails details = new LoginUserDetails();
        details.setUserId("student-1");
        details.setLoginName("student");
        details.setName("Student One");
        details.setUserType("2");
        details.setPost("student");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, List.of())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void myRoomsUsesAuthenticatedUserId() {
        controller.myRooms(1, 20, "math", "21,31");

        verify(service).listMyRooms(1, 20, "student-1", "math", "21,31");
    }

    @Test
    void studentCannotAccessRoomManagementList() {
        var error = assertThrows(com.wts.common.exception.BizException.class,
                () -> controller.list(1, 20, null, null));

        assertEquals(403, error.getCode());
        verify(service, never()).list(anyInt(), anyInt(), any(), any());
    }

    @Test
    void adminCanAccessRoomManagementList() {
        authenticate("admin-1", "admin", "Admin One", "1", "teacher", Set.of("EXAM_PUBLISH"));

        controller.list(1, 20, null, null);

        verify(service).list(1, 20, null, null, List.of("admin-1"));
    }

    @Test
    void adminCanReadAssignedRoomUsers() {
        authenticate("admin-1", "admin", "Admin One", "1", "teacher", Set.of("EXAM_PUBLISH"));
        ExamRoom room = new ExamRoom();
        room.setId("room-1");
        room.setCuser("admin-1");
        when(service.getDetail("room-1")).thenReturn(room);

        controller.getAssignedUsers("room-1");

        verify(service).getAssignedUsers("room-1");
    }

    @Test
    void studentCannotReadAssignedRoomUsers() {
        var error = assertThrows(com.wts.common.exception.BizException.class,
                () -> controller.getAssignedUsers("room-1"));

        assertEquals(403, error.getCode());
        verify(service, never()).getAssignedUsers(any());
    }

    @Test
    void teacherCannotManageOthersRoom() {
        authenticate("teacher-1", "teacher", "Teacher One", "1", "teacher", Set.of("EXAM_PUBLISH"));
        ExamRoom room = new ExamRoom();
        room.setId("room-1");
        room.setCuser("admin-1");
        when(service.getDetail("room-1")).thenReturn(room);

        var error = assertThrows(com.wts.common.exception.BizException.class,
                () -> controller.getAssignedUsers("room-1"));

        assertEquals(403, error.getCode());
        verify(service, never()).getAssignedUsers(any());
    }

    private void authenticate(String userId, String loginName, String name, String userType, String post, Set<String> permissions) {
        LoginUserDetails details = new LoginUserDetails();
        details.setUserId(userId);
        details.setLoginName(loginName);
        details.setName(name);
        details.setUserType(userType);
        details.setPost(post);
        details.setPermissions(permissions);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, List.of())
        );
    }
}