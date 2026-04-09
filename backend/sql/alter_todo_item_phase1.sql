ALTER TABLE todo_item ADD (
    priority NUMBER(2) DEFAULT 3 NOT NULL,
    due_date TIMESTAMP NULL,
    category VARCHAR2(100 CHAR),
    tags VARCHAR2(500 CHAR)
);

ALTER TABLE todo_item ADD CONSTRAINT ck_todo_item_priority CHECK (priority BETWEEN 1 AND 5);

COMMENT ON COLUMN todo_item.priority IS '优先级，1最低，5最高';
COMMENT ON COLUMN todo_item.due_date IS '截止时间';
COMMENT ON COLUMN todo_item.category IS '待办分类';
COMMENT ON COLUMN todo_item.tags IS '标签，使用逗号分隔';

CREATE INDEX idx_todo_item_status ON todo_item(status);
CREATE INDEX idx_todo_item_priority ON todo_item(priority);
CREATE INDEX idx_todo_item_category ON todo_item(category);
CREATE INDEX idx_todo_item_due_date ON todo_item(due_date);
