ALTER TABLE todo_item
    ADD remind_at TIMESTAMP NULL;

COMMENT ON COLUMN todo_item.remind_at IS '提醒时间';

CREATE INDEX idx_todo_item_remind_at ON todo_item(remind_at);
