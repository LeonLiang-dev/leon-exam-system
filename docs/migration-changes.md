# 重构变更清单

本文档记录从 Spring 4 + Hibernate + JSP 重构为 Spring Boot 3 + React + MyBatis-Plus 的所有变更。

## 一、架构迁移

| 项目 | 旧版 | 新版 |
|------|------|------|
| 后端框架 | Spring 4 MVC | Spring Boot 3.3.6 |
| JDK | JDK 8 | JDK 17 (Temurin 17.0.19) |
| ORM | Hibernate 4 | MyBatis-Plus 3.5.7 |
| 安全框架 | 自定义 Filter | Spring Security 6 + JWT |
| 前端 | JSP + jQuery | React 18 + TypeScript + UmiJS 4 |
| UI库 | Bootstrap | Ant Design 5 + Pro Components |
| 构建 | Maven + WAR | Maven + JAR（内嵌前端） |
| 数据库 | MySQL 5.x | MySQL 8.0 |

## 二、Bug修复（26项）

### 后端修复（CardServiceImpl.java — 8项）

1. **答题室状态检查顺序** — `enterRoom()` 中将房间状态检查移到查找已有答卷之前，避免房间未开放时返回旧答卷
2. **试卷排序** — `ExamRoomPaper` 排序改用 `getId()`（原 `getSort()` 字段不存在）
3. **提交后防修改** — `saveAnswers()` 增加 `pstate === "11"` 检查，已提交答卷禁止修改
4. **答卷所有权校验** — `submit()` 增加 `userId.equals(card.getUserid())` 检查
5. **自动评分去重** — `autoGrade()` 先删除旧的 CardPoint 再插入，防止重复评分记录
6. **评分完成标记** — `autoGrade()` 中 `cp.setComplete(hasAnswer ? "1" : "0")`，未答题标记为0
7. **阅卷状态校验** — `judge()` 增加 `pstate === "16" || "21"` 检查，只有已提交答卷才能批改
8. **阅卷总分一致性** — `judge()` 更新分数后重新查询数据库计算总分

### 后端修复（RoomServiceImpl.java — 1项）

9. **答题室状态筛选** — 支持逗号分隔的多状态查询 `pstate=21,31`

### 后端修复（其他 — 3项）

10. **答题室关闭检查** — `getExamPaper()` 增加房间已关闭状态检查
11. **ExamSubject 实体** — 新增 `point` 字段（默认分值）
12. **Dashboard 统计API** — 新增 `GET /api/v1/dashboard/stats` 接口

### 前端修复（Card/index.tsx — 答题页 — 5项）

13. **计时器精度** — 从 `Statistic.Countdown` 改为基于 `starttime` 计算的真实剩余时间
14. **自动提交** — `useEffect` 监听 `remaining === 0` 触发，避免闭包过期
15. **浏览器返回** — `history.replace` 替换历史记录，防止返回到答题页
16. **重复提交** — `submitted` ref 防止多次提交
17. **已提交重入** — catch 中检测"已提交"消息，直接跳转结果页

### 前端修复（Card/Result.tsx — 成绩页 — 2项）

18. **待阅卷显示** — `pstate === "16"` 时显示"待阅卷"而非0分
19. **待批改题目** — 客观题已答但得分为0时显示"待批改"状态

### 前端修复（Card/Judge.tsx — 阅卷页 — 3项）

20. **只发送主观题分数** — 过滤 `tiptype === "5" || "6"` 的题目，避免覆盖客观题自动评分
21. **答案/分数 Map key 大小写** — 统一使用 `a.versionid || a.versionId` 兼容后端返回格式
22. **未评分支线确认** — 有未评分题目时弹出确认对话框

### 前端修复（Login + app.tsx — 登录跳转 — 3项）

23. **管理员登录闪现学生页** — `window.location.href` 替代 `history.push`，强制全页面刷新确保 `getInitialState` 先于路由判断执行
24. **用户类型检查** — `onPageChange` 中增加 `userType &&` guard，防止状态未加载时误判
25. **路径匹配** — 使用 `pathname === p || pathname.startsWith(p + '/')` 精确匹配
26. **响应拦截器** — 检查 `code !== 200` reject 请求，Blob 类型自动跳过

## 三、功能增强

### 题目默认分值
- `ExamSubject` 新增 `point` 字段
- 题库管理页面增加"默认分值"列和表单字段
- 试卷管理批量添加时自动填入题目默认分值，支持手动修改

### 批量添加试卷题目
- 试卷管理"管理题目"弹窗支持多选题目
- 显示已选题目数量和总分预览
- 题目选项显示题型和默认分值

### 仪表盘数据面板
- 新增 `DashboardController` 统计API
- 前端展示：题目总数、试卷总数、已发布答题室、答卷数量、用户数量
- 加载状态和响应式布局

