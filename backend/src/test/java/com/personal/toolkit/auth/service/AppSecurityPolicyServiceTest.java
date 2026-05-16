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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppSecurityPolicyServiceTest {

    @Mock
    private AppSecurityPolicyRepository appSecurityPolicyRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    private JwtProperties jwtProperties;
    private SecurityPolicyBootstrapProperties securityPolicyBootstrapProperties;
    private AppSecurityPolicyService appSecurityPolicyService;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setExpiration(Duration.ofHours(12));
        securityPolicyBootstrapProperties = new SecurityPolicyBootstrapProperties();
        securityPolicyBootstrapProperties.setBootstrapAllowlist(List.of("bootstrap@example.com"));
        appSecurityPolicyService = new AppSecurityPolicyService(
                appSecurityPolicyRepository,
                appUserRepository,
                currentUserProvider,
                jwtProperties,
                securityPolicyBootstrapProperties
        );
    }

    @Test
    void getPolicyShouldFallbackToConfiguredDefaultsWhenPolicyMissing() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(1L);
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(createUser("bootstrap", "bootstrap@example.com")));
        when(appSecurityPolicyRepository.findById(AppSecurityPolicy.GLOBAL_POLICY_ID)).thenReturn(Optional.empty());

        SecurityPolicyResponse response = appSecurityPolicyService.getPolicyForCurrentUser();

        assertNull(response.getAccessTokenTtlSeconds());
        assertEquals(43200L, response.getEffectiveAccessTokenTtlSeconds());
        assertEquals(false, response.isPasswordExpiryEnabled());
        assertNull(response.getPasswordExpiryDays());
    }

    @Test
    void updatePolicyShouldPersistRuntimeValuesForAllowlistedUser() {
        AppSecurityPolicy existingPolicy = new AppSecurityPolicy();
        existingPolicy.setId(AppSecurityPolicy.GLOBAL_POLICY_ID);
        existingPolicy.setPasswordExpiryEnabled(false);

        SecurityPolicyUpdateRequest request = new SecurityPolicyUpdateRequest();
        request.setAccessTokenTtlSeconds(1800L);
        request.setPasswordExpiryEnabled(true);
        request.setPasswordExpiryDays(90);

        when(currentUserProvider.getCurrentUserId()).thenReturn(1L);
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(createUser("bootstrap", "bootstrap@example.com")));
        when(appSecurityPolicyRepository.findById(AppSecurityPolicy.GLOBAL_POLICY_ID)).thenReturn(Optional.of(existingPolicy));
        when(appSecurityPolicyRepository.save(any(AppSecurityPolicy.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SecurityPolicyResponse response = appSecurityPolicyService.updatePolicyForCurrentUser(request);

        assertEquals(1800L, response.getAccessTokenTtlSeconds());
        assertEquals(1800L, response.getEffectiveAccessTokenTtlSeconds());
        assertEquals(true, response.isPasswordExpiryEnabled());
        assertEquals(90, response.getPasswordExpiryDays());
    }

    @Test
    void getPolicyShouldRejectUserOutsideBootstrapAllowlist() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(2L);
        when(appUserRepository.findById(2L)).thenReturn(Optional.of(createUser("alice", "alice@example.com")));

        ApiException exception = assertThrows(ApiException.class, () -> appSecurityPolicyService.getPolicyForCurrentUser());

        assertEquals("SECURITY_POLICY_ACCESS_DENIED", exception.getCode());
        assertEquals(403, exception.getStatusCode().value());
    }

    private AppUser createUser(String username, String email) {
        AppUser user = new AppUser();
        user.setId("bootstrap".equals(username) ? 1L : 2L);
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash("encoded-password");
        return user;
    }
}
