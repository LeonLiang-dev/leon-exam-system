package com.wts.exam.service.impl;

import com.wts.common.exception.BizException;
import com.wts.exam.dto.CardAnswerDTO;
import com.wts.exam.dto.CardSubmitDTO;
import com.wts.exam.dto.ExamPaperVO;
import com.wts.exam.dto.JudgeDTO;
import com.wts.exam.entity.ExamCard;
import com.wts.exam.entity.ExamCardAnswer;
import com.wts.exam.entity.ExamCardPoint;
import com.wts.exam.entity.ExamPaperSubject;
import com.wts.exam.entity.ExamPaper;
import com.wts.exam.entity.ExamRoom;
import com.wts.exam.entity.ExamRoomPaper;
import com.wts.exam.entity.ExamSubjectAnswer;
import com.wts.exam.entity.ExamSubjectVersion;
import com.wts.exam.mapper.ExamCardAnswerMapper;
import com.wts.exam.mapper.ExamCardMapper;
import com.wts.exam.mapper.ExamCardPointMapper;
import com.wts.exam.mapper.ExamPaperChapterMapper;
import com.wts.exam.mapper.ExamPaperMapper;
import com.wts.exam.mapper.ExamPaperSubjectMapper;
import com.wts.exam.mapper.ExamRoomMapper;
import com.wts.exam.mapper.ExamRoomPaperMapper;
import com.wts.exam.mapper.ExamRoomUserMapper;
import com.wts.exam.mapper.ExamSubjectAnswerMapper;
import com.wts.exam.mapper.ExamSubjectMapper;
import com.wts.exam.mapper.ExamSubjectVersionMapper;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardServiceImplLifecycleTest {

    @Mock
    private ExamCardMapper cardMapper;
    @Mock
    private ExamCardAnswerMapper cardAnswerMapper;
    @Mock
    private ExamCardPointMapper cardPointMapper;
    @Mock
    private ExamSubjectVersionMapper versionMapper;
    @Mock
    private ExamSubjectAnswerMapper answerMapper;
    @Mock
    private ExamPaperSubjectMapper paperSubjectMapper;
    @Mock
    private ExamRoomMapper roomMapper;
    @Mock
    private ExamRoomPaperMapper roomPaperMapper;
    @Mock
    private ExamRoomUserMapper roomUserMapper;
    @Mock
    private ExamPaperMapper paperMapper;
    @Mock
    private ExamPaperChapterMapper chapterMapper;
    @Mock
    private ExamSubjectMapper subjectMapper;

    private CardServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CardServiceImpl(
                cardMapper,
                cardAnswerMapper,
                cardPointMapper,
                versionMapper,
                answerMapper,
                paperSubjectMapper,
                roomMapper,
                roomPaperMapper,
                paperMapper,
                chapterMapper,
                subjectMapper,
                new RoomParticipationPolicy(roomUserMapper),
                new CardAnswerGrader()
        );
    }

    @Test
    void enterRoomRejectsUnopenedRoomWithoutCreatingCard() {
        ExamRoom room = room("room-1", "11");
        when(roomMapper.selectById("room-1")).thenReturn(room);
        when(cardMapper.selectOne(any())).thenReturn(null);

        BizException error = assertThrows(
                BizException.class,
                () -> service.enterRoom("room-1", "user-1", "Student One", false)
        );

        assertEquals("答题室未开放", error.getMessage());
        verify(roomPaperMapper, never()).selectList(any());
        verify(cardMapper, never()).insert(any(ExamCard.class));
    }

    @Test
    void enterRoomRejectsRoomBeforeStartTime() {
        ExamRoom room = room("room-1", "21");
        room.setStarttime(ExamTimeUtils.format(LocalDateTime.now().plusHours(1)));
        when(roomMapper.selectById("room-1")).thenReturn(room);
        when(cardMapper.selectOne(any())).thenReturn(null);

        BizException error = assertThrows(
                BizException.class,
                () -> service.enterRoom("room-1", "user-1", "Student One", false)
        );

        assertEquals("答题室未开始", error.getMessage());
        verify(roomPaperMapper, never()).selectList(any());
        verify(cardMapper, never()).insert(any(ExamCard.class));
    }

    @Test
    void enterRoomRejectsRoomAfterEndTime() {
        ExamRoom room = room("room-1", "21");
        room.setEndtime(ExamTimeUtils.format(LocalDateTime.now().minusMinutes(1)));
        when(roomMapper.selectById("room-1")).thenReturn(room);
        when(cardMapper.selectOne(any())).thenReturn(null);

        BizException error = assertThrows(
                BizException.class,
                () -> service.enterRoom("room-1", "user-1", "Student One", false)
        );

        assertEquals("答题室已结束，无法作答", error.getMessage());
        verify(roomPaperMapper, never()).selectList(any());
        verify(cardMapper, never()).insert(any(ExamCard.class));
    }

    @Test
    void enterRoomRejectsFinishedCardForStudent() {
        ExamCard finished = card("card-1", "paper-1", "room-1", "user-1", "16");
        when(roomMapper.selectById("room-1")).thenReturn(room("room-1", "31"));
        when(cardMapper.selectOne(any())).thenReturn(finished);

        BizException error = assertThrows(
                BizException.class,
                () -> service.enterRoom("room-1", "user-1", "Student One", false)
        );

        assertEquals("答卷已提交，无需再次进入", error.getMessage());
        verify(roomPaperMapper, never()).selectList(any());
        verify(cardMapper, never()).insert(any(ExamCard.class));
    }

    @Test
    void enterRoomReturnsFinishedCardForAdmin() {
        ExamCard finished = card("card-1", "paper-1", "room-1", "admin-1", "16");
        when(roomMapper.selectById("room-1")).thenReturn(room("room-1", "31"));
        when(cardMapper.selectOne(any())).thenReturn(finished);

        ExamCard card = service.enterRoom("room-1", "admin-1", "Admin One", true);

        assertEquals("card-1", card.getId());
        assertEquals("16", card.getPstate());
        verify(roomPaperMapper, never()).selectList(any());
        verify(cardMapper, never()).insert(any(ExamCard.class));
    }

    @Test
    void enterRoomCreatesInProgressCardForAuthenticatedUser() {
        when(roomMapper.selectById("room-1")).thenReturn(room("room-1", "21"));
        when(cardMapper.selectOne(any())).thenReturn(null);
        when(roomPaperMapper.selectList(any())).thenReturn(List.of(roomPaper("room-1", "paper-1")));

        ExamCard card = service.enterRoom("room-1", "user-1", "Student One", false);

        assertNotNull(card.getId());
        assertEquals("room-1", card.getRoomid());
        assertEquals("paper-1", card.getPaperid());
        assertEquals("user-1", card.getUserid());
        assertEquals("11", card.getPstate());
        assertEquals(0f, card.getPoint());
        assertEquals("0", card.getOvertime());
        assertEquals("0", card.getStatistical());
        verify(cardMapper).insert(card);
    }

    @Test
    void enterRoomRejectsPrivateRoomWhenStudentIsNotAssigned() {
        ExamRoom room = room("room-1", "21");
        room.setPublictype("2");
        when(roomMapper.selectById("room-1")).thenReturn(room);
        when(cardMapper.selectOne(any())).thenReturn(null);
        when(roomUserMapper.selectCount(any())).thenReturn(0L);

        BizException error = assertThrows(
                BizException.class,
                () -> service.enterRoom("room-1", "user-1", "Student One", false)
        );

        assertEquals(403, error.getCode());
        assertEquals("无权进入此答题室", error.getMessage());
        verify(roomPaperMapper, never()).selectList(any());
        verify(cardMapper, never()).insert(any(ExamCard.class));
    }

    @Test
    void enterRoomCreatesCardForAssignedPrivateRoomStudent() {
        ExamRoom room = room("room-1", "21");
        room.setPublictype("2");
        when(roomMapper.selectById("room-1")).thenReturn(room);
        when(cardMapper.selectOne(any())).thenReturn(null);
        when(roomUserMapper.selectCount(any())).thenReturn(1L);
        when(roomPaperMapper.selectList(any())).thenReturn(List.of(roomPaper("room-1", "paper-1")));

        ExamCard card = service.enterRoom("room-1", "user-1", "Student One", false);

        assertEquals("room-1", card.getRoomid());
        assertEquals("user-1", card.getUserid());
        verify(cardMapper).insert(card);
    }

    @Test
    void enterRoomAdminBypassesPrivateRoomAssignment() {
        ExamRoom room = room("room-1", "21");
        room.setPublictype("2");
        when(roomMapper.selectById("room-1")).thenReturn(room);
        when(cardMapper.selectOne(any())).thenReturn(null);
        when(roomPaperMapper.selectList(any())).thenReturn(List.of(roomPaper("room-1", "paper-1")));

        ExamCard card = service.enterRoom("room-1", "teacher-1", "Teacher One", true);

        assertEquals("teacher-1", card.getUserid());
        verifyNoInteractions(roomUserMapper);
        verify(cardMapper).insert(card);
    }

    @Test
    void saveAnswersRejectsSubmittedCard() {
        ExamCard card = card("card-1", "paper-1", "room-1", "user-1", "16");
        when(cardMapper.selectById("card-1")).thenReturn(card);

        BizException error = assertThrows(
                BizException.class,
                () -> service.saveAnswers("card-1", submitDto("version-1", "answer-1", "true"), "user-1")
        );

        assertEquals("答卷已提交，无法修改", error.getMessage());
        verifyNoInteractions(cardAnswerMapper);
    }

    @Test
    void saveAnswersRejectsUserWhoDoesNotOwnCard() {
        ExamCard card = card("card-1", "paper-1", "room-1", "owner-1", "11");
        when(cardMapper.selectById("card-1")).thenReturn(card);

        BizException error = assertThrows(
                BizException.class,
                () -> service.saveAnswers("card-1", submitDto("version-1", "answer-1", "true"), "user-2")
        );

        assertEquals("无权操作此答卷", error.getMessage());
        verifyNoInteractions(roomMapper, cardAnswerMapper);
    }

    @Test
    void submitRejectsUserWhoDoesNotOwnCard() {
        ExamCard card = card("card-1", "paper-1", "room-1", "owner-1", "11");
        when(cardMapper.selectById("card-1")).thenReturn(card);

        BizException error = assertThrows(
                BizException.class,
                () -> service.submit("card-1", submitDto("version-1", "answer-1", "true"), "user-2")
        );

        assertEquals("无权操作此答卷", error.getMessage());
        verifyNoInteractions(cardAnswerMapper, cardPointMapper, paperSubjectMapper);
    }

    @Test
    void submitSavesAnswersAndAutoGradesSingleChoice() {
        ExamCard card = card("card-1", "paper-1", "room-1", "user-1", "11");
        when(cardMapper.selectById("card-1")).thenReturn(card);
        when(roomMapper.selectById("room-1")).thenReturn(room("room-1", "21"));
        when(cardAnswerMapper.selectList(any()))
                .thenReturn(List.of())
                .thenReturn(List.of(cardAnswer("card-1", "version-1", "answer-1", "true")));
        when(paperSubjectMapper.selectList(any()))
                .thenReturn(List.of(paperSubject("paper-1", "subject-1", "version-1", 5)));
        when(versionMapper.selectList(any()))
                .thenReturn(List.of(subjectVersion("version-1", "subject-1", "2")));
        when(answerMapper.selectList(any()))
                .thenReturn(List.of(subjectAnswer("answer-1", "version-1", "1")));

        ExamCard submitted = service.submit("card-1", submitDto("version-1", "answer-1", "true"), "user-1");

        assertEquals("21", submitted.getPstate());
        assertEquals(5f, submitted.getPoint());
        assertEquals(1, submitted.getCompletenum());
        assertEquals(1, submitted.getAllnum());
        assertNotNull(submitted.getSubmittime());
        assertEquals("AUTO", submitted.getAdjudgeuser());
        assertEquals("系统自动阅卷", submitted.getAdjudgeusername());
        assertEquals("AUTO", submitted.getAdjudgeuseruuid());
        assertNotNull(submitted.getAdjudgetime());

        ArgumentCaptor<ExamCardAnswer> answerCaptor = ArgumentCaptor.forClass(ExamCardAnswer.class);
        verify(cardAnswerMapper).insert(answerCaptor.capture());
        assertEquals("card-1", answerCaptor.getValue().getCardid());
        assertEquals("version-1", answerCaptor.getValue().getVersionid());
        assertEquals("answer-1", answerCaptor.getValue().getAnswerid());
        assertEquals("user-1", answerCaptor.getValue().getCuser());
        assertEquals("true", answerCaptor.getValue().getValstr());

        ArgumentCaptor<ExamCardPoint> pointCaptor = ArgumentCaptor.forClass(ExamCardPoint.class);
        verify(cardPointMapper).insert(pointCaptor.capture());
        assertEquals("card-1", pointCaptor.getValue().getCardid());
        assertEquals("version-1", pointCaptor.getValue().getVersionid());
        assertEquals(5, pointCaptor.getValue().getPoint());
        assertEquals(5, pointCaptor.getValue().getMpoint());
        assertEquals("1", pointCaptor.getValue().getComplete());

        verify(cardMapper).updateById(card);
    }

    @Test
    void submitKeepsCardSubmittedWhenPaperContainsManualQuestion() {
        ExamCard card = card("card-1", "paper-1", "room-1", "user-1", "11");
        when(cardMapper.selectById("card-1")).thenReturn(card);
        when(roomMapper.selectById("room-1")).thenReturn(room("room-1", "21"));
        when(cardAnswerMapper.selectList(any()))
                .thenReturn(List.of())
                .thenReturn(List.of(cardAnswer("card-1", "version-1", "answer-1", "true")));
        when(paperSubjectMapper.selectList(any()))
                .thenReturn(List.of(
                        paperSubject("paper-1", "subject-1", "version-1", 5),
                        paperSubject("paper-1", "subject-2", "version-2", 10)
                ));
        when(versionMapper.selectList(any()))
                .thenReturn(List.of(
                        subjectVersion("version-1", "subject-1", "2"),
                        subjectVersion("version-2", "subject-2", "5")
                ));
        when(answerMapper.selectList(any()))
                .thenReturn(List.of(subjectAnswer("answer-1", "version-1", "1")));

        ExamCard submitted = service.submit("card-1", submitDto("version-1", "answer-1", "true"), "user-1");

        assertEquals("16", submitted.getPstate());
        assertEquals(5f, submitted.getPoint());
        assertEquals(1, submitted.getCompletenum());
        assertEquals(2, submitted.getAllnum());
        assertNotNull(submitted.getSubmittime());
        assertNull(submitted.getAdjudgetime());
        verify(cardPointMapper, times(2)).insert(any(ExamCardPoint.class));
        verify(cardMapper).updateById(card);
    }

    @Test
    void getExamPaperRejectsUserWhoDoesNotOwnCard() {
        ExamCard card = card("card-1", "paper-1", "room-1", "owner-1", "11");
        when(cardMapper.selectById("card-1")).thenReturn(card);

        BizException error = assertThrows(
                BizException.class,
                () -> service.getExamPaper("card-1", "user-2")
        );

        assertEquals("无权操作此答卷", error.getMessage());
        verifyNoInteractions(roomMapper, paperMapper, paperSubjectMapper);
    }

    @Test
    void getExamPaperForReviewRejectsInProgressCard() {
        ExamCard card = card("card-1", "paper-1", "room-1", "user-1", "11");
        when(cardMapper.selectById("card-1")).thenReturn(card);

        BizException error = assertThrows(
                BizException.class,
                () -> service.getExamPaperForReview("card-1")
        );

        assertEquals("只有已提交的答卷才能阅卷", error.getMessage());
        verifyNoInteractions(roomMapper, paperMapper, paperSubjectMapper);
    }

    @Test
    void studentResultRejectsBeforeRoomEndTime() {
        ExamCard card = card("card-1", "paper-1", "room-1", "user-1", "16");
        ExamRoom room = room("room-1", "21");
        room.setEndtime(ExamTimeUtils.format(LocalDateTime.now().plusMinutes(30)));
        when(cardMapper.selectById("card-1")).thenReturn(card);
        when(roomMapper.selectById("room-1")).thenReturn(room);

        BizException error = assertThrows(
                BizException.class,
                () -> service.getResult("card-1", "user-1", false)
        );

        assertEquals("考试结束后可查看成绩", error.getMessage());
        verifyNoInteractions(cardAnswerMapper, cardPointMapper);
    }

    @Test
    void studentResultRejectsWhenRoomHidesResults() {
        ExamCard card = card("card-1", "paper-1", "room-1", "user-1", "21");
        ExamRoom room = room("room-1", "31");
        room.setResultstype("3");
        room.setEndtime(ExamTimeUtils.format(LocalDateTime.now().minusMinutes(1)));
        when(cardMapper.selectById("card-1")).thenReturn(card);
        when(roomMapper.selectById("room-1")).thenReturn(room);

        BizException error = assertThrows(
                BizException.class,
                () -> service.getResult("card-1", "user-1", false)
        );

        assertEquals("该答题室不显示成绩", error.getMessage());
        verifyNoInteractions(cardAnswerMapper, cardPointMapper);
    }

    @Test
    void studentResultAllowsOwnSubmittedCardAfterRoomEndTime() {
        ExamCard card = card("card-1", "paper-1", "room-1", "user-1", "16");
        ExamRoom room = room("room-1", "21");
        room.setResultstype("1");
        room.setEndtime(ExamTimeUtils.format(LocalDateTime.now().minusMinutes(1)));
        when(cardMapper.selectById("card-1")).thenReturn(card);
        when(roomMapper.selectById("room-1")).thenReturn(room);

        ExamCard result = service.getResult("card-1", "user-1", false);

        assertEquals(card, result);
    }

    @Test
    void getExamPaperForReviewReturnsSubmittedPaperView() {
        ExamCard card = card("card-1", "paper-1", "room-1", "user-1", "16");
        ExamPaper paper = new ExamPaper();
        paper.setId("paper-1");
        paper.setName("期中考试");
        when(cardMapper.selectById("card-1")).thenReturn(card);
        when(roomMapper.selectById("room-1")).thenReturn(room("room-1", "31"));
        when(paperMapper.selectById("paper-1")).thenReturn(paper);
        when(chapterMapper.selectList(any())).thenReturn(List.of());
        when(paperSubjectMapper.selectList(any())).thenReturn(List.of());
        when(versionMapper.selectList(any())).thenReturn(List.of());
        when(subjectMapper.selectList(any())).thenReturn(List.of());
        when(answerMapper.selectList(any())).thenReturn(List.of());
        when(cardAnswerMapper.selectList(any())).thenReturn(List.of());

        ExamPaperVO vo = service.getExamPaperForReview("card-1");

        assertEquals(card, vo.getCard());
        assertEquals("期中考试", vo.getPaperName());
        assertEquals(60, vo.getTimelen());
        assertEquals(List.of(), vo.getChapters());
        assertEquals(List.of(), vo.getSavedAnswers());
    }

    @Test
    void judgeUpdatesProvidedPointsAndRecalculatesTotal() {
        ExamCard card = card("card-1", "paper-1", "room-1", "user-1", "16");
        ExamCardPoint manualPoint = cardPoint("card-1", "version-1", 0);
        ExamCardPoint objectivePoint = cardPoint("card-1", "version-2", 3);
        manualPoint.setMpoint(10);
        objectivePoint.setMpoint(3);
        when(cardMapper.selectById("card-1")).thenReturn(card);
        when(cardPointMapper.selectList(any()))
                .thenReturn(List.of(manualPoint, objectivePoint))
                .thenReturn(List.of(manualPoint, objectivePoint));
        when(versionMapper.selectList(any()))
                .thenReturn(List.of(subjectVersion("version-1", "subject-1", "5")));

        service.judge("card-1", judgeDto("version-1", 7), "teacher-1", "Teacher One");

        assertEquals(7, manualPoint.getPoint());
        assertEquals(10f, card.getPoint());
        assertEquals("teacher-1", card.getAdjudgeuser());
        assertEquals("Teacher One", card.getAdjudgeusername());
        assertEquals("teacher-1", card.getAdjudgeuseruuid());
        assertEquals("21", card.getPstate());
        assertNotNull(card.getAdjudgetime());

        verify(cardPointMapper).updateById(manualPoint);
        verify(cardMapper).updateById(card);
    }

    @Test
    void judgeRejectsObjectivePointOverride() {
        ExamCard card = card("card-1", "paper-1", "room-1", "user-1", "16");
        ExamCardPoint objectivePoint = cardPoint("card-1", "version-1", 3);
        objectivePoint.setMpoint(3);
        when(cardMapper.selectById("card-1")).thenReturn(card);
        when(cardPointMapper.selectList(any())).thenReturn(List.of(objectivePoint));
        when(versionMapper.selectList(any()))
                .thenReturn(List.of(subjectVersion("version-1", "subject-1", "2")));

        BizException error = assertThrows(BizException.class,
                () -> service.judge("card-1", judgeDto("version-1", 2), "teacher-1", "Teacher One"));

        assertEquals("客观题由系统自动评阅，不允许手动改分", error.getMessage());
        verify(cardPointMapper, never()).updateById(any(ExamCardPoint.class));
        verify(cardMapper, never()).updateById(any(ExamCard.class));
    }

    @Test
    void judgeRejectsPointAboveMax() {
        ExamCard card = card("card-1", "paper-1", "room-1", "user-1", "16");
        ExamCardPoint manualPoint = cardPoint("card-1", "version-1", 0);
        manualPoint.setMpoint(5);
        when(cardMapper.selectById("card-1")).thenReturn(card);
        when(cardPointMapper.selectList(any())).thenReturn(List.of(manualPoint));
        when(versionMapper.selectList(any()))
                .thenReturn(List.of(subjectVersion("version-1", "subject-1", "5")));

        BizException error = assertThrows(BizException.class,
                () -> service.judge("card-1", judgeDto("version-1", 6), "teacher-1", "Teacher One"));

        assertEquals("评分不能超出题目分值", error.getMessage());
        verify(cardPointMapper, never()).updateById(any(ExamCardPoint.class));
        verify(cardMapper, never()).updateById(any(ExamCard.class));
    }

    private static ExamRoom room(String id, String pstate) {
        ExamRoom room = new ExamRoom();
        room.setId(id);
        room.setPstate(pstate);
        return room;
    }

    private static ExamRoomPaper roomPaper(String roomId, String paperId) {
        ExamRoomPaper roomPaper = new ExamRoomPaper();
        roomPaper.setId("room-paper-1");
        roomPaper.setRoomid(roomId);
        roomPaper.setPaperid(paperId);
        return roomPaper;
    }

    private static ExamCard card(String id, String paperId, String roomId, String userId, String pstate) {
        ExamCard card = new ExamCard();
        card.setId(id);
        card.setPaperid(paperId);
        card.setRoomid(roomId);
        card.setUserid(userId);
        card.setPstate(pstate);
        return card;
    }

    private static CardSubmitDTO submitDto(String versionId, String answerId, String value) {
        CardAnswerDTO answer = new CardAnswerDTO();
        answer.setVersionid(versionId);
        answer.setAnswerid(answerId);
        answer.setValstr(value);

        CardSubmitDTO dto = new CardSubmitDTO();
        dto.setAnswers(List.of(answer));
        return dto;
    }

    private static ExamCardAnswer cardAnswer(String cardId, String versionId, String answerId, String value) {
        ExamCardAnswer answer = new ExamCardAnswer();
        answer.setId("card-answer-1");
        answer.setCardid(cardId);
        answer.setVersionid(versionId);
        answer.setAnswerid(answerId);
        answer.setValstr(value);
        return answer;
    }

    private static ExamPaperSubject paperSubject(String paperId, String subjectId, String versionId, int point) {
        ExamPaperSubject subject = new ExamPaperSubject();
        subject.setId("paper-subject-1");
        subject.setPaperid(paperId);
        subject.setSubjectid(subjectId);
        subject.setVersionid(versionId);
        subject.setPoint(point);
        return subject;
    }

    private static ExamSubjectVersion subjectVersion(String versionId, String subjectId, String tiptype) {
        ExamSubjectVersion version = new ExamSubjectVersion();
        version.setId(versionId);
        version.setSubjectid(subjectId);
        version.setTiptype(tiptype);
        return version;
    }

    private static ExamSubjectAnswer subjectAnswer(String answerId, String versionId, String rightanswer) {
        ExamSubjectAnswer answer = new ExamSubjectAnswer();
        answer.setId(answerId);
        answer.setVersionid(versionId);
        answer.setRightanswer(rightanswer);
        return answer;
    }

    private static JudgeDTO judgeDto(String versionId, int point) {
        JudgeDTO.JudgePointDTO pointDto = new JudgeDTO.JudgePointDTO();
        pointDto.setVersionId(versionId);
        pointDto.setPoint(point);

        JudgeDTO dto = new JudgeDTO();
        dto.setPoints(List.of(pointDto));
        return dto;
    }

    private static ExamCardPoint cardPoint(String cardId, String versionId, int point) {
        ExamCardPoint cardPoint = new ExamCardPoint();
        cardPoint.setId(versionId + "-point");
        cardPoint.setCardid(cardId);
        cardPoint.setVersionid(versionId);
        cardPoint.setPoint(point);
        return cardPoint;
    }
}
