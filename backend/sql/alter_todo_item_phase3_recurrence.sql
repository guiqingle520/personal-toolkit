ALTER TABLE todo_item ADD (
    recurrence_type VARCHAR2(32 CHAR) DEFAULT 'NONE' NOT NULL,
    recurrence_interval NUMBER(4) DEFAULT 1 NOT NULL,
    recurrence_end_time TIMESTAMP NULL,
    next_trigger_time TIMESTAMP NULL,
    completed_at TIMESTAMP NULL
);

ALTER TABLE todo_item ADD CONSTRAINT ck_todo_item_recurrence_type
    CHECK (recurrence_type IN ('NONE', 'DAILY', 'WEEKLY', 'MONTHLY'));

ALTER TABLE todo_item ADD CONSTRAINT ck_todo_item_recurrence_interval
    CHECK (recurrence_interval >= 1);

COMMENT ON COLUMN todo_item.recurrence_type IS '重复任务类型，支持 NONE / DAILY / WEEKLY / MONTHLY';
COMMENT ON COLUMN todo_item.recurrence_interval IS '重复任务间隔，最小为 1';
COMMENT ON COLUMN todo_item.recurrence_end_time IS '重复任务截止时间，超过后不再生成下一次';
COMMENT ON COLUMN todo_item.next_trigger_time IS '下一次触发时间，用于生成下一条重复任务';
COMMENT ON COLUMN todo_item.completed_at IS '任务完成时间';

CREATE INDEX idx_todo_item_next_trigger_time ON todo_item(next_trigger_time);
CREATE INDEX idx_todo_item_recurrence_type ON todo_item(recurrence_type);
