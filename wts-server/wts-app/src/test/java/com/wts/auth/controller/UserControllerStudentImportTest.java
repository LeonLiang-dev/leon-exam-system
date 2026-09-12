package com.wts.auth.controller;

import com.wts.auth.dto.BatchIdsDTO;
import com.wts.auth.dto.StudentImportResult;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.InputStream;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerStudentImportTest {

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
    void teacherCanImportStudents() {
        authenticate("teacher-1", "teacher", Set.of("CLASS_IMPORT"));
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "students.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "content".getBytes()
        );
        StudentImportResult result = new StudentImportResult();
        when(userService.importStudents(any(InputStream.class), eq("teacher-1"))).thenReturn(result);

        controller.importStudents(file);

        verify(userService).importStudents(any(InputStream.class), eq("teacher-1"));
    }

    @Test
    void studentCannotImportStudents() {
        authenticate("student-1", "student", Set.of());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "students.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "content".getBytes()
        );

        BizException error = assertThrows(BizException.class,
                () -> controller.importStudents(file));

        assertEquals(403, error.getCode());
        verify(userService, never()).importStudents(any(), any());
    }

    @Test
    void batchDisableDelegatesToService() {
        authenticate("operator-1", "platform_admin", Set.of());
        BatchIdsDTO dto = new BatchIdsDTO();
        dto.setIds(List.of("user-1", "user-2"));

        controller.batchDisable(dto);

        verify(userService).disableUsers(List.of("user-1", "user-2"), "operator-1");
    }

    @Test
    void batchHardDeleteDelegatesToService() {
        authenticate("operator-1", "platform_admin", Set.of());
        BatchIdsDTO dto = new BatchIdsDTO();
        dto.setIds(List.of("user-1", "user-2"));

        controller.batchHardDelete(dto);

        verify(userService).hardDeleteUsers(List.of("user-1", "user-2"), "operator-1");
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