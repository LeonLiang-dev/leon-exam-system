package com.wts.exam.enums;

import java.util.Set;

public final class QuestionTypeRules {
    private QuestionTypeRules() {
    }

    public static final String FILL_BLANK = "1";
    public static final String SINGLE_CHOICE = "2";
    public static final String MULTIPLE_CHOICE = "3";
    public static final String TRUE_FALSE = "4";
    public static final String SUBJECTIVE = "5";

    private static final Set<String> SUPPORTED_TYPES = Set.of(
            FILL_BLANK,
            SINGLE_CHOICE,
            MULTIPLE_CHOICE,
            TRUE_FALSE,
            SUBJECTIVE
    );

    public static boolean isSupportedType(String tipType) {
        return SUPPORTED_TYPES.contains(tipType);
    }

    public static boolean isFillBlank(String tipType) {
        return FILL_BLANK.equals(tipType);
    }

    public static boolean isSubjective(String tipType) {
        return SUBJECTIVE.equals(tipType);
    }

    public static boolean isObjective(String tipType) {
        return SINGLE_CHOICE.equals(tipType)
                || MULTIPLE_CHOICE.equals(tipType)
                || TRUE_FALSE.equals(tipType);
    }

    public static boolean alwaysNeedsManualReview(String tipType) {
        return SUBJECTIVE.equals(tipType);
    }
}
