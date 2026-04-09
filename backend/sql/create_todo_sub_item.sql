CREATE TABLE todo_sub_item (
    id NUMBER(19) PRIMARY KEY,
    todo_id NUMBER(19) NOT NULL,
    title VARCHAR2(200) NOT NULL,
    status VARCHAR2(32) DEFAULT 'PENDING' NOT NULL,
    sort_order NUMBER(10) DEFAULT 0 NOT NULL,
    create_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    update_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL
);

ALTER TABLE todo_sub_item
ADD CONSTRAINT fk_todo_sub_item_todo_id
FOREIGN KEY (todo_id) REFERENCES todo_item(id);

ALTER TABLE todo_sub_item
ADD CONSTRAINT chk_todo_sub_item_status
CHECK (status IN ('PENDING', 'DONE'));

CREATE SEQUENCE todo_sub_item_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE INDEX idx_todo_sub_item_todo_id ON todo_sub_item(todo_id);
CREATE INDEX idx_todo_sub_item_todo_sort ON todo_sub_item(todo_id, sort_order);
CREATE INDEX idx_todo_sub_item_todo_status ON todo_sub_item(todo_id, status);
