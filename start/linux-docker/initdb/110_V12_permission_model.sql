-- V12: 用户职位与权限模型
-- 在 alone_auth_user 增加 职位(post)/权限点(perms)/班级(class_name) 三列，
-- 并按旧 type 兼容回填 post：
--   type=3 超级管理员 -> platform_admin（学院领导/平台管理员）
--   type=2 学生       -> student
--   type=1 管理员     -> teacher（教师，默认获得发布考试/试题管理/导入班级权限）

-- 1) 加列（幂等：列不存在才加）
SET @col := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'alone_auth_user' AND COLUMN_NAME = 'POST'
);
SET @sql := IF(@col = 0,
  'ALTER TABLE alone_auth_user ADD COLUMN POST varchar(20) NOT NULL DEFAULT ''student'' AFTER TYPE',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'alone_auth_user' AND COLUMN_NAME = 'PERMS'
);
SET @sql := IF(@col = 0,
  'ALTER TABLE alone_auth_user ADD COLUMN PERMS varchar(255) DEFAULT NULL AFTER POST',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'alone_auth_user' AND COLUMN_NAME = 'CLASS_NAME'
);
SET @sql := IF(@col = 0,
  'ALTER TABLE alone_auth_user ADD COLUMN CLASS_NAME varchar(100) DEFAULT NULL AFTER PERMS',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2) 按旧 type 回填 post（仅对未设置过的存量数据）
UPDATE alone_auth_user SET POST = 'platform_admin' WHERE POST = 'student' AND TYPE = '3';
UPDATE alone_auth_user SET POST = 'student' WHERE POST = 'student' AND TYPE = '2';

-- type=1 账号升级为教师：默认获得发布考试/试题管理/导入班级权限
UPDATE alone_auth_user
SET POST = 'teacher',
    PERMS = COALESCE(NULLIF(PERMS, ''), 'EXAM_PUBLISH,SUBJECT_MANAGE,CLASS_IMPORT')
WHERE POST = 'student' AND TYPE = '1';

-- 2.5) 平台管理员 PERMS 置空（空 = 全部权限，由代码恒放行）
UPDATE alone_auth_user SET PERMS = NULL WHERE POST = 'platform_admin';