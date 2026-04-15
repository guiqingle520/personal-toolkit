package com.personal.toolkit.auth.service;

import com.personal.toolkit.auth.dto.AuthLoginRequest;
import com.personal.toolkit.auth.dto.AuthRegisterRequest;
import com.personal.toolkit.auth.dto.AuthTokenResponse;
import com.personal.toolkit.auth.dto.UserProfileResponse;
import com.personal.toolkit.auth.entity.AppUser;
import com.personal.toolkit.auth.repository.AppUserRepository;
import com.personal.toolkit.auth.security.AppUserPrincipal;
import com.personal.toolkit.auth.security.CurrentUserProvider;
import com.personal.toolkit.auth.security.JwtTokenService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

/**
 * 处理注册、登录和当前用户查询逻辑，为前端提供无状态 JWT 鉴权能力。
 */
@Service
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final CurrentUserProvider currentUserProvider;

    public AuthService(AppUserRepository appUserRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtTokenService jwtTokenService,
                       CurrentUserProvider currentUserProvider) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenService = jwtTokenService;
        this.currentUserProvider = currentUserProvider;
    }

    /**
     * 注册新用户并立即签发 JWT，便于前端在注册成功后直接进入已登录状态。
     *
     * @param request 注册请求体
     * @return 包含 JWT 与当前用户信息的认证响应
     */
    @Transactional
    public AuthTokenResponse register(AuthRegisterRequest request) {
        String normalizedUsername = normalizeUsername(request.getUsername());
        String normalizedEmail = normalizeEmail(request.getEmail());
        validateUniqueUser(normalizedUsername, normalizedEmail);

        AppUser appUser = new AppUser();
        appUser.setUsername(normalizedUsername);
        appUser.setEmail(normalizedEmail);
        appUser.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        AppUser savedUser = appUserRepository.save(appUser);
        AppUserPrincipal principal = AppUserPrincipal.from(savedUser);
        return new AuthTokenResponse(jwtTokenService.generateToken(principal), toProfile(savedUser));
    }

    /**
     * 校验登录凭据并签发新的 JWT，失败时返回统一 401 业务异常。
     *
     * @param request 登录请求体
     * @return 包含 JWT 与当前用户信息的认证响应
     */
    public AuthTokenResponse login(AuthLoginRequest request) {
        try {
            String normalizedIdentifier = normalizeUsername(request.getUsername());
            String resolvedUsername = resolveLoginUsername(normalizedIdentifier);
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(resolvedUsername, request.getPassword())
            );
            AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
            AppUser appUser = appUserRepository.findById(principal.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));
            return new AuthTokenResponse(jwtTokenService.generateToken(principal), toProfile(appUser));
        } catch (BadCredentialsException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
    }

    /**
     * 将登录输入解析为真实用户名：优先按用户名匹配，其次按邮箱匹配，避免要求前端区分两种登录入口。
     *
     * @param identifier 用户输入的用户名或邮箱
     * @return 可交给 Spring Security 认证管理器的真实用户名
     */
    private String resolveLoginUsername(String identifier) {
        return appUserRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(identifier, normalizeEmail(identifier))
                .map(AppUser::getUsername)
                .orElse(identifier);
    }

    /**
     * 返回当前已登录用户概要信息，供前端刷新登录态时复用。
     *
     * @return 当前用户概要信息
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUserProfile() {
        Long userId = currentUserProvider.getCurrentUserId();
        AppUser appUser = appUserRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required"));
        return toProfile(appUser);
    }

    /**
     * 规范化用户名，统一去除首尾空白并转为原样大小写存储下的比较基准。
     *
     * @param username 原始用户名
     * @return 规范化后的用户名
     */
    private String normalizeUsername(String username) {
        return username.trim();
    }

    /**
     * 规范化邮箱，统一转为小写后存储，避免大小写差异导致重复注册。
     *
     * @param email 原始邮箱
     * @return 规范化后的邮箱
     */
    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * 校验用户名和邮箱的唯一性，命中冲突时返回 409 业务异常。
     *
     * @param username 规范化后的用户名
     * @param email 规范化后的邮箱
     */
    private void validateUniqueUser(String username, String email) {
        if (appUserRepository.existsByUsernameIgnoreCase(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }
        if (appUserRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }
    }

    /**
     * 将用户实体映射为前端可消费的安全概要信息，避免泄露密码哈希等敏感字段。
     *
     * @param appUser 用户实体
     * @return 用户概要响应
     */
    private UserProfileResponse toProfile(AppUser appUser) {
        return new UserProfileResponse(appUser.getId(), appUser.getUsername(), appUser.getEmail());
    }
}
