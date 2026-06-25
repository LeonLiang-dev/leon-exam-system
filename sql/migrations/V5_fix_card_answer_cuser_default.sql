SET @card_answer_cuser_exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'wts_card_answer'
      AND COLUMN_NAME = 'CUSER'
);

SET @fix_card_answer_cuser_sql := IF(
    @card_answer_cuser_exists = 1,
    'ALTER TABLE wts_card_answer MODIFY COLUMN CUSER varchar(32) NOT NULL DEFAULT ''''',
    'SELECT 1'
);

PREPARE fix_card_answer_cuser_stmt FROM @fix_card_answer_cuser_sql;
EXECUTE fix_card_answer_cuser_stmt;
DEALLOCATE PREPARE fix_card_answer_cuser_stmt;
