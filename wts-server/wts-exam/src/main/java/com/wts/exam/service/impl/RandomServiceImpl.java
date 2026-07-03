package com.wts.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wts.common.exception.BizException;
import com.wts.exam.dto.RandomItemDTO;
import com.wts.exam.entity.*;
import com.wts.exam.enums.QuestionTypeRules;
import com.wts.exam.mapper.*;
import com.wts.exam.service.RandomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RandomServiceImpl implements RandomService {

    private final ExamRandomItemMapper itemMapper;
    private final ExamRandomStepMapper stepMapper;
    private final ExamPaperMapper paperMapper;
    private final ExamPaperChapterMapper chapterMapper;
    private final ExamPaperSubjectMapper paperSubjectMapper;
    private final ExamSubjectMapper subjectMapper;
    private final ExamSubjectVersionMapper versionMapper;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Override
    public List<ExamRandomItem> listItems() {
        return itemMapper.selectList(new LambdaQueryWrapper<ExamRandomItem>().orderByDesc(ExamRandomItem::getId));
    }

    @Override
    @Transactional
    public ExamRandomItem createItem(RandomItemDTO dto, String operatorId) {
        ExamRandomItem item = new ExamRandomItem();
        item.setId(UUID.randomUUID().toString().replace("-", ""));
        item.setName(dto.getName());
        item.setCuser(valueOrEmpty(operatorId));
        item.setCtime(LocalDateTime.now().format(FMT));
        item.setPstate("1");
        item.setPcontent("");
        itemMapper.insert(item);
        return item;
    }

    @Override
    @Transactional
    public ExamRandomItem updateItem(String id, RandomItemDTO dto) {
        ExamRandomItem item = itemMapper.selectById(id);
        if (item == null) throw BizException.notFound("随机规则");
        item.setName(dto.getName());
        itemMapper.updateById(item);
        return item;
    }

    @Override
    @Transactional
    public void deleteItem(String id) {
        stepMapper.delete(new LambdaQueryWrapper<ExamRandomStep>().eq(ExamRandomStep::getItemid, id));
        itemMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void deleteItemsBatch(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            throw BizException.fail("请选择要删除的随机规则");
        }
        for (String id : ids) {
            deleteItem(id);
        }
    }

    @Override
    public List<ExamRandomStep> getSteps(String itemId) {
        return stepMapper.selectList(
                new LambdaQueryWrapper<ExamRandomStep>()
                        .eq(ExamRandomStep::getItemid, itemId)
                        .orderByAsc(ExamRandomStep::getSort));
    }

    @Override
    @Transactional
    public ExamRandomStep addStep(String itemId, RandomItemDTO.RandomStepDTO dto) {
        String tiptype = valueOrDefault(dto.getTiptype(), QuestionTypeRules.SINGLE_CHOICE);
        if (!QuestionTypeRules.isSupportedType(tiptype)) {
            throw BizException.fail("不支持的题型");
        }
        ExamRandomStep step = new ExamRandomStep();
        step.setId(UUID.randomUUID().toString().replace("-", ""));
        step.setItemid(itemId);
        step.setName(dto.getName());
        step.setSort(dto.getSort() != null ? dto.getSort() : 1);
        step.setSubnum(dto.getSubnum() != null ? dto.getSubnum() : 0);
        step.setSubpoint(dto.getSubpoint() != null ? dto.getSubpoint() : 0);
        step.setTiptype(tiptype);
        step.setTypeid(dto.getTypeid());
        step.setKnowid(dto.getKnowid());
        step.setPcontent("");
        stepMapper.insert(step);
        return step;
    }

    @Override
    @Transactional
    public ExamRandomStep updateStep(String id, RandomItemDTO.RandomStepDTO dto) {
        ExamRandomStep step = stepMapper.selectById(id);
        if (step == null) throw BizException.notFound("规则步骤");
        step.setName(dto.getName());
        step.setSort(dto.getSort());
        step.setSubnum(dto.getSubnum());
        step.setSubpoint(dto.getSubpoint());
        if (!QuestionTypeRules.isSupportedType(dto.getTiptype())) {
            throw BizException.fail("不支持的题型");
        }
        step.setTiptype(dto.getTiptype());
        step.setTypeid(dto.getTypeid());
        step.setKnowid(dto.getKnowid());
        stepMapper.updateById(step);
        return step;
    }

    @Override
    @Transactional
    public void deleteStep(String id) {
        stepMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void deleteStepsBatch(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            throw BizException.fail("请选择要删除的规则步骤");
        }
        for (String id : ids) {
            deleteStep(id);
        }
    }

    @Override
    @Transactional
    public List<String> generatePapers(String itemId, int count, String operatorId) {
        ExamRandomItem item = itemMapper.selectById(itemId);
        if (item == null) throw BizException.notFound("随机规则");

        List<ExamRandomStep> steps = getSteps(itemId);
        if (steps.isEmpty()) throw BizException.fail("规则步骤为空");

        List<String> paperIds = new ArrayList<>();
        String now = LocalDateTime.now().format(FMT);

        for (int n = 0; n < count; n++) {
            // Create paper
            ExamPaper paper = new ExamPaper();
            String paperId = UUID.randomUUID().toString().replace("-", "");
            paper.setId(paperId);
            paper.setName(item.getName() + "-" + (n + 1));
            paper.setPstate("1");
            paper.setPcontent("");
            paper.setCtime(now);
            paper.setEtime(now);
            paper.setCuser(valueOrEmpty(operatorId));
            paper.setCusername("");
            paper.setEuser(valueOrEmpty(operatorId));
            paper.setEusername("");
            paper.setUuid(paperId);
            paper.setSubjectnum(0);
            paper.setPointnum(0);
            paper.setCompletetnum(0);
            paper.setAvgpoint(0);
            paper.setToppoint(0);
            paper.setLowpoint(0);
            paper.setBooknum(0);
            paper.setAdvicetime(0);
            paperMapper.insert(paper);

            // Create default chapter
            ExamPaperChapter chapter = new ExamPaperChapter();
            chapter.setId(UUID.randomUUID().toString().replace("-", ""));
            chapter.setPaperid(paperId);
            chapter.setName("默认章节");
            chapter.setSort(1);
            chapter.setStype("1");
            chapter.setPtype("2");
            chapter.setInitpoint(0);
            chapter.setParentid("NONE");
            chapter.setTreecode(chapter.getId());
            chapterMapper.insert(chapter);

            int totalSubjects = 0;
            int totalPoints = 0;

            for (ExamRandomStep step : steps) {
                // Find matching subjects
                LambdaQueryWrapper<ExamSubjectVersion> versionWrapper = new LambdaQueryWrapper<ExamSubjectVersion>()
                        .eq(ExamSubjectVersion::getPstate, "1");
                if (step.getTiptype() != null) {
                    versionWrapper.eq(ExamSubjectVersion::getTiptype, step.getTiptype());
                }

                List<ExamSubjectVersion> versions = versionMapper.selectList(versionWrapper);
                if (versions.isEmpty()) continue;

                // Get subject IDs from versions, filter by typeid if needed
                List<String> subjectIds = versions.stream()
                        .map(ExamSubjectVersion::getSubjectid)
                        .distinct()
                        .collect(Collectors.toList());

                if (step.getTypeid() != null && !step.getTypeid().isEmpty()) {
                    LambdaQueryWrapper<ExamSubject> subjectWrapper = new LambdaQueryWrapper<ExamSubject>()
                            .in(ExamSubject::getId, subjectIds)
                            .eq(ExamSubject::getTypeid, step.getTypeid())
                            .eq(ExamSubject::getPstate, "1");
                    subjectIds = subjectMapper.selectList(subjectWrapper).stream()
                            .map(ExamSubject::getId)
                            .collect(Collectors.toList());
                }

                if (subjectIds.isEmpty()) continue;

                // Randomly select subnum subjects
                Collections.shuffle(subjectIds);
                int selectCount = Math.min(step.getSubnum(), subjectIds.size());
                List<String> selectedIds = subjectIds.subList(0, selectCount);

                for (String subjectId : selectedIds) {
                    // Find the latest version for this subject
                    ExamSubjectVersion version = versionMapper.selectOne(
                            new LambdaQueryWrapper<ExamSubjectVersion>()
                                    .eq(ExamSubjectVersion::getSubjectid, subjectId)
                                    .eq(ExamSubjectVersion::getPstate, "1")
                                    .orderByDesc(ExamSubjectVersion::getCtime)
                                    .last("LIMIT 1"));
                    if (version == null) continue;

                    ExamPaperSubject ps = new ExamPaperSubject();
                    ps.setId(UUID.randomUUID().toString().replace("-", ""));
                    ps.setPaperid(paperId);
                    ps.setSubjectid(subjectId);
                    ps.setVersionid(version.getId());
                    ps.setChapterid(chapter.getId());
                    ps.setSort(++totalSubjects);
                    ps.setPoint(step.getSubpoint());
                    paperSubjectMapper.insert(ps);
                    totalPoints += step.getSubpoint();
                }
            }

            // Update paper stats
            paper.setSubjectnum(totalSubjects);
            paper.setPointnum(totalPoints);
            paperMapper.updateById(paper);
            paperIds.add(paperId);
        }

        return paperIds;
    }

    private String valueOrEmpty(String value) {
        return value != null ? value : "";
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value != null && !value.isBlank() ? value : defaultValue;
    }
}
