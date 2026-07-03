# 数据库说明

## 数据库信息

- 数据库名: `wts`
- 字符集: `utf8mb4`
- 排序规则: `utf8mb4_general_ci`
- 引擎: InnoDB

## 初始化脚本

| 文件 | 用途 |
|------|------|
| `sql/init/wts.v1.4.1.sql` | 完整初始化脚本，包含基础权限、管理员账号和示例/历史数据 |
| `sql/init/wts.empty.sql` | 空库结构脚本，仅包含表结构，不包含 `INSERT` 数据 |
| `sql/migrations/V2_add_point_column.sql` | v2.0 增量迁移，给题目表补充分值字段 |
| `sql/migrations/V10_question_type_spec_reset.sql` | v3.0 题型规范迁移，清理旧考试业务数据并补充阅卷字段 |

## 核心表结构

### 题目相关

| 表名 | 说明 | 主键 |
|------|------|------|
| wts_subject | 题目主表 | id (UUID) |
| wts_subject_version | 题目版本 | id (UUID) |
| wts_subject_answer | 题目答案 | id (UUID) |
| wts_subject_type | 题目分类 | id (UUID) |

**wts_subject 关键字段：**
```sql
id          VARCHAR(32)   -- UUID主键
typeid      VARCHAR(32)   -- 分类ID
versionid   VARCHAR(32)   -- 当前版本ID
pstate      VARCHAR(10)   -- 状态: 1=正常, 0=已删除
introduction TEXT          -- 题目简介
level       INT           -- 难度: 1=简单, 2=中等, 3=困难
point       INT           -- 默认分值 (v2.0新增)
donum       INT           -- 使用次数
```

**wts_subject_version 关键字段：**
```sql
id          VARCHAR(32)   -- UUID主键
subjectid   VARCHAR(32)   -- 所属题目ID
tiptype     VARCHAR(10)   -- 题型: 1=填空, 2=单选, 3=多选, 4=判断, 5=问答, 6=附件
tipstr      TEXT          -- 题目内容
tipnote     TEXT          -- 题目说明
pcontent    TEXT          -- 附件内容
pstate      VARCHAR(10)   -- 状态
ctime       VARCHAR(20)   -- 创建时间 (yyyyMMddHHmmss)
cuser       VARCHAR(32)   -- 创建人ID
cusername   VARCHAR(100)  -- 创建人姓名
```

**wts_subject_answer 关键字段：**
```sql
id          VARCHAR(32)   -- UUID主键
versionid   VARCHAR(32)   -- 所属版本ID
answer      TEXT          -- 选项内容
rightanswer VARCHAR(10)   -- 是否正确: 0=错误, 1=正确
sort        INT           -- 排序
pointweight INT           -- 分值权重
answernote  TEXT          -- 答案说明
pstate      VARCHAR(10)   -- 状态
```

### 试卷相关

| 表名 | 说明 |
|------|------|
| wts_paper | 试卷主表 |
| wts_paper_chapter | 试卷章节 |
| wts_paper_subject | 试卷题目关联 |

**wts_paper_subject 关键字段：**
```sql
id          VARCHAR(32)   -- UUID主键
paperid     VARCHAR(32)   -- 试卷ID
subjectid   VARCHAR(32)   -- 题目ID
versionid   VARCHAR(32)   -- 题目版本ID
chapterid   VARCHAR(32)   -- 章节ID
sort        INT           -- 排序
point       INT           -- 本题分值（可覆盖题目默认分值）
```

### 答题室相关

| 表名 | 说明 |
|------|------|
| wts_room | 答题室主表 |
| wts_room_paper | 答题室试卷关联 |
| wts_room_user | 答题室用户 |

**wts_room 关键字段：**
```sql
id          VARCHAR(32)   -- UUID主键
name        VARCHAR(200)  -- 答题室名称
pstate      VARCHAR(10)   -- 状态: 11=草稿, 21=已发布, 31=已关闭
starttime   VARCHAR(20)   -- 开始时间
endtime     VARCHAR(20)   -- 结束时间
timelen     INT           -- 答题时长(分钟)
```

### 答卷相关

| 表名 | 说明 |
|------|------|
| wts_card | 答卷主表 |
| wts_card_answer | 答卷答案 |
| wts_card_point | 答卷得分 |

**wts_card 关键字段：**
```sql
id          VARCHAR(32)   -- UUID主键
roomid      VARCHAR(32)   -- 答题室ID
paperid     VARCHAR(32)   -- 试卷ID
userid      VARCHAR(32)   -- 用户ID
pstate      VARCHAR(10)   -- 状态: 11=答题中, 16=已提交, 21=已阅卷
starttime   VARCHAR(20)   -- 开始时间
endtime     VARCHAR(20)   -- 提交时间
totalscore  INT           -- 总分
```

**wts_card_point 关键字段：**
```sql
id          VARCHAR(32)   -- UUID主键
cardid      VARCHAR(32)   -- 答卷ID
subjectid   VARCHAR(32)   -- 题目ID
versionid   VARCHAR(32)   -- 版本ID
point       INT           -- 得分
complete    VARCHAR(10)   -- 是否作答: 0=未答, 1=已答
```

### 用户相关

| 表名 | 说明 |
|------|------|
| alone_auth_user | 用户表 |
| alone_auth_organization | 组织机构 |

**alone_auth_user 关键字段：**
```sql
id          VARCHAR(32)   -- UUID主键
loginname   VARCHAR(100)  -- 登录名
password    VARCHAR(200)  -- 密码(BCrypt)
name        VARCHAR(100)  -- 用户名
type        VARCHAR(10)   -- 类型: 1=管理员, 2=学生, 3=超级管理员
state       VARCHAR(10)   -- 状态: 1=正常, 0=禁用
```

## 状态枚举速查

### 答题室 (wts_room.pstate)
| 值 | 含义 |
|----|------|
| 11 | 草稿 |
| 21 | 已发布 |
| 31 | 已关闭 |

### 答卷 (wts_card.pstate)
| 值 | 含义 |
|----|------|
| 11 | 答题中 |
| 16 | 已提交 |
| 21 | 已阅卷 |

### 题型 (wts_subject_version.tiptype)
| 值 | 含义 |
|----|------|
| 1 | 填空题 |
| 2 | 单选题 |
| 3 | 多选题 |
| 4 | 判断题 |
| 5 | 问答题 |
| 6 | 附件题 |

## 迁移SQL

### V2_add_point_column.sql
```sql
ALTER TABLE wts_subject ADD COLUMN point INT DEFAULT 1 AFTER level;
```
为题目表新增默认分值字段，默认值为1。

## 常用SQL

```sql
-- 查看所有题目
SELECT id, introduction, level, point, pstate FROM wts_subject WHERE pstate = '1';

-- 查看答题室统计
SELECT r.name, COUNT(c.id) as card_count
FROM wts_room r LEFT JOIN wts_card c ON r.id = c.roomid
WHERE r.pstate = '21'
GROUP BY r.id;

-- 查看某答卷的得分明细
SELECT s.introduction, cp.point, cp.complete
FROM wts_card_point cp
JOIN wts_subject s ON cp.subjectid = s.id
WHERE cp.cardid = '卡片ID'
ORDER BY cp.id;
```
