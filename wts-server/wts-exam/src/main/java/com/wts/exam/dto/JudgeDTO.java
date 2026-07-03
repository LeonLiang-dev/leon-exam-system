package com.wts.exam.dto;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class JudgeDTO implements Serializable {
    private List<JudgePointDTO> points;
    private List<JudgeAnswerPointDTO> answerPoints;

    @Data
    public static class JudgePointDTO implements Serializable {
        private String versionId;
        private Integer point;
        private String reviewComment;
    }

    @Data
    public static class JudgeAnswerPointDTO implements Serializable {
        private String versionId;
        private String answerId;
        private Integer point;
        private String reviewComment;
    }
}
