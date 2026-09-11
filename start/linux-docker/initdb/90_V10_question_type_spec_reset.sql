-- Align existing local deployments with the V1.1 five-question-type specification.
-- This intentionally clears legacy exam business data while preserving auth/system data.

SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wts_card_answer' AND COLUMN_NAME = 'POINT') = 0,
    'ALTER TABLE wts_card_answer ADD COLUMN POINT int DEFAULT 0',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wts_card_answer' AND COLUMN_NAME = 'MPOINT') = 0,
    'ALTER TABLE wts_card_answer ADD COLUMN MPOINT int DEFAULT 0',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wts_card_answer' AND COLUMN_NAME = 'REVIEW_REQUIRED') = 0,
    'ALTER TABLE wts_card_answer ADD COLUMN REVIEW_REQUIRED varchar(1) DEFAULT ''0''',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wts_card_answer' AND COLUMN_NAME = 'REVIEW_REASON') = 0,
    'ALTER TABLE wts_card_answer ADD COLUMN REVIEW_REASON varchar(64) DEFAULT ''''',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wts_card_answer' AND COLUMN_NAME = 'REVIEW_COMMENT') = 0,
    'ALTER TABLE wts_card_answer ADD COLUMN REVIEW_COMMENT varchar(512) DEFAULT ''''',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wts_card_point' AND COLUMN_NAME = 'REVIEW_REQUIRED') = 0,
    'ALTER TABLE wts_card_point ADD COLUMN REVIEW_REQUIRED varchar(1) DEFAULT ''0''',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wts_card_point' AND COLUMN_NAME = 'REVIEW_REASON') = 0,
    'ALTER TABLE wts_card_point ADD COLUMN REVIEW_REASON varchar(64) DEFAULT ''''',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wts_card_point' AND COLUMN_NAME = 'REVIEW_COMMENT') = 0,
    'ALTER TABLE wts_card_point ADD COLUMN REVIEW_COMMENT varchar(512) DEFAULT ''''',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

DELETE FROM wts_card_answer;
DELETE FROM wts_card_point;
DELETE FROM wts_card;

DELETE FROM wts_room_scoreeval;
DELETE FROM wts_room_usergroup;
DELETE FROM wts_room_user;
DELETE FROM wts_room_paper;
DELETE FROM wts_room;

DELETE FROM wts_paper_subject;
DELETE FROM wts_paper_chapter;
DELETE FROM wts_paper_userown;
DELETE FROM wts_paper;

DELETE FROM wts_random_step;
DELETE FROM wts_random_item;

DELETE FROM wts_subject_answer;
DELETE FROM wts_subject_version;
DELETE FROM wts_subject_userown;
DELETE FROM wts_subject;
