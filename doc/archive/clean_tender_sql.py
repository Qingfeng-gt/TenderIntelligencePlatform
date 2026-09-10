# -*- coding: utf-8 -*-
"""
一次性清理 tender.sql 种子数据,收敛为"系统实际使用"的最小集合。

规则来源: doc/SQL脚本说明.md + 代码引用核对(后端 DictTypeConstants/@PreAuthorize/JobHandler,
前端 admin-web views 与 dict.ts 实际引用)见 2026-09-09 讨论。
前提: tender.sql 每行一条 INSERT(已校验 1927 行 = 1927 条 INSERT)。

用法: python clean_tender_sql.py  backend/sql/mysql/tender.sql
输出: 同目录 tender.sql.clean(人工复核 diff 后覆盖)
"""
import re
import sys

# ---------------- 保留集合 ----------------
KEEP_DICT_TYPES = {
    'user_type', 'common_status', 'system_user_sex', 'system_data_scope',
    'system_login_type', 'system_login_result', 'system_menu_type',
    'system_role_type', 'system_notify_template_type',
    'infra_boolean_string', 'infra_config_type',
    'infra_api_error_log_process_status', 'infra_operate_type',
    'infra_job_status', 'infra_job_log_status',
}
# 孤儿按钮菜单(parent 已删, 均为已裁模块权限)
DELETE_MENU_IDS = {450, 451, 459, 581, 582, 778, 779, 780, 781, 782, 783,
                   784, 785, 786, 787, 788, 789, 790, 848}
KEEP_ROLE_IDS = {1}                       # 仅超级管理员
KEEP_USER_IDS = {1}                       # 仅 admin
KEEP_USER_ROLE = {(1, 1)}                 # 仅 admin -> 超管
KEEP_JOB_HANDLERS = {'accessLogCleanJob', 'errorLogCleanJob', 'jobLogCleanJob'}
KEEP_CONFIG_IDS = {2, 13}                 # system.user.init-password / register-enabled
KEEP_FILE_CONFIG_IDS = {4}                # 仅数据库存储, 改写为 master
KEEP_OAUTH2_CLIENT_IDS = {1}              # 仅平台客户端

# 新增: tokenCleanJob(代码有实现, 种子缺失); 追加在 jobLogCleanJob 之后
JOB_TOKEN_CLEAN_LINE = ("INSERT INTO `infra_job` (`id`, `name`, `status`, `handler_name`, "
    "`handler_param`, `cron_expression`, `retry_count`, `retry_interval`, `monitor_timeout`, "
    "`creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES "
    "(13001, '登录令牌清理 Job', 1, 'tokenCleanJob', NULL, '0 0 2 * * ?', "
    "0, 0, 0, 'admin', '2026-09-09 00:00:00', 'admin', '2026-09-09 00:00:00', b'0');")

# 平台公告: 替换三行垃圾公告为一条干净公告
NOTICE_CLEAN_LINE = ("INSERT INTO `system_notice` (`id`, `title`, `content`, `type`, `status`, "
    "`creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES "
    "(1, '平台公告', '<p>欢迎使用招投标智能平台。</p>', 1, 0, 'admin', '2026-09-09 00:00:00', "
    "'1', '2026-09-09 00:00:00', b'0', 1);")

# 补 2 条缺失的 dict_type(前端/后端引用, 种子只有 dict_data 行, type 行被之前裁剪误删):
# system_menu_type(菜单类型)、system_data_scope(数据范围)
DICT_TYPE_FIX_LINES = [
    "INSERT INTO `system_dict_type` (`id`, `name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `deleted_time`) VALUES (20002, '数据范围', 'system_data_scope', 0, NULL, 'admin', '2021-01-05 17:03:48', '1', '2021-01-05 17:03:48', b'0', NULL);",
    "INSERT INTO `system_dict_type` (`id`, `name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `deleted_time`) VALUES (20003, '菜单类型', 'system_menu_type', 0, NULL, 'admin', '2021-01-05 17:03:48', '1', '2021-01-05 17:03:48', b'0', NULL);",
]


