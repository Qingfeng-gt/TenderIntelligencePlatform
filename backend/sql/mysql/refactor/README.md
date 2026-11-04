# P0 增量重构脚本

> 目标：在**不重建现有库**的前提下，按 [`doc/数据/13-数据模型-目标设计.md`](../../../../doc/数据/13-数据模型-目标设计.md) 一次性落地 P0 数据地基。
>
> 与 `../tender.sql`、`../notice.sql` 的区别：
> - `tender.sql` / `notice.sql` 是**全量初始化脚本**（DROP + CREATE），只用于新库；
> - 本目录是**增量迁移脚本**（CREATE IF NOT EXISTS / ALTER 哨兵 / INSERT ... ON DUPLICATE），用于已存在的 `tender` 库；
> - P0 稳定后，再把最终结构折叠回全量初始化脚本，本目录保留作迁移历史。

---

## 一、文件清单

| 文件 | 作用 | 前置 | 可重复执行 |
|---|---|---|---|
| `P0__run_all.sh` | **一键执行**：备份 → P0-1 → P0-2 → P0-3 → P0-4 → 校验 | — | 是 |
| `P0-1__base_data.sql` | `base_area`、`base_code`、`base_field_mapping` + 省级/演示城市/编码种子 | 无 | 是 |
| `P0-1__base_data_rollback.sql` | 删除 P0-1 三张表 | — | 是 |
| `P0-2__enterprise.sql` | `enterprise`、`enterprise_role`、`enterprise_contact`、`enterprise_alias` | P0-1 | 是 |
| `P0-2__enterprise_backfill.sql` | 从 `notice.tender_person` / `agency` 回填企业主档与角色 | P0-2 建表 | 是 |
| `P0-2__enterprise_validate.sql` | 企业回填只读校验 | P0-2 回填 | — |
| `P0-2__enterprise_backfill_cleanup.sql` | 只撤销回填数据，保留表结构 | P0-2 | 是 |
| `P0-2__enterprise_rollback.sql` | 删除 P0-2 四张表 | — | 是 |
| `P0-3__tender_model.sql` | `project`、`tender_project`、`tender_lot`、`notice_change`、`notice_enterprise`；扩展 `notice` / `notice_attachment` | P0-1/P0-2 | 是 |
| `P0-3__notice_backfill.sql` | 旧 `notice` → 四层模型；回填 `notice_enterprise` | P0-3 建表、P0-2 回填 | 是 |
| `P0-3__notice_validate.sql` | 四层归并只读校验 | P0-3 回填 | — |
| `P0-3__notice_backfill_cleanup.sql` | 只撤销 P0-3 回填数据，保留表结构 | P0-3 | 是 |
| `P0-3__tender_model_rollback.sql` | 删除 P0-3 新增表（默认保留新列） | — | 是 |
| `P0-4__crawl_governance.sql` | `crawler_channel`、`crawler_fetch_log`、`crawl_raw`、`crawl_parse_rule`、`crawl_merge_log`、`notice_quality`；扩展 `crawler_site` / `crawler_task` | 现有采集表 | 是 |
| `P0-4__crawler_channel_backfill.sql` | `crawler_site.channels` JSON → `crawler_channel` | P0-4 建表 | 是 |
| `P0-4__crawl_validate.sql` | 频道/日志表只读校验 | P0-4 回填 | — |
| `P0-4__crawl_governance_cleanup.sql` | 只撤销频道回填与站点扩展字段 | P0-4 | 是 |
| `P0-4__crawl_governance_rollback.sql` | 删除 P0-4 新增表（默认保留新列） | — | 是 |
| `P0-5__compat_check.sql` | 旧列/新列/新表对账 | P0-3/P0-4 | — |
| `P0-5__compat_runbook.md` | 兼容期运行手册、切换/回滚步骤 | — | — |

---

## 二、一键执行（推荐）

```bash
cd backend/sql/mysql/refactor

# 先看会执行什么，不连库
./P0__run_all.sh --dry-run

# 正式执行（建议在测试库先跑）
DB_HOST=127.0.0.1 DB_PORT=3306 DB_USER=root DB_PASS='你的密码' DB_NAME=tender \
  ./P0__run_all.sh --yes
```

