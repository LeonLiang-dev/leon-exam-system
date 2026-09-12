#!/bin/sh
# 前端容器启动脚本：首次启动安装依赖，然后启动 Umi dev server（HMR 热更新）
set -e

cd /workspace

# node_modules 卷为空（首次启动）时安装依赖
if [ ! -d node_modules/.bin ]; then
  echo "==== 首次启动，安装前端依赖 (npmmirror 镜像) ===="
  npm install --registry=https://registry.npmmirror.com
fi

echo "==== LeonExam 前端启动中 (max dev) ===="
exec npm run dev