def first_int(line):
    m = re.match(r"INSERT INTO `.*?` \([^)]*?\) VALUES \((\d+)", line)
    return int(m.group(1))


def quoted(line):
    """按顺序提取单引号字符串([''] 视为转义), 返回 list"""
    return re.findall(r"'((?:[^']|'')*)'", line)


def transform(src_path):
    out_lines = []
    stats = {}
    job_clean_inserted = False
    last_dict_type_line = None
    with open(src_path, encoding='utf-8') as f:
        for line in f:
            st = line.strip()
            table = None
            m = re.match(r"^INSERT INTO `(\w+)` ", st)
            if m:
                table = m.group(1)
                if table == 'system_dict_type':
                    last_dict_type_line = len(out_lines)

            if table is None:
                out_lines.append(line)
                continue

            keep = True
            if table == 'system_dict_data':
                keep = len(quoted(st)) > 2 and quoted(st)[2] in KEEP_DICT_TYPES
            elif table == 'system_dict_type':
                keep = len(quoted(st)) > 1 and quoted(st)[1] in KEEP_DICT_TYPES
            elif table == 'system_menu':
                keep = first_int(st) not in DELETE_MENU_IDS
            elif table == 'system_role_menu':
                keep = False  # 全部删除(role1 24 行悬空; role2 随角色删除)
            elif table == 'system_role':
                keep = first_int(st) in KEEP_ROLE_IDS
            elif table == 'system_user_role':
                a, b, c = re.findall(r"\((\d+),\s*(\d+),\s*(\d+)", st)[0]
                keep = (int(b), int(c)) in KEEP_USER_ROLE
            elif table == 'system_users':
                keep = first_int(st) in KEEP_USER_IDS
            elif table == 'system_notice':
                if first_int(st) == 1:
                    out_lines.append(NOTICE_CLEAN_LINE + '\n')
                    stats['notice_rewritten'] = stats.get('notice_rewritten', 0) + 1
                    continue
                keep = False
            elif table == 'system_notify_message':
                keep = False  # 82 条自测消息全删
            elif table == 'infra_job':
                handler = quoted(st)[1] if len(quoted(st)) > 1 else ''
                keep = handler in KEEP_JOB_HANDLERS
                if keep and handler == 'jobLogCleanJob' and not job_clean_inserted:
                    out_lines.append(line)
                    out_lines.append(JOB_TOKEN_CLEAN_LINE + '\n')
                    job_clean_inserted = True
                    continue
            elif table == 'infra_config':
                keep = first_int(st) in KEEP_CONFIG_IDS
            elif table == 'infra_file_config':
                if first_int(st) in KEEP_FILE_CONFIG_IDS:
                    st = st.replace("'我是数据库', b'0'", "'我是数据库', b'1'")
                    out_lines.append(st + '\n')
                    continue
                keep = False
            elif table == 'system_oauth2_client':
                keep = first_int(st) in KEEP_OAUTH2_CLIENT_IDS

            if keep:
                out_lines.append(line)
            stats[table] = stats.get(table, 0) + (1 if not keep else 0)

    deleted = sum(v for k, v in stats.items() if not k.endswith('_rewritten'))
    print('删除明细:', {k: v for k, v in stats.items() if v})
    print('合计删除行数:', deleted)
    if last_dict_type_line is not None:
        out_lines[last_dict_type_line + 1:last_dict_type_line + 1] = [l + '\n' for l in DICT_TYPE_FIX_LINES]
        print('补充缺失 dict_type 行:', len(DICT_TYPE_FIX_LINES))
    return out_lines


def main():
    src = sys.argv[1]
    out = transform(src)
    with open(src + '.clean', 'w', encoding='utf-8', newline='\n') as f:
        f.writelines(out)
    print('输出:', src + '.clean')


if __name__ == '__main__':
    main()
