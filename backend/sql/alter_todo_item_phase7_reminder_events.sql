CREATE SEQUENCE todo_reminder_event_seq
    START WITH 1
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

CREATE TABLE todo_reminder_event (
    id NUMBER(19) PRIMARY KEY,
    todo_id NUMBER(19) NOT NULL,
    user_id NUMBER(19) NOT NULL,
    scheduled_at TIMESTAMP NOT NULL,
    status VARCHAR2(32 CHAR) NOT NULL,
    sent_at TIMESTAMP NULL,
    read_at TIMESTAMP NULL,
    dedupe_key VARCHAR2(128 CHAR) NOT NULL,
    create_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    update_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT fk_todo_reminder_event_todo_id FOREIGN KEY (todo_id) REFERENCES todo_item(id),
    CONSTRAINT fk_todo_reminder_event_user_id FOREIGN KEY (user_id) REFERENCES app_user(id),
    CONSTRAINT ck_todo_reminder_event_status CHECK (status IN ('PENDING', 'SENT', 'READ', 'CANCELLED'))
);

COMMENT ON TABLE todo_reminder_event IS 'Todo 站内提醒事件表';
COMMENT ON COLUMN todo_reminder_event.todo_id IS '关联的待办事项主键';
COMMENT ON COLUMN todo_reminder_event.user_id IS '提醒所属用户主键';
COMMENT ON COLUMN todo_reminder_event.scheduled_at IS '计划提醒时间';
COMMENT ON COLUMN todo_reminder_event.status IS '提醒状态';
COMMENT ON COLUMN todo_reminder_event.sent_at IS '提醒进入站内列表的时间';
COMMENT ON COLUMN todo_reminder_event.read_at IS '提醒已读时间';
COMMENT ON COLUMN todo_reminder_event.dedupe_key IS '提醒幂等去重键';
COMMENT ON COLUMN todo_reminder_event.create_time IS '创建时间';
COMMENT ON COLUMN todo_reminder_event.update_time IS '更新时间';

CREATE UNIQUE INDEX uk_todo_reminder_event_dedupe_key ON todo_reminder_event(dedupe_key);
CREATE INDEX idx_todo_reminder_event_user_status ON todo_reminder_event(user_id, status);
CREATE INDEX idx_todo_reminder_event_scheduled_status ON todo_reminder_event(scheduled_at, status);
CREATE INDEX idx_todo_reminder_event_todo_id ON todo_reminder_event(todo_id);
