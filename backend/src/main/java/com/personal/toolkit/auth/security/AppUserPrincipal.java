package com.personal.toolkit.auth.security;

import com.personal.toolkit.auth.entity.AppUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * 封装认证用户的主键与凭据，供 JWT 过滤器和业务层共享当前用户标识。
 */
public class AppUserPrincipal implements UserDetails {

    private final Long id;
    private final String username;
    private final String email;
    private final String passwordHash;

    public AppUserPrincipal(Long id, String username, String email, String passwordHash) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    /**
     * 基于应用用户实体构建鉴权主体对象，避免多处手写字段拷贝。
     *
     * @param user 已持久化用户实体
     * @return 对应的鉴权主体对象
     */
    public static AppUserPrincipal from(AppUser user) {
        return new AppUserPrincipal(user.getId(), user.getUsername(), user.getEmail(), user.getPasswordHash());
    }

    /**
     * 返回当前用户主键。
     *
     * @return 当前用户主键
     */
    public Long getId() {
        return id;
    }

    /**
     * 返回当前用户邮箱。
     *
     * @return 当前用户邮箱
     */
    public String getEmail() {
        return email;
    }

    /**
     * 返回当前用户拥有的权限集合，当前版本固定授予基础 USER 角色。
     *
     * @return 权限集合
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    /**
     * 返回加密后的登录密码。
     *
     * @return 加密后的登录密码
     */
    @Override
    public String getPassword() {
        return passwordHash;
    }

    /**
     * 返回登录用户名。
     *
     * @return 登录用户名
     */
    @Override
    public String getUsername() {
        return username;
    }

    /**
     * 当前版本不支持禁用状态，固定返回 true。
     *
     * @return 账户是否未过期
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * 当前版本不支持锁定状态，固定返回 true。
     *
     * @return 账户是否未锁定
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * 当前版本不支持凭据过期状态，固定返回 true。
     *
     * @return 凭据是否未过期
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * 当前版本不支持禁用状态，固定返回 true。
     *
     * @return 账户是否启用
     */
    @Override
    public boolean isEnabled() {
        return true;
    }
}
