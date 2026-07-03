package com.wts.exam.service.impl;

import com.wts.common.exception.BizException;
import com.wts.exam.dto.AnswerDTO;
import com.wts.exam.dto.SubjectDTO;
import com.wts.exam.enums.QuestionTypeRules;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class SubjectRuleValidator {
    private static final String RIGHT_ANSWER = "1";
    private static final String WRONG_ANSWER = "0";
    private static final int MIN_OPTIONS = 2;
    private static final int MAX_OPTIONS = 10;

    private SubjectRuleValidator() {
    }

    public static List<AnswerDTO> validateAndNormalize(SubjectDTO dto) {
        if (dto == null) {
            throw BizException.fail("题目不能为空");
        }
        if (!StringUtils.hasText(dto.getTipstr())) {
            throw BizException.fail("题干不能为空");
        }
        if (dto.getPoint() == null || dto.getPoint() <= 0) {
            throw BizException.fail("题目分值必须大于0");
        }
        if (!QuestionTypeRules.isSupportedType(dto.getTiptype())) {
            throw BizException.fail("不支持的题型");
        }

        return switch (dto.getTiptype()) {
            case QuestionTypeRules.SINGLE_CHOICE -> validateSingleChoice(dto.getAnswers());
            case QuestionTypeRules.MULTIPLE_CHOICE -> validateMultipleChoice(dto.getAnswers());
            case QuestionTypeRules.TRUE_FALSE -> validateTrueFalse(dto.getAnswers());
            case QuestionTypeRules.FILL_BLANK -> validateFillBlank(dto.getAnswers());
            case QuestionTypeRules.SUBJECTIVE -> normalizeSubjective();
            default -> throw BizException.fail("不支持的题型");
        };
    }

    private static List<AnswerDTO> validateSingleChoice(List<AnswerDTO> answers) {
        List<AnswerDTO> normalized = normalizeOptions(answers);
        long correctCount = countCorrect(normalized);
        if (correctCount != 1) {
            throw BizException.fail("单选题必须且只能设置一个正确答案");
        }
        return normalized;
    }

    private static List<AnswerDTO> validateMultipleChoice(List<AnswerDTO> answers) {
        List<AnswerDTO> normalized = normalizeOptions(answers);
        long correctCount = countCorrect(normalized);
        if (correctCount < 2) {
            throw BizException.fail("多选题必须设置至少两个正确答案");
        }
        if (correctCount == normalized.size()) {
            throw BizException.fail("多选题至少需要一个错误选项");
        }
        return normalized;
    }

    private static List<AnswerDTO> validateTrueFalse(List<AnswerDTO> answers) {
        if (answers == null || countCorrect(answers) != 1) {
            throw BizException.fail("判断题必须设置一个正确答案");
        }

        boolean correctIsTrue = false;
        boolean foundCorrect = false;
        for (int i = 0; i < answers.size(); i++) {
            AnswerDTO answer = answers.get(i);
            if (RIGHT_ANSWER.equals(defaultRight(answer))) {
                String text = answer != null ? trim(answer.getAnswer()) : "";
                correctIsTrue = "正确".equals(text) || "对".equals(text) || i == 0;
                foundCorrect = true;
                break;
            }
        }
        if (!foundCorrect) {
            throw BizException.fail("判断题必须设置一个正确答案");
        }

        List<AnswerDTO> normalized = new ArrayList<>();
        normalized.add(answer("正确", correctIsTrue ? RIGHT_ANSWER : WRONG_ANSWER, 1));
        normalized.add(answer("错误", correctIsTrue ? WRONG_ANSWER : RIGHT_ANSWER, 2));
        return normalized;
    }

    private static List<AnswerDTO> validateFillBlank(List<AnswerDTO> answers) {
        if (answers == null || answers.isEmpty()) {
            throw BizException.fail("填空题至少需要设置一个空");
        }

        List<AnswerDTO> normalized = new ArrayList<>();
        for (int i = 0; i < answers.size(); i++) {
            AnswerDTO answer = answers.get(i);
            if (answer == null || !StringUtils.hasText(answer.getAnswer())) {
                throw BizException.fail("填空题每个空都必须设置标准答案");
            }
            if (answer.getPointweight() != null && answer.getPointweight() <= 0) {
                throw BizException.fail("填空题每个空的分值权重必须大于0");
            }
            AnswerDTO copy = answer(trim(answer.getAnswer()), RIGHT_ANSWER, i + 1);
            copy.setAnswernote(answer.getAnswernote());
            copy.setPointweight(answer.getPointweight());
            copy.setGroupno(i + 1);
            copy.setPcontent(answer.getPcontent());
            normalized.add(copy);
        }
        return normalized;
    }

    private static List<AnswerDTO> normalizeSubjective() {
        AnswerDTO answer = answer("", WRONG_ANSWER, 1);
        answer.setAnswernote("");
        return List.of(answer);
    }

    private static List<AnswerDTO> normalizeOptions(List<AnswerDTO> answers) {
        if (answers == null || answers.size() < MIN_OPTIONS || answers.size() > MAX_OPTIONS) {
            throw BizException.fail("选择题必须设置2到10个选项");
        }

        List<AnswerDTO> normalized = new ArrayList<>();
        for (int i = 0; i < answers.size(); i++) {
            AnswerDTO answer = answers.get(i);
            if (answer == null || !StringUtils.hasText(answer.getAnswer())) {
                throw BizException.fail("每个选项内容不能为空");
            }
            AnswerDTO copy = answer(trim(answer.getAnswer()), defaultRight(answer), i + 1);
            copy.setAnswernote(answer.getAnswernote());
            copy.setPointweight(answer.getPointweight());
            copy.setGroupno(answer.getGroupno());
            copy.setPcontent(answer.getPcontent());
            normalized.add(copy);
        }
        return normalized;
    }

    private static long countCorrect(List<AnswerDTO> answers) {
        if (answers == null) {
            return 0;
        }
        return answers.stream()
                .filter(answer -> RIGHT_ANSWER.equals(defaultRight(answer)))
                .count();
    }

    private static AnswerDTO answer(String text, String rightanswer, int sort) {
        AnswerDTO dto = new AnswerDTO();
        dto.setAnswer(text);
        dto.setRightanswer(rightanswer);
        dto.setSort(sort);
        return dto;
    }

    private static String defaultRight(AnswerDTO answer) {
        return answer != null && RIGHT_ANSWER.equals(answer.getRightanswer())
                ? RIGHT_ANSWER
                : WRONG_ANSWER;
    }

    private static String trim(String value) {
        return value != null ? value.trim() : "";
    }
}
