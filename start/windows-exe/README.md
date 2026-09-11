# Windows 教师机 exe（现状）

Windows 方案维持现有流程不变，无需在本目录新建脚本。相关入口：

- 构建：项目根目录 `build.bat`（`preflight` 检查依赖，`package` 打 MSI 安装包，`package-exe` 打 EXE 安装包）。
- 启动器源码：`launcher/`（Swing 启动器：内置 MariaDB + 内置 JRE + 后端 JAR，双击启动并显示学生访问地址）。
- 详细文档：`docs/windows-teacher-installer.md`。

本目录仅为 `start/` 总览中「Windows 系统」的占位入口，具体部署方法见上述文档。
