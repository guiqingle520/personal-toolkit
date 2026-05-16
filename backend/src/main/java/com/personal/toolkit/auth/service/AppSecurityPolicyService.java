package com.personal.toolkit.auth.service;

import com.personal.toolkit.auth.config.JwtProperties;
import com.personal.toolkit.auth.config.SecurityPolicyBootstrapProperties;
import com.personal.toolkit.auth.dto.SecurityPolicyResponse;
import com.personal.toolkit.auth.dto.SecurityPolicyUpdateRequest;
import com.personal.toolkit.auth.entity.AppSecurityPolicy;
import com.personal.toolkit.auth.entity.AppUser;
import com.personal.toolkit.auth.repository.AppSecurityPolicyRepository;
import com.personal.toolkit.auth.repository.AppUserRepository;
import com.personal.toolkit.auth.security.CurrentUserProvider;
import com.personal.toolkit.common.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 提供全局运行时安全策略的读取、更新与生效值解析能力。
 */
@Service
public class AppSecurityPolicyService {

    private final AppSecurityPolicyRepository appSecurityPolicyRepository;
    private final AppUserRepository appUserRepository;
    private final CurrentUserProvider currentUserProvider;
    private final JwtProperties jwtProperties;
    private final SecurityPolicyBootstrapProperties securityPolicyBootstrapProperties;

    public AppSecurityPolicyService(AppSecurityPolicyRepository appSecurityPolicyRepository,
                                    AppUserRepository appUserRepository,
                                    CurrentUserProvider currentUserProvider,
                                    JwtProperties jwtProperties,
                                    SecurityPolicyBootstrapProperties securityPolicyBootstrapProperties) {
        this.appSecurityPolicyRepository = appSecurityPolicyRepository;
        this.appUserRepository = appUserRepository;
        this.currentUserProvider = currentUserProvider;
        this.jwtProperties = jwtProperties;
        this.securityPolicyBootstrapProperties = securityPolicyBootstrapProperties;
    }

    /**
     * 返回当前安全策略配置快照，仅允许启动期放行名单内的用户访问。
     *
     * @return 当前安全策略响应
     */
    @Transactional(readOnly = true)
    public SecurityPolicyResponse getPolicyForCurrentUser() {
        requireBootstrapAllowlistAccess();
        return toResponse(findCurrentPolicy().orElse(null));
    }

    /**
     * 更新当前运行时安全策略，仅允许启动期放行名单内的用户访问。
     *
     * @param request 安全策略更新请求
     * @return 更新后的安全策略响应
     */
    @Transactional
    public SecurityPolicyResponse updatePolicyForCurrentUser(SecurityPolicyUpdateRequest request) {
        requireBootstrapAllowlistAccess();
        validateUpdateRequest(request);

        AppSecurityPolicy policy = findCurrentPolicy().orElseGet(this::createDefaultPolicyEntity);
        policy.setAccessTokenTtlSeconds(request.getAccessTokenTtlSeconds());
        policy.setPasswordExpiryEnabled(Boolean.TRUE.equals(request.getPasswordExpiryEnabled()));
        policy.setPasswordExpiryDays(request.getPasswordExpiryDays());

        AppSecurityPolicy savedPolicy = appSecurityPolicyRepository.save(policy);
        return toResponse(savedPolicy);
    }

    /**
     * 解析当前新签发访问令牌应使用的 TTL，优先取数据库策略，缺失时安全回退到静态配置。
     *
     * @return 生效中的访问令牌 TTL
     */
    @Transactional(readOnly = true)
    public Duration resolveAccessTokenTtl() {
        return findCurrentPolicy()
                .map(AppSecurityPolicy::getAccessTokenTtlSeconds)
                .filter(seconds -> seconds != null && seconds > 0)
                .map(Duration::ofSeconds)
                .orElseGet(this::resolveConfiguredAccessTokenTtl);
    }

    /**
     * 返回当前密码过期策略是否真正生效；只有显式开启且天数字段有效时才算启用。
     *
     * @return 密码过期策略是否生效
     */
    @Transactional(readOnly = true)
    public boolean isPasswordExpiryEnabled() {
        return findCurrentPolicy()
                .filter(AppSecurityPolicy::isPasswordExpiryEnabled)
                .map(AppSecurityPolicy::getPasswordExpiryDays)
                .filter(days -> days != null && days > 0)
                .isPresent();
    }

    /**
     * 返回当前密码过期天数配置。
     *
     * @return 密码过期天数，未配置时返回 null
     */
    @Transactional(readOnly = true)
    public Integer getPasswordExpiryDays() {
        return findCurrentPolicy()
                .map(AppSecurityPolicy::getPasswordExpiryDays)
                .orElse(null);
    }

    private java.util.Optional<AppSecurityPolicy> findCurrentPolicy() {
        return appSecurityPolicyRepository.findById(AppSecurityPolicy.GLOBAL_POLICY_ID);
    }

    private AppSecurityPolicy createDefaultPolicyEntity() {
        AppSecurityPolicy policy = new AppSecurityPolicy();
        policy.setId(AppSecurityPolicy.GLOBAL_POLICY_ID);
        policy.setPasswordExpiryEnabled(false);
        return policy;
    }

    private SecurityPolicyResponse toResponse(AppSecurityPolicy policy) {
        Long configuredAccessTokenTtlSeconds = policy == null ? null : policy.getAccessTokenTtlSeconds();
        boolean passwordExpiryEnabled = policy != null && policy.isPasswordExpiryEnabled();
        Integer passwordExpiryDays = policy == null ? null : policy.getPasswordExpiryDays();
        return new SecurityPolicyResponse(
                configuredAccessTokenTtlSeconds,
                resolveAccessTokenTtl().getSeconds(),
                passwordExpiryEnabled,
                passwordExpiryDays
        );
    }

    private Duration resolveConfiguredAccessTokenTtl() {
        return jwtProperties.getExpiration() == null ? Duration.ofHours(12) : jwtProperties.getExpiration();
    }

    private void validateUpdateRequest(SecurityPolicyUpdateRequest request) {
        if (Boolean.TRUE.equals(request.getPasswordExpiryEnabled())
                && (request.getPasswordExpiryDays() == null || request.getPasswordExpiryDays() <= 0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "passwordExpiryDays must be provided when passwordExpiryEnabled is true");
        }
    }

    private void requireBootstrapAllowlistAccess() {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        AppUser appUser = appUserRepository.findById(currentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required"));

        Set<String> allowlist = securityPolicyBootstrapProperties.getBootstrapAllowlist()
                .stream()
                .map(this::normalizeIdentifier)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toSet());

        String normalizedUsername = normalizeIdentifier(appUser.getUsername());
        String normalizedEmail = normalizeIdentifier(appUser.getEmail());
        if (allowlist.contains(normalizedUsername) || allowlist.contains(normalizedEmail)) {
            return;
        }

        throw new ApiException(HttpStatus.FORBIDDEN,
                "SECURITY_POLICY_ACCESS_DENIED",
                "Security policy access denied");
    }

    private String normalizeIdentifier(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
