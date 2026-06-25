package com.wts.exam.service.impl;

import com.wts.common.exception.BizException;
import com.wts.exam.entity.ExamPaper;
import com.wts.exam.entity.ExamPaperChapter;
import com.wts.exam.entity.ExamPaperSubject;
import com.wts.exam.entity.ExamRandomItem;
import com.wts.exam.entity.ExamRandomStep;
import com.wts.exam.entity.ExamSubject;
import com.wts.exam.entity.ExamSubjectVersion;
import com.wts.exam.mapper.ExamPaperChapterMapper;
import com.wts.exam.mapper.ExamPaperMapper;
import com.wts.exam.mapper.ExamPaperSubjectMapper;
import com.wts.exam.mapper.ExamRandomItemMapper;
import com.wts.exam.mapper.ExamRandomStepMapper;
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
class RandomServiceImplTest {

    @Mock
    private ExamRandomItemMapper itemMapper;
    @Mock
    private ExamRandomStepMapper stepMapper;
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

    private RandomServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RandomServiceImpl(
                itemMapper,
                stepMapper,
                paperMapper,
                chapterMapper,
                paperSubjectMapper,
                subjectMapper,
                versionMapper
        );
    }

    @Test
    void generatePapersRejectsMissingItem() {
        when(itemMapper.selectById("missing")).thenReturn(null);

        BizException error = assertThrows(BizException.class,
                () -> service.generatePapers("missing", 1, "teacher-1"));

        assertEquals(404, error.getCode());
        verifyNoInteractions(paperMapper, chapterMapper, paperSubjectMapper);
    }

    @Test
    void generatePapersRejectsEmptySteps() {
        when(itemMapper.selectById("item-1")).thenReturn(item("随机规则"));
        when(stepMapper.selectList(any())).thenReturn(List.of());

        BizException error = assertThrows(BizException.class,
                () -> service.generatePapers("item-1", 1, "teacher-1"));

        assertEquals("规则步骤为空", error.getMessage());
        verify(paperMapper, never()).insert(any(ExamPaper.class));
    }

    @Test
    void generatePapersCreatesPaperChapterSubjectLinksAndTotals() {
        ExamSubjectVersion listedVersion = version("version-1", "subject-1", "2", "20260101000000");
        when(itemMapper.selectById("item-1")).thenReturn(item("随机规则"));
        when(stepMapper.selectList(any())).thenReturn(List.of(step("步骤一", "2", "type-1", 1, 5)));
        when(versionMapper.selectList(any())).thenReturn(List.of(listedVersion));
        when(subjectMapper.selectList(any())).thenReturn(List.of(subject("subject-1", "type-1")));
        when(versionMapper.selectOne(any())).thenReturn(listedVersion);

        List<String> paperIds = service.generatePapers("item-1", 1, "teacher-1");

        assertEquals(1, paperIds.size());
        ArgumentCaptor<ExamPaper> paperCaptor = ArgumentCaptor.forClass(ExamPaper.class);
        verify(paperMapper).insert(paperCaptor.capture());
        ExamPaper paper = paperCaptor.getValue();
        assertEquals(paper.getId(), paperIds.get(0));
        assertEquals("随机规则-1", paper.getName());
        assertEquals("teacher-1", paper.getCuser());
        assertEquals("1", paper.getPstate());

        ArgumentCaptor<ExamPaperChapter> chapterCaptor = ArgumentCaptor.forClass(ExamPaperChapter.class);
        verify(chapterMapper).insert(chapterCaptor.capture());
        ExamPaperChapter chapter = chapterCaptor.getValue();
        assertEquals(paper.getId(), chapter.getPaperid());
        assertEquals("默认章节", chapter.getName());

        ArgumentCaptor<ExamPaperSubject> paperSubjectCaptor = ArgumentCaptor.forClass(ExamPaperSubject.class);
        verify(paperSubjectMapper).insert(paperSubjectCaptor.capture());
        ExamPaperSubject paperSubject = paperSubjectCaptor.getValue();
        assertEquals(paper.getId(), paperSubject.getPaperid());
        assertEquals("subject-1", paperSubject.getSubjectid());
        assertEquals("version-1", paperSubject.getVersionid());
        assertEquals(chapter.getId(), paperSubject.getChapterid());
        assertEquals(1, paperSubject.getSort());
        assertEquals(5, paperSubject.getPoint());

        assertEquals(1, paper.getSubjectnum());
        assertEquals(5, paper.getPointnum());
        verify(paperMapper).updateById(paper);
    }

    @Test
    void deleteItemsBatchRejectsEmptySelection() {
        BizException error = assertThrows(BizException.class,
                () -> service.deleteItemsBatch(List.of()));

        assertEquals("请选择要删除的随机规则", error.getMessage());
        verifyNoInteractions(stepMapper, itemMapper);
    }

    @Test
    void addStepPersistsRuleConfiguration() {
        com.wts.exam.dto.RandomItemDTO.RandomStepDTO dto = new com.wts.exam.dto.RandomItemDTO.RandomStepDTO();
        dto.setName("单选题");
        dto.setSort(2);
        dto.setSubnum(3);
        dto.setSubpoint(4);
        dto.setTiptype("2");
        dto.setTypeid("type-1");
        dto.setKnowid("know-1");

        ExamRandomStep step = service.addStep("item-1", dto);

        ArgumentCaptor<ExamRandomStep> stepCaptor = ArgumentCaptor.forClass(ExamRandomStep.class);
        verify(stepMapper).insert(stepCaptor.capture());
        assertEquals(step, stepCaptor.getValue());
        assertNotNull(step.getId());
        assertEquals("item-1", step.getItemid());
        assertEquals("单选题", step.getName());
        assertEquals(2, step.getSort());
        assertEquals(3, step.getSubnum());
        assertEquals(4, step.getSubpoint());
        assertEquals("2", step.getTiptype());
        assertEquals("type-1", step.getTypeid());
        assertEquals("know-1", step.getKnowid());
    }

    private static ExamRandomItem item(String name) {
        ExamRandomItem item = new ExamRandomItem();
        item.setId("item-1");
        item.setName(name);
        return item;
    }

    private static ExamRandomStep step(String name, String tiptype, String typeid, int subnum, int subpoint) {
        ExamRandomStep step = new ExamRandomStep();
        step.setId("step-1");
        step.setName(name);
        step.setTiptype(tiptype);
        step.setTypeid(typeid);
        step.setSubnum(subnum);
        step.setSubpoint(subpoint);
        step.setSort(1);
        return step;
    }

    private static ExamSubjectVersion version(String id, String subjectId, String tiptype, String ctime) {
        ExamSubjectVersion version = new ExamSubjectVersion();
        version.setId(id);
        version.setSubjectid(subjectId);
        version.setTiptype(tiptype);
        version.setCtime(ctime);
        version.setPstate("1");
        return version;
    }

    private static ExamSubject subject(String id, String typeid) {
        ExamSubject subject = new ExamSubject();
        subject.setId(id);
        subject.setTypeid(typeid);
        subject.setPstate("1");
        return subject;
    }
}
