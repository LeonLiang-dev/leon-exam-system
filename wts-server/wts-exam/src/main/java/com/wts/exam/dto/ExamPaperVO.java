package com.wts.exam.dto;

import com.wts.exam.entity.ExamCard;
import com.wts.exam.entity.ExamCardAnswer;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ExamPaperVO implements Serializable {
    private ExamCard card;
    private String paperName;
    private Integer timelen;
    private List<ChapterVO> chapters;
    private List<ExamCardAnswer> savedAnswers;

    @Data
    public static class ChapterVO implements Serializable {
        private String id;
        private String name;
        private Integer sort;
        private List<SubjectVO> subjects;
    }

    @Data
    public static class SubjectVO implements Serializable {
        private String paperSubjectId;
        private String subjectId;
        private String versionId;
        private Integer point;
        private String introduction;
        private String tiptype;
        private String tipstr;
        private String tipnote;
        private String pcontent;
        private List<AnswerOptionVO> answers;
    }

    @Data
    public static class AnswerOptionVO implements Serializable {
        private String id;
        private String answer;
        private Integer sort;
        private String pcontent;
        private String rightanswer;
        private String answernote;
        private Integer pointweight;
    }
}
