package com.personal.toolkit.auth.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * 基于 Spring Security 上下文解析当前用户身份，向业务层屏蔽安全框架细节。
 */
@Component
public class SecurityCurrentUserProvider implements CurrentUserProvider {

    /**
     * 从当前线程绑定的认证上下文中提取已登录用户信息，缺失或类型不符时返回 401。
     *
     * @return 当前已登录用户信息
     */
    @Override
    public AuthenticatedUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUserPrincipal principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return new AuthenticatedUser(principal.getId(), principal.getUsername());
    }
}
