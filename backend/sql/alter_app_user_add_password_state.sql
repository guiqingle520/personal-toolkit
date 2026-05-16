ALTER TABLE app_user ADD (
    password_change_required NUMBER(1) DEFAULT 0 NOT NULL,
    password_changed_at TIMESTAMP
);

UPDATE app_user
SET password_change_required = 0
WHERE password_change_required IS NULL;

UPDATE app_user
SET password_changed_at = SYSTIMESTAMP
WHERE password_changed_at IS NULL;

ALTER TABLE app_user MODIFY (
    password_changed_at TIMESTAMP NOT NULL
);

COMMENT ON COLUMN app_user.password_change_required IS '是否要求用户在放行其他接口前先修改密码';
COMMENT ON COLUMN app_user.password_changed_at IS '密码最近一次修改时间';
