# Windows 教师机安装包

该方案面向“教师机双击运行，局域网学生浏览器访问”的部署方式。

## 运行效果

安装后双击 `LeonExam`：

1. 启动内置 MariaDB，仅监听 `127.0.0.1:3307`，端口可在启动器中修改
2. 首次运行自动创建并导入 `wts` 数据库
3. 启动 Spring Boot 后端，默认监听 `8080`，端口可在启动器中修改
4. 自动打开教师端 `http://127.0.0.1:8080`
5. 启动器窗口显示学生访问地址，例如 `http://172.18.3.45:8080`

学生电脑必须和教师机处于允许的局域网网段。默认会自动允许教师机 IPv4 所在 `/24` 网段，例如教师机是 `172.18.3.45` 时允许 `172.18.3.0/24`。

普通 JAR 部署默认不启用该限制；Windows 教师机启动器首次创建的外部配置会默认启用。

## Windows 构建机要求

- Windows 10/11 x64
- JDK 17+，需要包含 `jpackage`
- Maven 3.8+
- Node.js 18+ / npm 9+
- WiX Toolset 3.x，用于让 `jpackage --type exe` 生成安装包，并需要 `candle.exe` / `light.exe` 在 PATH 中
- MariaDB Windows ZIP 发行包

## MariaDB 放置方式

下载并解压 MariaDB Windows ZIP，将解压后的内容放到：

```text
packaging\windows\mariadb\
  bin\mysqld.exe
  bin\mysql.exe
  bin\mysqladmin.exe
  bin\mariadb-install-db.exe
```

不要把 MariaDB 二进制提交到源码目录；它只需要存在于 Windows 构建机。

## 构建安装包

先在项目根目录运行预检。PowerShell 下需要带 `.\`：

```powershell
.\build.bat preflight
```

预检会提前确认：

- JDK 17+ 的 `java` 和 `jpackage`
- Maven
- Node.js，以及 `npm` 或本地 Max 构建命令
- WiX Toolset 的 `candle.exe` 和 `light.exe`
- MariaDB ZIP 运行时文件
- 初始化 SQL 和迁移 SQL

预检通过后运行：

```powershell
.\build.bat package
```

构建流程会：

1. 构建前端
2. 嵌入 Spring Boot 静态资源
3. 打包后端 JAR
4. 编译 Java Swing 启动器
5. 准备 jpackage payload
6. 生成 Windows 安装包

输出目录：

```text
dist\jpackage\
```

如果 `preflight` 已通过但 `package` 失败，优先查看命令行最后一个 `ERROR:` 提示；启动器运行阶段的日志会显示在启动器窗口中。

## 从 2.x 安装到 3.0.0

`3.0.0` 是重置版本，安装包使用新的 3.x 升级标识，不再依赖 2.x 安装包的 MSI 源文件。这样可以避免 Windows Installer 在升级时提示寻找 `LeonExam-2.0.17.msi`。

如果机器上已经出现“找不到 `LeonExam-2.0.17.msi`”的窗口，取消旧安装器流程后，使用重新构建的 `3.0.0` 安装包安装。旧的 2.x 卸载项可能仍留在 Windows“应用和功能”列表中；如需清理，可以使用 Windows 官方安装/卸载疑难解答工具移除旧记录。

## 运行数据目录

安装目录只放程序文件。运行数据放在：

```text
C:\ProgramData\LeonExam\
  config\launcher.properties
  config\application.yml
  mysql\
  logs\
  uploads\
```

安装器只覆盖程序文件，不直接删除 `C:\ProgramData\LeonExam`。启动器检测到已有数据库、上传文件或外部配置时，会询问是否覆盖旧数据。

选择覆盖后，启动器会先备份旧数据，再重新初始化干净数据库和文件目录。备份位置：

```text
C:\ProgramData\LeonExam\backup\yyyyMMdd-HHmmss\
```

需要无人值守覆盖时，可在启动前设置环境变量：

```powershell
set LEON_EXAM_OVERWRITE_DATA=1
LeonExam.exe
```

也可以使用启动参数：

```powershell
LeonExam.exe --overwrite-data
```

## 端口设置

启动器 GUI 提供两个端口设置：

| 配置 | 默认值 | 说明 |
|------|--------|------|
| 项目端口 | `8080` | Spring Boot 后端、教师端和学生端访问端口 |
| 数据库端口 | `3307` | 内置 MariaDB 本机监听端口 |

设置会保存到：

```text
C:\ProgramData\LeonExam\config\launcher.properties
```

启动器会根据当前端口自动更新：

```text
C:\ProgramData\LeonExam\config\application.yml
```

如果项目端口或数据库端口被占用，启动器会在启动前提示修改端口或关闭占用程序。项目端口修改后，学生访问地址和 Windows 防火墙放行端口也要同步使用新的项目端口。

## 手动覆盖允许网段

默认配置由启动器首次运行时创建：

```yaml
app:
  access:
    lan:
      enabled: true
      auto-same-subnet: true
      allowed-cidrs: ""
```

如果学校网络需要手动指定，编辑：

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

说明：

- `enabled: false` 会关闭 IP 限制
- `auto-same-subnet: true` 会自动允许教师机所有非回环 IPv4 的 `/24` 网段
- `allowed-cidrs` 可以追加或替代自动网段

## 防火墙

教师机需要允许学生访问启动器中配置的项目端口，默认是 TCP `8080`。如果学生浏览器无法打开地址，优先检查 Windows 防火墙或安全软件是否拦截 `LeonExam` / Java 进程。

MariaDB 只监听 `127.0.0.1` 的数据库端口，默认是 `3307`，不会暴露给学生电脑。
