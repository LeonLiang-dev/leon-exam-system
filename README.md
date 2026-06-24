# Leon 在线考试系统 / Nemotion

基于 Spring Boot 3、React 18、UmiJS 4 和 Ant Design 5 重构升级的在线考试系统，面向教师机本地部署、局域网学生访问的教学考试场景。

本项目根据开源项目 [WTS 在线考试答题系统](https://gitee.com/macplus/WTS) 进行技术栈升级、代码结构整理、功能修复和 Windows 教师机一体化打包改造。原 WTS 项目定位为在线答题系统，覆盖在线考试、在线练习、问卷/练题等场景，并支持单选、多选、填空、问答、判断、附件等多类题型。本项目保留其核心业务思路，并将运行方式调整为更适合机房、课堂和局域网考试的部署形态。

## 目录

- [主要特性](#主要特性)
- [技术栈](#技术栈)
- [项目结构](#项目结构)
- [环境要求](#环境要求)
- [快速启动](#快速启动)
- [数据库脚本](#数据库脚本)
- [默认账号](#默认账号)
- [Windows 教师机安装包](#windows-教师机安装包)
- [局域网访问控制](#局域网访问控制)
- [常用命令](#常用命令)
- [开发说明](#开发说明)
- [常见问题](#常见问题)
- [来源与许可](#来源与许可)

## 主要特性

### 考试业务

- 题目管理：支持题目分类、题目增删改查、Excel 批量导入、答案与解析维护。
- 题型支持：填空题、单选题、多选题、判断题、问答题、附件题。
- 试卷管理：支持手工组卷、章节管理、题目分值配置。
- 随机组卷：按题目分类、题型、数量和分值规则自动抽题。
- 答题室管理：创建答题室、关联试卷、发布/关闭考试、配置考试时间。
- 学生考试：学生查看可参加考试、在线答题、自动计时、提交答卷。
- 阅卷与成绩：客观题自动判分，主观题/附件题人工阅卷，支持成绩明细查看。
- 数据面板：展示题目、试卷、答题室、答卷和用户统计。

### 管理与安全

- 管理员、教师/管理端、学生端分角色访问。
- 基于 JWT 的登录认证。
- Spring Security 6 权限控制。
- 可配置 CORS 允许来源。
- 教师机部署时支持局域网同网段访问限制。

### 部署增强

- 前端构建后自动嵌入 Spring Boot 静态资源。
- 支持单 JAR 部署。
- 支持 Windows 安装包：
  - 启动器 exe
  - 内置 JRE
  - 内置 MariaDB
  - 内置 Spring Boot 后端
  - 首次运行自动初始化数据库
  - 双击启动后自动打开教师端
  - 启动器显示学生访问地址

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端框架 | Spring Boot 3.3.6 |
| Java 版本 | JDK 17 |
| 安全认证 | Spring Security 6 + JWT |
| ORM | MyBatis-Plus 3.5.7 |
| 数据库 | MySQL 8 / MariaDB |
| 前端框架 | React 18 + TypeScript |
| 构建框架 | UmiJS Max 4 |
| UI | Ant Design 5 + Ant Design Pro Components |
| Excel | Apache POI |
| 测试 | JUnit 5 + Mockito |
| Windows 打包 | jpackage + WiX Toolset |

## 项目结构

```text
nemotion/
├── wts-server/                    # Spring Boot 后端多模块项目
│   ├── wts-app/                   # 后端启动模块，包含 application.yml 和静态资源入口
│   ├── wts-auth/                  # 登录、认证、用户、组织机构
│   ├── wts-common/                # 公共配置、JWT、CORS、局域网过滤、返回结构
│   ├── wts-exam/                  # 考试核心业务
│   └── wts-system/                # 系统模块预留
├── wts-web/                       # React + UmiJS 前端
│   ├── src/pages/                 # 页面
│   ├── src/services/              # API 请求
│   └── .umirc.ts                  # Umi 配置
├── launcher/                      # Windows 教师机启动器
│   └── src/main/java/...          # Swing 启动器源码
├── packaging/windows/mariadb/     # MariaDB Windows ZIP 解压占位目录
├── sql/
│   ├── init/                      # 初始化 SQL
│   └── migrations/                # 增量迁移 SQL
├── templates/                     # Excel 导入模板
├── docs/                          # 设计、部署、数据库与打包文档
├── build.sh                       # macOS/Linux 一键构建 JAR
└── build.bat                      # Windows 构建与安装包脚本
```

## 环境要求

### 开发环境

- JDK 17+
- Maven 3.8+
- Node.js 18+
- npm 9+
- MySQL 8 或 MariaDB

### Windows 安装包构建环境

- Windows 10/11 x64
- JDK 17+，必须包含 `jpackage`
- Maven 3.8+
- Node.js 18+ / npm 9+
- WiX Toolset 3.x，要求 `candle.exe` 和 `light.exe` 在 PATH 中
- MariaDB Windows ZIP 发行包

## 快速启动

### 1. 克隆项目

```bash
git clone git@github.com:LeonLiang-dev/Nemotion-.git
cd Nemotion-
```

### 2. 初始化数据库

创建数据库：

```bash
mysql -u root -p -e "CREATE DATABASE wts DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"
```

导入完整初始化脚本：

```bash
mysql -u root -p wts < sql/init/wts.v1.4.1.sql
mysql -u root -p wts < sql/migrations/V2_add_point_column.sql
```

如果只需要空库结构，不需要示例/历史数据：

```bash
mysql -u root -p wts < sql/init/wts.empty.sql
mysql -u root -p wts < sql/migrations/V2_add_point_column.sql
```

### 3. 配置后端数据库

编辑：

```text
wts-server/wts-app/src/main/resources/application-dev.yml
```

示例：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/wts?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: 12345678
```

### 4. 启动后端

```bash
cd wts-server
mvn clean package -DskipTests
java -jar wts-app/target/wts-app-2.0.0-SNAPSHOT.jar --spring.profiles.active=dev
```

后端默认地址：

```text
http://localhost:8080
```

健康检查：

```text
http://localhost:8080/api/v1/health
```

### 5. 启动前端

```bash
cd wts-web
npm install --registry=https://registry.npmmirror.com
npm run dev
```

前端默认地址：

```text
http://localhost:8000
```

开发环境中，前端会通过代理访问后端 API。

## 数据库脚本

| 文件 | 说明 |
|------|------|
| `sql/init/wts.v1.4.1.sql` | 完整初始化脚本，包含基础权限、管理员账号和示例/历史数据 |
| `sql/init/wts.empty.sql` | 空库结构脚本，仅包含表结构，不包含 `INSERT` 数据 |
| `sql/migrations/V2_add_point_column.sql` | 增量迁移脚本，给题目表增加默认分值字段 |
| `wts-server/wts-app/src/main/resources/db/` | 后端内置的 Flyway 风格结构脚本 |

说明：

- Windows 教师机安装包默认使用 `sql/init/wts.v1.4.1.sql` 初始化，便于首次运行后直接登录和使用。
- 如果你要部署一个完全干净的数据库，可以手动使用 `wts.empty.sql`。

## 默认账号

完整初始化脚本中包含默认管理账号：

| 账号 | 密码 | 说明 |
|------|------|------|
| `sysadmin` | `12345678` | 系统管理员 |
| `admin` | `12345678` | 管理员 |

生产环境部署后请尽快修改默认密码。

## Windows 教师机安装包

本项目支持构建教师机一体化安装包。目标效果是：

1. 教师机安装后双击 `LeonExam`。
2. 启动器自动启动内置 MariaDB。
3. 首次运行自动创建并导入数据库。
4. 自动启动 Spring Boot 后端。
5. 自动打开教师端浏览器。
6. 启动器显示学生访问地址，例如 `http://172.18.3.45:8080`。
7. 局域网学生电脑通过浏览器访问教师机地址参加考试。

### MariaDB 放置方式

下载 MariaDB Windows ZIP 版本，解压后将内容放到：

```text
packaging\windows\mariadb\
  bin\mysqld.exe
  bin\mysql.exe
  bin\mysqladmin.exe
  bin\mariadb-install-db.exe
```

不要提交 MariaDB 二进制文件。仓库中只保留 `packaging/windows/mariadb/README.md` 说明文件。

### 构建安装包

PowerShell 下执行：

```powershell
.\build.bat preflight
.\build.bat package
```

输出目录：

```text
dist\jpackage\
```

安装包内包含：

- Windows 启动器 exe
- 内置 JRE
- Spring Boot 后端 JAR
- 前端静态资源
- MariaDB 运行时
- SQL 初始化脚本

更多细节见：

- [Windows 教师机安装包文档](docs/windows-teacher-installer.md)

## 局域网访问控制

教师机安装包默认启用局域网访问限制：

```yaml
app:
  access:
    lan:
      enabled: true
      auto-same-subnet: true
      allowed-cidrs: ""
```

默认行为：

- 教师机本机 `127.0.0.1` 永远允许访问。
- 自动检测教师机非回环 IPv4 地址。
- 自动允许教师机所在 `/24` 网段。
- 例如教师机 IP 为 `172.18.3.45`，则允许 `172.18.3.0/24`。

如需手动覆盖，编辑 Windows 数据目录中的配置：

```text
C:\ProgramData\LeonExam\config\application.yml
```

示例：

```yaml
app:
  access:
    lan:
      enabled: true
      auto-same-subnet: false
      allowed-cidrs: 172.18.3.0/24,172.18.4.0/24
```

普通 JAR 部署默认不启用该限制。如需启用，可以通过环境变量配置：

```bash
LAN_ACCESS_ENABLED=true
LAN_AUTO_SAME_SUBNET=true
LAN_ALLOWED_CIDRS=172.18.3.0/24
```

## 常用命令

### 一键构建可执行 JAR

macOS/Linux：

```bash
chmod +x build.sh
./build.sh
```

Windows：

```powershell
.\build.bat
```

构建产物：

```text
dist/wts-app-2.0.0-SNAPSHOT.jar
```

### 后端测试

```bash
cd wts-server
mvn test
```

### 启动器打包

```bash
cd launcher
mvn package
```

### 前端构建

```bash
cd wts-web
npm install --registry=https://registry.npmmirror.com
npm run build
```

## 开发说明

### 后端模块边界

- `wts-app`：启动入口和应用配置。
- `wts-auth`：认证、用户、组织机构和权限相关接口。
- `wts-common`：公共返回结构、异常处理、JWT、CORS、局域网过滤器。
- `wts-exam`：题目、试卷、答题室、答卷、阅卷、随机组卷。
- `wts-system`：系统模块预留。

### 前端约定

- 页面放在 `wts-web/src/pages`。
- API 调用放在 `wts-web/src/services`。
- 请求拦截与 Token 处理在 `wts-web/src/services/request.ts`。
- 登录和全局运行配置在 `wts-web/src/app.tsx`。

### 打包约定

- Git 仓库不提交 `node_modules`、`target`、`dist`、MariaDB 二进制和生成的静态资源。
- 前端构建产物会在构建时复制到 `wts-server/wts-app/src/main/resources/static`。
- Windows 安装包由 `build.bat package` 生成。

## 常见问题

### 1. Windows PowerShell 提示找不到 `build.bat`

PowerShell 不会默认执行当前目录脚本，需要加 `.\`：

```powershell
.\build.bat preflight
```

### 2. 前端构建提示 `'max' 不是内部或外部命令`

说明前端依赖没有安装。执行：

```powershell
cd .\wts-web
npm install --registry=https://registry.npmmirror.com
cd ..
.\build.bat package
```

### 3. 学生电脑打不开教师机地址

优先检查：

- 学生电脑和教师机是否在同一网段。
- 启动器显示的学生访问地址是否正确。
- Windows 防火墙是否放行 TCP `8080`。
- 是否被安全软件拦截。
- 是否配置了手动 CIDR，但没有包含学生 IP。

### 4. MariaDB 初始化失败

新版启动器已兼容不同 MariaDB ZIP 中的初始化命令。如果之前初始化失败留下残缺数据目录，可以删除后重试：

```powershell
Remove-Item "C:\ProgramData\LeonExam\mysql" -Recurse -Force
```

### 5. 普通 JAR 和 Windows 安装包有什么区别

普通 JAR：

- 需要用户自己准备数据库和 JRE。
- 更适合服务器部署。

Windows 安装包：

- 自带 JRE。
- 自带 MariaDB。
- 自带启动器。
- 适合教师机双击运行、学生局域网访问。

## 来源与许可

本项目基于 [macplus/WTS 在线考试答题系统](https://gitee.com/macplus/WTS) 进行技术栈升级和部署改造。原项目采用 GPL-3.0 许可，本项目继续保留 GPL-3.0 许可。

详见 [LICENSE](LICENSE)。
