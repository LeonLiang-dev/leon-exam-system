package com.wts.common.security;

import com.wts.common.exception.BizException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Single module for reading the current authenticated user.
 */
@Component
public class CurrentUserProvider {

    public CurrentUser require() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw BizException.fail(401, "未登录或登录已过期");
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof LoginUserDetails userDetails)) {
            throw BizException.fail(401, "未登录或登录已过期");
        }

        if (!StringUtils.hasText(userDetails.getUserId())) {
            throw BizException.fail(401, "未登录或登录已过期");
        }

        return new CurrentUser(
                userDetails.getUserId(),
                userDetails.getLoginName(),
                userDetails.getName(),
                userDetails.getUserType(),
                userDetails.getPost(),
                userDetails.getPermissions(),
                userDetails.getOrgIds()
        );
    }
}
