package com.wts.auth.controller;

import com.wts.auth.dto.BatchIdsDTO;
import com.wts.auth.dto.UserDTO;
import com.wts.auth.entity.SysUser;
import com.wts.auth.mapper.SysOrganizationMapper;
import com.wts.auth.mapper.SysUserMapper;
import com.wts.auth.mapper.SysUserorgMapper;
import com.wts.auth.service.PermissionService;
import com.wts.auth.service.UserService;
import com.wts.common.exception.BizException;
import com.wts.common.security.CurrentUserProvider;
import com.wts.common.security.LoginUserDetails;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 学生全院共用：任何教职工都能管理任意学生（更新/禁用/删除/重置密码），
 * 而普通教师仍不能管理教职工账号。
 */
@ExtendWith(MockitoExtension.class)
class UserControllerStudentSharedAccessTest {

    @Mock
    private UserService userService;
    @Mock
    private SysUserMapper userMapper;
    @Mock
    private SysUserorgMapper userorgMapper;
    @Mock
    private SysOrganizationMapper organizationMapper;

    private UserController controller;

    @BeforeEach
    void setUp() {
        PermissionService permissionService =
                new PermissionService(userMapper, userorgMapper, organizationMapper);
        controller = new UserController(userService, permissionService, new CurrentUserProvider());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void teacherCanDisableAnyStudentAcrossCollege() {
        authenticate("teacher-1", "teacher", Set.of("CLASS_IMPORT"));
        BatchIdsDTO dto = new BatchIdsDTO();
        dto.setIds(List.of("student-a", "student-b"));
        when(userService.listByIds(any())).thenReturn(List.of(student("student-a"), student("student-b")));

        controller.batchDisable(dto);

        verify(userService).disableUsers(List.of("student-a", "student-b"), "teacher-1");
    }

    @Test
    void teacherCanResetAnyStudentPassword() {
        authenticate("teacher-1", "teacher", Set.of("CLASS_IMPORT"));
        when(userService.listByIds(any())).thenReturn(List.of(student("student-a")));

        controller.resetPassword("student-a");

        verify(userService).resetPassword("student-a", "teacher-1");
    }

    @Test
    void teacherCanHardDeleteAnyStudent() {
        authenticate("teacher-1", "teacher", Set.of("CLASS_IMPORT"));
        when(userService.listByIds(any())).thenReturn(List.of(student("student-a")));

        controller.hardDelete("student-a");

        verify(userService).hardDeleteUser("student-a", "teacher-1");
    }

    @Test
    void teacherCanUpdateStudent() {
        authenticate("teacher-1", "teacher", Set.of("CLASS_IMPORT"));
        UserDTO dto = new UserDTO();
        dto.setName("新名字");
        when(userService.listByIds(any())).thenReturn(List.of(student("student-a")));
        when(userService.updateUser(eq("student-a"), any(), eq("teacher-1"))).thenReturn(student("student-a"));

        controller.update("student-a", dto);

        verify(userService).updateUser(eq("student-a"), any(), eq("teacher-1"));
    }

    @Test
    void teacherCanCreateStudentOnly() {
        authenticate("teacher-1", "teacher", Set.of("CLASS_IMPORT"));
        UserDTO dto = new UserDTO();
        dto.setPost("student");
        when(userService.createUser(any(), eq("teacher-1"))).thenReturn(student("student-a"));

        controller.create(dto);

        verify(userService).createUser(dto, "teacher-1");
    }

    @Test
    void teacherCannotCreateStaffAccount() {
        authenticate("teacher-1", "teacher", Set.of("CLASS_IMPORT"));
        UserDTO dto = new UserDTO();
        dto.setPost("teacher");

        BizException error = assertThrows(BizException.class, () -> controller.create(dto));

        assertEquals(403, error.getCode());
        verify(userService, never()).createUser(any(), any());
    }

    @Test
    void teacherCannotChangeStudentPostToStaff() {
        authenticate("teacher-1", "teacher", Set.of("CLASS_IMPORT"));
        UserDTO dto = new UserDTO();
        dto.setPost("teacher");
        when(userService.listByIds(any())).thenReturn(List.of(student("student-a")));

        BizException error = assertThrows(BizException.class, () -> controller.update("student-a", dto));

        assertEquals(403, error.getCode());
        verify(userService, never()).updateUser(any(), any(), any());
    }

    @Test
    void teacherCannotManageStaffAccount() {
        authenticate("teacher-1", "teacher", Set.of("CLASS_IMPORT"));
        SysUser staff = new SysUser();
        staff.setId("staff-1");
        staff.setPost("teacher");
        when(userService.listByIds(any())).thenReturn(List.of(staff));

        BizException error = assertThrows(BizException.class, () -> controller.resetPassword("staff-1"));

        assertEquals(403, error.getCode());
        verify(userService, never()).resetPassword(any(), any());
    }

    @Test
    void studentCannotManageStudents() {
        authenticate("student-1", "student", Set.of());
        when(userService.listByIds(any())).thenReturn(List.of(student("student-a")));

        BizException error = assertThrows(BizException.class, () -> controller.resetPassword("student-a"));

        assertEquals(403, error.getCode());
        verify(userService, never()).resetPassword(any(), any());
    }

    private static SysUser student(String id) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setPost("student");
        return user;
    }

    private void authenticate(String userId, String post, Set<String> permissions) {
        LoginUserDetails details = new LoginUserDetails();
        details.setUserId(userId);
        details.setLoginName(userId);
        details.setPost(post);
        details.setPermissions(permissions);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, List.of())
        );
    }
}