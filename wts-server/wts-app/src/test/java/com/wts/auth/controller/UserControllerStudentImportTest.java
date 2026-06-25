package com.wts.auth.controller;

import com.wts.auth.dto.BatchIdsDTO;
import com.wts.auth.dto.StudentImportResult;
import com.wts.auth.service.UserService;
import com.wts.common.exception.BizException;
import com.wts.common.security.LoginUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.util.List;

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

    private UserController controller;

    @BeforeEach
    void setUp() {
        controller = new UserController(userService);
    }

    @Test
    void teacherCanImportStudents() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "students.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "content".getBytes()
        );
        StudentImportResult result = new StudentImportResult();
        when(userService.importStudents(any(InputStream.class), eq("teacher-1"))).thenReturn(result);

        controller.importStudents(file, loginUser("teacher-1", "1"));

        verify(userService).importStudents(any(InputStream.class), eq("teacher-1"));
    }

    @Test
    void studentCannotImportStudents() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "students.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "content".getBytes()
        );

        BizException error = assertThrows(BizException.class,
                () -> controller.importStudents(file, loginUser("student-1", "2")));

        assertEquals(403, error.getCode());
        verify(userService, never()).importStudents(any(), any());
    }

    @Test
    void batchDisableDelegatesToService() {
        BatchIdsDTO dto = new BatchIdsDTO();
        dto.setIds(List.of("user-1", "user-2"));

        controller.batchDisable(dto, loginUser("operator-1", "1"));

        verify(userService).disableUsers(List.of("user-1", "user-2"), "operator-1");
    }

    @Test
    void batchHardDeleteDelegatesToService() {
        BatchIdsDTO dto = new BatchIdsDTO();
        dto.setIds(List.of("user-1", "user-2"));

        controller.batchHardDelete(dto, loginUser("operator-1", "1"));

        verify(userService).hardDeleteUsers(List.of("user-1", "user-2"), "operator-1");
    }

    private static LoginUserDetails loginUser(String userId, String userType) {
        LoginUserDetails details = new LoginUserDetails();
        details.setUserId(userId);
        details.setUserType(userType);
        return details;
    }
}
