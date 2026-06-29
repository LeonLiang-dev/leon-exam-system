package com.wts.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wts.common.exception.BizException;
import com.wts.common.result.PageResult;
import com.wts.exam.dto.AnswerDTO;
import com.wts.exam.dto.SubjectDTO;
import com.wts.exam.dto.SubjectQueryDTO;
import com.wts.exam.entity.ExamSubject;
import com.wts.exam.entity.ExamSubjectAnswer;
import com.wts.exam.entity.ExamSubjectVersion;
import com.wts.exam.mapper.ExamSubjectAnswerMapper;
import com.wts.exam.mapper.ExamSubjectMapper;
import com.wts.exam.mapper.ExamSubjectVersionMapper;
import com.wts.exam.service.SubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubjectServiceImpl implements SubjectService {

    private final ExamSubjectMapper subjectMapper;
    private final ExamSubjectVersionMapper versionMapper;
    private final ExamSubjectAnswerMapper answerMapper;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Override
    public PageResult<ExamSubject> list(SubjectQueryDTO query) {
        LambdaQueryWrapper<ExamSubject> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like(ExamSubject::getIntroduction, query.getKeyword());
        }
        if (StringUtils.hasText(query.getTypeid())) {
            wrapper.eq(ExamSubject::getTypeid, query.getTypeid());
        }
        if (StringUtils.hasText(query.getPstate())) {
            wrapper.eq(ExamSubject::getPstate, query.getPstate());
        }
        // Filter by tiptype via version subquery is complex; for now filter by typeid
        wrapper.orderByDesc(ExamSubject::getUuid);

        Page<ExamSubject> page = subjectMapper.selectPage(
                new Page<>(query.getPage(), query.getSize()), wrapper);
        return PageResult.of(page);
    }

    @Override
    public ExamSubject getDetail(String id) {
        ExamSubject subject = subjectMapper.selectById(id);
        if (subject == null) throw BizException.notFound("题目");
        return subject;
    }

    @Override
    @Transactional
    public ExamSubject create(SubjectDTO dto, String operatorId, String operatorName) {
        String now = LocalDateTime.now().format(FMT);
        String subjectId = UUID.randomUUID().toString().replace("-", "");
        String versionId = UUID.randomUUID().toString().replace("-", "");

        // Create subject
        ExamSubject subject = new ExamSubject();
        subject.setId(subjectId);
        subject.setTypeid(dto.getTypeid());
        subject.setVersionid(versionId);
        subject.setPstate("1");
        subject.setIntroduction(dto.getTipstr());
        subject.setLevel(dto.getLevel() != null ? dto.getLevel() : 1);
        subject.setPoint(dto.getPoint() != null ? dto.getPoint() : 1);
        subject.setUuid(subjectId);
        subject.setPraisenum(0);
        subject.setCommentnum(0);
        subject.setAnalysisnum(0);
        subject.setDonum(0);
        subject.setRightnum(0);
        subjectMapper.insert(subject);

        // Create version
        ExamSubjectVersion version = new ExamSubjectVersion();
        version.setId(versionId);
        version.setSubjectid(subjectId);
        version.setTiptype(dto.getTiptype());
        version.setTipstr(dto.getTipstr());
        version.setTipnote(dto.getTipnote());
        version.setPcontent(dto.getPcontent());
        version.setCtime(now);
        version.setCuser(operatorId);
        version.setCusername(operatorName);
        version.setPstate("1");
        versionMapper.insert(version);

        // Create answers
        if (dto.getAnswers() != null) {
            for (AnswerDTO ansDto : dto.getAnswers()) {
                ExamSubjectAnswer answer = new ExamSubjectAnswer();
                answer.setId(UUID.randomUUID().toString().replace("-", ""));
                answer.setVersionid(versionId);
                answer.setAnswer(ansDto.getAnswer());
                answer.setAnswernote(ansDto.getAnswernote());
                answer.setRightanswer(ansDto.getRightanswer());
                answer.setSort(ansDto.getSort());
                answer.setPointweight(ansDto.getPointweight());
                answer.setGroupno(ansDto.getGroupno());
                answer.setPcontent(ansDto.getPcontent());
                answer.setPstate("1");
                answer.setCuser(operatorId);
                answer.setCusername(operatorName);
                answer.setCtime(now);
                answer.setUuid(answer.getId());
                answerMapper.insert(answer);
            }
        }

        return subject;
    }

    @Override
    @Transactional
    public ExamSubject update(String id, SubjectDTO dto, String operatorId, String operatorName) {
        ExamSubject subject = subjectMapper.selectById(id);
        if (subject == null) throw BizException.notFound("题目");

        String now = LocalDateTime.now().format(FMT);
        String newVersionId = UUID.randomUUID().toString().replace("-", "");

        // Create new version
        ExamSubjectVersion version = new ExamSubjectVersion();
        version.setId(newVersionId);
        version.setSubjectid(id);
        version.setTiptype(dto.getTiptype());
        version.setTipstr(dto.getTipstr());
        version.setTipnote(dto.getTipnote());
        version.setPcontent(dto.getPcontent());
        version.setCtime(now);
        version.setCuser(operatorId);
        version.setCusername(operatorName);
        version.setPstate("1");
        versionMapper.insert(version);

        // Create new answers for new version
        if (dto.getAnswers() != null) {
            for (AnswerDTO ansDto : dto.getAnswers()) {
                ExamSubjectAnswer answer = new ExamSubjectAnswer();
                answer.setId(UUID.randomUUID().toString().replace("-", ""));
                answer.setVersionid(newVersionId);
                answer.setAnswer(ansDto.getAnswer());
                answer.setAnswernote(ansDto.getAnswernote());
                answer.setRightanswer(ansDto.getRightanswer());
                answer.setSort(ansDto.getSort());
                answer.setPointweight(ansDto.getPointweight());
                answer.setGroupno(ansDto.getGroupno());
                answer.setPcontent(ansDto.getPcontent());
                answer.setPstate("1");
                answer.setCuser(operatorId);
                answer.setCusername(operatorName);
                answer.setCtime(now);
                answer.setUuid(answer.getId());
                answerMapper.insert(answer);
            }
        }

        // Update subject to point to new version
        subject.setVersionid(newVersionId);
        if (dto.getTypeid() != null) subject.setTypeid(dto.getTypeid());
        if (dto.getTipstr() != null) subject.setIntroduction(dto.getTipstr());
        if (dto.getLevel() != null) subject.setLevel(dto.getLevel());
        if (dto.getPoint() != null) subject.setPoint(dto.getPoint());
        subjectMapper.updateById(subject);

        return subject;
    }

    @Override
    @Transactional
    public void delete(String id, String operatorId) {
        ExamSubject subject = subjectMapper.selectById(id);
        if (subject == null) throw BizException.notFound("题目");
        subjectMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void deleteBatch(List<String> ids, String operatorId) {
        if (ids == null || ids.isEmpty()) {
            throw BizException.fail("请选择要删除的题目");
        }
        for (String id : ids) {
            delete(id, operatorId);
        }
    }

    @Override
    public ExamSubjectVersion getCurrentVersion(String subjectId) {
        ExamSubject subject = subjectMapper.selectById(subjectId);
        if (subject == null) throw BizException.notFound("题目");
        return versionMapper.selectById(subject.getVersionid());
    }

    @Override
    public List<ExamSubjectAnswer> getVersionAnswers(String versionId) {
        return answerMapper.selectList(
                new LambdaQueryWrapper<ExamSubjectAnswer>()
                        .eq(ExamSubjectAnswer::getVersionid, versionId)
                        .orderByAsc(ExamSubjectAnswer::getSort)
        );
    }
}
