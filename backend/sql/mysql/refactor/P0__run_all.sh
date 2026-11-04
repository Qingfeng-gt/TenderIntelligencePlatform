#!/usr/bin/env bash
# ============================================================
# P0 一键执行脚本（历史旧库迁移用）
#
# ⚠️ 新库不需要执行本脚本：
#    直接执行 backend/sql/mysql/tender.sql + notice.sql 即可，
#    notice.sql 已是 P0 终态结构（含四层模型、企业、治理表与演示数据）。
#
# 用法:
#   DB_PASS=你的密码 ./P0__run_all.sh --yes
#   DB_HOST=127.0.0.1 DB_PORT=3306 DB_USER=root DB_NAME=tender ./P0__run_all.sh
#   ./P0__run_all.sh --dry-run
# 选项:
#   --yes, -y       跳过确认
#   --no-backfill   只建表/扩展，不跑回填
#   --no-validate   不跑校验
#   --keep-legacy   不执行 P0-6（保留旧列，仅用于回滚演练）
#   --dry-run       只打印将执行的命令，不连库
# ============================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_USER="${DB_USER:-root}"
DB_NAME="${DB_NAME:-tender}"
DB_PASS="${DB_PASS:-}"

RUN_BACKFILL=1
RUN_VALIDATE=1
KEEP_LEGACY=0
ASSUME_YES=0
DRY_RUN=0

usage() {
  sed -n '2,18p' "$0" | sed 's/^# \{0,1\}//'
}

for arg in "$@"; do
  case "$arg" in
    --yes|-y) ASSUME_YES=1 ;;
    --no-backfill) RUN_BACKFILL=0 ;;
    --no-validate) RUN_VALIDATE=0 ;;
    --keep-legacy) KEEP_LEGACY=1 ;;
    --dry-run) DRY_RUN=1; ASSUME_YES=1 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "未知参数: $arg" >&2; usage; exit 1 ;;
  esac
done

if [[ "$DRY_RUN" -eq 0 ]]; then
  command -v mysql >/dev/null 2>&1 || { echo "找不到 mysql 客户端" >&2; exit 1; }
  command -v mysqldump >/dev/null 2>&1 || { echo "找不到 mysqldump" >&2; exit 1; }
fi

MYSQL_ARGS=(-h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER")
if [[ -n "$DB_PASS" ]]; then
  MYSQL_ARGS+=("-p${DB_PASS}")
fi

run_sql() {
  local file="$1"
  echo "==> $file"
  if [[ "$DRY_RUN" -eq 1 ]]; then
    echo "    mysql ${MYSQL_ARGS[*]} $DB_NAME < $SCRIPT_DIR/$file"
  else
    mysql "${MYSQL_ARGS[@]}" "$DB_NAME" < "$SCRIPT_DIR/$file"
  fi
}

# P0-6 执行后旧列不存在，回填脚本无法再运行；此时自动跳过回填。
has_legacy_columns() {
  if [[ "$DRY_RUN" -eq 1 ]]; then
    return 0
  fi
  local cnt
  cnt=$(mysql "${MYSQL_ARGS[@]}" -N -B "$DB_NAME" -e \
    "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = '$DB_NAME' AND TABLE_NAME = 'notice' AND COLUMN_NAME = 'type'")
  [[ "$cnt" == "1" ]]
}

if [[ "$ASSUME_YES" -eq 0 ]]; then
  echo "即将对数据库 [$DB_NAME @ $DB_HOST:$DB_PORT] 执行 P0 增量脚本。"
  echo "脚本会先备份，再建表/扩展/回填；默认执行 P0-6 删除旧列。"
  read -r -p "确认继续？输入 yes 继续: " answer
  [[ "$answer" == "yes" ]] || { echo "已取消"; exit 1; }
fi

BACKUP_DIR="${BACKUP_DIR:-$SCRIPT_DIR/backup}"
BACKUP_FILE="$BACKUP_DIR/${DB_NAME}_before_p0_$(date +%Y%m%d_%H%M%S).sql"

if [[ "$DRY_RUN" -eq 0 ]]; then
  mkdir -p "$BACKUP_DIR"
  echo "==> 备份到 $BACKUP_FILE"
  mysqldump "${MYSQL_ARGS[@]}" --single-transaction --routines --triggers "$DB_NAME" > "$BACKUP_FILE"
else
  echo "==> [dry-run] 备份到 $BACKUP_FILE"
fi

# P0-1 基础数据
run_sql "P0-1__base_data.sql"

# P0-2 企业主体
run_sql "P0-2__enterprise.sql"
if [[ "$RUN_BACKFILL" -eq 1 ]]; then
  if has_legacy_columns; then
    run_sql "P0-2__enterprise_backfill.sql"
  else
    echo "==> 旧列已删除，跳过 P0-2__enterprise_backfill.sql"
  fi
fi

# P0-3 标讯四层
run_sql "P0-3__tender_model.sql"
if [[ "$RUN_BACKFILL" -eq 1 ]]; then
  if has_legacy_columns; then
    run_sql "P0-3__notice_backfill.sql"
  else
    echo "==> 旧列已删除，跳过 P0-3__notice_backfill.sql"
  fi
fi

# P0-4 采集治理
run_sql "P0-4__crawl_governance.sql"
if [[ "$RUN_BACKFILL" -eq 1 ]]; then
  run_sql "P0-4__crawler_channel_backfill.sql"
fi

# P0-6 删除旧列（终态；--keep-legacy 可跳过）
if [[ "$KEEP_LEGACY" -eq 0 ]]; then
  run_sql "P0-6__drop_legacy_columns.sql"
else
  echo "==> --keep-legacy：跳过 P0-6__drop_legacy_columns.sql"
fi

# 校验（只读）
if [[ "$RUN_VALIDATE" -eq 1 ]]; then
  run_sql "P0-2__enterprise_validate.sql"
  run_sql "P0-3__notice_validate.sql"
  run_sql "P0-4__crawl_validate.sql"
  run_sql "P0-5__compat_check.sql"
fi

echo "P0 执行完成。备份文件: $BACKUP_FILE"
