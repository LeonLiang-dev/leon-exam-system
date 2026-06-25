package com.wts.exam.service;

import com.wts.common.result.PageResult;
import com.wts.exam.dto.SubjectDTO;
import com.wts.exam.dto.SubjectQueryDTO;
import com.wts.exam.entity.ExamSubject;
import com.wts.exam.entity.ExamSubjectAnswer;
import com.wts.exam.entity.ExamSubjectVersion;

import java.util.List;

public interface SubjectService {
    PageResult<ExamSubject> list(SubjectQueryDTO query);
    ExamSubject getDetail(String id);
    ExamSubject create(SubjectDTO dto, String operatorId, String operatorName);
    ExamSubject update(String id, SubjectDTO dto, String operatorId, String operatorName);
    void delete(String id, String operatorId);
    void deleteBatch(List<String> ids, String operatorId);
    ExamSubjectVersion getCurrentVersion(String subjectId);
    List<ExamSubjectAnswer> getVersionAnswers(String versionId);
}
