package com.wts.auth.security;

import com.wts.auth.service.PermissionService;
import com.wts.common.security.JwtUtils;
import com.wts.common.security.LoginUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

/**
 * JWT 认证过滤器
 * 从请求头中解析 JWT Token，验证后设置 SecurityContext
 * 每次请求从数据库重载职位/权限/组织，保证权限调整即时生效
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final PermissionService permissionService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);

        if (StringUtils.hasText(token) && jwtUtils.validateToken(token)) {
            String userId = jwtUtils.getUserId(token);
            String loginName = jwtUtils.getLoginName(token);
            String name = jwtUtils.getName(token);
            String userType = jwtUtils.getUserType(token);

            LoginUserDetails userDetails = new LoginUserDetails();
            userDetails.setUserId(userId);
            userDetails.setLoginName(loginName);
            userDetails.setName(name);
            userDetails.setUserType(userType);

            PermissionService.UserIdentity identity = permissionService.loadIdentity(userId);
            if (identity != null) {
                userDetails.setPost(identity.post());
                userDetails.setPermissions(identity.perms());
                userDetails.setOrgIds(identity.orgIds());
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, new ArrayList<>());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
