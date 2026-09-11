SET @room_table_exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'wts_room'
);

SET @fix_room_audit_defaults_sql := IF(
    @room_table_exists = 1,
    'ALTER TABLE wts_room
        MODIFY COLUMN EUSER varchar(32) NOT NULL DEFAULT '''',
        MODIFY COLUMN EUSERNAME varchar(64) NOT NULL DEFAULT '''',
        MODIFY COLUMN CUSER varchar(32) NOT NULL DEFAULT '''',
        MODIFY COLUMN CUSERNAME varchar(64) NOT NULL DEFAULT '''',
        MODIFY COLUMN ETIME varchar(16) NOT NULL DEFAULT '''',
        MODIFY COLUMN CTIME varchar(16) NOT NULL DEFAULT ''''',
    'SELECT 1'
);

PREPARE fix_room_audit_defaults_stmt FROM @fix_room_audit_defaults_sql;
EXECUTE fix_room_audit_defaults_stmt;
DEALLOCATE PREPARE fix_room_audit_defaults_stmt;
