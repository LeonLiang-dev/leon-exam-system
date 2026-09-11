SET @overtime_column_exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'wts_card'
      AND COLUMN_NAME = 'OVERTIME'
);

SET @fix_overtime_sql := IF(
    @overtime_column_exists = 1,
    'ALTER TABLE wts_card MODIFY COLUMN OVERTIME varchar(2) NOT NULL DEFAULT ''0''',
    'SELECT 1'
);

PREPARE fix_overtime_stmt FROM @fix_overtime_sql;
EXECUTE fix_overtime_stmt;
DEALLOCATE PREPARE fix_overtime_stmt;
