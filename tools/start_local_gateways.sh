#!/usr/bin/env bash
# 本机桥接服务启动脚本。只读取 .private 下的私有配置，Key 不打印、不进仓库、不进 APK。
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TRIPO_CONFIG="$ROOT/.private/tripo.json"
SEMANTIC_CONFIG="$ROOT/.private/semantic_llm.json"
TRIPO_PORT=8787
SEMANTIC_PORT=8798

info() { printf '%s\n' "$*"; }

check_mode() {
  local file="$1"
  [ -f "$file" ] || { info "缺少私有配置：$file"; return 1; }
  local perm
  perm="$(stat -f '%Lp' "$file")"
  if [ "$perm" != "600" ]; then
    info "权限不是 600（当前 $perm），正在修正：$file"
    chmod 600 "$file"
  fi
  return 0
}

start_tripo() {
  if ! check_mode "$TRIPO_CONFIG"; then
    info "跳过 Tripo 网关。先执行：python3 tools/tripo_gateway.py --init 并填写 tripo_api_key"
    return 0
  fi
  if ! python3 "$ROOT/tools/tripo_gateway.py" --check-config >/dev/null 2>&1; then
    info "跳过 Tripo 网关。请先在 $TRIPO_CONFIG 填写 tripo_api_key"
    return 0
  fi
  python3 "$ROOT/tools/tripo_gateway.py" --allow-generation --port "$TRIPO_PORT" &
  info "Tripo 网关已启动：http://127.0.0.1:$TRIPO_PORT（已允许付费生成）"
}

start_semantic() {
  if ! check_mode "$SEMANTIC_CONFIG"; then
    info "跳过语义网关。先执行：python3 tools/semantic_room_gateway.py --init 并填写 api_key"
    return 0
  fi
  if ! python3 "$ROOT/tools/semantic_room_gateway.py" --check-config >/dev/null 2>&1; then
    info "跳过语义网关。请先在 $SEMANTIC_CONFIG 填写 api_key（未填写时 App 会退回本地关键词约束）"
    return 0
  fi
  python3 "$ROOT/tools/semantic_room_gateway.py" --port "$SEMANTIC_PORT" &
  info "语义网关已启动：http://127.0.0.1:$SEMANTIC_PORT"
}

info "私有配置目录：$ROOT/.private （已排除 Git）"
start_tripo
start_semantic
info "模拟器访问宿主机地址：http://10.0.2.2:8787 与 http://10.0.2.2:8798"
info "按 Ctrl+C 停止全部网关。"

trap 'kill 0' INT TERM
wait
