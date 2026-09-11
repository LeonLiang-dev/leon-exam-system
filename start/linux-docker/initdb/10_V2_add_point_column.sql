SET @column_exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'wts_subject'
      AND COLUMN_NAME = 'POINT'
);

SET @add_point_sql := IF(
    @column_exists = 0,
    'ALTER TABLE wts_subject ADD COLUMN POINT INT DEFAULT 1 AFTER LEVEL',
    'SELECT 1'
);

PREPARE add_point_stmt FROM @add_point_sql;
EXECUTE add_point_stmt;
DEALLOCATE PREPARE add_point_stmt;
