package com.wts.exam.service.impl;

import com.wts.exam.entity.ExamCardAnswer;
import com.wts.exam.entity.ExamSubjectAnswer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CardAnswerGraderTest {

    private final CardAnswerGrader grader = new CardAnswerGrader();

    @Test
    void singleChoiceScoresOnlySelectedCorrectAnswer() {
        assertEquals(100, grader.calculateWeight(
                "2",
                List.of(cardAnswer("a-1", "true")),
                List.of(subjectAnswer("a-1", "1"))
        ));
        assertEquals(0, grader.calculateWeight(
                "2",
                List.of(cardAnswer("a-2", "true")),
                List.of(subjectAnswer("a-1", "1"))
        ));
    }

    @Test
    void checkboxRequiresEveryCorrectAnswerAndNoExtraSelection() {
        List<ExamSubjectAnswer> correctAnswers = List.of(
                subjectAnswer("a-1", "1"),
                subjectAnswer("a-2", "1"),
                subjectAnswer("a-3", "0")
        );

        assertEquals(100, grader.calculateWeight(
                "3",
                List.of(cardAnswer("a-1", "true"), cardAnswer("a-2", "true")),
                correctAnswers
        ));
        assertEquals(0, grader.calculateWeight(
                "3",
                List.of(cardAnswer("a-1", "true")),
                correctAnswers
        ));
        assertEquals(0, grader.calculateWeight(
                "3",
                List.of(cardAnswer("a-1", "true"), cardAnswer("a-2", "true"), cardAnswer("a-3", "true")),
                correctAnswers
        ));
    }

    @Test
    void vacancyScoresMatchedWeightedBlanksWithAlternatives() {
        List<ExamSubjectAnswer> correctAnswers = List.of(
                subjectAnswer("blank-1", "Java|JDK", "1", 40),
                subjectAnswer("blank-2", "Spring Boot", "1", 60)
        );

        assertEquals(40, grader.calculateWeight(
                "1",
                List.of(cardAnswer("blank-1", " jdk ")),
                correctAnswers
        ));
        assertEquals(100, grader.calculateWeight(
                "1",
                List.of(cardAnswer("blank-1", "java"), cardAnswer("blank-2", "Spring Boot")),
                correctAnswers
        ));
    }

    @Test
    void subjectiveTypesNeedManualScoring() {
        assertEquals(0, grader.calculateWeight(
                "5",
                List.of(cardAnswer("a-1", "some answer")),
                List.of(subjectAnswer("a-1", "1"))
        ));
    }

    @Test
    void vacancyFlagsUnmatchedAnsweredBlankForManualReview() {
        CardAnswerGrader.GradeResult result = grader.grade(
                "1",
                List.of(cardAnswer("blank-1", "Jetty")),
                List.of(subjectAnswer("blank-1", "Tomcat|Apache Tomcat", "1", 100))
        );

        assertEquals(0, result.weight());
        assertTrue(result.reviewRequired());
        assertTrue(result.blankResults().get("blank-1").reviewRequired());
    }

    private static ExamCardAnswer cardAnswer(String answerId, String value) {
        ExamCardAnswer answer = new ExamCardAnswer();
        answer.setAnswerid(answerId);
        answer.setValstr(value);
        return answer;
    }

    private static ExamSubjectAnswer subjectAnswer(String answerId, String rightanswer) {
        return subjectAnswer(answerId, null, rightanswer, null);
    }

    private static ExamSubjectAnswer subjectAnswer(String answerId, String answerText, String rightanswer, Integer weight) {
        ExamSubjectAnswer answer = new ExamSubjectAnswer();
        answer.setId(answerId);
        answer.setAnswer(answerText);
        answer.setRightanswer(rightanswer);
        answer.setPointweight(weight);
        return answer;
    }
}
