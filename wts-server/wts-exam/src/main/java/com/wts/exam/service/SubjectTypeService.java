package com.wts.exam.service;

import com.wts.exam.entity.ExamSubjectType;
import java.util.List;

public interface SubjectTypeService {
    List<ExamSubjectType> getTree();

    /** 按创建人可见范围过滤的分类树；ownerIds 为 null 表示全部 */
    List<ExamSubjectType> getTree(List<String> ownerIds);

    ExamSubjectType create(ExamSubjectType type, String operatorId);
    ExamSubjectType update(String id, ExamSubjectType type, String operatorId);
    void delete(String id, String operatorId);
    void deleteBatch(List<String> ids, String operatorId);
}
