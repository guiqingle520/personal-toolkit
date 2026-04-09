CREATE SEQUENCE todo_item_seq
    START WITH 1
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

CREATE TABLE todo_item (
    id NUMBER(19) PRIMARY KEY,
    title VARCHAR2(200 CHAR) NOT NULL,
    status VARCHAR2(32 CHAR) DEFAULT 'PENDING' NOT NULL,
    priority NUMBER(2) DEFAULT 3 NOT NULL,
    due_date TIMESTAMP NULL,
    category VARCHAR2(100 CHAR),
    tags VARCHAR2(500 CHAR),
    deleted_at TIMESTAMP NULL,
    create_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    update_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT ck_todo_item_status CHECK (status IN ('PENDING', 'DONE')),
    CONSTRAINT ck_todo_item_priority CHECK (priority BETWEEN 1 AND 5)
);

COMMENT ON TABLE todo_item IS '个人待办事项表';
COMMENT ON COLUMN todo_item.title IS '待办标题';
COMMENT ON COLUMN todo_item.status IS '待办状态';
COMMENT ON COLUMN todo_item.priority IS '优先级，1最低，5最高';
COMMENT ON COLUMN todo_item.due_date IS '截止时间';
COMMENT ON COLUMN todo_item.category IS '待办分类';
COMMENT ON COLUMN todo_item.tags IS '标签，使用逗号分隔';
COMMENT ON COLUMN todo_item.deleted_at IS '软删除时间';
COMMENT ON COLUMN todo_item.create_time IS '创建时间';
COMMENT ON COLUMN todo_item.update_time IS '更新时间';

CREATE INDEX idx_todo_item_status ON todo_item(status);
CREATE INDEX idx_todo_item_priority ON todo_item(priority);
CREATE INDEX idx_todo_item_category ON todo_item(category);
CREATE INDEX idx_todo_item_due_date ON todo_item(due_date);
CREATE INDEX idx_todo_item_deleted_at ON todo_item(deleted_at);
