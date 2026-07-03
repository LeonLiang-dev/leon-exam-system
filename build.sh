#!/bin/bash
set -e

# ============================================================
# Leon在线考试系统 一键打包脚本
# 使用方法:
#   ./build.sh          — 打包可执行 JAR（跨平台）
#   ./build.sh package  — 用 jpackage 生成本机安装包
# ============================================================

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
WEB_DIR="$PROJECT_DIR/wts-web"
SERVER_DIR="$PROJECT_DIR/wts-server"
STATIC_DIR="$SERVER_DIR/wts-app/src/main/resources/static"
JAR_NAME="wts-app-3.0.0-SNAPSHOT.jar"
JAR_PATH="$SERVER_DIR/wts-app/target/$JAR_NAME"
DIST_DIR="$PROJECT_DIR/dist"

echo "=========================================="
echo "  Leon在线考试系统 — 打包构建"
echo "=========================================="

# ---- Step 1: 构建前端 ----
echo ""
echo "[1/3] 构建前端..."
cd "$WEB_DIR"
if command -v npm >/dev/null 2>&1; then
    npm run build
elif [ -x "./node_modules/.bin/max" ]; then
    ./node_modules/.bin/max build
else
    echo "错误: 未找到 npm，也未找到本地 Max 构建命令 ./node_modules/.bin/max"
    exit 1
fi

# UmiJS 输出到 dist/ 目录
FRONTEND_DIST="$WEB_DIR/dist"
if [ ! -d "$FRONTEND_DIST" ]; then
    echo "错误: 前端构建失败，未找到 dist/ 目录"
    exit 1
fi

# ---- Step 2: 嵌入前端到 Spring Boot ----
echo ""
echo "[2/3] 嵌入前端资源..."
rm -rf "$STATIC_DIR"
mkdir -p "$STATIC_DIR"
cp -r "$FRONTEND_DIST"/* "$STATIC_DIR/"
echo "  已复制前端文件到 $STATIC_DIR"

# ---- Step 3: 打包后端 JAR ----
echo ""
echo "[3/3] 打包后端..."
cd "$SERVER_DIR"
mvn clean package -DskipTests -q

if [ ! -f "$JAR_PATH" ]; then
    echo "错误: JAR 打包失败"
    exit 1
fi
echo "  JAR 已生成: $JAR_PATH"

# ---- 输出目录 ----
mkdir -p "$DIST_DIR"
cp "$JAR_PATH" "$DIST_DIR/"
echo ""
echo "=========================================="
echo "  构建完成!"
echo "  可执行 JAR: $DIST_DIR/$JAR_NAME"
echo ""
echo "  运行方式:"
echo "    java -jar $DIST_DIR/$JAR_NAME --spring.profiles.active=prod"
echo ""
echo "  也可以指定数据库:"
echo "    DB_URL=jdbc:mysql://192.168.1.100:3306/wts \\"
echo "    DB_USERNAME=root \\"
echo "    DB_PASSWORD=mypass \\"
echo "    java -jar $DIST_DIR/$JAR_NAME --spring.profiles.active=prod"
echo "=========================================="

# ---- jpackage（可选）----
if [ "$1" = "package" ]; then
    echo ""
    echo "正在生成本机安装包 (jpackage)..."
    JPACKAGE_DIR="$DIST_DIR/jpackage"
    rm -rf "$JPACKAGE_DIR/LeonExam.app" "$JPACKAGE_DIR/app"
    mkdir -p "$JPACKAGE_DIR"

    # 先解压 JAR 到 app 目录（jpackage 需要）
    APP_DIR="$JPACKAGE_DIR/app"
    mkdir -p "$APP_DIR"
    cp "$JAR_PATH" "$APP_DIR/"

    # 复制配置文件到外部，方便用户修改
    cp "$SERVER_DIR/wts-app/src/main/resources/application-prod.yml" "$DIST_DIR/application.yml"

    jpackage \
        --name LeonExam \
        --type app-image \
        --dest "$JPACKAGE_DIR" \
        --input "$APP_DIR" \
        --main-jar "$JAR_NAME" \
        --main-class org.springframework.boot.loader.launch.JarLauncher \
        --java-options "-Dspring.profiles.active=prod" \
        --java-options "-Xms256m" \
        --java-options "-Xmx1024m" \
        --app-version "3.0.0" \
        --description "Leon在线考试系统" \
        --vendor "Leon"

    echo ""
    echo "=========================================="
    echo "  本机安装包已生成!"
    echo "  位置: $JPACKAGE_DIR/LeonExam.app/"
    echo ""
    echo "  运行: open $JPACKAGE_DIR/LeonExam.app"
    echo "=========================================="
fi
