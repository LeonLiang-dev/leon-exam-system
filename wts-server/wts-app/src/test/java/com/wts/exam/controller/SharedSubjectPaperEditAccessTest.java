package com.wts.exam.controller;

import com.wts.auth.enums.Permission;
import com.wts.auth.service.PermissionService;
import com.wts.common.exception.BizException;
import com.wts.common.security.CurrentUserProvider;
import com.wts.common.security.LoginUserDetails;
import com.wts.exam.dto.BatchIdsDTO;
import com.wts.exam.dto.PaperDTO;
import com.wts.exam.dto.SubjectDTO;
import com.wts.exam.entity.ExamPaper;
import com.wts.exam.entity.ExamSubjectVersion;
import com.wts.exam.service.PaperService;
import com.wts.exam.service.SubjectService;
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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 题目/试卷「全院共用」后的编辑/删除权限：仅创建者本人或教研室主任/平台管理员可改删。
 */
@ExtendWith(MockitoExtension.class)
class SharedSubjectPaperEditAccessTest {

    @Mock
    private SubjectService subjectService;
    @Mock
    private PaperService paperService;
    @Mock
    private PermissionService permissionService;

    private CurrentUserProvider currentUserProvider;
    private SubjectController subjectController;
    private PaperController paperController;

    @BeforeEach
    void setUp() {
        currentUserProvider = new CurrentUserProvider();
        subjectController = new SubjectController(subjectService, null, currentUserProvider, permissionService);
        paperController = new PaperController(paperService, currentUserProvider, permissionService);
        // 控制层使用 mock 权限服务，不会拦截 SUBJECT_MANAGE 以外的校验项
        authenticate("teacher-1", "teacher", "Teacher One", "1");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void teacherCanEditOwnSubject() {
        ExamSubjectVersion version = versionOwnedBy("teacher-1");
        when(subjectService.getCurrentVersion("subject-1")).thenReturn(version);

        subjectController.update("subject-1", new SubjectDTO());

        verify(subjectService).update(any(String.class), any(SubjectDTO.class), any(String.class), any(String.class));
    }

    @Test
    void teacherCannotEditOthersSubject() {
        when(subjectService.getCurrentVersion("subject-1")).thenReturn(versionOwnedBy("teacher-2"));

        assertThrows(BizException.class, () -> subjectController.update("subject-1", new SubjectDTO()));

        verify(subjectService, never()).update(any(), any(), any(), any());
    }

    @Test
    void deptManagerCanEditAnySubject() {
        authenticate("director-1", "director", "Director One", "1", "director");
        when(subjectService.getCurrentVersion("subject-1")).thenReturn(versionOwnedBy("teacher-2"));

        subjectController.update("subject-1", new SubjectDTO());

        verify(subjectService).update(any(String.class), any(SubjectDTO.class), any(String.class), any(String.class));
    }

    @Test
    void teacherCannotBatchDeleteOthersSubjects() {
        when(subjectService.getCurrentVersion("subject-1")).thenReturn(versionOwnedBy("teacher-1"));
        when(subjectService.getCurrentVersion("subject-2")).thenReturn(versionOwnedBy("teacher-2"));
        BatchIdsDTO dto = new BatchIdsDTO();
        dto.setIds(List.of("subject-1", "subject-2"));

        assertThrows(BizException.class, () -> subjectController.batchDelete(dto));

        verify(subjectService, never()).deleteBatch(any(), any());
    }

    @Test
    void teacherCanEditOwnPaper() {
        when(paperService.getDetail("paper-1")).thenReturn(paperOwnedBy("teacher-1"));

        paperController.update("paper-1", new PaperDTO());

        verify(paperService).update(any(String.class), any(PaperDTO.class), any(String.class));
    }

    @Test
    void teacherCannotEditOthersPaper() {
        when(paperService.getDetail("paper-1")).thenReturn(paperOwnedBy("teacher-2"));

        assertThrows(BizException.class, () -> paperController.update("paper-1", new PaperDTO()));

        verify(paperService, never()).update(any(), any(), any());
    }

    @Test
    void platformAdminCanEditOthersPaper() {
        authenticate("admin-1", "admin", "Admin One", "3", "platform_admin");
        when(paperService.getDetail("paper-1")).thenReturn(paperOwnedBy("teacher-2"));

        paperController.update("paper-1", new PaperDTO());

        verify(paperService).update(any(String.class), any(PaperDTO.class), any(String.class));
    }

    @Test
    void teacherCannotBatchDeleteOthersPapers() {
        when(paperService.getDetail("paper-1")).thenReturn(paperOwnedBy("teacher-1"));
        when(paperService.getDetail("paper-2")).thenReturn(paperOwnedBy("teacher-2"));
        BatchIdsDTO dto = new BatchIdsDTO();
        dto.setIds(List.of("paper-1", "paper-2"));

        assertThrows(BizException.class, () -> paperController.batchDelete(dto));

        verify(paperService, never()).deleteBatch(any(), any());
    }

    private ExamSubjectVersion versionOwnedBy(String creator) {
        ExamSubjectVersion version = new ExamSubjectVersion();
        version.setCuser(creator);
        return version;
    }

    private ExamPaper paperOwnedBy(String creator) {
        ExamPaper paper = new ExamPaper();
        paper.setCuser(creator);
        return paper;
    }

    private void authenticate(String userId, String loginName, String name, String userType) {
        authenticate(userId, loginName, name, userType, null);
    }

    private void authenticate(String userId, String loginName, String name, String userType, String post) {
        LoginUserDetails details = new LoginUserDetails();
        details.setUserId(userId);
        details.setLoginName(loginName);
        details.setName(name);
        details.setUserType(userType);
        details.setPost(post);
        details.setPermissions(Set.of(Permission.SUBJECT_MANAGE.name()));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, List.of())
        );
    }
}