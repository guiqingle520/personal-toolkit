CREATE SEQUENCE todo_saved_view_seq
    START WITH 1
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

CREATE TABLE todo_saved_view (
    id NUMBER(19) PRIMARY KEY,
    user_id NUMBER(19) NOT NULL,
    name VARCHAR2(100 CHAR) NOT NULL,
    is_default NUMBER(1) DEFAULT 0 NOT NULL,
    filters_json CLOB NOT NULL,
    create_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    update_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT fk_todo_saved_view_user_id FOREIGN KEY (user_id) REFERENCES app_user(id),
    CONSTRAINT ck_todo_saved_view_default CHECK (is_default IN (0, 1))
);

COMMENT ON TABLE todo_saved_view IS 'Todo 已保存筛选视图表';
COMMENT ON COLUMN todo_saved_view.user_id IS '视图所属用户主键';
COMMENT ON COLUMN todo_saved_view.name IS '保存视图名称';
COMMENT ON COLUMN todo_saved_view.is_default IS '是否为默认视图，1 表示是';
COMMENT ON COLUMN todo_saved_view.filters_json IS '筛选字段 JSON';
COMMENT ON COLUMN todo_saved_view.create_time IS '创建时间';
COMMENT ON COLUMN todo_saved_view.update_time IS '更新时间';

CREATE UNIQUE INDEX uk_todo_saved_view_user_name ON todo_saved_view(user_id, name);
CREATE INDEX idx_todo_saved_view_user_default ON todo_saved_view(user_id, is_default);
