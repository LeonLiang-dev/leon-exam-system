package com.wts.auth.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class LoginVO {
    private String accessToken;
    private String refreshToken;
    private String userId;
    private String loginName;
    private String name;
    private String userType;
    /** 职位（student/teacher/director/deputy/platform_admin） */
    private String post;
}
