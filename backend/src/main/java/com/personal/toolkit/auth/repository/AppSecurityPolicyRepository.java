package com.personal.toolkit.auth.repository;

import com.personal.toolkit.auth.entity.AppSecurityPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 提供全局安全策略的持久化能力，当前版本固定维护单条全局记录。
 */
public interface AppSecurityPolicyRepository extends JpaRepository<AppSecurityPolicy, Long> {
}
