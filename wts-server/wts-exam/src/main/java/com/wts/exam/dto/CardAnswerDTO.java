package com.wts.exam.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class CardAnswerDTO {
    /** 兼容前端驼峰字段 versionId */
    @JsonAlias({"versionId", "versionID"})
    private String versionid;

    /** 兼容前端驼峰字段 answerId */
    @JsonAlias("answerId")
    private String answerid;

    private String valstr;
}
