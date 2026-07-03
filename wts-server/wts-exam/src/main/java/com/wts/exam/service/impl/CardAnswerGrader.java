package com.wts.exam.service.impl;

import com.wts.exam.entity.ExamCardAnswer;
import com.wts.exam.entity.ExamSubjectAnswer;
import com.wts.exam.enums.TipType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class CardAnswerGrader {

    public int calculateWeight(String tipType, List<ExamCardAnswer> answers, List<ExamSubjectAnswer> correctAnswers) {
        return grade(tipType, answers, correctAnswers).weight();
    }

    public GradeResult grade(String tipType, List<ExamCardAnswer> answers, List<ExamSubjectAnswer> correctAnswers) {
        if (answers.isEmpty()) return GradeResult.empty();
        TipType tt = TipType.fromCode(tipType);
        if (tt == null) return GradeResult.empty();

        switch (tt) {
            case SELECT:
            case JUDGE:
                for (ExamCardAnswer ca : answers) {
                    if ("true".equals(ca.getValstr())) {
                        for (ExamSubjectAnswer sa : correctAnswers) {
                            if (sa.getId().equals(ca.getAnswerid()) && "1".equals(sa.getRightanswer())) {
                                return GradeResult.weight(100);
                            }
                        }
                    }
                }
                return GradeResult.empty();
            case CHECKBOX:
                boolean allCorrect = true;
                for (ExamSubjectAnswer sa : correctAnswers) {
                    if ("1".equals(sa.getRightanswer())) {
                        boolean found = answers.stream().anyMatch(
                                ca -> ca.getAnswerid().equals(sa.getId()) && "true".equals(ca.getValstr()));
                        if (!found) { allCorrect = false; break; }
                    }
                }
                if (allCorrect) {
                    for (ExamCardAnswer ca : answers) {
                        if ("true".equals(ca.getValstr())) {
                            boolean isCorrect = correctAnswers.stream().anyMatch(
                                    sa -> sa.getId().equals(ca.getAnswerid()) && "1".equals(sa.getRightanswer()));
                            if (!isCorrect) { allCorrect = false; break; }
                        }
                    }
                }
                return GradeResult.weight(allCorrect ? 100 : 0);
            case VACANCY:
                int totalWeight = 0;
                int matchedWeight = 0;
                boolean reviewRequired = false;
                Map<String, BlankResult> blankResults = new LinkedHashMap<>();
                for (ExamSubjectAnswer sa : correctAnswers) {
                    int w = sa.getPointweight() != null && sa.getPointweight() > 0 ? sa.getPointweight() : 100;
                    totalWeight += w;
                    boolean matched = false;
                    boolean answered = false;
                    for (ExamCardAnswer ca : answers) {
                        if (ca.getAnswerid().equals(sa.getId())) {
                            String studentValue = ca.getValstr() != null ? ca.getValstr().trim() : "";
                            answered = StringUtils.hasText(studentValue);
                            String[] alternatives = sa.getAnswer() != null ? sa.getAnswer().split("\\|") : new String[0];
                            for (String alt : alternatives) {
                                if (alt.trim().equalsIgnoreCase(studentValue)) {
                                    matched = true;
                                    matchedWeight += w;
                                    break;
                                }
                            }
                            break;
                        }
                    }
                    if (answered && !matched) {
                        reviewRequired = true;
                    }
                    blankResults.put(sa.getId(), new BlankResult(w, answered, matched, answered && !matched));
                }
                int weight = totalWeight > 0 ? matchedWeight * 100 / totalWeight : 0;
                return new GradeResult(weight, reviewRequired, reviewRequired ? "FILL_BLANK_UNMATCHED" : "", blankResults);
            case INTERLOCUTION:
            case FILEUP:
                return GradeResult.empty();
            default:
                return GradeResult.empty();
        }
    }

    public record GradeResult(
            int weight,
            boolean reviewRequired,
            String reviewReason,
            Map<String, BlankResult> blankResults
    ) {
        static GradeResult empty() {
            return weight(0);
        }

        static GradeResult weight(int weight) {
            return new GradeResult(weight, false, "", Map.of());
        }
    }

    public record BlankResult(
            int weight,
            boolean answered,
            boolean matched,
            boolean reviewRequired
    ) {
    }
}
