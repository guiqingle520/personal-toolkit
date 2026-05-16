package com.personal.toolkit.auth.service;

import com.personal.toolkit.auth.entity.AppUser;
import com.personal.toolkit.auth.repository.AppUserRepository;
import com.personal.toolkit.common.exception.ApiException;
import io.jsonwebtoken.Claims;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 负责解析密码生命周期策略，并在登录与请求阶段给出统一业务错误。
 */
@Service
public class PasswordPolicyService {

    private final AppUserRepository appUserRepository;
    private final AppSecurityPolicyService appSecurityPolicyService;

    public PasswordPolicyService(AppUserRepository appUserRepository,
                                 AppSecurityPolicyService appSecurityPolicyService) {
        this.appUserRepository = appUserRepository;
        this.appSecurityPolicyService = appSecurityPolicyService;
    }

    /**
     * 根据 JWT 声明重新加载数据库用户，确保请求期策略校验使用最新用户状态。
     *
     * @param claims 已校验 JWT 声明
     * @return 数据库中的最新用户实体
     */
    @Transactional(readOnly = true)
    public Optional<AppUser> findUserForClaims(Claims claims) {
        Long userId = claims.get("userId", Long.class);
        if (userId != null) {
            return appUserRepository.findById(userId);
        }
        String username = claims.getSubject();
        if (username == null || username.trim().isEmpty()) {
            return Optional.empty();
        }
        return appUserRepository.findByUsernameIgnoreCase(username);
    }

    /**
     * 在登录成功后但签发 JWT 前执行密码策略校验，命中策略时阻止发放新令牌。
     *
     * @param appUser 已认证用户实体
     */
    @Transactional(readOnly = true)
    public void assertLoginAllowed(AppUser appUser) {
        resolvePasswordPolicyViolation(appUser).ifPresent(ex -> {
            throw ex;
        });
    }

    /**
     * 判断当前用户是否必须先完成改密后才能继续访问其他受保护接口。
     *
     * @param appUser 用户实体
     * @return 是否需要强制改密
     */
    @Transactional(readOnly = true)
    public boolean isPasswordChangeRequired(AppUser appUser) {
        return resolvePasswordPolicyViolation(appUser).isPresent();
    }

    /**
     * 解析当前用户是否命中密码过期或强制改密策略。
     *
     * @param appUser 用户实体
     * @return 若命中策略则返回对应业务异常
     */
    @Transactional(readOnly = true)
    public Optional<ApiException> resolvePasswordPolicyViolation(AppUser appUser) {
        if (appUser.isPasswordChangeRequired()) {
            return Optional.of(new ApiException(
                    HttpStatus.FORBIDDEN,
                    "PASSWORD_CHANGE_REQUIRED",
                    "Password change required"
            ));
        }

        if (!appSecurityPolicyService.isPasswordExpiryEnabled()) {
            return Optional.empty();
        }

        Integer passwordExpiryDays = appSecurityPolicyService.getPasswordExpiryDays();
        if (passwordExpiryDays == null || passwordExpiryDays <= 0) {
            return Optional.empty();
        }

        LocalDateTime passwordChangedAt = appUser.getPasswordChangedAt();
        if (passwordChangedAt == null || !passwordChangedAt.plusDays(passwordExpiryDays).isAfter(LocalDateTime.now())) {
            return Optional.of(new ApiException(
                    HttpStatus.FORBIDDEN,
                    "PASSWORD_EXPIRED",
                    "Password expired"
            ));
        }

        return Optional.empty();
    }
}
