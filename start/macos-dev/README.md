# macOS 本地 Docker 全栈调试

在 macOS 上用 Docker 起一整套调试环境：`mariadb` + 后端（JDK 17 + Maven）+ 前端（Node 20 + Umi dev server），
源码通过 bind mount 挂载进容器，改完代码即时生效，无需在本机安装 JDK / Node。

## 架构

```text
浏览器 http://localhost:8000
   │
   ▼
frontend 容器 (node:20-alpine, npm run dev, HMR)
   │  /api 代理 → http://backend:8080  (PROXY_TARGET)
   ▼
backend 容器 (maven:3.9-eclipse-temurin-17, mvn spring-boot:run, dev)
   │  数据源 → jdbc:mysql://db:3306/wts
   ▼
db 容器 (mariadb:10.11, 首次启动自动执行 initdb SQL)
```

源码挂载：

| 容器 | 挂载目录 | 说明 |
|------|----------|------|
| backend | `../../wts-server` → `/workspace` | 改后端代码后 `docker compose restart backend` |
| frontend | `../../wts-web` → `/workspace` | 改前端代码 HMR 自动生效 |
| db | `../linux-docker/initdb` → `/docker-entrypoint-initdb.d` | 复用机房部署的初始化 SQL，单一数据源 |

## 前置要求

- macOS + Docker Desktop（已登录启动）。
- 仓库已克隆到本机（脚本依赖 `git` 和 Docker）。

## 首次启动

```bash
cd start/macos-dev

# 可选：定制端口/密码
cp .env.example .env

docker compose up -d
```

首次启动会做两件事，需要几分钟：

1. backend 容器内 Maven 下载依赖（已挂载 `m2-repo` 卷缓存，后续秒级启动）；
2. frontend 容器内 `npm install`（已挂载 `node-modules` 卷缓存）。

启动完成后：

```bash
docker compose ps                 # 三个容器应为 running/healthy
curl http://localhost:8080/api/v1/health   # 后端健康检查
```

浏览器访问 **http://localhost:8000**，默认管理员 `sysadmin` / `123456`。

## 日常开发

| 场景 | 操作 |
|------|------|
| 改后端 Java 代码 | `docker compose restart backend`（启动时自动重新编译） |
| 改前端代码 | 无需操作，Umi HMR 自动热更新 |
| 新增前端依赖 | `docker compose exec frontend npm install --registry=https://registry.npmmirror.com` |
| 查看日志 | `docker compose logs -f backend` / `docker compose logs -f frontend` |
| 停止 | `docker compose down`（保留数据卷） |
| 重置数据库 | `docker compose down -v && docker compose up -d`（清空所有考试数据） |

## 自动更新代码

`update.sh` 会从 git 远端拉取最新代码，并按变更范围自动重启对应容器：

```bash
./update.sh
```

- 后端/前端代码有更新 → 重启对应容器（后端重启即重新编译）；
- `start/macos-dev/` 配置或脚本有更新 → 整体重建容器；
- SQL 有变更 → 仅提示，表结构需手工处理；
- 本地有未提交改动时脚本会中止，避免 pull 冲突。

配合定时任务可完全自动化（示例：每天 9:00）：

```bash
crontab -e
0 9 * * * /path/to/nemotion/start/macos-dev/update.sh >> /tmp/leonexam-update.log 2>&1
```

## 常见问题

- **端口冲突**：本机 3306 / 8080 / 8000 被占用时，改 `.env` 中的 `DB_PORT` / `SERVER_PORT` / `FRONTEND_PORT` 后 `docker compose up -d`。
- **后端连不上数据库**：等 `db` 容器 healthy 后 backend 会自动重试；可 `docker compose logs db` 查看初始化进度。
- **改 `.umirc.ts` 代理后不生效**：重启 frontend 容器（`docker compose restart frontend`）。
- **SQL 结构变更**：删除 `db-data` 卷重新初始化（会清空数据），或手工执行迁移 SQL。
