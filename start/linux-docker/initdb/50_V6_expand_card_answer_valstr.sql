SET @card_answer_valstr_exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'wts_card_answer'
      AND COLUMN_NAME = 'VALSTR'
);

SET @expand_card_answer_valstr_sql := IF(
    @card_answer_valstr_exists = 1,
    'ALTER TABLE wts_card_answer MODIFY COLUMN VALSTR mediumtext NOT NULL',
    'SELECT 1'
);

PREPARE expand_card_answer_valstr_stmt FROM @expand_card_answer_valstr_sql;
EXECUTE expand_card_answer_valstr_stmt;
DEALLOCATE PREPARE expand_card_answer_valstr_stmt;
