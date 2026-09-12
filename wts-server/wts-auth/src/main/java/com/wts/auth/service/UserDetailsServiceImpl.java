package com.wts.auth.service;

import com.wts.auth.entity.SysUser;
import com.wts.auth.mapper.SysUserMapper;
import com.wts.auth.mapper.SysActiontreeMapper;
import com.wts.auth.entity.SysActiontree;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wts.common.security.LoginUserDetails;
import com.wts.common.utils.PasswordUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final SysUserMapper userMapper;
    private final SysActiontreeMapper actiontreeMapper;
    private final PermissionService permissionService;

    @Override
    public UserDetails loadUserByUsername(String loginName) throws UsernameNotFoundException {
        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getLoginname, loginName)
                        .eq(SysUser::getState, "1")
        );

        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + loginName);
        }

        LoginUserDetails details = new LoginUserDetails();
        details.setUserId(user.getId());
        details.setLoginName(user.getLoginname());
        details.setName(user.getName());
        details.setPassword(user.getPassword());
        details.setUserType(user.getType());
        details.setAuthorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));

        PermissionService.UserIdentity identity = permissionService.loadIdentity(user.getId());
        if (identity != null) {
            details.setPost(identity.post());
            details.setPermissions(identity.perms());
            details.setOrgIds(identity.orgIds());
        }

        return details;
    }
}
