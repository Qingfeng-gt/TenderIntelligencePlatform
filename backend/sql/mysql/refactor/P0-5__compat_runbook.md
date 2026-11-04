# P0-5 终态运行手册

> 2026-11-04 更新：P0 兼容期已结束。项目从未上线，不保留旧列与旧接口。
> 本文替代原「兼容期运行手册」，只描述终态、对账与回滚。

---

## 一、终态

| 项 | 状态 |
|---|---|
| `notice` 旧列（`type` / `province` / `city` / `tender_person` / `agency` / `deadline` / `source`） | **已在开发库删除（2026-11-04）**；备份 `backup/tender_before_p0_6_20261104_103716.sql` |
| `notice` 新列（`notice_type_code` / `province_code` / `city_code` / `tenderer_enterprise_id` / `agency_enterprise_id` / `bid_deadline` / `source_platform` 等） | 唯一读写入参出参 |
| 四层模型 `project` / `tender_project` / `tender_lot` | 采集写入与项目视角读路径 |
| 企业主体 `enterprise*` / `notice_enterprise` | 招标人/代理机构名称来源 |
| 采集写链路 | `NoticeApi.upsert(NoticeDTO)` → `NoticeIngestionService`，只写新模型 |
| 读接口 | `/notice/page|detail|overview|provinces|industries` 与 `/notice/project/page|detail` |
| 旧接口 `/notice/list|get|stats` | **已删除** |

`notice.industry` 保留为 crawler 规则抽取的业务字段（非国标代码）；国标代码在 `notice.industry_code`。

---

## 二、执行与对账

```bash
cd backend/sql/mysql/refactor

# 已执行过 P0-1~P0-5 的库：自动跳过回填，执行 P0-6 + 终态对账
DB_PASS='你的密码' ./P0__run_all.sh --yes

# 只做只读对账
mysql -uroot -p tender < P0-5__compat_check.sql
```

`P0-5__compat_check.sql` 重点看：

- `legacy_columns` = 0；
- `notice_with_project` / `notice_with_tender_project` / `notice_with_lot` = `notice_total`；
- `empty_province_code` / `empty_city_code` 是否集中在未导入的区划；
- `crawler_channel` 每个站点的频道数是否符合预期。

---

## 三、回滚

| 场景 | 脚本 |
|---|---|
| 恢复旧列结构（数据不恢复） | `P0-6__drop_legacy_columns_rollback.sql` |
| 只撤 P0-2 回填数据 | `P0-2__enterprise_backfill_cleanup.sql` |
| 只撤 P0-3 回填数据 | `P0-3__notice_backfill_cleanup.sql` |
| 只撤 P0-4 回填数据 | `P0-4__crawl_governance_cleanup.sql` |
| 撤 P0-2 / P0-3 / P0-4 / P0-1 表 | 对应 `*_rollback.sql` |

> P0-6 删除的旧列数据不可恢复；回滚只恢复结构，业务代码已不再读写这些列。

---

## 四、边界

- `base_area` 只有 34 省 + 17 个演示城市；`city_code` 空值需导入完整 GB/T 2260 数据解决。
- 旧 `notice.sql` 仍是历史全量初始化脚本；新库初始化后需再执行本目录 P0-1 ~ P0-6，才能得到终态结构。
- P0-2/P0-3 回填脚本读取旧列，只能在 P0-6 之前执行。
