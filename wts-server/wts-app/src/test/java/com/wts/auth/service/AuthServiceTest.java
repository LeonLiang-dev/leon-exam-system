package com.wts.auth.service;

import com.wts.auth.dto.LoginDTO;
import com.wts.auth.dto.LoginVO;
import com.wts.auth.dto.MenuVO;
import com.wts.auth.entity.SysActiontree;
import com.wts.auth.entity.SysUser;
import com.wts.auth.mapper.SysActiontreeMapper;
import com.wts.auth.mapper.SysUserMapper;
import com.wts.common.exception.BizException;
import com.wts.common.security.JwtUtils;
import com.wts.common.utils.PasswordUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtUtils jwtUtils;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private SysUserMapper userMapper;
    @Mock
    private SysActiontreeMapper actiontreeMapper;

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(authenticationManager, jwtUtils, passwordEncoder, userMapper, actiontreeMapper);
    }

    @Test
    void loginWithBcryptPasswordReturnsTokensAndUpdatesLoginTime() {
        SysUser user = user("user-1", "teacher", "$2a$10$bcrypt", "1");
        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches("123456", "$2a$10$bcrypt")).thenReturn(true);
        when(jwtUtils.generateAccessToken("user-1", "teacher", "Teacher One", "1")).thenReturn("access-token");
        when(jwtUtils.generateRefreshToken("user-1")).thenReturn("refresh-token");

        LoginVO vo = service.login(loginDto("teacher", "123456"));

        assertEquals("access-token", vo.getAccessToken());
        assertEquals("refresh-token", vo.getRefreshToken());
        assertEquals("user-1", vo.getUserId());
        assertEquals("teacher", vo.getLoginName());
        assertEquals("Teacher One", vo.getName());
        assertEquals("1", vo.getUserType());
        assertNotNull(user.getLogintime());
        assertEquals("$2a$10$bcrypt", user.getPassword());
        verify(userMapper).updateById(user);
    }

    @Test
    void loginUpgradesLegacyMd5PasswordToBcrypt() {
        String md5Password = PasswordUtils.md5Password("123123", "student01");
        SysUser user = user("student-1", "student01", md5Password, "2");
        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.encode("123123")).thenReturn("$2a$10$new-bcrypt");
        when(jwtUtils.generateAccessToken("student-1", "student01", "Teacher One", "2")).thenReturn("access-token");
        when(jwtUtils.generateRefreshToken("student-1")).thenReturn("refresh-token");

        LoginVO vo = service.login(loginDto("student01", "123123"));

        assertEquals("access-token", vo.getAccessToken());
        assertEquals("$2a$10$new-bcrypt", user.getPassword());
        assertNotNull(user.getLogintime());
        verify(userMapper).updateById(user);
    }

    @Test
    void loginRejectsWrongPassword() {
        SysUser user = user("user-1", "teacher", "$2a$10$bcrypt", "1");
        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches("bad", "$2a$10$bcrypt")).thenReturn(false);

        BizException error = assertThrows(BizException.class,
                () -> service.login(loginDto("teacher", "bad")));

        assertEquals("用户名或密码错误", error.getMessage());
        verify(userMapper, never()).updateById(any(SysUser.class));
    }

    @Test
    void refreshTokenRejectsAccessToken() {
        when(jwtUtils.validateToken("access-token")).thenReturn(true);
        when(jwtUtils.getTokenType("access-token")).thenReturn("access");

        BizException error = assertThrows(BizException.class,
                () -> service.refreshToken("access-token"));

        assertEquals(400, error.getCode());
        assertEquals("Token类型错误", error.getMessage());
        verify(userMapper, never()).selectById(any());
    }

    @Test
    void refreshTokenReturnsNewTokensForEnabledUser() {
        SysUser user = user("user-1", "teacher", "$2a$10$bcrypt", "1");
        when(jwtUtils.validateToken("refresh-token")).thenReturn(true);
        when(jwtUtils.getTokenType("refresh-token")).thenReturn("refresh");
        when(jwtUtils.getUserId("refresh-token")).thenReturn("user-1");
        when(userMapper.selectById("user-1")).thenReturn(user);
        when(jwtUtils.generateAccessToken("user-1", "teacher", "Teacher One", "1")).thenReturn("new-access");
        when(jwtUtils.generateRefreshToken("user-1")).thenReturn("new-refresh");

        LoginVO vo = service.refreshToken("refresh-token");

        assertEquals("new-access", vo.getAccessToken());
        assertEquals("new-refresh", vo.getRefreshToken());
        assertEquals("user-1", vo.getUserId());
    }

    @Test
    void getCurrentUserHidesPassword() {
        SysUser user = user("user-1", "teacher", "$2a$10$bcrypt", "1");
        when(userMapper.selectById("user-1")).thenReturn(user);

        SysUser current = service.getCurrentUser("user-1");

        assertEquals("user-1", current.getId());
        assertNull(current.getPassword());
    }

    @Test
    void getUserMenusBuildsTreeAndUsesParamsAsPath() {
        when(actiontreeMapper.selectList(any())).thenReturn(List.of(
                menu("root", "NONE", "考试管理", "1", 1, null, null),
                menu("child", "root", "题目管理", "2", 2, "action-subject", "/exam/subject"),
                menu("child-action-path", "root", "试卷管理", "2", 3, "/exam/paper", null)
        ));

        List<MenuVO> menus = service.getUserMenus("user-1");

        assertEquals(1, menus.size());
        assertEquals("root", menus.get(0).getId());
        assertEquals("考试管理", menus.get(0).getName());
        assertEquals(2, menus.get(0).getChildren().size());
        assertEquals("/exam/subject", menus.get(0).getChildren().get(0).getPath());
        assertEquals("/exam/paper", menus.get(0).getChildren().get(1).getPath());
        assertNull(menus.get(0).getChildren().get(0).getChildren());
    }

    private static LoginDTO loginDto(String loginName, String password) {
        LoginDTO dto = new LoginDTO();
        dto.setLoginName(loginName);
        dto.setPassword(password);
        return dto;
    }

    private static SysUser user(String id, String loginName, String password, String type) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setLoginname(loginName);
        user.setPassword(password);
        user.setName("Teacher One");
        user.setType(type);
        user.setState("1");
        return user;
    }

    private static SysActiontree menu(
            String id,
            String parentId,
            String name,
            String type,
            int sort,
            String actionId,
            String params) {
        SysActiontree node = new SysActiontree();
        node.setId(id);
        node.setParentid(parentId);
        node.setName(name);
        node.setType(type);
        node.setSort(sort);
        node.setActionid(actionId);
        node.setParams(params);
        node.setState("1");
        return node;
    }
}
