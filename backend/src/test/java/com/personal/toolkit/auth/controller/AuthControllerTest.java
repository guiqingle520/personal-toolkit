package com.personal.toolkit.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.personal.toolkit.auth.config.SecurityConfig;
import com.personal.toolkit.auth.dto.AuthLoginRequest;
import com.personal.toolkit.auth.dto.AuthRegisterRequest;
import com.personal.toolkit.auth.dto.AuthTokenResponse;
import com.personal.toolkit.auth.dto.CaptchaResponse;
import com.personal.toolkit.auth.dto.UserProfileResponse;
import com.personal.toolkit.auth.security.AppUserDetailsService;
import com.personal.toolkit.auth.security.JwtAuthenticationFilter;
import com.personal.toolkit.auth.security.JwtTokenService;
import com.personal.toolkit.auth.security.RestAuthenticationEntryPoint;
import com.personal.toolkit.auth.service.AuthService;
import com.personal.toolkit.auth.service.CaptchaService;
import com.personal.toolkit.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 验证认证接口的响应契约、鉴权要求与异常返回结构。
 */
@WebMvcTest(AuthController.class)
@Import({
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RestAuthenticationEntryPoint.class
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private CaptchaService captchaService;

    @MockBean
    private JwtTokenService jwtTokenService;

    @MockBean
    private AppUserDetailsService appUserDetailsService;

    /**
     * 注册接口应允许匿名访问，并返回统一成功响应和 JWT 数据。
     */
    @Test
    void registerShouldReturnCreatedResponse() throws Exception {
        AuthRegisterRequest request = new AuthRegisterRequest();
        request.setUsername("alice");
        request.setEmail("alice@example.com");
        request.setPassword("password123");

        when(authService.register(any(AuthRegisterRequest.class))).thenReturn(authTokenResponse());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.data.token").value("jwt-token"))
                .andExpect(jsonPath("$.data.user.username").value("alice"));
    }

    /**
     * 登录接口应允许匿名访问，并返回 JWT 与当前用户信息。
     */
    @Test
    void loginShouldReturnTokenResponse() throws Exception {
        AuthLoginRequest request = new AuthLoginRequest();
        request.setUsername("alice");
        request.setPassword("password123");
        request.setCaptchaId("captcha-id");
        request.setCaptchaCode("AB12C");

        when(authService.login(any(AuthLoginRequest.class))).thenReturn(authTokenResponse());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.token").value("jwt-token"))
                .andExpect(jsonPath("$.data.user.email").value("alice@example.com"));
    }

    /**
     * 验证码接口应允许匿名访问，并返回验证码标识与图片数据。
     */
    @Test
    void captchaShouldReturnCaptchaPayload() throws Exception {
        when(captchaService.issueCaptcha(any())).thenReturn(new CaptchaResponse("captcha-id", "data:image/svg+xml;base64,abc", 120));

        mockMvc.perform(get("/api/auth/captcha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Captcha generated successfully"))
                .andExpect(jsonPath("$.data.captchaId").value("captcha-id"))
                .andExpect(jsonPath("$.data.expiresInSeconds").value(120));

        verify(captchaService).issueCaptcha("127.0.0.1");
    }

    /**
     * me 接口在未认证时应返回统一 401 错误响应。
     */
    @Test
    void meShouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    /**
     * me 接口在已认证时应返回当前用户概要信息。
     */
    @Test
    @WithMockUser(username = "alice")
    void meShouldReturnCurrentUser() throws Exception {
        when(authService.getCurrentUserProfile()).thenReturn(new UserProfileResponse(1L, "alice", "alice@example.com"));

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Current user fetched successfully"))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.username").value("alice"));
    }

    /**
     * logout 接口在已认证时应返回无状态登出成功响应。
     */
    @Test
    @WithMockUser(username = "alice")
    void logoutShouldReturnSuccessResponse() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Logout successful"));
    }

    /**
     * 登录业务异常应被转换为统一错误响应结构。
     */
    @Test
    void loginShouldReturnBusinessError() throws Exception {
        AuthLoginRequest request = new AuthLoginRequest();
        request.setUsername("alice");
        request.setPassword("bad-password");
        request.setCaptchaId("captcha-id");
        request.setCaptchaCode("AB12C");
        when(authService.login(any(AuthLoginRequest.class)))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Invalid username or password"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    private AuthTokenResponse authTokenResponse() {
        return new AuthTokenResponse("jwt-token", new UserProfileResponse(1L, "alice", "alice@example.com"));
    }
}
