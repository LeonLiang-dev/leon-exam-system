package com.wts.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wts.auth.dto.LoginDTO;
import com.wts.auth.dto.LoginVO;
import com.wts.auth.dto.MenuVO;
import com.wts.auth.entity.SysActiontree;
import com.wts.auth.entity.SysUser;
import com.wts.auth.mapper.SysActiontreeMapper;
import com.wts.auth.mapper.SysUserMapper;
import com.wts.common.exception.BizException;
import com.wts.common.security.JwtUtils;
import com.wts.common.security.LoginUserDetails;
import com.wts.common.utils.PasswordUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;
    private final SysUserMapper userMapper;
    private final SysActiontreeMapper actiontreeMapper;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * 用户登录
     */
    public LoginVO login(LoginDTO dto) {
        String loginName = dto.getLoginName();
        String rawPassword = dto.getPassword();

        // 先查用户，判断密码类型
        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getLoginname, loginName)
                        .eq(SysUser::getState, "1")
        );
        if (user == null) {
            throw BizException.notFound("用户不存在或已禁用");
        }

        // 用 AuthenticationManager 认证（UserDetailsService 已加载用户）
        // 但密码验证需要兼容旧 MD5，所以我们手动处理
        boolean passwordMatch = false;

        if (PasswordUtils.isBCrypt(user.getPassword())) {
            // 新密码: BCrypt
            passwordMatch = passwordEncoder.matches(rawPassword, user.getPassword());
        } else if (PasswordUtils.isMd5(user.getPassword())) {
            // 旧密码: MD5(password + loginname)
            String md5Hash = PasswordUtils.md5Password(rawPassword, loginName);
            passwordMatch = md5Hash.equalsIgnoreCase(user.getPassword());
        }

        if (!passwordMatch) {
            throw BizException.fail("用户名或密码错误");
        }

        // 更新最后登录时间
        String now = LocalDateTime.now().format(FMT);
        user.setLogintime(now);

        // 登录成功后，自动升级密码为 BCrypt (渐进式迁移)
        if (!PasswordUtils.isBCrypt(user.getPassword())) {
            String bcryptPassword = passwordEncoder.encode(rawPassword);
            user.setPassword(bcryptPassword);
            log.info("用户 {} 密码已自动升级为 BCrypt", loginName);
        }

        userMapper.updateById(user);

        // 生成 Token
        String accessToken = jwtUtils.generateAccessToken(user.getId(), user.getLoginname(), user.getName(), user.getType());
        String refreshToken = jwtUtils.generateRefreshToken(user.getId());

        LoginVO vo = new LoginVO();
        vo.setAccessToken(accessToken);
        vo.setRefreshToken(refreshToken);
        vo.setUserId(user.getId());
        vo.setLoginName(user.getLoginname());
        vo.setName(user.getName());
        vo.setUserType(user.getType());
        vo.setPost(resolvePost(user));
        return vo;
    }

    /** 职位缺失时按旧 type 推导，保证登录响应总是带 post */
    private String resolvePost(SysUser user) {
        if (user.getPost() != null && !user.getPost().isBlank()) {
            return user.getPost();
        }
        if ("2".equals(user.getType())) {
            return "student";
        }
        if ("3".equals(user.getType())) {
            return "platform_admin";
        }
        return "teacher";
    }

    /**
     * 刷新 Token
     */
    public LoginVO refreshToken(String refreshToken) {
        if (!jwtUtils.validateToken(refreshToken)) {
            throw BizException.fail(401, "刷新Token无效或已过期");
        }

        String tokenType = jwtUtils.getTokenType(refreshToken);
        if (!"refresh".equals(tokenType)) {
            throw BizException.fail(400, "Token类型错误");
        }

        String userId = jwtUtils.getUserId(refreshToken);
        SysUser user = userMapper.selectById(userId);
        if (user == null || !"1".equals(user.getState())) {
            throw BizException.notFound("用户不存在或已禁用");
        }

        String newAccessToken = jwtUtils.generateAccessToken(user.getId(), user.getLoginname(), user.getName(), user.getType());
        String newRefreshToken = jwtUtils.generateRefreshToken(user.getId());

        LoginVO vo = new LoginVO();
        vo.setAccessToken(newAccessToken);
        vo.setRefreshToken(newRefreshToken);
        vo.setUserId(user.getId());
        vo.setLoginName(user.getLoginname());
        vo.setName(user.getName());
        vo.setUserType(user.getType());
        vo.setPost(resolvePost(user));
        return vo;
    }

    /**
     * 获取当前用户信息
     */
    public SysUser getCurrentUser(String userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw BizException.notFound("用户不存在");
        }
        user.setPassword(null); // 不返回密码
        return user;
    }

    /**
     * 获取当前用户菜单树
     */
    public List<MenuVO> getUserMenus(String userId) {
        // 查询所有菜单节点 (type=1分类, type=2菜单)
        List<SysActiontree> allNodes = actiontreeMapper.selectList(
                new LambdaQueryWrapper<SysActiontree>()
                        .in(SysActiontree::getType, "1", "2")
                        .eq(SysActiontree::getState, "1")
                        .orderByAsc(SysActiontree::getSort)
        );

        // 构建树
        return buildMenuTree(allNodes, "NONE");
    }

    private List<MenuVO> buildMenuTree(List<SysActiontree> allNodes, String parentId) {
        return allNodes.stream()
                .filter(node -> parentId.equals(node.getParentid()))
                .map(node -> {
                    MenuVO vo = new MenuVO();
                    vo.setId(node.getId());
                    vo.setName(node.getName());
                    vo.setIcon(node.getIcon());
                    vo.setSort(node.getSort());
                    // 菜单节点的 path 用 params 字段存储，如果没有则用 actionid
                    vo.setPath(node.getParams() != null && !node.getParams().isEmpty()
                            ? node.getParams() : node.getActionid());
                    vo.setChildren(buildMenuTree(allNodes, node.getId()));
                    if (vo.getChildren().isEmpty()) {
                        vo.setChildren(null);
                    }
                    return vo;
                })
                .collect(Collectors.toList());
    }
}
