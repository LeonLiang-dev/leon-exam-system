package com.wts.auth.service;

import com.wts.auth.entity.SysUser;
import com.wts.auth.mapper.SysUserMapper;
import com.wts.auth.mapper.SysUserorgMapper;
import com.wts.common.exception.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceDeleteTest {

    @Mock
    private SysUserMapper userMapper;
    @Mock
    private SysUserorgMapper userorgMapper;
    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(userMapper, userorgMapper, passwordEncoder);
    }

    @Test
    void disableUsersMarksSelectedUsersDisabled() {
        SysUser user1 = user("user-1", "teacher1");
        SysUser user2 = user("user-2", "student1");
        when(userMapper.selectBatchIds(List.of("user-1", "user-2"))).thenReturn(List.of(user1, user2));

        service.disableUsers(List.of("user-1", "user-2"), "operator-1");

        assertEquals("0", user1.getState());
        assertEquals("0", user2.getState());
        assertEquals("operator-1", user1.getMuser());
        assertEquals("operator-1", user2.getMuser());
        verify(userMapper).updateById(user1);
        verify(userMapper).updateById(user2);
    }

    @Test
    void hardDeleteUsersDeletesOrganizationLinksBeforeUsers() {
        SysUser user1 = user("user-1", "teacher1");
        SysUser user2 = user("user-2", "student1");
        when(userMapper.selectBatchIds(List.of("user-1", "user-2"))).thenReturn(List.of(user1, user2));

        service.hardDeleteUsers(List.of("user-1", "user-2"), "operator-1");

        verify(userorgMapper).delete(any());
        verify(userMapper).deleteBatchIds(List.of("user-1", "user-2"));
    }

    @Test
    void cannotDisableCurrentUser() {
        SysUser current = user("operator-1", "teacher1");
        when(userMapper.selectBatchIds(List.of("operator-1"))).thenReturn(List.of(current));

        BizException error = assertThrows(BizException.class,
                () -> service.disableUsers(List.of("operator-1"), "operator-1"));

        assertEquals("不能禁用当前登录用户", error.getMessage());
        verify(userMapper, never()).updateById(any(SysUser.class));
    }

    @Test
    void cannotHardDeleteSysadmin() {
        SysUser sysadmin = user("sysadmin-id", "sysadmin");
        when(userMapper.selectBatchIds(List.of("sysadmin-id"))).thenReturn(List.of(sysadmin));

        BizException error = assertThrows(BizException.class,
                () -> service.hardDeleteUsers(List.of("sysadmin-id"), "operator-1"));

        assertEquals("不能删除内置超级管理员 sysadmin", error.getMessage());
        verify(userorgMapper, never()).delete(any());
        verify(userMapper, never()).deleteBatchIds(any());
    }

    private static SysUser user(String id, String loginName) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setLoginname(loginName);
        user.setState("1");
        return user;
    }
}
