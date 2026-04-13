package com.personal.toolkit.auth.controller;

import com.personal.toolkit.auth.dto.AuthLoginRequest;
import com.personal.toolkit.auth.dto.AuthRegisterRequest;
import com.personal.toolkit.auth.dto.AuthTokenResponse;
import com.personal.toolkit.auth.dto.UserProfileResponse;
import com.personal.toolkit.auth.service.AuthService;
import com.personal.toolkit.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    public AuthController(AuthService authService) {
        this.authService = authService;
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
    public ResponseEntity<ApiResponse<AuthTokenResponse>> login(@Valid @RequestBody AuthLoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Login successful", authService.login(request)));
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
}
