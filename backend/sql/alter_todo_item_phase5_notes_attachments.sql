ALTER TABLE todo_item
    ADD notes VARCHAR2(2000 CHAR) NULL;

ALTER TABLE todo_item
    ADD attachment_links VARCHAR2(2000 CHAR) NULL;

COMMENT ON COLUMN todo_item.notes IS '任务备注';
COMMENT ON COLUMN todo_item.attachment_links IS '附件链接，使用换行分隔';
