package com.wts.common.security;

import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Spring Security 用户详情实现
 */
@Data
public class LoginUserDetails implements UserDetails {

    private String userId;
    private String loginName;
    private String name;
    private String password;
    private String userType;
    /** 职位: student/teacher/director/deputy/platform_admin */
    private String post;
    private Set<String> permissions;
    /** 用户所属组织节点 id 列表 */
    private List<String> orgIds;
    private Collection<? extends GrantedAuthority> authorities;

    @Override
    public String getUsername() {
        return loginName;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
