package com.wts.exam.service.impl;

import com.wts.common.exception.BizException;
import com.wts.exam.dto.AnswerDTO;
import com.wts.exam.dto.SubjectDTO;
import com.wts.exam.entity.ExamSubject;
import com.wts.exam.entity.ExamSubjectAnswer;
import com.wts.exam.entity.ExamSubjectVersion;
import com.wts.exam.mapper.ExamSubjectAnswerMapper;
import com.wts.exam.mapper.ExamSubjectMapper;
import com.wts.exam.mapper.ExamSubjectVersionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubjectServiceImplTest {

    @Mock
    private ExamSubjectMapper subjectMapper;
    @Mock
    private ExamSubjectVersionMapper versionMapper;
    @Mock
    private ExamSubjectAnswerMapper answerMapper;

    private SubjectServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SubjectServiceImpl(subjectMapper, versionMapper, answerMapper);
    }

    @Test
    void createInsertsSubjectVersionAndAnswersWithDefaults() {
        SubjectDTO dto = subjectDto();

        ExamSubject created = service.create(dto, "teacher-1", "Teacher One");

        ArgumentCaptor<ExamSubject> subjectCaptor = ArgumentCaptor.forClass(ExamSubject.class);
        verify(subjectMapper).insert(subjectCaptor.capture());
        ExamSubject subject = subjectCaptor.getValue();
        assertEquals(created, subject);
        assertNotNull(subject.getId());
        assertEquals(subject.getId(), subject.getUuid());
        assertEquals("type-1", subject.getTypeid());
        assertEquals("题干", subject.getIntroduction());
        assertEquals("1", subject.getPstate());
        assertEquals(1, subject.getLevel());
        assertEquals(1, subject.getPoint());
        assertEquals(0, subject.getPraisenum());
        assertEquals(0, subject.getCommentnum());

        ArgumentCaptor<ExamSubjectVersion> versionCaptor = ArgumentCaptor.forClass(ExamSubjectVersion.class);
        verify(versionMapper).insert(versionCaptor.capture());
        ExamSubjectVersion version = versionCaptor.getValue();
        assertEquals(subject.getVersionid(), version.getId());
        assertEquals(subject.getId(), version.getSubjectid());
        assertEquals("2", version.getTiptype());
        assertEquals("题干", version.getTipstr());
        assertEquals("teacher-1", version.getCuser());
        assertEquals("Teacher One", version.getCusername());

        ArgumentCaptor<ExamSubjectAnswer> answerCaptor = ArgumentCaptor.forClass(ExamSubjectAnswer.class);
        verify(answerMapper).insert(answerCaptor.capture());
        ExamSubjectAnswer answer = answerCaptor.getValue();
        assertEquals(version.getId(), answer.getVersionid());
        assertEquals("A", answer.getAnswer());
        assertEquals("1", answer.getRightanswer());
        assertEquals(1, answer.getSort());
        assertEquals("teacher-1", answer.getCuser());
        assertEquals("Teacher One", answer.getCusername());
        assertEquals(answer.getId(), answer.getUuid());
    }

    @Test
    void updateCreatesNewVersionAndKeepsExistingFieldsWhenDtoDoesNotOverrideThem() {
        ExamSubject subject = subject("subject-1", "old-version");
        when(subjectMapper.selectById("subject-1")).thenReturn(subject);
        SubjectDTO dto = new SubjectDTO();
        dto.setTiptype("4");
        dto.setTipstr("新版题干");
        dto.setAnswers(List.of(answerDto("正确", "1", 1)));

        ExamSubject updated = service.update("subject-1", dto, "teacher-1", "Teacher One");

        assertEquals(subject, updated);
        assertNotEquals("old-version", subject.getVersionid());
        assertEquals("新版题干", subject.getIntroduction());
        assertEquals("type-old", subject.getTypeid());
        assertEquals(2, subject.getLevel());
        assertEquals(3, subject.getPoint());
        verify(subjectMapper).updateById(subject);

        ArgumentCaptor<ExamSubjectVersion> versionCaptor = ArgumentCaptor.forClass(ExamSubjectVersion.class);
        verify(versionMapper).insert(versionCaptor.capture());
        assertEquals("subject-1", versionCaptor.getValue().getSubjectid());
        assertEquals(subject.getVersionid(), versionCaptor.getValue().getId());
        assertEquals("4", versionCaptor.getValue().getTiptype());

        ArgumentCaptor<ExamSubjectAnswer> answerCaptor = ArgumentCaptor.forClass(ExamSubjectAnswer.class);
        verify(answerMapper).insert(answerCaptor.capture());
        assertEquals(subject.getVersionid(), answerCaptor.getValue().getVersionid());
    }

    @Test
    void deleteRemovesSubjectRecord() {
        ExamSubject subject = subject("subject-1", "version-1");
        when(subjectMapper.selectById("subject-1")).thenReturn(subject);

        service.delete("subject-1", "teacher-1");

        verify(subjectMapper).deleteById("subject-1");
        verify(subjectMapper, never()).updateById(any(ExamSubject.class));
    }

    @Test
    void deleteBatchRejectsEmptySelection() {
        BizException error = assertThrows(BizException.class,
                () -> service.deleteBatch(List.of(), "teacher-1"));

        assertEquals("请选择要删除的题目", error.getMessage());
        verifyNoInteractions(subjectMapper, versionMapper, answerMapper);
    }

    @Test
    void updateRejectsMissingSubject() {
        when(subjectMapper.selectById("missing")).thenReturn(null);

        BizException error = assertThrows(BizException.class,
                () -> service.update("missing", new SubjectDTO(), "teacher-1", "Teacher One"));

        assertEquals(404, error.getCode());
        verify(versionMapper, never()).insert(any(ExamSubjectVersion.class));
        verify(answerMapper, never()).insert(any(ExamSubjectAnswer.class));
    }

    private static SubjectDTO subjectDto() {
        SubjectDTO dto = new SubjectDTO();
        dto.setTypeid("type-1");
        dto.setTiptype("2");
        dto.setTipstr("题干");
        dto.setTipnote("解析提示");
        dto.setPcontent("正文");
        dto.setAnswers(List.of(answerDto("A", "1", 1)));
        return dto;
    }

    private static AnswerDTO answerDto(String answer, String rightanswer, int sort) {
        AnswerDTO dto = new AnswerDTO();
        dto.setAnswer(answer);
        dto.setRightanswer(rightanswer);
        dto.setSort(sort);
        dto.setAnswernote("答案说明");
        dto.setPointweight(100);
        dto.setGroupno(1);
        dto.setPcontent("选项正文");
        return dto;
    }

    private static ExamSubject subject(String id, String versionId) {
        ExamSubject subject = new ExamSubject();
        subject.setId(id);
        subject.setVersionid(versionId);
        subject.setTypeid("type-old");
        subject.setIntroduction("旧题干");
        subject.setLevel(2);
        subject.setPoint(3);
        subject.setPstate("1");
        return subject;
    }
}
