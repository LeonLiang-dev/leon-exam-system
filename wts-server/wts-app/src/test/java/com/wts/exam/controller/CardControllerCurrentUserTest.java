package com.wts.exam.controller;

import com.wts.common.security.CurrentUserProvider;
import com.wts.common.security.LoginUserDetails;
import com.wts.exam.dto.CardSubmitDTO;
import com.wts.exam.dto.JudgeDTO;
import com.wts.exam.entity.ExamCard;
import com.wts.exam.service.CardService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class CardControllerCurrentUserTest {

    @Mock
    private CardService service;

    private CardController controller;

    @BeforeEach
    void setUp() {
        controller = new CardController(service, new CurrentUserProvider());

        LoginUserDetails details = new LoginUserDetails();
        details.setUserId("user-1");
        details.setLoginName("teacher");
        details.setName("Teacher One");
        details.setUserType("1");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, List.of())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void enterRoomUsesAuthenticatedUser() {
        ExamCard card = new ExamCard();
        when(service.enterRoom("room-1", "user-1", "Teacher One", true)).thenReturn(card);

        controller.enterRoom("room-1");

        verify(service).enterRoom("room-1", "user-1", "Teacher One", true);
    }

    @Test
    void enterRoomPassesStudentFlagForEligibility() {
        authenticate("student-1", "student", "Student One", "2");
        ExamCard card = new ExamCard();
        when(service.enterRoom("room-1", "student-1", "Student One", false)).thenReturn(card);

        controller.enterRoom("room-1");

        verify(service).enterRoom("room-1", "student-1", "Student One", false);
    }

    @Test
    void submitUsesAuthenticatedUserId() {
        CardSubmitDTO dto = new CardSubmitDTO();

        controller.submit("card-1", dto);

        verify(service).submit("card-1", dto, "user-1");
    }

    @Test
    void judgeUsesAuthenticatedUser() {
        JudgeDTO dto = new JudgeDTO();

        controller.judge("card-1", dto);

        verify(service).judge("card-1", dto, "user-1", "Teacher One");
    }

    @Test
    void studentCannotJudgeCard() {
        authenticate("student-1", "student", "Student One", "2");
        JudgeDTO dto = new JudgeDTO();

        var error = assertThrows(com.wts.common.exception.BizException.class, () -> controller.judge("card-1", dto));

        assertEquals(403, error.getCode());
        verify(service, never()).judge(any(), any(), any(), any());
    }

    @Test
    void studentCannotReadAnotherUsersResult() {
        authenticate("student-1", "student", "Student One", "2");
        when(service.getResult("card-1", "student-1", false))
                .thenThrow(com.wts.common.exception.BizException.forbidden("无权查看此成绩"));

        var error = assertThrows(com.wts.common.exception.BizException.class, () -> controller.detail("card-1"));

        assertEquals(403, error.getCode());
        verify(service).getResult("card-1", "student-1", false);
        verify(service, never()).getCardAnswers("card-1");
        verify(service, never()).getCardPoints("card-1");
        verify(service, never()).getExamPaperForReview("card-1");
    }

    @Test
    void studentCanReadOwnResultWhenServiceAllows() {
        authenticate("student-1", "student", "Student One", "2");
        ExamCard card = new ExamCard();
        card.setId("card-1");
        card.setUserid("student-1");
        when(service.getResult("card-1", "student-1", false)).thenReturn(card);
        when(service.getCardAnswers("card-1")).thenReturn(List.of());
        when(service.getCardPoints("card-1")).thenReturn(List.of());

        controller.detail("card-1");

        verify(service).getResult("card-1", "student-1", false);
        verify(service).getCardAnswers("card-1");
        verify(service).getCardPoints("card-1");
        verify(service).getExamPaperForReview("card-1");
    }

    @Test
    void adminCanReadCardResult() {
        ExamCard card = new ExamCard();
        card.setId("card-1");
        card.setUserid("student-1");
        when(service.getResult("card-1", "user-1", true)).thenReturn(card);
        when(service.getCardAnswers("card-1")).thenReturn(List.of());
        when(service.getCardPoints("card-1")).thenReturn(List.of());

        controller.detail("card-1");

        verify(service).getResult("card-1", "user-1", true);
        verify(service).getCardAnswers("card-1");
        verify(service).getCardPoints("card-1");
        verify(service).getExamPaperForReview("card-1");
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
