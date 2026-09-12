package com.wts.auth.dto;

import lombok.Data;

@Data
public class UserDTO {
    private String id;
    private String name;
    private String loginname;
    private String type;
    private String state;
    private String comments;
    private String imgid;
    /** 职位: student/teacher/director/deputy/platform_admin（可选，不传则沿用默认规则） */
    private String post;
    /** 权限点（逗号分隔，可选；不传则按职位默认权限） */
    private String perms;
    /** 组织归属节点 id（可选，落库 alone_auth_userorg） */
    private String orgId;
    /** 班级标签（可选） */
    private String className;
}
