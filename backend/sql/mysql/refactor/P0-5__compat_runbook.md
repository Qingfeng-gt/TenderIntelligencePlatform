# P0-5 兼容期运行手册

> 目标：在代码兼容层完成前，让**旧系统继续可用**，新模型逐步接管。
> 结论：P0-1 ~ P0-4 全部是**增量**变更；旧列、旧 API、旧前端不受影响。

---

## 一、当前状态

| 项 | 状态 |
|---|---|
| 旧列（`notice.type`、`deadline`、`province`、`city`、`industry`、`tender_person`、`agency`） | **保留**，旧接口/前端继续读 |
| 新列（`notice.notice_type_code`、`bid_deadline`、`province_code`、`tenderer_enterprise_id` 等） | 已加，回填脚本会同步 |
| 新表（`project` / `tender_project` / `tender_lot` / `enterprise*` / `crawler_channel` / `crawl_raw` 等） | 已建，尚未被旧代码读写 |
| 采集链路 | **仍写旧列**，不写新模型 |
| 前端 | 无改动 |

---

## 二、过渡期规则

1. **旧读路径不动**：旧接口继续读 `notice` 旧列；新表只做旁路。
2. **每次采集后跑一次回填**：
   ```bash
   mysql -uroot -p tender < backend/sql/mysql/refactor/P0-3__notice_backfill.sql
   ```
   该脚本只处理 `notice.project_id IS NULL` 的公告，已归一的不会重复建项目；企业关联按唯一键幂等。
3. **新公告的标记**：回填会把新公告的 `parser_version` 写成 `P0-BACKFILL`，仅用于过渡期识别；代码接入双写后停用。
4. **不要删旧列**：至少保留一个发布版本，确认新读路径无回归后再评估。
5. **对账**：每天跑一次 `P0-5__compat_check.sql`，重点看：
   - `notice_with_project / notice_with_tender_project / notice_with_lot` 是否等于 `notice_total`；
   - `tenderer_unlinked / agency_unlinked` 是否可接受；
   - `empty_province_code / empty_city_code` 是否集中在未导入的区划。

---

## 三、代码接入顺序（下一部分）

| 步骤 | 内容 | 退出条件 |
|---|---|---|
| 1 | 新增 `base` / `enterprise` / `tender` 模块的 DO/Mapper/Service | 新表可被代码读写 |
| 2 | 采集 `NoticeUpsertService` 双写：旧列 + 新列 + 确保 project/tender/lot | 新公告不再依赖 P0-3 回填 |
| 3 | 新增 project-centric 读接口（列表/详情/搜索），旧接口保留 | 新接口与旧接口结果抽样一致 |
| 4 | 前端按「列表 → 详情 → 搜索」逐页切新接口 | 单页灰度通过 |
| 5 | 停跑 P0-3 回填；保留旧列只读 | 连续 7 天对账无差异 |
| 6 | 删除旧列/旧接口（单独发布） | 旧读流量归零 |

---

## 四、切换与回滚

- **功能开关**：按接口/页面维度切新读路径；出问题立即切回旧列。
- **数据库回滚**：
  - 只撤数据：`P0-3__notice_backfill_cleanup.sql`、`P0-4__crawl_governance_cleanup.sql`；
  - 撤表：`P0-3__tender_model_rollback.sql`、`P0-4__crawl_governance_rollback.sql`；
  - 撤列：见各 rollback 文件末尾注释段（默认不执行）。
- **禁止**：在未备份、未跑对账的情况下直接删旧列或旧表。

---

## 五、退出标准

- [ ] 连续 7 天 `P0-5__compat_check.sql` 的归并完整度为 100%；
- [ ] 新项目/标段/公告的读写由代码完成，不再依赖回填脚本；
- [ ] 旧接口与新接口抽样 100 条结果一致；
- [ ] 前端三条读路径全部切新接口并灰度通过；
- [ ] 回滚演练通过（关开关 → 旧路径可用）。
