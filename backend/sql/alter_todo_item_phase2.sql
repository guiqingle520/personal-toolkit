ALTER TABLE todo_item ADD (
    deleted_at TIMESTAMP NULL
);

COMMENT ON COLUMN todo_item.deleted_at IS '软删除时间';

CREATE INDEX idx_todo_item_deleted_at ON todo_item(deleted_at);
