CREATE TABLE app_security_policy (
    id NUMBER(19) PRIMARY KEY,
    access_token_ttl_seconds NUMBER(19),
    password_expiry_enabled NUMBER(1) DEFAULT 0 NOT NULL,
    password_expiry_days NUMBER(10),
    create_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    update_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT ck_app_security_policy_singleton CHECK (id = 1),
    CONSTRAINT ck_app_security_policy_access_ttl_positive CHECK (
        access_token_ttl_seconds IS NULL OR access_token_ttl_seconds > 0
    ),
    CONSTRAINT ck_app_security_policy_expiry_days_positive CHECK (
        password_expiry_days IS NULL OR password_expiry_days > 0
    ),
    CONSTRAINT ck_app_security_policy_expiry_consistency CHECK (
        password_expiry_enabled = 0 OR password_expiry_days IS NOT NULL
    )
);

COMMENT ON TABLE app_security_policy IS '应用全局运行时安全策略表';
COMMENT ON COLUMN app_security_policy.access_token_ttl_seconds IS '新签发访问令牌 TTL（秒），为空时回退到静态 JWT 配置';
COMMENT ON COLUMN app_security_policy.password_expiry_enabled IS '是否启用密码过期拦截';
COMMENT ON COLUMN app_security_policy.password_expiry_days IS '密码过期天数';
COMMENT ON COLUMN app_security_policy.create_time IS '创建时间';
COMMENT ON COLUMN app_security_policy.update_time IS '更新时间';

INSERT INTO app_security_policy (
    id,
    access_token_ttl_seconds,
    password_expiry_enabled,
    password_expiry_days
) VALUES (
    1,
    NULL,
    0,
    NULL
);