脚本行为：

1. 自动 `mysqldump` 备份到 `refactor/backup/tender_before_p0_<时间>.sql`；
2. 按 P0-1 → P0-2 → P0-3 → P0-4 顺序执行；
3. 默认执行回填；`--no-backfill` 只建表/扩展；
4. 默认执行校验；`--no-validate` 跳过；
5. **不删除旧表、不删除旧列**。

---

## 三、手动执行顺序

```bash
# 0) 先备份（必须）
mysqldump -uroot -p tender > tender_before_p0_$(date +%Y%m%d_%H%M).sql

# 1) 基础数据
mysql -uroot -p tender < P0-1__base_data.sql

# 2) 企业主体
mysql -uroot -p tender < P0-2__enterprise.sql
mysql -uroot -p tender < P0-2__enterprise_backfill.sql

# 3) 标讯四层
mysql -uroot -p tender < P0-3__tender_model.sql
mysql -uroot -p tender < P0-3__notice_backfill.sql

# 4) 采集治理
mysql -uroot -p tender < P0-4__crawl_governance.sql
mysql -uroot -p tender < P0-4__crawler_channel_backfill.sql

# 5) 校验
mysql -uroot -p tender < P0-2__enterprise_validate.sql
mysql -uroot -p tender < P0-3__notice_validate.sql
mysql -uroot -p tender < P0-4__crawl_validate.sql
mysql -uroot -p tender < P0-5__compat_check.sql
```

---

## 四、安全约定

1. **先备份**：P0-2/P0-3 回填会写入企业、项目、标段、关联表，执行前必须 `mysqldump`。
2. **先测试库**：不要直接在生产库执行；至少先在本地/测试库跑一遍。
3. **前向脚本不删数据**：建表/扩展脚本只 `CREATE TABLE IF NOT EXISTS` / `ALTER TABLE ADD COLUMN`，不含 `DROP`、`TRUNCATE`、`DELETE`。
4. **扩展幂等**：`notice` / `notice_attachment` / `crawler_site` / `crawler_task` 的扩展用“哨兵列”判断，重复执行不会重复加列。
5. **回填幂等**：项目按 `uk_project_key`；招标项目/标段按 `creator='P0-BACKFILL'` 的 `NOT EXISTS`；企业关联按唯一键。
6. **旧表不动**：P0 阶段不删除、不改名、不修改旧列类型；旧接口/旧前端继续可用。
7. **代码未接入前不要切换读路径**：新表先存在，业务仍读旧表；P0-5 再切。

---

## 五、已知边界

| 项 | 说明 |
|---|---|
| 行政区划 | `base_area` 只保证**省级 34 个 + 17 个演示城市**；全量地市/区县需用 GB/T 2260 数据文件导入，不要手工补。 |
| 编码值 | `base_code` 中 `procurement_method` 等代码值为**占位稳定标识**，最终数值需按 V2.0 附录核对后替换；替换只改 `name`/`code`，不改引用列类型。 |
| 项目归并 | P0 回填按 `SHA256(项目编号或标题 | region_code | 项目编号)` 生成 `project_key`；跨源站同一项目的精确归并、人工合并要等 `crawl_merge_log` 接入后完善。 |
| 标段 | 旧数据没有标段信息，统一生成 `lot_no='DEFAULT'` 的默认标段；真实多标段要等解析器接入。 |
| 企业回填 | 只从 `tender_person` / `agency` 抽名称；不解析联系人、不做信用代码补全；同名不同主体需人工合并。 |
| 采集链路 | 已切到 `NoticeApi.upsert(NoticeDTO)` → `NoticeIngestionService`：直接写 notice 新列、project/tender_project/tender_lot、enterprise*、notice_quality、crawl_merge_log；`crawl_raw` / `crawler_fetch_log` 由 `CrawlTelemetryService` 写入；频道改读 `crawler_channel`。 |
| 全文索引 | `notice` 的 ngram 全文索引未包含在本批脚本中；需要时单独加，避免 ngram 插件缺失导致整批失败。 |
| 排序规则 | 新表**跟随数据库默认 collation**。当前 `tender` 库是 `utf8mb4_0900_ai_ci`；脚本不要显式写 `utf8mb4_unicode_ci`，否则与旧 `notice` 比较时报 `ERROR 1267 Illegal mix of collations`。 |

