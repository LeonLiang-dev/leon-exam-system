#!/bin/sh
# 后端容器启动脚本：以 dev profile 启动 Spring Boot
# 每次容器重启都会先重新编译，因此改完代码后 restart backend 即可生效
set -e

cd /workspace

# 先编译并安装所有模块到本地仓库（wts-app 依赖 wts-auth/wts-exam 等兄弟模块）
echo "==== 编译并安装所有模块 (mvn install -DskipTests) ===="
mvn -q install -DskipTests

echo "==== LeonExam 后端启动中 (mvn spring-boot:run, dev profile) ===="
# 只对 wts-app 模块执行 spring-boot:run（-am 会把父工程也带入 run 导致报错）
exec mvn -pl wts-app spring-boot:run -Dspring-boot.run.profiles=dev
