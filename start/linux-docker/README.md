# Linux Docker Compose 部署（机房推荐）

用两个容器跑起整个系统：`mariadb`（数据库）+ `app`（后端，前端已内嵌）。

## 目录结构

```text
linux-docker/
├── Dockerfile            # 后端镜像（Temurin JDK 17 JRE）
├── docker-compose.yml    # db + app 编排
├── .env.example          # 环境变量模板（复制为 .env 后填写）
├── .dockerignore
├── app/                  # 放置后端 JAR（app.jar）
└── initdb/               # 首次启动自动执行的建库 SQL（按文件名顺序）
```

## 前置要求

- 一台 Linux 服务器（x86_64），已安装 Docker Engine + Docker Compose 插件。
- 已构建好的后端 JAR（在项目根目录执行 `./build.sh`，产物为 `dist/wts-app-3.0.0-SNAPSHOT.jar`）。
  JAR 是跨平台产物，在 Windows / macOS / Linux 任意机器上构建后都可直接使用。
- 机房若无外网：提前在有网机器上 `docker pull eclipse-temurin:17-jre-alpine mariadb:10.11`，
  用 `docker save` 导出镜像 tar 包，拷到机房服务器 `docker load` 导入。

## 部署步骤

### 1. 放置后端 JAR

```bash
mkdir -p app
cp ../../dist/wts-app-3.0.0-SNAPSHOT.jar app/app.jar
```

### 2. 配置环境变量

```bash
cp .env.example .env
vim .env   # 修改 DB_PASSWORD / DB_ROOT_PASSWORD / JWT_SECRET
```

> 生成强随机值：`openssl rand -base64 32`

### 3. 启动

```bash
docker compose up -d --build
```

首次启动会自动：创建数据库 → 按顺序执行 `initdb/` 下的 SQL（空库结构 → 种子管理员 → 增量迁移）→ 后端连接数据库并启动。

### 4. 验证

```bash
docker compose ps                  # 两个容器均为 running / healthy
curl http://127.0.0.1:8080/api/v1/health
```

浏览器访问 `http://<服务器IP>:8080`，默认管理员 `sysadmin` / `123456`（部署后请立即修改密码）。

## 关键配置说明

| 项 | 默认 | 说明 |
|----|------|------|
| 后端端口 | `8080` | 学生/教师访问端口，通过 `.env` 的 `SERVER_PORT` 修改 |
| 数据库端口 | `3306`（仅宿主机本机） | 只绑定 `127.0.0.1`，学生网络不可达 |
| 局域网限制 | 开启，自动放行 `/24` | 后端使用 **host 网络模式**，能看到宿主机真实网卡，`LAN_AUTO_SAME_SUBNET=true` 会自动放行服务器所在网段；如需手动指定，改 `LAN_ALLOWED_CIDRS` |

### 局域网访问控制

默认行为与 Windows 教师机一致：本机 + 服务器所在 `/24` 网段允许访问，其余拒绝。
修改 `.env`：

```ini
# 关闭自动网段探测，改为手动白名单
LAN_AUTO_SAME_SUBNET=false
LAN_ALLOWED_CIDRS=192.168.1.0/24,10.10.0.0/16
```

### 数据持久化

- 数据库数据保存在 Docker 卷 `db-data`（`/var/lib/mysql`）。
- 附件/上传目录保存在 Docker 卷 `uploads`（`/app/uploads`）。
- 两者都由 `docker compose down` 保留，不会被删除；只有 `docker compose down -v` 才会清除。

## 常用运维命令

```bash
docker compose logs -f app       # 查看后端日志
docker compose logs db           # 查看数据库日志
docker compose restart app       # 重启后端
docker compose down              # 停止（保留数据）
docker compose down -v           # 停止并清空数据卷（慎用，会丢数据）
docker compose pull && docker compose up -d   # 升级镜像后重启
```

## 升级

1. 重新构建后端 JAR，覆盖 `app/app.jar`。
2. `docker compose up -d --build`。
3. 数据库结构变更请放入 `sql/migrations/` 并在需要时手动执行（`initdb/` 只在数据卷为空时执行一次）。

## 防火墙

机房服务器建议只放行后端端口：

```bash
# firewalld
firewall-cmd --permanent --add-port=8080/tcp && firewall-cmd --reload
# 或 ufw
ufw allow 8080/tcp
```

## 常见问题

- **容器起不来，日志提示 `Database ... not available`**：数据库首次初始化需要时间，等 `db` 容器 healthy 后 `app` 会自动重试（`restart: unless-stopped`）。
- **想重新初始化数据库**：`docker compose down -v && docker compose up -d`（会清空所有考试数据，慎用）。
- **修改端口后学生访问**：同时修改 `.env` 的 `SERVER_PORT` 和防火墙放行端口。
