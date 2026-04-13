package com.personal.toolkit.auth.security;

import com.personal.toolkit.auth.repository.AppUserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 按用户名加载应用用户鉴权主体，供登录认证和 JWT 解析复用。
 */
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final AppUserRepository appUserRepository;

    public AppUserDetailsService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    /**
     * 按用户名加载用户详情，不存在时抛出标准鉴权异常。
     *
     * @param username 登录用户名
     * @return Spring Security 可识别的用户详情对象
     */
    @Override
    public UserDetails loadUserByUsername(String username) {
        return appUserRepository.findByUsernameIgnoreCase(username)
                .map(AppUserPrincipal::from)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}
