package com.personal.toolkit.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.personal.toolkit.auth.entity.AppUser;
import com.personal.toolkit.auth.service.PasswordPolicyService;
import com.personal.toolkit.common.exception.ApiException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private PasswordPolicyService passwordPolicyService;

    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        jwtAuthenticationFilter = new JwtAuthenticationFilter(
                jwtTokenService,
                passwordPolicyService,
                new ObjectMapper().registerModule(new JavaTimeModule())
        );
    }

    @Test
    void shouldBlockAuthenticatedRequestWhenPasswordExpired() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/todos");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer token-value");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();
        Claims claims = Jwts.claims().subject("alice").add("userId", 1L).build();
        AppUser appUser = createUser();

        when(jwtTokenService.parseToken("token-value")).thenReturn(claims);
        when(passwordPolicyService.findUserForClaims(claims)).thenReturn(Optional.of(appUser));
        when(passwordPolicyService.resolvePasswordPolicyViolation(appUser)).thenReturn(Optional.of(
                new ApiException(HttpStatus.FORBIDDEN, "PASSWORD_EXPIRED", "Password expired")
        ));

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertEquals(403, response.getStatus());
        assertEquals(
                "PASSWORD_EXPIRED",
                new ObjectMapper().registerModule(new JavaTimeModule()).readTree(response.getContentAsString()).get("code").asText()
        );
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldAllowChangePasswordEndpointWhenPasswordChangeRequired() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/change-password");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer token-value");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();
        Claims claims = Jwts.claims().subject("alice").add("userId", 1L).build();
        AppUser appUser = createUser();
        appUser.setPasswordChangeRequired(true);

        when(jwtTokenService.parseToken("token-value")).thenReturn(claims);
        when(passwordPolicyService.findUserForClaims(claims)).thenReturn(Optional.of(appUser));
        when(passwordPolicyService.resolvePasswordPolicyViolation(appUser)).thenReturn(Optional.of(
                new ApiException(HttpStatus.FORBIDDEN, "PASSWORD_CHANGE_REQUIRED", "Password change required")
        ));

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertEquals(200, response.getStatus());
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    private AppUser createUser() {
        AppUser appUser = new AppUser();
        appUser.setId(1L);
        appUser.setUsername("alice");
        appUser.setEmail("alice@example.com");
        appUser.setPasswordHash("encoded-password");
        appUser.setPasswordChangedAt(LocalDateTime.now());
        return appUser;
    }
}
