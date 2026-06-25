package com.wts.exam.service.impl;

import com.wts.common.exception.BizException;
import com.wts.exam.dto.PaperDTO;
import com.wts.exam.entity.ExamPaper;
import com.wts.exam.entity.ExamPaperChapter;
import com.wts.exam.entity.ExamPaperSubject;
import com.wts.exam.entity.ExamSubject;
import com.wts.exam.mapper.ExamPaperChapterMapper;
import com.wts.exam.mapper.ExamPaperMapper;
import com.wts.exam.mapper.ExamPaperSubjectMapper;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaperServiceImplTest {

    @Mock
    private ExamPaperMapper paperMapper;
    @Mock
    private ExamPaperChapterMapper chapterMapper;
    @Mock
    private ExamPaperSubjectMapper paperSubjectMapper;
    @Mock
    private ExamSubjectMapper subjectMapper;
    @Mock
    private ExamSubjectVersionMapper versionMapper;

    private PaperServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PaperServiceImpl(paperMapper, chapterMapper, paperSubjectMapper, subjectMapper, versionMapper);
    }

    @Test
    void createInsertsPaperAndDefaultChapter() {
        PaperDTO dto = new PaperDTO();
        dto.setName("期末试卷");
        dto.setExamtypeid("type-1");
        dto.setPapernote("说明");

        ExamPaper created = service.create(dto, "teacher-1", "Teacher One");

        ArgumentCaptor<ExamPaper> paperCaptor = ArgumentCaptor.forClass(ExamPaper.class);
        verify(paperMapper).insert(paperCaptor.capture());
        ExamPaper paper = paperCaptor.getValue();
        assertEquals(created, paper);
        assertNotNull(paper.getId());
        assertEquals(paper.getId(), paper.getUuid());
        assertEquals("期末试卷", paper.getName());
        assertEquals("type-1", paper.getExamtypeid());
        assertEquals("1", paper.getPstate());
        assertEquals(60, paper.getAdvicetime());
        assertEquals(0, paper.getSubjectnum());
        assertEquals(0, paper.getPointnum());
        assertEquals(0, paper.getBooknum());

        ArgumentCaptor<ExamPaperChapter> chapterCaptor = ArgumentCaptor.forClass(ExamPaperChapter.class);
        verify(chapterMapper).insert(chapterCaptor.capture());
        ExamPaperChapter chapter = chapterCaptor.getValue();
        assertEquals(paper.getId(), chapter.getPaperid());
        assertEquals("默认章节", chapter.getName());
        assertEquals("NONE", chapter.getParentid());
        assertEquals(chapter.getId(), chapter.getTreecode());
        assertEquals(1, chapter.getSort());
    }

    @Test
    void addSubjectUsesCurrentVersionAndUpdatesPaperTotals() {
        ExamSubject subject = subject("subject-1", "version-current");
        ExamPaper paper = paper("paper-1", 2, 10);
        when(subjectMapper.selectById("subject-1")).thenReturn(subject);
        when(paperSubjectMapper.selectCount(any())).thenReturn(2L);
        when(paperMapper.selectById("paper-1")).thenReturn(paper);

        service.addSubject("paper-1", "subject-1", null, "chapter-1", null, 5);

        ArgumentCaptor<ExamPaperSubject> paperSubjectCaptor = ArgumentCaptor.forClass(ExamPaperSubject.class);
        verify(paperSubjectMapper).insert(paperSubjectCaptor.capture());
        ExamPaperSubject paperSubject = paperSubjectCaptor.getValue();
        assertEquals("paper-1", paperSubject.getPaperid());
        assertEquals("subject-1", paperSubject.getSubjectid());
        assertEquals("version-current", paperSubject.getVersionid());
        assertEquals("chapter-1", paperSubject.getChapterid());
        assertEquals(3, paperSubject.getSort());
        assertEquals(5, paperSubject.getPoint());

        assertEquals(3, paper.getSubjectnum());
        assertEquals(15, paper.getPointnum());
        verify(paperMapper).updateById(paper);
    }

    @Test
    void addSubjectRejectsMissingSubject() {
        when(subjectMapper.selectById("missing")).thenReturn(null);

        BizException error = assertThrows(BizException.class,
                () -> service.addSubject("paper-1", "missing", null, "chapter-1", null, 5));

        assertEquals(404, error.getCode());
        verifyNoInteractions(paperMapper, paperSubjectMapper);
    }

    @Test
    void removeSubjectDeletesLinksAndUpdatesPaperTotals() {
        ExamPaper paper = paper("paper-1", 2, 10);
        ExamPaperSubject existing = new ExamPaperSubject();
        existing.setPaperid("paper-1");
        existing.setSubjectid("subject-1");
        existing.setPoint(4);
        when(paperSubjectMapper.selectList(any())).thenReturn(List.of(existing));
        when(paperMapper.selectById("paper-1")).thenReturn(paper);

        service.removeSubject("paper-1", "subject-1");

        verify(paperSubjectMapper).delete(any());
        assertEquals(1, paper.getSubjectnum());
        assertEquals(6, paper.getPointnum());
        verify(paperMapper).updateById(paper);
    }

    @Test
    void deleteBatchRejectsEmptySelection() {
        BizException error = assertThrows(BizException.class,
                () -> service.deleteBatch(List.of(), "teacher-1"));

        assertEquals("请选择要删除的试卷", error.getMessage());
        verify(paperSubjectMapper, never()).delete(any());
        verify(chapterMapper, never()).delete(any());
        verify(paperMapper, never()).deleteById(any());
    }

    private static ExamSubject subject(String id, String versionId) {
        ExamSubject subject = new ExamSubject();
        subject.setId(id);
        subject.setVersionid(versionId);
        return subject;
    }

    private static ExamPaper paper(String id, int subjectnum, int pointnum) {
        ExamPaper paper = new ExamPaper();
        paper.setId(id);
        paper.setSubjectnum(subjectnum);
        paper.setPointnum(pointnum);
        return paper;
    }
}
