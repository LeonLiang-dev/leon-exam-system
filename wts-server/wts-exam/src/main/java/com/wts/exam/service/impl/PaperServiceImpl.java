package com.wts.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wts.common.exception.BizException;
import com.wts.common.result.PageResult;
import com.wts.exam.dto.PaperDTO;
import com.wts.exam.entity.*;
import com.wts.exam.mapper.*;
import com.wts.exam.service.PaperService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaperServiceImpl implements PaperService {
    private final ExamPaperMapper paperMapper;
    private final ExamPaperChapterMapper chapterMapper;
    private final ExamPaperSubjectMapper paperSubjectMapper;
    private final ExamSubjectMapper subjectMapper;
    private final ExamSubjectVersionMapper versionMapper;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Override
    public PageResult<ExamPaper> list(int page, int size, String keyword) {
        LambdaQueryWrapper<ExamPaper> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(ExamPaper::getName, keyword);
        }
        wrapper.orderByDesc(ExamPaper::getCtime);
        return PageResult.of(paperMapper.selectPage(new Page<>(page, size), wrapper));
    }

    @Override
    public ExamPaper getDetail(String id) {
        ExamPaper paper = paperMapper.selectById(id);
        if (paper == null) throw BizException.notFound("试卷");
        return paper;
    }

    @Override
    @Transactional
    public ExamPaper create(PaperDTO dto, String operatorId, String operatorName) {
        ExamPaper paper = new ExamPaper();
        paper.setId(UUID.randomUUID().toString().replace("-", ""));
        paper.setUuid(paper.getId());
        paper.setName(dto.getName());
        paper.setPapernote(dto.getPapernote());
        paper.setExamtypeid(dto.getExamtypeid());
        paper.setAdvicetime(dto.getAdvicetime() != null ? dto.getAdvicetime() : 60);
        paper.setKnowid(dto.getKnowid());
        paper.setPstate("1");
        paper.setSubjectnum(0);
        paper.setPointnum(0);
        paper.setCompletetnum(0);
        paper.setAvgpoint(0);
        paper.setToppoint(0);
        paper.setLowpoint(0);
        paper.setBooknum(0);
        String now = LocalDateTime.now().format(FMT);
        paper.setCtime(now);
        paper.setEtime(now);
        paperMapper.insert(paper);

        // Create default chapter
        ExamPaperChapter chapter = new ExamPaperChapter();
        chapter.setId(UUID.randomUUID().toString().replace("-", ""));
        chapter.setPaperid(paper.getId());
        chapter.setName("默认章节");
        chapter.setParentid("NONE");
        chapter.setTreecode(chapter.getId());
        chapter.setSort(1);
        chapter.setStype("1");
        chapter.setPtype("2");
        chapter.setInitpoint(0);
        chapterMapper.insert(chapter);

        return paper;
    }

    @Override
    @Transactional
    public ExamPaper update(String id, PaperDTO dto, String operatorId) {
        ExamPaper paper = paperMapper.selectById(id);
        if (paper == null) throw BizException.notFound("试卷");
        paper.setName(dto.getName());
        paper.setPapernote(dto.getPapernote());
        paper.setExamtypeid(dto.getExamtypeid());
        paper.setAdvicetime(dto.getAdvicetime());
        paperMapper.updateById(paper);
        return paper;
    }

    @Override
    @Transactional
    public void delete(String id, String operatorId) {
        paperSubjectMapper.delete(new LambdaQueryWrapper<ExamPaperSubject>().eq(ExamPaperSubject::getPaperid, id));
        chapterMapper.delete(new LambdaQueryWrapper<ExamPaperChapter>().eq(ExamPaperChapter::getPaperid, id));
        paperMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void deleteBatch(List<String> ids, String operatorId) {
        if (ids == null || ids.isEmpty()) {
            throw BizException.fail("请选择要删除的试卷");
        }
        for (String id : ids) {
            delete(id, operatorId);
        }
    }

    @Override
    @Transactional
    public void addSubject(String paperId, String subjectId, String versionId, String chapterId, Integer sort, Integer point) {
        ExamSubject subject = subjectMapper.selectById(subjectId);
        if (subject == null) throw BizException.notFound("题目");
        ExamPaperSubject ps = new ExamPaperSubject();
        ps.setId(UUID.randomUUID().toString().replace("-", ""));
        ps.setPaperid(paperId);
        ps.setSubjectid(subjectId);
        ps.setVersionid(versionId != null ? versionId : subject.getVersionid());
        ps.setChapterid(chapterId);
        ps.setPoint(point != null ? point : 0);
        if (sort != null) {
            ps.setSort(sort);
        } else {
            Long count = paperSubjectMapper.selectCount(
                    new LambdaQueryWrapper<ExamPaperSubject>().eq(ExamPaperSubject::getPaperid, paperId));
            ps.setSort(count.intValue() + 1);
        }
        paperSubjectMapper.insert(ps);

        // Update paper subject count
        ExamPaper paper = paperMapper.selectById(paperId);
        if (paper != null) {
            paper.setSubjectnum(paper.getSubjectnum() + 1);
            paper.setPointnum(paper.getPointnum() + ps.getPoint());
            paperMapper.updateById(paper);
        }
    }

    @Override
    @Transactional
    public void removeSubject(String paperId, String subjectId) {
        // Get the points of the subject being removed before deleting
        List<ExamPaperSubject> existing = paperSubjectMapper.selectList(
                new LambdaQueryWrapper<ExamPaperSubject>()
                        .eq(ExamPaperSubject::getPaperid, paperId)
                        .eq(ExamPaperSubject::getSubjectid, subjectId));
        int removedPoints = existing.stream().mapToInt(ps -> ps.getPoint() != null ? ps.getPoint().intValue() : 0).sum();

        paperSubjectMapper.delete(new LambdaQueryWrapper<ExamPaperSubject>()
                .eq(ExamPaperSubject::getPaperid, paperId)
                .eq(ExamPaperSubject::getSubjectid, subjectId));
        ExamPaper paper = paperMapper.selectById(paperId);
        if (paper != null) {
            paper.setSubjectnum(Math.max(0, paper.getSubjectnum() - 1));
            paper.setPointnum(Math.max(0, paper.getPointnum() - removedPoints));
            paperMapper.updateById(paper);
        }
    }

    @Override
    public List<ExamPaperChapter> getChapters(String paperId) {
        return chapterMapper.selectList(new LambdaQueryWrapper<ExamPaperChapter>()
                .eq(ExamPaperChapter::getPaperid, paperId)
                .orderByAsc(ExamPaperChapter::getSort));
    }

    @Override
    @Transactional
    public ExamPaperChapter addChapter(String paperId, String name, Integer sort) {
        ExamPaperChapter chapter = new ExamPaperChapter();
        chapter.setId(UUID.randomUUID().toString().replace("-", ""));
        chapter.setPaperid(paperId);
        chapter.setName(name);
        chapter.setParentid("NONE");
        chapter.setTreecode(chapter.getId());
        chapter.setSort(sort != null ? sort : 1);
        chapter.setStype("1");
        chapter.setPtype("2");
        chapter.setInitpoint(0);
        chapterMapper.insert(chapter);
        return chapter;
    }

    @Override
    @Transactional
    public void removeChapter(String chapterId) {
        chapterMapper.deleteById(chapterId);
    }

    @Override
    public List<ExamPaperSubject> getPaperSubjects(String paperId) {
        return paperSubjectMapper.selectList(new LambdaQueryWrapper<ExamPaperSubject>()
                .eq(ExamPaperSubject::getPaperid, paperId)
                .orderByAsc(ExamPaperSubject::getSort));
    }
}
