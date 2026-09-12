package com.wts.common.security;

import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;

/**
 * Authenticated user details used by application modules.
 */
public record CurrentUser(
        String id,
        String loginName,
        String name,
        String userType,
        String post,
        Set<String> permissions,
        List<String> orgIds) {

    /** 职位常量（与 wts-auth 枚举 UserPost 对应，避免跨模块依赖） */
    public static final String POST_STUDENT = "student";
    public static final String POST_TEACHER = "teacher";
    public static final String POST_DIRECTOR = "director";
    public static final String POST_DEPUTY = "deputy";
    public static final String POST_PLATFORM_ADMIN = "platform_admin";

    /** 兼容：老数据没有 post 时，按 userType 推导（1管理员/3超级管理员视为教职工） */
    public String postOrFallback() {
        if (StringUtils.hasText(post)) {
            return post;
        }
        if ("2".equals(userType)) {
            return POST_STUDENT;
        }
        return POST_TEACHER;
    }

    public String displayName() {
        if (StringUtils.hasText(name)) {
            return name;
        }
        return StringUtils.hasText(loginName) ? loginName : id;
    }

    public boolean isAdmin() {
        return "1".equals(userType) || "3".equals(userType);
    }

    /** 教职工（教师及以上，区别于学生） */
    public boolean isStaff() {
        return !POST_STUDENT.equals(postOrFallback());
    }

    /** 平台管理员（学院领导） */
    public boolean isPlatformAdmin() {
        return POST_PLATFORM_ADMIN.equals(postOrFallback());
    }

    /** 教研室负责人（主任/副主任） */
    public boolean isDeptManager() {
        String postValue = postOrFallback();
        return POST_DIRECTOR.equals(postValue) || POST_DEPUTY.equals(postValue);
    }

    /**
     * 是否具备指定权限点。平台管理员恒为 true；
     * 未指定权限点时仅要求教职工身份。
     */
    public boolean hasPerm(String permission) {
        if (isPlatformAdmin()) {
            return true;
        }
        if (permission == null || permission.isBlank()) {
            return isStaff();
        }
        return permissions != null && permissions.contains(permission);
    }
}