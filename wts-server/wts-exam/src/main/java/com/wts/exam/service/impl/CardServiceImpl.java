package com.wts.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wts.common.exception.BizException;
import com.wts.common.result.PageResult;
import com.wts.exam.dto.CardAnswerDTO;
import com.wts.exam.dto.CardSubmitDTO;
import com.wts.exam.dto.ExamPaperVO;
import com.wts.exam.dto.JudgeDTO;
import com.wts.exam.entity.*;
import com.wts.exam.mapper.*;
import com.wts.exam.service.CardService;
import com.wts.exam.util.ExamTimeUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CardServiceImpl implements CardService {
    private final ExamCardMapper cardMapper;
    private final ExamCardAnswerMapper cardAnswerMapper;
    private final ExamCardPointMapper cardPointMapper;
    private final ExamSubjectVersionMapper versionMapper;
    private final ExamSubjectAnswerMapper answerMapper;
    private final ExamPaperSubjectMapper paperSubjectMapper;
    private final ExamRoomMapper roomMapper;
    private final ExamRoomPaperMapper roomPaperMapper;
    private final ExamPaperMapper paperMapper;
    private final ExamPaperChapterMapper chapterMapper;
    private final ExamSubjectMapper subjectMapper;
    private final RoomParticipationPolicy roomParticipationPolicy;
    private final CardAnswerGrader cardAnswerGrader;
    private static final String CARD_IN_PROGRESS = "11";
    private static final String CARD_SUBMITTED = "16";
    private static final String CARD_JUDGED = "21";
    private static final String ROOM_PUBLISHED = "21";
    private static final String ROOM_CLOSED = "31";

    @Override
    @Transactional
    public ExamCard enterRoom(String roomId, String userId, String userName, boolean roomAdmin) {
        ExamRoom room = roomMapper.selectById(roomId);
        if (room == null) throw BizException.notFound("答题室");

        ExamCard finished = cardMapper.selectOne(
                new LambdaQueryWrapper<ExamCard>()
                        .eq(ExamCard::getRoomid, roomId)
                        .eq(ExamCard::getUserid, userId)
                        .in(ExamCard::getPstate, CARD_SUBMITTED, CARD_JUDGED));
        if (finished != null) {
            if (roomAdmin) {
                return finished;
            }
            throw BizException.fail("答卷已提交，无需再次进入");
        }

        if (!roomAdmin) {
            roomParticipationPolicy.requireParticipant(room, userId);
        }
        ensureRoomAllowsAnswer(room);

        ExamCard existing = cardMapper.selectOne(
                new LambdaQueryWrapper<ExamCard>()
                        .eq(ExamCard::getRoomid, roomId)
                        .eq(ExamCard::getUserid, userId)
                        .eq(ExamCard::getPstate, CARD_IN_PROGRESS));
        if (existing != null) return existing;

        // Get the first paper in the room (ordered by sort)
        List<ExamRoomPaper> roomPapers = roomPaperMapper.selectList(
                new LambdaQueryWrapper<ExamRoomPaper>()
                        .eq(ExamRoomPaper::getRoomid, roomId)
                        .orderByAsc(ExamRoomPaper::getId));
        if (roomPapers.isEmpty()) throw BizException.notFound("试卷");

        String now = ExamTimeUtils.nowCompact();
        ExamCard card = new ExamCard();
        card.setId(UUID.randomUUID().toString().replace("-", ""));
        card.setRoomid(roomId);
        card.setPaperid(roomPapers.get(0).getPaperid());
        card.setUserid(userId);
        card.setUseruuid(userId);
        card.setPstate(CARD_IN_PROGRESS);
        card.setPoint(0f);
        card.setCompletenum(0);
        card.setAllnum(0);
        card.setOvertime("0");
        card.setStarttime(now);
        card.setEndtime(room.getEndtime());
        card.setResultstype(room.getResultstype());
        card.setRoomuuid(roomId);
        card.setPaperuuid(roomPapers.get(0).getPaperid());
        card.setStatistical("0");
        cardMapper.insert(card);
        return card;
    }

    @Override
    @Transactional
    public void saveAnswers(String cardId, CardSubmitDTO dto, String userId) {
        ExamCard card = cardMapper.selectById(cardId);
        if (card == null) throw BizException.notFound("答卷");
        ensureCardOwner(card, userId);
        ensureCardCanAnswer(card);
        saveAnswersForCard(cardId, dto, userId);
    }

    private void saveAnswersForCard(String cardId, CardSubmitDTO dto, String userId) {
        List<CardAnswerDTO> answers = dto != null && dto.getAnswers() != null
                ? dto.getAnswers()
                : Collections.emptyList();

        // Batch fetch all existing answers for this card (1 query)
        List<ExamCardAnswer> existingList = cardAnswerMapper.selectList(
                new LambdaQueryWrapper<ExamCardAnswer>()
                        .eq(ExamCardAnswer::getCardid, cardId));
        // Key: "versionid|answerid" -> ExamCardAnswer
        Map<String, ExamCardAnswer> existingMap = new HashMap<>();
        for (ExamCardAnswer a : existingList) {
            existingMap.put(a.getVersionid() + "|" + a.getAnswerid(), a);
        }

        List<ExamCardAnswer> toInsert = new ArrayList<>();
        List<ExamCardAnswer> toUpdate = new ArrayList<>();
        String now = ExamTimeUtils.nowCompact();

        for (CardAnswerDTO ansDto : answers) {
            String key = ansDto.getVersionid() + "|" + ansDto.getAnswerid();
            ExamCardAnswer existing = existingMap.get(key);
            if (existing != null) {
                existing.setValstr(ansDto.getValstr());
                toUpdate.add(existing);
            } else {
                ExamCardAnswer ca = new ExamCardAnswer();
                ca.setId(UUID.randomUUID().toString().replace("-", ""));
                ca.setCardid(cardId);
                ca.setVersionid(ansDto.getVersionid());
                ca.setAnswerid(ansDto.getAnswerid());
                ca.setCuser(userId);
                ca.setValstr(ansDto.getValstr());
                ca.setPstate("1");
                ca.setCtime(now);
                toInsert.add(ca);
            }
        }

        for (ExamCardAnswer ca : toUpdate) {
            cardAnswerMapper.updateById(ca);
        }
        for (ExamCardAnswer ca : toInsert) {
            cardAnswerMapper.insert(ca);
        }
    }

    @Override
    @Transactional
    public ExamCard submit(String cardId, CardSubmitDTO dto, String userId) {
        ExamCard card = cardMapper.selectById(cardId);
        if (card == null) throw BizException.notFound("答卷");
        ensureCardOwner(card, userId);
        ensureCardCanAnswer(card);
        saveAnswersForCard(cardId, dto, userId);
        autoGrade(card);
        card.setPstate(CARD_SUBMITTED);
        card.setSubmittime(ExamTimeUtils.nowCompact());
        cardMapper.updateById(card);
        return card;
    }

    @Override
    public ExamCard getResult(String cardId) {
        ExamCard card = cardMapper.selectById(cardId);
        if (card == null) throw BizException.notFound("答卷");
        return card;
    }

    @Override
    public ExamCard getResult(String cardId, String userId, boolean roomAdmin) {
        ExamCard card = getResult(cardId);
        if (roomAdmin) {
            return card;
        }
        ensureResultOwner(card, userId);
        ensureStudentCanViewResult(card);
        return card;
    }

    @Override
    public List<ExamCardAnswer> getCardAnswers(String cardId) {
        return cardAnswerMapper.selectList(
                new LambdaQueryWrapper<ExamCardAnswer>()
                        .eq(ExamCardAnswer::getCardid, cardId));
    }

    @Override
    public List<ExamCardPoint> getCardPoints(String cardId) {
        return cardPointMapper.selectList(
                new LambdaQueryWrapper<ExamCardPoint>()
                        .eq(ExamCardPoint::getCardid, cardId));
    }

    @Override
    @Transactional
    public void judge(String cardId, JudgeDTO dto, String judgeUserId, String judgeUserName) {
        ExamCard card = cardMapper.selectById(cardId);
        if (card == null) throw BizException.notFound("答卷");
        if (!CARD_SUBMITTED.equals(card.getPstate()) && !CARD_JUDGED.equals(card.getPstate()))
            throw BizException.fail("只有已提交的答卷才能批改");

        // Update per-question scores if provided
        if (dto != null && dto.getPoints() != null) {
            // Batch fetch all card points (1 query)
            Map<String, ExamCardPoint> pointMap = cardPointMapper.selectList(
                    new LambdaQueryWrapper<ExamCardPoint>()
                            .eq(ExamCardPoint::getCardid, cardId))
                    .stream().collect(Collectors.toMap(ExamCardPoint::getVersionid, p -> p));

            for (JudgeDTO.JudgePointDTO pointDto : dto.getPoints()) {
                ExamCardPoint cp = pointMap.get(pointDto.getVersionId());
                if (cp != null) {
                    cp.setPoint(pointDto.getPoint());
                    cardPointMapper.updateById(cp);
                }
            }
            // Re-query all points for consistent total
            int totalPoint = cardPointMapper.selectList(
                    new LambdaQueryWrapper<ExamCardPoint>()
                            .eq(ExamCardPoint::getCardid, cardId))
                    .stream().mapToInt(p -> p.getPoint() != null ? p.getPoint() : 0).sum();
            card.setPoint((float) totalPoint);
        }

        card.setAdjudgeuser(judgeUserId);
        card.setAdjudgeusername(judgeUserName);
        card.setAdjudgetime(ExamTimeUtils.nowCompact());
        card.setAdjudgeuseruuid(judgeUserId);
        card.setPstate(CARD_JUDGED);
        cardMapper.updateById(card);
    }

    @Override
    @Transactional
    public void judgeBatch(List<String> cardIds, String judgeUserId, String judgeUserName) {
        if (cardIds == null || cardIds.isEmpty()) {
            throw BizException.fail("请选择要阅卷的答卷");
        }
        for (String cardId : cardIds) {
            judge(cardId, null, judgeUserId, judgeUserName);
        }
    }

    private void autoGrade(ExamCard card) {
        // Delete existing points to prevent duplicates
        cardPointMapper.delete(
                new LambdaQueryWrapper<ExamCardPoint>()
                        .eq(ExamCardPoint::getCardid, card.getId()));

        List<ExamPaperSubject> paperSubjects = paperSubjectMapper.selectList(
                new LambdaQueryWrapper<ExamPaperSubject>()
                        .eq(ExamPaperSubject::getPaperid, card.getPaperid()));
        if (paperSubjects.isEmpty()) return;

        Set<String> versionIds = paperSubjects.stream()
                .map(ExamPaperSubject::getVersionid)
                .collect(Collectors.toSet());

        // Batch query: all versions for this paper (1 query)
        Map<String, ExamSubjectVersion> versionMap = versionMapper.selectList(
                new LambdaQueryWrapper<ExamSubjectVersion>()
                        .in(ExamSubjectVersion::getId, versionIds))
                .stream().collect(Collectors.toMap(ExamSubjectVersion::getId, v -> v));

        // Batch query: all student answers for this card (1 query)
        Map<String, List<ExamCardAnswer>> answersByVersion = cardAnswerMapper.selectList(
                new LambdaQueryWrapper<ExamCardAnswer>()
                        .eq(ExamCardAnswer::getCardid, card.getId()))
                .stream().collect(Collectors.groupingBy(ExamCardAnswer::getVersionid));

        // Batch query: all correct answers for these versions (1 query)
        Map<String, List<ExamSubjectAnswer>> correctAnswersByVersion = answerMapper.selectList(
                new LambdaQueryWrapper<ExamSubjectAnswer>()
                        .in(ExamSubjectAnswer::getVersionid, versionIds))
                .stream().collect(Collectors.groupingBy(ExamSubjectAnswer::getVersionid));

        float totalPoint = 0;
        int completeNum = 0;

        for (ExamPaperSubject ps : paperSubjects) {
            ExamSubjectVersion version = versionMap.get(ps.getVersionid());
            if (version == null) continue;
            String tipType = version.getTiptype();

            List<ExamCardAnswer> answers = answersByVersion.getOrDefault(ps.getVersionid(), Collections.emptyList());

            boolean hasAnswer = !answers.isEmpty() && answers.stream()
                    .anyMatch(a -> a.getValstr() != null && !a.getValstr().isEmpty());
            if (hasAnswer) completeNum++;

            List<ExamSubjectAnswer> correctAnswers = correctAnswersByVersion.getOrDefault(ps.getVersionid(), Collections.emptyList());
            int weight = cardAnswerGrader.calculateWeight(tipType, answers, correctAnswers);
            float earnedPoint = ps.getPoint() * weight / 100f;
            totalPoint += earnedPoint;

            ExamCardPoint cp = new ExamCardPoint();
            cp.setId(UUID.randomUUID().toString().replace("-", ""));
            cp.setCardid(card.getId());
            cp.setVersionid(ps.getVersionid());
            cp.setPoint(Math.round(earnedPoint));
            cp.setMpoint(ps.getPoint());
            cp.setComplete(hasAnswer ? "1" : "0");
            cardPointMapper.insert(cp);
        }
        card.setPoint(totalPoint);
        card.setCompletenum(completeNum);
        card.setAllnum(paperSubjects.size());
    }

    @Override
    public ExamPaperVO getExamPaper(String cardId, String userId) {
        ExamCard card = cardMapper.selectById(cardId);
        if (card == null) throw BizException.notFound("答卷");
        ensureCardOwner(card, userId);
        ensureCardCanAnswer(card);

        return buildExamPaper(card);
    }

    @Override
    public ExamPaperVO getExamPaperForReview(String cardId) {
        ExamCard card = cardMapper.selectById(cardId);
        if (card == null) throw BizException.notFound("答卷");
        if (!CARD_SUBMITTED.equals(card.getPstate()) && !CARD_JUDGED.equals(card.getPstate())) {
            throw BizException.fail("只有已提交的答卷才能阅卷");
        }

        return buildExamPaper(card);
    }

    private ExamPaperVO buildExamPaper(ExamCard card) {
        String cardId = card.getId();

        ExamRoom room = roomMapper.selectById(card.getRoomid());
        ExamPaper paper = paperMapper.selectById(card.getPaperid());
        if (room != null) {
            if (card.getEndtime() == null) {
                card.setEndtime(room.getEndtime());
            }
            if (card.getResultstype() == null) {
                card.setResultstype(room.getResultstype());
            }
        }

        // Get chapters
        List<ExamPaperChapter> chapters = chapterMapper.selectList(
                new LambdaQueryWrapper<ExamPaperChapter>()
                        .eq(ExamPaperChapter::getPaperid, card.getPaperid())
                        .orderByAsc(ExamPaperChapter::getSort));

        // Get all paper subjects for this paper (1 query)
        List<ExamPaperSubject> paperSubjects = paperSubjectMapper.selectList(
                new LambdaQueryWrapper<ExamPaperSubject>()
                        .eq(ExamPaperSubject::getPaperid, card.getPaperid())
                        .orderByAsc(ExamPaperSubject::getSort));

        Set<String> versionIds = paperSubjects.stream()
                .map(ExamPaperSubject::getVersionid)
                .collect(Collectors.toSet());
        Set<String> subjectIds = paperSubjects.stream()
                .map(ExamPaperSubject::getSubjectid)
                .collect(Collectors.toSet());

        // Batch query: all versions (1 query)
        Map<String, ExamSubjectVersion> versionMap = versionMapper.selectList(
                new LambdaQueryWrapper<ExamSubjectVersion>()
                        .in(ExamSubjectVersion::getId, versionIds))
                .stream().collect(Collectors.toMap(ExamSubjectVersion::getId, v -> v));

        // Batch query: all subjects (1 query)
        Map<String, ExamSubject> subjectMap = subjectMapper.selectList(
                new LambdaQueryWrapper<ExamSubject>()
                        .in(ExamSubject::getId, subjectIds))
                .stream().collect(Collectors.toMap(ExamSubject::getId, s -> s));

        // Batch query: all answers for these versions (1 query)
        Map<String, List<ExamSubjectAnswer>> answersByVersion = answerMapper.selectList(
                new LambdaQueryWrapper<ExamSubjectAnswer>()
                        .in(ExamSubjectAnswer::getVersionid, versionIds)
                        .orderByAsc(ExamSubjectAnswer::getSort))
                .stream().collect(Collectors.groupingBy(ExamSubjectAnswer::getVersionid));

        Map<String, List<ExamPaperSubject>> subjectsByChapter = paperSubjects.stream()
                .collect(Collectors.groupingBy(ps -> ps.getChapterid() != null ? ps.getChapterid() : ""));

        List<ExamPaperVO.ChapterVO> chapterVOs = new ArrayList<>();
        for (ExamPaperChapter ch : chapters) {
            ExamPaperVO.ChapterVO cvo = new ExamPaperVO.ChapterVO();
            cvo.setId(ch.getId());
            cvo.setName(ch.getName());
            cvo.setSort(ch.getSort());

            List<ExamPaperSubject> chSubjects = subjectsByChapter.getOrDefault(ch.getId(), Collections.emptyList());
            cvo.setSubjects(buildSubjectVOs(chSubjects, versionMap, subjectMap, answersByVersion));
            chapterVOs.add(cvo);
        }

        // Handle subjects without a chapter
        List<ExamPaperSubject> noChapter = subjectsByChapter.getOrDefault("", Collections.emptyList());
        if (!noChapter.isEmpty()) {
            ExamPaperVO.ChapterVO cvo = new ExamPaperVO.ChapterVO();
            cvo.setId("__default__");
            cvo.setName("默认章节");
            cvo.setSort(0);
            cvo.setSubjects(buildSubjectVOs(noChapter, versionMap, subjectMap, answersByVersion));
            chapterVOs.add(cvo);
        }

        ExamPaperVO vo = new ExamPaperVO();
        vo.setCard(card);
        vo.setPaperName(paper != null ? paper.getName() : "");
        vo.setTimelen(room != null && room.getTimelen() != null ? room.getTimelen() : 60);
        vo.setChapters(chapterVOs);
        // Include previously saved answers so the exam page can restore state
        vo.setSavedAnswers(getCardAnswers(cardId));
        return vo;
    }

    private void ensureCardOwner(ExamCard card, String userId) {
        if (!Objects.equals(userId, card.getUserid())) {
            throw BizException.fail("无权操作此答卷");
        }
    }

    private void ensureResultOwner(ExamCard card, String userId) {
        if (!Objects.equals(userId, card.getUserid())) {
            throw BizException.forbidden("无权查看此成绩");
        }
    }

    private void ensureStudentCanViewResult(ExamCard card) {
        if (!CARD_SUBMITTED.equals(card.getPstate()) && !CARD_JUDGED.equals(card.getPstate())) {
            throw BizException.fail("答卷尚未提交，无法查看成绩");
        }

        ExamRoom room = roomMapper.selectById(card.getRoomid());
        if (room == null) throw BizException.notFound("答题室");

        String resultstype = room.getResultstype() != null ? room.getResultstype() : "1";
        if ("3".equals(resultstype)) {
            throw BizException.fail("该答题室不显示成绩");
        }

        LocalDateTime end = ExamTimeUtils.parseNullable(room.getEndtime());
        if (end != null && LocalDateTime.now().isBefore(end)) {
            throw BizException.fail("考试结束后可查看成绩");
        }

        if ("2".equals(resultstype) && !CARD_JUDGED.equals(card.getPstate())) {
            throw BizException.fail("阅卷后可查看成绩");
        }
    }

    private void ensureCardCanAnswer(ExamCard card) {
        if (!CARD_IN_PROGRESS.equals(card.getPstate())) {
            throw BizException.fail("答卷已提交，无法修改");
        }
        ExamRoom room = roomMapper.selectById(card.getRoomid());
        if (room == null) throw BizException.notFound("答题室");
        ensureRoomAllowsAnswer(room);
    }

    private void ensureRoomAllowsAnswer(ExamRoom room) {
        if (ROOM_CLOSED.equals(room.getPstate())) {
            throw BizException.fail("答题室已关闭，无法作答");
        }
        if (!ROOM_PUBLISHED.equals(room.getPstate())) {
            throw BizException.fail("答题室未开放");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = ExamTimeUtils.parseNullable(room.getStarttime());
        if (start != null && now.isBefore(start)) {
            throw BizException.fail("答题室未开始");
        }
        LocalDateTime end = ExamTimeUtils.parseNullable(room.getEndtime());
        if (end != null && now.isAfter(end)) {
            throw BizException.fail("答题室已结束，无法作答");
        }
    }

    private List<ExamPaperVO.SubjectVO> buildSubjectVOs(
            List<ExamPaperSubject> paperSubjects,
            Map<String, ExamSubjectVersion> versionMap,
            Map<String, ExamSubject> subjectMap,
            Map<String, List<ExamSubjectAnswer>> answersByVersion) {
        List<ExamPaperVO.SubjectVO> result = new ArrayList<>();
        for (ExamPaperSubject ps : paperSubjects) {
            ExamSubjectVersion version = versionMap.get(ps.getVersionid());
            if (version == null) continue;
            ExamSubject subject = subjectMap.get(ps.getSubjectid());

            ExamPaperVO.SubjectVO svo = new ExamPaperVO.SubjectVO();
            svo.setPaperSubjectId(ps.getId());
            svo.setSubjectId(ps.getSubjectid());
            svo.setVersionId(ps.getVersionid());
            svo.setPoint(ps.getPoint() != null ? ps.getPoint().intValue() : 0);
            svo.setIntroduction(subject != null ? subject.getIntroduction() : null);
            svo.setTiptype(version.getTiptype());
            svo.setTipstr(version.getTipstr());
            svo.setTipnote(version.getTipnote());

            List<ExamSubjectAnswer> dbAnswers = answersByVersion.getOrDefault(ps.getVersionid(), Collections.emptyList());
            List<ExamPaperVO.AnswerOptionVO> answerVOs = dbAnswers.stream().map(a -> {
                ExamPaperVO.AnswerOptionVO avo = new ExamPaperVO.AnswerOptionVO();
                avo.setId(a.getId());
                avo.setAnswer(a.getAnswer());
                avo.setSort(a.getSort());
                avo.setPcontent(a.getPcontent());
                return avo;
            }).collect(Collectors.toList());
            svo.setAnswers(answerVOs);
            result.add(svo);
        }
        return result;
    }

    @Override
    public PageResult<ExamCard> getRoomCards(String roomId, int page, int size) {
        LambdaQueryWrapper<ExamCard> wrapper = new LambdaQueryWrapper<ExamCard>()
                .eq(ExamCard::getRoomid, roomId)
                .orderByDesc(ExamCard::getSubmittime);
        return PageResult.of(cardMapper.selectPage(new Page<>(page, size), wrapper));
    }
}
