package com.wts.exam.dto;

import lombok.Data;

@Data
public class SubjectQueryDTO {
    private Integer page = 1;
    private Integer size = 20;
    private String keyword;
    private String typeid;
    private String tiptype;
    private String pstate;
    /** 组织（教研室）过滤：按题目创建人所属组织子树筛选 */
    private String orgId;
}
