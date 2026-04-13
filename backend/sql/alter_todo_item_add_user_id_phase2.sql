DECLARE
    v_null_count NUMBER := 0;
    v_not_null_count NUMBER := 0;
    v_fk_count NUMBER := 0;
    v_idx_user_count NUMBER := 0;
    v_idx_user_deleted_count NUMBER := 0;
BEGIN
    UPDATE todo_item
       SET user_id = :legacy_user_id
     WHERE user_id IS NULL;

    SELECT COUNT(*)
      INTO v_null_count
      FROM todo_item
     WHERE user_id IS NULL;

    IF v_null_count > 0 THEN
        RAISE_APPLICATION_ERROR(-20001, 'todo_item.user_id still contains NULL values after legacy backfill.');
    END IF;

    SELECT COUNT(*)
      INTO v_not_null_count
      FROM user_tab_columns
     WHERE table_name = 'TODO_ITEM'
       AND column_name = 'USER_ID'
       AND nullable = 'N';

    IF v_not_null_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE todo_item MODIFY user_id NOT NULL';
    END IF;

    SELECT COUNT(*)
      INTO v_fk_count
      FROM user_constraints
     WHERE table_name = 'TODO_ITEM'
       AND constraint_name = 'FK_TODO_ITEM_USER_ID';

    IF v_fk_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE todo_item ADD CONSTRAINT fk_todo_item_user_id FOREIGN KEY (user_id) REFERENCES app_user(id)';
    END IF;

    SELECT COUNT(*)
      INTO v_idx_user_count
      FROM user_indexes
     WHERE table_name = 'TODO_ITEM'
       AND index_name = 'IDX_TODO_ITEM_USER_ID';

    IF v_idx_user_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX idx_todo_item_user_id ON todo_item(user_id)';
    END IF;

    SELECT COUNT(*)
      INTO v_idx_user_deleted_count
      FROM user_indexes
     WHERE table_name = 'TODO_ITEM'
       AND index_name = 'IDX_TODO_ITEM_USER_DELETED';

    IF v_idx_user_deleted_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX idx_todo_item_user_deleted ON todo_item(user_id, deleted_at)';
    END IF;
END;
/
