package com.personal.toolkit.auth.controller;

import com.personal.toolkit.auth.dto.AuthLoginRequest;
import com.personal.toolkit.auth.dto.AuthRegisterRequest;
import com.personal.toolkit.auth.dto.AuthTokenResponse;
import com.personal.toolkit.auth.dto.CaptchaResponse;
import com.personal.toolkit.auth.dto.UserProfileResponse;
import com.personal.toolkit.auth.service.AuthService;
import com.personal.toolkit.auth.service.CaptchaService;
import com.personal.toolkit.common.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 暴露注册、登录、登出和当前用户查询接口，统一返回 ApiResponse 响应结构。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final CaptchaService captchaService;

    public AuthController(AuthService authService, CaptchaService captchaService) {
        this.authService = authService;
        this.captchaService = captchaService;
    }

    /**
     * 生成登录随机验证码，前端据此渲染图片并在登录时提交验证码标识与答案。
     *
     * @return 验证码响应
     */
    @GetMapping("/captcha")
    public ResponseEntity<ApiResponse<CaptchaResponse>> captcha(HttpServletRequest servletRequest) {
        String clientIp = extractClientIp(servletRequest);
        return ResponseEntity.ok(ApiResponse.success("Captcha generated successfully", captchaService.issueCaptcha(clientIp)));
    }

    /**
     * 注册新用户并返回已登录态所需的 JWT 与用户信息。
     *
     * @param request 注册请求体
     * @return 注册成功响应
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> register(@Valid @RequestBody AuthRegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", authService.register(request)));
    }

    /**
     * 校验用户名密码并签发 JWT。
     *
     * @param request 登录请求体
     * @return 登录成功响应
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> login(@Valid @RequestBody AuthLoginRequest request,
                                                                HttpServletRequest servletRequest) {
        String clientIp = extractClientIp(servletRequest);
        captchaService.enforceLoginThrottle(clientIp, request.getUsername());
        captchaService.validateCaptcha(request.getCaptchaId(), request.getCaptchaCode());
        try {
            AuthTokenResponse response = authService.login(request);
            captchaService.clearLoginFailure(clientIp, request.getUsername());
            return ResponseEntity.ok(ApiResponse.success("Login successful", response));
        } catch (ResponseStatusException ex) {
            if (ex.getStatusCode().value() == HttpStatus.UNAUTHORIZED.value()) {
                captchaService.recordLoginFailure(clientIp, request.getUsername());
            }
            throw ex;
        }
    }

    /**
     * 返回当前登录用户概要信息，用于前端刷新登录态。
     *
     * @return 当前用户响应
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> me() {
        return ResponseEntity.ok(ApiResponse.success("Current user fetched successfully", authService.getCurrentUserProfile()));
    }

    /**
     * 提供无状态登出成功响应，当前版本不做服务端 Token 撤销。
     *
     * @return 登出成功响应
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        return ResponseEntity.ok(ApiResponse.success("Logout successful"));
    }

    private String extractClientIp(HttpServletRequest request) {
        boolean fromTrustedProxy = request.getHeader("X-Forwarded-For") != null
                || request.getHeader("X-Real-IP") != null;

        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (fromTrustedProxy && forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        if (fromTrustedProxy && realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        String remoteAddr = request.getRemoteAddr();
        return remoteAddr == null ? "unknown" : remoteAddr;
    }
}
