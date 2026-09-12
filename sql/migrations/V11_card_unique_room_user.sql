-- V11: 答卷唯一性约束
-- 同一用户在同一答题室只允许存在一张答卷，防止并发/重复进入产生重复答卷。
-- 清理历史重复数据（保留每组中 id 最大的一张），再加唯一索引（幂等）。

-- 1) 删除重复答卷的答题明细（同一 roomid+userid 且 id 不是最大者）
DELETE FROM wts_card_answer
WHERE cardid IN (
  SELECT id FROM (
    SELECT c1.id
    FROM wts_card c1
    JOIN wts_card c2 ON c1.roomid = c2.roomid AND c1.userid = c2.userid AND c1.id < c2.id
  ) AS dup_card
);

-- 2) 删除重复答卷的得分明细
DELETE FROM wts_card_point
WHERE cardid IN (
  SELECT id FROM (
    SELECT c1.id
    FROM wts_card c1
    JOIN wts_card c2 ON c1.roomid = c2.roomid AND c1.userid = c2.userid AND c1.id < c2.id
  ) AS dup_card
);

-- 3) 删除重复答卷
DELETE FROM wts_card
WHERE id IN (
  SELECT id FROM (
    SELECT c1.id
    FROM wts_card c1
    JOIN wts_card c2 ON c1.roomid = c2.roomid AND c1.userid = c2.userid AND c1.id < c2.id
  ) AS dup_card
);

-- 4) 唯一索引（已存在则跳过）
SET @idx_exists := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wts_card' AND INDEX_NAME = 'uk_card_room_user'
);
SET @add_idx_sql := IF(
  @idx_exists = 0,
  'ALTER TABLE wts_card ADD UNIQUE INDEX uk_card_room_user (roomid, userid)',
  'SELECT 1'
);
PREPARE stmt FROM @add_idx_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;