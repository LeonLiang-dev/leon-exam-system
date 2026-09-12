#!/usr/bin/env bash
# ============================================================
# Leon 在线考试系统 — 本地调试环境自动更新脚本
#
# 功能: 从 git 远端拉取最新代码，并按变更范围重启对应容器
#   - wts-server/ 或 pom.xml 变更  -> 重启 backend（自动重新编译）
#   - wts-web/    变更             -> 重启 frontend
#   - start/macos-dev/ 变更        -> 重建容器（应用新编排配置）
#   - sql/ 变更                    -> 仅提示，数据库结构需手工处理
#
# 用法:
#   ./update.sh              # 拉取并更新
#   配合定时任务可自动执行，例如每天 9:00:
#     0 9 * * * /Users/<you>/.../start/macos-dev/update.sh >> /tmp/leonexam-update.log 2>&1
# ============================================================
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# docker compose 需要在本目录读取 docker-compose.yml 和 .env
cd "$SCRIPT_DIR"

# ---------- 1. 检查本地是否有未提交改动 ----------
cd "$REPO_ROOT"
if [ -n "$(git status --porcelain)" ]; then
  echo "[WARN] 检测到本地未提交改动，请先提交或暂存后再执行更新："
  git status --short
  exit 1
fi

# ---------- 2. 拉取最新代码 ----------
PREV="$(git rev-parse HEAD)"
echo ">>> 正在从 origin 拉取最新代码..."
git pull --ff-only
NEW="$(git rev-parse HEAD)"

if [ "$PREV" = "$NEW" ]; then
  echo ">>> 代码已是最新，无需重启。"
  exit 0
fi

CHANGED="$(git diff --name-only "$PREV" "$NEW")"

# 容器在运行则 restart（重启即重新编译），未运行则 up 启动
restart_or_up() {
  local svc="$1"
  if docker inspect -f '{{.State.Running}}' "leonexam-dev-$svc" 2>/dev/null | grep -q true; then
    docker compose restart "$svc"
  else
    docker compose up -d --no-deps "$svc"
  fi
}

# ---------- 3. 按变更范围更新 ----------
# 部署编排本身有变更：整体重建以应用新配置
if echo "$CHANGED" | grep -qE '^start/macos-dev/'; then
  echo ">>> 部署配置/脚本有更新，重建容器..."
  docker compose up -d --build
  echo ">>> 更新完成。"
  exit 0
fi

if echo "$CHANGED" | grep -qE '^(wts-server/|pom.xml)'; then
  echo ">>> 后端代码有更新，重启 backend（启动时自动重新编译）..."
  restart_or_up backend
fi

if echo "$CHANGED" | grep -qE '^wts-web/'; then
  echo ">>> 前端代码有更新，重启 frontend ..."
  restart_or_up frontend
fi

if echo "$CHANGED" | grep -qE '^(sql/|start/linux-docker/initdb/)'; then
  echo "[WARN] SQL 有变更：若涉及表结构，需手工处理数据库（重建 db 卷或执行迁移 SQL）。"
fi

echo ">>> 更新完成。访问 http://localhost:${FRONTEND_PORT:-8000}"
