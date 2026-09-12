package com.wts.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wts.auth.entity.SysOrganization;
import com.wts.auth.entity.SysUserorg;
import com.wts.auth.mapper.SysOrganizationMapper;
import com.wts.auth.mapper.SysUserorgMapper;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubjectServiceImpl implements SubjectService {

    private final ExamSubjectMapper subjectMapper;
    private final ExamSubjectVersionMapper versionMapper;
    private final ExamSubjectAnswerMapper answerMapper;
    private final SysUserorgMapper userorgMapper;
    private final SysOrganizationMapper organizationMapper;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Override
    public PageResult<ExamSubject> list(SubjectQueryDTO query) {
        return list(query, null);
    }

    @Override
    public PageResult<ExamSubject> list(SubjectQueryDTO query, List<String> ownerIds) {
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
        if (StringUtils.hasText(query.getOrgId())) {
            Set<String> orgUserIds = usersInOrgSubtree(query.getOrgId());
            applyOwnerScope(wrapper, mergeScopes(ownerIds, orgUserIds));
        } else {
            applyOwnerScope(wrapper, ownerIds);
        }
        // Filter by tiptype via version subquery is complex; for now filter by typeid
        wrapper.orderByDesc(ExamSubject::getUuid);

        Page<ExamSubject> page = subjectMapper.selectPage(
                new Page<>(query.getPage(), query.getSize()), wrapper);
        fillOrgNames(page.getRecords());
        return PageResult.of(page);
    }

    /** 创建人范围过滤：题目主表无创建人字段，通过其当前版本(version.cuser)反查。 */
    private void applyOwnerScope(LambdaQueryWrapper<ExamSubject> wrapper, List<String> ownerIds) {
        if (ownerIds == null) {
            return;
        }
        if (ownerIds.isEmpty()) {
            wrapper.eq(ExamSubject::getUuid, "__NONE__");
            return;
        }
        List<String> versionIds = versionMapper.selectList(
                        new LambdaQueryWrapper<ExamSubjectVersion>()
                                .in(ExamSubjectVersion::getCuser, ownerIds))
                .stream().map(ExamSubjectVersion::getId).distinct().toList();
        if (versionIds.isEmpty()) {
            wrapper.eq(ExamSubject::getUuid, "__NONE__");
        } else {
            wrapper.in(ExamSubject::getVersionid, versionIds);
        }
    }

    /** 组织范围与可见范围的交集：orgUserIds 为空时保持原范围 */
    private List<String> mergeScopes(List<String> visibleOwnerIds, Set<String> orgUserIds) {
        if (visibleOwnerIds == null) {
            return new ArrayList<>(orgUserIds);
        }
        return visibleOwnerIds.stream().filter(orgUserIds::contains).collect(Collectors.toList());
    }

    /** 展开组织节点及其全部子孙节点 → 归属用户 id 集合 */
    private Set<String> usersInOrgSubtree(String orgId) {
        Set<String> result = new HashSet<>();
        List<SysOrganization> all = organizationMapper.selectList(null);
        if (all.isEmpty()) {
            return result;
        }
        Map<String, List<SysOrganization>> childrenByParent = new HashMap<>();
        for (SysOrganization org : all) {
            childrenByParent.computeIfAbsent(org.getParentid(), k -> new ArrayList<>()).add(org);
        }
        collectSubtree(orgId, childrenByParent, result);
        if (result.isEmpty()) {
            return result;
        }
        return userorgMapper.selectList(
                        new LambdaQueryWrapper<SysUserorg>()
                                .in(SysUserorg::getOrganizationid, result))
                .stream()
                .map(SysUserorg::getUserid)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
    }

    private void collectSubtree(String nodeId, Map<String, List<SysOrganization>> childrenByParent, Set<String> out) {
        if (nodeId == null || !out.add(nodeId)) {
            return;
        }
        for (SysOrganization child : childrenByParent.getOrDefault(nodeId, List.of())) {
            collectSubtree(child.getId(), childrenByParent, out);
        }
    }

    /** 批量填充题目所属教研室名称（按当前版本创建人 → 组织归属） */
    private void fillOrgNames(List<ExamSubject> subjects) {
        if (subjects == null || subjects.isEmpty()) {
            return;
        }
        List<String> versionIds = subjects.stream()
                .map(ExamSubject::getVersionid)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (versionIds.isEmpty()) {
            return;
        }
        Map<String, String> versionToUser = versionMapper.selectList(
                        new LambdaQueryWrapper<ExamSubjectVersion>()
                                .in(ExamSubjectVersion::getId, versionIds))
                .stream()
                .collect(Collectors.toMap(ExamSubjectVersion::getId,
                        v -> StringUtils.hasText(v.getCuser()) ? v.getCuser() : "",
                        (a, b) -> a));
        Map<String, String> orgByUser = userorgMapper.selectList(
                        new LambdaQueryWrapper<SysUserorg>()
                                .in(SysUserorg::getUserid, new HashSet<>(versionToUser.values())))
                .stream()
                .collect(Collectors.toMap(SysUserorg::getUserid, SysUserorg::getOrganizationid, (a, b) -> a));
        if (orgByUser.isEmpty()) {
            return;
        }
        Map<String, String> nameByOrg = organizationMapper.selectList(
                        new LambdaQueryWrapper<SysOrganization>()
                                .in(SysOrganization::getId, new HashSet<>(orgByUser.values())))
                .stream()
                .collect(Collectors.toMap(SysOrganization::getId, SysOrganization::getName, (a, b) -> a));
        subjects.forEach(s -> {
            String userId = versionToUser.getOrDefault(s.getVersionid(), "");
            String orgId = userId != null ? orgByUser.get(userId) : null;
            s.setOrgName(orgId != null ? nameByOrg.get(orgId) : null);
        });
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
        List<AnswerDTO> normalizedAnswers = SubjectRuleValidator.validateAndNormalize(dto);

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
        subject.setPoint(dto.getPoint());
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
        version.setTipstr(valueOrEmpty(dto.getTipstr()));
        version.setTipnote(valueOrEmpty(dto.getTipnote()));
        version.setPcontent(valueOrEmpty(dto.getPcontent()));
        version.setCtime(now);
        version.setCuser(valueOrEmpty(operatorId));
        version.setCusername(valueOrEmpty(operatorName));
        version.setPstate("1");
        version.setAnswered("0");
        versionMapper.insert(version);

        // Create answers
        if (normalizedAnswers != null) {
            for (AnswerDTO ansDto : normalizedAnswers) {
                ExamSubjectAnswer answer = new ExamSubjectAnswer();
                answer.setId(UUID.randomUUID().toString().replace("-", ""));
                answer.setVersionid(versionId);
                answer.setAnswer(valueOrEmpty(ansDto.getAnswer()));
                answer.setAnswernote(valueOrEmpty(ansDto.getAnswernote()));
                answer.setRightanswer(valueOrDefault(ansDto.getRightanswer(), "0"));
                answer.setSort(ansDto.getSort() != null ? ansDto.getSort() : 1);
                answer.setPointweight(ansDto.getPointweight() != null ? ansDto.getPointweight() : 0);
                answer.setGroupno(ansDto.getGroupno());
                answer.setPcontent(valueOrEmpty(ansDto.getPcontent()));
                answer.setPstate("1");
                answer.setCuser(valueOrEmpty(operatorId));
                answer.setCusername(valueOrEmpty(operatorName));
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
        List<AnswerDTO> normalizedAnswers = SubjectRuleValidator.validateAndNormalize(dto);

        String now = LocalDateTime.now().format(FMT);
        String newVersionId = UUID.randomUUID().toString().replace("-", "");

        // Create new version
        ExamSubjectVersion version = new ExamSubjectVersion();
        version.setId(newVersionId);
        version.setSubjectid(id);
        version.setTiptype(dto.getTiptype());
        version.setTipstr(valueOrEmpty(dto.getTipstr()));
        version.setTipnote(valueOrEmpty(dto.getTipnote()));
        version.setPcontent(valueOrEmpty(dto.getPcontent()));
        version.setCtime(now);
        version.setCuser(valueOrEmpty(operatorId));
        version.setCusername(valueOrEmpty(operatorName));
        version.setPstate("1");
        version.setAnswered("0");
        versionMapper.insert(version);

        // Create new answers for new version
        if (normalizedAnswers != null) {
            for (AnswerDTO ansDto : normalizedAnswers) {
                ExamSubjectAnswer answer = new ExamSubjectAnswer();
                answer.setId(UUID.randomUUID().toString().replace("-", ""));
                answer.setVersionid(newVersionId);
                answer.setAnswer(valueOrEmpty(ansDto.getAnswer()));
                answer.setAnswernote(valueOrEmpty(ansDto.getAnswernote()));
                answer.setRightanswer(valueOrDefault(ansDto.getRightanswer(), "0"));
                answer.setSort(ansDto.getSort() != null ? ansDto.getSort() : 1);
                answer.setPointweight(ansDto.getPointweight() != null ? ansDto.getPointweight() : 0);
                answer.setGroupno(ansDto.getGroupno());
                answer.setPcontent(valueOrEmpty(ansDto.getPcontent()));
                answer.setPstate("1");
                answer.setCuser(valueOrEmpty(operatorId));
                answer.setCusername(valueOrEmpty(operatorName));
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
        subject.setPoint(dto.getPoint());
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

    private String valueOrEmpty(String value) {
        return value != null ? value : "";
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value != null && !value.isBlank() ? value : defaultValue;
    }
}
