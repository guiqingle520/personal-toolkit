package com.personal.toolkit.auth.service;

import com.personal.toolkit.auth.dto.AuthLoginRequest;
import com.personal.toolkit.auth.dto.AuthChangePasswordRequest;
import com.personal.toolkit.auth.dto.AuthTokenResponse;
import com.personal.toolkit.auth.entity.AppUser;
import com.personal.toolkit.auth.repository.AppUserRepository;
import com.personal.toolkit.auth.security.AppUserPrincipal;
import com.personal.toolkit.auth.security.CurrentUserProvider;
import com.personal.toolkit.auth.security.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证登录标识既支持用户名也支持邮箱，同时仍沿用当前 Spring Security 用户名认证链路。
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private PasswordPolicyService passwordPolicyService;

    @Mock
    private Authentication authentication;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                appUserRepository,
                passwordEncoder,
                authenticationManager,
                jwtTokenService,
                currentUserProvider,
                passwordPolicyService
        );
    }

    /**
     * 当用户输入邮箱登录时，应先解析到真实用户名，再交给认证管理器完成密码校验。
     */
    @Test
    void loginShouldAuthenticateWithResolvedUsernameWhenEmailProvided() {
        AuthLoginRequest request = new AuthLoginRequest();
        request.setUsername("Alice@Example.com");
        request.setPassword("password123");
        request.setCaptchaId("captcha-id");
        request.setCaptchaCode("AB12C");

        AppUser appUser = createUser();
        AppUserPrincipal principal = AppUserPrincipal.from(appUser);

        when(appUserRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase("Alice@Example.com", "alice@example.com"))
                .thenReturn(Optional.of(appUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(principal);
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(appUser));
        when(jwtTokenService.generateToken(principal)).thenReturn("jwt-token");
        when(passwordPolicyService.isPasswordChangeRequired(appUser)).thenReturn(false);

        AuthTokenResponse response = authService.login(request);

        assertEquals("jwt-token", response.getToken());
        assertEquals("alice", response.getUser().getUsername());
        verify(authenticationManager).authenticate(eq(new UsernamePasswordAuthenticationToken("alice", "password123")));
        verify(passwordPolicyService).isPasswordChangeRequired(appUser);
    }

    @Test
    void loginShouldReturnLimitedProfileWhenPasswordChangeIsRequired() {
        AuthLoginRequest request = new AuthLoginRequest();
        request.setUsername("alice");
        request.setPassword("password123");

        AppUser appUser = createUser();
        appUser.setPasswordChangeRequired(true);
        AppUserPrincipal principal = AppUserPrincipal.from(appUser);

        when(appUserRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase("alice", "alice"))
                .thenReturn(Optional.of(appUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(principal);
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(appUser));
        when(jwtTokenService.generateToken(principal)).thenReturn("jwt-token");
        when(passwordPolicyService.isPasswordChangeRequired(appUser)).thenReturn(true);

        AuthTokenResponse response = authService.login(request);

        assertEquals("jwt-token", response.getToken());
        assertEquals("alice", response.getUser().getUsername());
        assertEquals(true, response.getUser().isPasswordChangeRequired());
    }

    @Test
    void changePasswordShouldClearPasswordChangeRequiredFlag() {
        AuthChangePasswordRequest request = new AuthChangePasswordRequest();
        request.setCurrentPassword("old-password");
        request.setNewPassword("new-password-123");
        request.setConfirmPassword("new-password-123");

        AppUser appUser = createUser();
        appUser.setPasswordChangeRequired(true);
        appUser.setPasswordChangedAt(LocalDateTime.of(2025, 1, 1, 12, 0));

        when(currentUserProvider.getCurrentUserId()).thenReturn(1L);
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(appUser));
        when(passwordEncoder.matches("old-password", "encoded-password")).thenReturn(true);
        when(passwordEncoder.encode("new-password-123")).thenReturn("new-encoded-password");
        when(appUserRepository.save(appUser)).thenAnswer(invocation -> invocation.getArgument(0));

        var response = authService.changePassword(request);

        assertFalse(response.isPasswordChangeRequired());
        assertEquals("alice", response.getUsername());
        assertFalse(appUser.isPasswordChangeRequired());
        assertEquals("new-encoded-password", appUser.getPasswordHash());
    }

    @Test
    void changePasswordShouldRejectMismatchedConfirmation() {
        AuthChangePasswordRequest request = new AuthChangePasswordRequest();
        request.setCurrentPassword("old-password");
        request.setNewPassword("new-password-123");
        request.setConfirmPassword("different-password");

        AppUser appUser = createUser();

        when(currentUserProvider.getCurrentUserId()).thenReturn(1L);
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(appUser));
        when(passwordEncoder.matches("old-password", "encoded-password")).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> authService.changePassword(request));

        assertEquals(400, exception.getStatusCode().value());
        assertEquals("Password confirmation does not match", exception.getReason());
    }

    private AppUser createUser() {
        AppUser user = new AppUser();
        user.setId(1L);
        user.setUsername("alice");
        user.setEmail("alice@example.com");
        user.setPasswordHash("encoded-password");
        user.setPasswordChangedAt(LocalDateTime.now());
        return user;
    }
}