---

## 六、回滚

| 场景 | 脚本 |
|---|---|
| 只撤 P0-2 回填数据 | `P0-2__enterprise_backfill_cleanup.sql` |
| 只撤 P0-3 回填数据 | `P0-3__notice_backfill_cleanup.sql` |
| 只撤 P0-4 回填数据 | `P0-4__crawl_governance_cleanup.sql` |
| 撤 P0-2 表 | `P0-2__enterprise_rollback.sql` |
| 撤 P0-3 表（保留新列） | `P0-3__tender_model_rollback.sql` |
| 撤 P0-4 表（保留新列） | `P0-4__crawl_governance_rollback.sql` |
| 撤 P0-1 表 | `P0-1__base_data_rollback.sql` |

> 撤列语句在 P0-3/P0-4 的 rollback 文件末尾注释段，默认不执行；删除列会丢数据，务必先备份。

---

## 七、执行后应看到什么

- `base_area`：约 51 行（34 省 + 17 演示城市）；
- `base_code`：约 50 行编码；
- `enterprise`：从 20 条种子公告抽出的企业名（数量取决于原文重复度）；
- `project` / `tender_project` / `tender_lot`：每条公告至少关联到 1 个项目、1 个招标项目、1 个默认标段；
- `notice_enterprise`：TENDERER / AGENCY 关联；
- `crawler_channel`：`ccgp` 约 22 行、`ggzy` 约 10 行、`ztb_gz` 约 3 行；
- `P0-5__compat_check.sql` 中 `notice_with_project` 应等于 `notice_total`。

---

## 八、下一步

P0 数据地基已完成，写入侧已接入，读路径已整体切换。剩余：

1. ~~**领域模块拆分**~~ **已完成**：`ProjectDO` / `TenderProjectDO` / `TenderLotDO` / `EnterpriseDO` / `NoticeEnterpriseDO` / `NoticeChangeDO` / `BaseAreaDO` / `BaseCodeDO` 及对应 Mapper/Service 已落在 `tender-module-notice`；旧扁平接口 `/notice/list`、`/notice/get`、`/notice/stats` 已删除，不保留兼容。
2. ~~**采集治理代码**~~ **已完成**：频道改读 `crawler_channel`；适配器经 `NoticeApi.upsert(NoticeDTO)` 入库，由 `NoticeIngestionService` 直接写 notice 新列 + 四层模型 + 企业 + `notice_quality` + `crawl_merge_log`；新增 `crawl_raw` / `crawler_fetch_log` 写入；旧 `NoticeUpsertService` / `NoticeModelSyncService` 已删除；
3. ~~**project-centric 新读接口**~~ **已完成**：`/notice/project/page`、`/notice/project/detail`；
4. ~~**前端逐页切换**~~ **已完成**：`user-web` 首页/列表/详情全部改调 `/notice/page`、`/notice/detail`、`/notice/overview`；筛选项改为 `/notice/provinces`、`/notice/industries`；
5. **停跑回填、删旧列**（旧列仍由采集写入，读路径已不再依赖）。

切换与回滚细节见 `P0-5__compat_runbook.md`。

---

## 九、本次执行记录（2026-11-03，开发库）

| 项 | 结果 |
|---|---|
| 迁移结果 | P0-1 ~ P0-5 全部执行成功 |
| `notice` | 728 条，全部关联 `project_id` / `tender_project_id` / `lot_id` |
| `project` | 706 条（728 条公告按 `project_key` 归并） |
| `tender_project` / `tender_lot` | 各 706 条 |
| `enterprise` / `enterprise_role` | 613 / 614 条 |
| `notice_enterprise` | 832 条（TENDERER / AGENCY） |
| `crawler_channel` | 35 条（ccgp 22 / ggzy 10 / ztb_gz 3） |
| 代码编译 | `mvn -o -DskipTests -pl tender-module-crawler -am compile` 通过 |
| 已知缺口 | `city_code` 仍有约 496 条为空（缺全量 GB/T 2260 地市数据）；`crawl_raw` / `crawler_fetch_log` 从 2026-11-03 起已由采集代码写入 |
