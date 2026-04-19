ALTER TABLE todo_item
    ADD owner_label VARCHAR2(100 CHAR) NULL;

ALTER TABLE todo_item
    ADD collaborators VARCHAR2(1000 CHAR) NULL;

ALTER TABLE todo_item
    ADD watchers VARCHAR2(1000 CHAR) NULL;

COMMENT ON COLUMN todo_item.owner_label IS '任务负责人显示名';
COMMENT ON COLUMN todo_item.collaborators IS '协作人，占位字段，使用逗号分隔';
COMMENT ON COLUMN todo_item.watchers IS '观察者，占位字段，使用逗号分隔';
