package com.personal.toolkit.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.personal.toolkit.auth.entity.AppUser;
import com.personal.toolkit.auth.service.PasswordPolicyService;
import com.personal.toolkit.common.api.ApiErrorResponse;
import com.personal.toolkit.common.exception.ApiException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 从 Authorization Bearer 头中解析 JWT，并将对应用户主体写入安全上下文。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String CHANGE_PASSWORD_PATH = "/api/auth/change-password";
    private static final String LOGOUT_PATH = "/api/auth/logout";

    private final JwtTokenService jwtTokenService;
    private final PasswordPolicyService passwordPolicyService;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService,
                                   PasswordPolicyService passwordPolicyService,
                                   ObjectMapper objectMapper) {
        this.jwtTokenService = jwtTokenService;
        this.passwordPolicyService = passwordPolicyService;
        this.objectMapper = objectMapper;
    }

    /**
     * 解析请求头中的 Bearer Token，校验通过后将用户身份写入当前请求的安全上下文。
     *
     * @param request 当前请求
     * @param response 当前响应
     * @param filterChain 后续过滤器链
     * @throws ServletException Servlet 异常
     * @throws IOException IO 异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        Claims claims;
        try {
            claims = jwtTokenService.parseToken(token);
        } catch (JwtException | IllegalArgumentException ex) {
            filterChain.doFilter(request, response);
            return;
        }

        AppUser appUser = passwordPolicyService.findUserForClaims(claims).orElse(null);
        if (appUser == null) {
            filterChain.doFilter(request, response);
            return;
        }

        java.util.Optional<ApiException> policyViolation = passwordPolicyService.resolvePasswordPolicyViolation(appUser);
        if (policyViolation.isPresent() && !isPasswordPolicyBypassPath(request)) {
            writePolicyErrorResponse(request, response, policyViolation.get());
            return;
        }

        AppUserPrincipal principal = AppUserPrincipal.from(appUser);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        );
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

    private boolean isPasswordPolicyBypassPath(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        return CHANGE_PASSWORD_PATH.equals(requestUri) || LOGOUT_PATH.equals(requestUri);
    }

    private void writePolicyErrorResponse(HttpServletRequest request,
                                          HttpServletResponse response,
                                          ApiException exception) throws IOException {
        response.setStatus(exception.getStatusCode().value());
        response.setContentType(org.springframework.http.MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiErrorResponse.of(
                exception.getCode(),
                exception.getMessage(),
                exception.getStatusCode().value(),
                request.getRequestURI()
        ));
    }
}
