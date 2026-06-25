package com.wts.exam.service;

import com.wts.exam.entity.ExamSubjectType;
import java.util.List;

public interface SubjectTypeService {
    List<ExamSubjectType> getTree();
    ExamSubjectType create(ExamSubjectType type, String operatorId);
    ExamSubjectType update(String id, ExamSubjectType type, String operatorId);
    void delete(String id, String operatorId);
    void deleteBatch(List<String> ids, String operatorId);
}
