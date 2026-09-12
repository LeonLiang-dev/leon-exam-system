# 部署启动方式总览

本目录按操作系统/部署方式归类，存放各自的启动与部署方法。各方案共用同一个后端产物：
`./build.sh`（或 `build.bat`）构建出的 `dist/wts-app-3.0.0-SNAPSHOT.jar`（前端已内嵌，跨平台）。

| 目录 | 目标系统 | 方案 | 说明 |
|------|----------|------|------|
| [linux-docker/](linux-docker/README.md) | Linux（机房推荐） | Docker Compose | `mariadb + 后端` 两个容器，首次启动自动建库导数据；支持 host 网络自动放行机房网段 |
| [macos-dev/](macos-dev/README.md) | macOS（本地调试） | Docker 全栈 + 自动更新 | `mariadb + 后端 + 前端` 三容器，源码挂载 HMR 热更新；`update.sh` 自动拉取代码并重启 |
| [windows-exe/](windows-exe/README.md) | Windows（教师机） | 启动器 exe 一键安装包 | 现状方案：`build.bat package` 打包，双击运行，详见 `docs/windows-teacher-installer.md` |

## 选择建议

- **Linux 机房（推荐）**：使用 `linux-docker/`。环境可复制、升级/换机方便、开机自启、崩溃自动重启。
- **Windows 教师机**：继续使用现有 exe 安装包流程。
- 暂不需要 Docker 的 Linux 场景，可直接用裸 JAR 部署：
  `java -jar dist/wts-app-3.0.0-SNAPSHOT.jar --spring.profiles.active=prod`（需自行安装 JDK 17 和 MySQL/MariaDB，数据库初始化 SQL 见仓库根目录 `sql/`，执行顺序：`wts.empty.sql` → `wts.clean-seed.sql` → `migrations/` 下全部脚本）。

## 公共环境变量（后端 prod 配置）

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `SERVER_PORT` | `8080` | 后端/访问端口 |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | 本地库 | 数据库连接 |
| `JWT_SECRET` | 内置弱密钥 | 生产必须替换 |
| `LAN_ACCESS_ENABLED` | `false` | 局域网访问限制开关 |
| `LAN_AUTO_SAME_SUBNET` | `true` | 自动放行本机 `/24` 网段 |
| `LAN_ALLOWED_CIDRS` | 空 | 手动追加允许网段，逗号分隔 |

默认管理员账号：`sysadmin` / `123456`（部署后请立即修改）。
