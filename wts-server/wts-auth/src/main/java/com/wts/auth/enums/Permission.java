package com.wts.auth.enums;

import java.util.Set;

/**
 * 细粒度功能权限点。
 * 权限记录在 alone_auth_user.PERMS（逗号分隔），platform_admin 的 PERMS 为空 = 全部权限。
 */
public enum Permission {

    EXAM_PUBLISH("发布考试"),
    SUBJECT_MANAGE("试题管理"),
    CLASS_IMPORT("导入班级"),
    USER_MANAGE("用户管理");

    private final String label;

    Permission(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** 教师默认权限 */
    public static final Set<String> TEACHER_DEFAULT_PERMS = Set.of(
            EXAM_PUBLISH.name(), SUBJECT_MANAGE.name(), CLASS_IMPORT.name());

    /** 主任/副主任在教师权限基础上追加的权限 */
    public static final Set<String> DEPT_MANAGER_EXTRA_PERMS = Set.of(USER_MANAGE.name());
}