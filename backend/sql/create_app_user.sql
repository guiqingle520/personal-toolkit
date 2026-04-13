CREATE SEQUENCE app_user_seq
    START WITH 1
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

CREATE TABLE app_user (
    id NUMBER(19) PRIMARY KEY,
    username VARCHAR2(100 CHAR) NOT NULL,
    email VARCHAR2(255 CHAR) NOT NULL,
    password_hash VARCHAR2(255 CHAR) NOT NULL,
    create_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    update_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT uk_app_user_username UNIQUE (username),
    CONSTRAINT uk_app_user_email UNIQUE (email)
);

COMMENT ON TABLE app_user IS '应用登录用户表';
COMMENT ON COLUMN app_user.username IS '登录用户名';
COMMENT ON COLUMN app_user.email IS '登录邮箱';
COMMENT ON COLUMN app_user.password_hash IS 'BCrypt 密码哈希';
COMMENT ON COLUMN app_user.create_time IS '创建时间';
COMMENT ON COLUMN app_user.update_time IS '更新时间';

CREATE INDEX idx_app_user_username ON app_user(username);
CREATE INDEX idx_app_user_email ON app_user(email);
