package com.wts.exam.service;

import com.wts.common.result.PageResult;
import com.wts.exam.dto.PaperDTO;
import com.wts.exam.entity.ExamPaper;
import com.wts.exam.entity.ExamPaperChapter;
import com.wts.exam.entity.ExamPaperSubject;

import java.util.List;

public interface PaperService {
    PageResult<ExamPaper> list(int page, int size, String keyword);
    ExamPaper getDetail(String id);
    ExamPaper create(PaperDTO dto, String operatorId, String operatorName);
    ExamPaper update(String id, PaperDTO dto, String operatorId);
    void delete(String id, String operatorId);
    void deleteBatch(List<String> ids, String operatorId);
    void addSubject(String paperId, String subjectId, String versionId, String chapterId, Integer sort, Integer point);
    void removeSubject(String paperId, String subjectId);
    List<ExamPaperChapter> getChapters(String paperId);
    ExamPaperChapter addChapter(String paperId, String name, Integer sort);
    void removeChapter(String chapterId);
    List<ExamPaperSubject> getPaperSubjects(String paperId);
}
