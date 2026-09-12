package com.wts.auth.enums;

/**
 * 用户职位（组织架构中的职责定位）。
 * 注意：wts-common 中的 CurrentUser 使用与之对应的字符串常量做判断，避免跨模块依赖。
 */
public enum UserPost {

    STUDENT("student"),
    TEACHER("teacher"),
    DIRECTOR("director"),
    DEPUTY("deputy"),
    PLATFORM_ADMIN("platform_admin");

    private final String code;

    UserPost(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    /** 是否为教职工（教师及以上） */
    public static boolean isStaff(String post) {
        return TEACHER.code.equals(post) || DIRECTOR.code.equals(post)
                || DEPUTY.code.equals(post) || PLATFORM_ADMIN.code.equals(post);
    }

    /** 是否教研室负责人（主任/副主任，具备本教研室用户管理权） */
    public static boolean isDeptManager(String post) {
        return DIRECTOR.code.equals(post) || DEPUTY.code.equals(post);
    }
}