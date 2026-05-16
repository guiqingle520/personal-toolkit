package com.personal.toolkit.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 聚合安全策略管理接口的启动期放行名单，避免在尚未引入角色模型前暴露高风险配置写入能力。
 */
@ConfigurationProperties(prefix = "app.auth.security-policy")
public class SecurityPolicyBootstrapProperties {

    private List<String> bootstrapAllowlist = new ArrayList<>();

    public List<String> getBootstrapAllowlist() {
        return bootstrapAllowlist;
    }

    public void setBootstrapAllowlist(List<String> bootstrapAllowlist) {
        this.bootstrapAllowlist = bootstrapAllowlist == null ? new ArrayList<>() : bootstrapAllowlist;
    }
}
