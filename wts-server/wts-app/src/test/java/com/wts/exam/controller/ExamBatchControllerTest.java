package com.wts.exam.controller;

import com.wts.common.exception.BizException;
import com.wts.common.security.CurrentUserProvider;
import com.wts.common.security.LoginUserDetails;
import com.wts.exam.dto.BatchIdsDTO;
import com.wts.exam.service.CardService;
import com.wts.exam.service.PaperService;
import com.wts.exam.service.RandomService;
import com.wts.exam.service.RoomService;
import com.wts.exam.service.SubjectImportService;
import com.wts.exam.service.SubjectService;
import com.wts.exam.service.SubjectTypeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ExamBatchControllerTest {

    @Mock
    private SubjectTypeService subjectTypeService;
    @Mock
    private SubjectService subjectService;
    @Mock
    private SubjectImportService subjectImportService;
    @Mock
    private PaperService paperService;
    @Mock
    private RoomService roomService;
    @Mock
    private RandomService randomService;
    @Mock
    private CardService cardService;

    private final CurrentUserProvider currentUserProvider = new CurrentUserProvider();

    @BeforeEach
    void setUp() {
        authenticate("admin-1", "admin", "Admin One", "1");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void subjectTypeBatchDeleteDelegatesNormalizedIds() {
        SubjectTypeController controller = new SubjectTypeController(subjectTypeService, currentUserProvider);

        controller.batchDelete(batchIds(" type-1 ", "type-2", "type-1"));

        verify(subjectTypeService).deleteBatch(List.of("type-1", "type-2"), "admin-1");
    }

    @Test
    void subjectBatchDeleteDelegatesNormalizedIds() {
        SubjectController controller = new SubjectController(subjectService, subjectImportService, currentUserProvider);

        controller.batchDelete(batchIds(" subject-1 ", "subject-2", "subject-1"));

        verify(subjectService).deleteBatch(List.of("subject-1", "subject-2"), "admin-1");
    }

    @Test
    void paperBatchDeleteDelegatesNormalizedIds() {
        PaperController controller = new PaperController(paperService, currentUserProvider);

        controller.batchDelete(batchIds(" paper-1 ", "paper-2", "paper-1"));

        verify(paperService).deleteBatch(List.of("paper-1", "paper-2"), "admin-1");
    }

    @Test
    void roomBatchActionsDelegateNormalizedIds() {
        RoomController controller = new RoomController(roomService, currentUserProvider);
        BatchIdsDTO dto = batchIds(" room-1 ", "room-2", "room-1");

        controller.batchPublish(dto);
        controller.batchClose(dto);
        controller.batchDelete(dto);

        List<String> ids = List.of("room-1", "room-2");
        verify(roomService).publishBatch(ids, "admin-1");
        verify(roomService).closeBatch(ids, "admin-1");
        verify(roomService).deleteBatch(ids, "admin-1");
    }

    @Test
    void randomBatchDeletesDelegateNormalizedIds() {
        RandomController controller = new RandomController(randomService, currentUserProvider);
        BatchIdsDTO dto = batchIds(" item-1 ", "item-2", "item-1");

        controller.batchDeleteItems(dto);
        controller.batchDeleteSteps(batchIds(" step-1 ", "step-2", "step-1"));

        verify(randomService).deleteItemsBatch(List.of("item-1", "item-2"));
        verify(randomService).deleteStepsBatch(List.of("step-1", "step-2"));
    }

    @Test
    void cardBatchJudgeDelegatesNormalizedIds() {
        CardController controller = new CardController(cardService, currentUserProvider);

        controller.batchJudge(batchIds(" card-1 ", "card-2", "card-1"));

        verify(cardService).judgeBatch(List.of("card-1", "card-2"), "admin-1", "Admin One");
    }

    @Test
    void studentCannotBatchDeleteSubjects() {
        authenticate("student-1", "student", "Student One", "2");
        SubjectController controller = new SubjectController(subjectService, subjectImportService, currentUserProvider);

        BizException error = assertThrows(BizException.class,
                () -> controller.batchDelete(batchIds("subject-1")));

        assertEquals(400, error.getCode());
        verify(subjectService, never()).deleteBatch(any(), any());
    }

    private static BatchIdsDTO batchIds(String... ids) {
        BatchIdsDTO dto = new BatchIdsDTO();
        dto.setIds(List.of(ids));
        return dto;
    }

    private void authenticate(String userId, String loginName, String name, String userType) {
        LoginUserDetails details = new LoginUserDetails();
        details.setUserId(userId);
        details.setLoginName(loginName);
        details.setName(name);
        details.setUserType(userType);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, List.of())
        );
    }
}
