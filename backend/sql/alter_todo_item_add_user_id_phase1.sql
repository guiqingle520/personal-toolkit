DECLARE
    v_column_count NUMBER := 0;
BEGIN
    SELECT COUNT(*)
      INTO v_column_count
      FROM user_tab_columns
     WHERE table_name = 'TODO_ITEM'
       AND column_name = 'USER_ID';

    IF v_column_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE todo_item ADD user_id NUMBER(19)';
    END IF;

    EXECUTE IMMEDIATE q'[COMMENT ON COLUMN todo_item.user_id IS '所属用户主键']';
END;
/
