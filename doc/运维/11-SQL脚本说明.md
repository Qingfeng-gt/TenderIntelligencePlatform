# SQL 脚本说明

> 编制：2026-09-10
> 说明 `backend/sql/mysql/` 下每个 SQL 文件的作用、依赖与执行顺序。
> 适用环境：MySQL 8.x；目标库 `tender`（与 `application.yaml` 数据源一致），字符集 `utf8mb4`。
> 执行方式：`mysql -uroot -p tender < 文件.sql`
>
> **结构约定**：所有文件均为**全量初始化脚本（DROP + CREATE）** —— 只在系统初始化时执行一次，面向「一次性建好库」，**不为已存在的库提供增量/演进片段**。需要改动直接改脚本并重新初始化。

---

## 一、文件总览（共 3 个）

| 文件 | 作用 | 表数 | 执行顺序 |
|---|---|---|---|
| `tender.sql` | 框架基础（`infra_*` / `system_*` / `member_user`）+ 最小种子 | 29 | ① 必执行 |
| `notice.sql` | 业务全量：标讯 / 采集 / 投标 + 菜单与站点种子 | 4 | ② 必执行（依赖 ①） |
| `quartz.sql` | Quartz 定时任务 `QRTZ_*` 表 | 11 | ③ 仅启用 Quartz 的环境 |

---

## 二、各文件详解

### 2.1 `tender.sql` —— 框架基础（必执行）

- Navicat 导出脚本，**须在已创建好的 `tender` 库中执行**（脚本本身不建库）。
- 共 **29 张表**：

| 分组 | 数量 | 内容 |
|---|---|---|
| `infra_*` | 11 | API 访问日志、API 异常日志、代码生成器表定义、代码生成器字段、配置、数据源配置、文件、文件配置、文件内容、定时任务定义、定时任务日志 |
| `system_*` | 17 | 字典数据、字典类型、登录日志、菜单、站内公告、站内信、通知模板、操作日志、角色、角色菜单、用户角色、用户 等 |
| `member_user` | 1 | **会员用户表**（本项目自建，用户端 `user-web` 账号体系） |

- **种子数据已收敛为系统必需的最小集合**（2026-09-09 清理，脚本见 `doc/archive/clean_tender_sql.py`，删除 1810 行、保留 120 行）：

| 类别 | 内容 |
|---|---|
| 用户 / 角色 | 仅 `admin`（绑定超级管理员 `super_admin`；超管权限旁路，不依赖 `role_menu`） |
| 字典 | 仅 15 个类型（后端 `DictTypeConstants` + 前端 `admin-web/src/utils/dict.ts` 实际引用的类型，含补回的 `system_menu_type` / `system_data_scope` 两条） |
| 菜单 | 45 条完整菜单树（系统管理 / 基础设施两大目录，`component` 均对应 `admin-web/src/views` 中的真实页面）；孤儿按钮与已裁模块权限已删 |
| 定时任务 | 4 条框架自带的日志清理任务：`accessLogCleanJob`(25) / `errorLogCleanJob`(26) / `jobLogCleanJob`(27) / `tokenCleanJob`(13001)；均 `0 0 0 * * ?` 或 `0 0 2 * * ?` |
| 系统参数 | 2 条（`system.user.init-password` / `register-enabled`） |
| 文件存储 | 1 条数据库存储（无密钥，master） |
| OAuth2 客户端 | 1 条平台客户端 |
| 站内公告 | 1 条 |
| 演示会员 | 1 条 `member_user`：`demo123` / `123456`（BCrypt），昵称「演示会员」 |

- **已删除的历史垃圾**（供追溯，勿再引入）：约 266 个已裁模块的字典 + 1300 行字典数据、20 个测试用户、6 个多余角色、92 行 `role_menu`（其中 24 行为悬空引用）、82 条自测站内消息、2 条测试公告、10 套示例文件存储（含假密钥）、24 条无实现 handler 的定时任务。
- ⚠️ 每张表先 `DROP TABLE IF EXISTS`，**只用于新库初始化或整体重置**。

> 说明：`member_user` 表**没有 `tenant_id` 列**（会员体系不参与租户维度）；当前 `application.yaml` 中也不存在租户相关配置（项目已单租户化，`tender.tenant.enable: false`）。

### 2.2 `notice.sql` —— 业务全量（必执行，须在 `tender.sql` 之后）

由原先的 `notice.sql` + `demo-crawler-bid.sql` + `demo-crawler-bid-seed.sql` + `demo-crawler-admin.sql` **四个脚本合并而成**，一次执行全部就绪。共 **4 张业务表 + 3 类种子**：

| 序号 | 表名 | 中文名 | 要点 |
|---|---|---|---|
| 1 | `notice` | 招投标公告（标讯） | 核心表。含爬虫扩展字段 `project_no`、`source_url`（**唯一去重键**，`uk_source_url`）、`deadline`、`open_time`、`region_code`；4 个业务索引（`idx_type_publish`、`idx_province`、`idx_industry`、`idx_publish_time`）；`AUTO_INCREMENT=100` |
| 2 | `crawler_site` | 爬虫站点配置 | `code` 唯一（对应 `SourceAdapter` 实现）、`enabled`、`channels`（频道 JSON）、`interval_ms`（详情页请求间隔，防反爬）、`config`（UA / 首访 URL 等） |
| 3 | `crawler_task` | 采集任务日志 | 起止时间、状态（`RUNNING`/`SUCCESS`/`FAILED`）、列表与详情抓取数、新增/更新数、失败原因 |
| 4 | `bid_project` | 用户投标项目 | `user_id`（演示默认 1，正式接会员）、`notice_id` 源公告、拟投标金额、投标文件名、状态机、备注 |

**种子数据三类**：

1. **`notice`** —— 20 条演示公告（`id` 1-20），四类齐全（tender/win/change/explore），标题与 HTML 正文与前端演示数据对齐，供开发联调；**正式数据由采集写入**。
2. **`crawler_site`** —— 2 行站点配置：
   - `ccgp` 中国政府采购网（4 个频道：公开招标 / 中标公告 / 更正公告 / 询价公告）；
   - `ztb_gz` 贵州省招标投标公共服务平台（JSON API 源站，`channels[].path` 为 `search` 接口的 `noticeType` 类别编码）。该站 2026-09-08 实测 `search` 列表接口**返回 0 条**（列表数据下线），`GetDetail` 仍可用，源站恢复后即自动采到数据。
3. **后台「数据采集中心」菜单** —— `system_menu` 一行，`id=12800`，path `/crawler`，component `crawler/index`，component_name `CrawlerCenter`，icon `ep:data-analysis`，parent_id 0（顶级菜单）。用 `ON DUPLICATE KEY UPDATE` 幂等写入。**超级管理员自动拥有全部菜单，无需 `system_role_menu` 记录。**

- **依赖**：`tender.sql`（需要其中的 `system_menu` 表）。
- ⚠️ 每张业务表先 `DROP TABLE IF EXISTS`。整体重置时会一并清空：`notice` 的演示数据、`crawler_site` 的采集配置、以及运行期写入 `crawler_task` / `bid_project` 的数据 —— **需要保留数据时切勿整库重跑**。

### 2.3 `quartz.sql` —— 定时任务表（按环境）

- 标准 Quartz JDBC JobStore 的 **11 张 `QRTZ_*` 表**：`QRTZ_BLOB_TRIGGERS`、`QRTZ_CALENDARS`、`QRTZ_CRON_TRIGGERS`、`QRTZ_FIRED_TRIGGERS`、`QRTZ_JOB_DETAILS`、`QRTZ_LOCKS`、`QRTZ_PAUSED_TRIGGER_GRPS`、`QRTZ_SCHEDULER_STATE`、`QRTZ_SIMPLE_TRIGGERS`、`QRTZ_SIMPROP_TRIGGERS`、`QRTZ_TRIGGERS`。
- **依赖**：无（独立于业务表），可在任意顺序执行。
- ⚠️ 同样是 DROP + 重建，重复执行会重置定时任务状态。
- **仅在启用 Quartz 的环境（`dev`/`test`）需要**。`local` 环境通过 `spring.autoconfigure.exclude: QuartzAutoConfiguration` 排除了 Quartz 自动配置，不需要本文件。
- **为什么 local 不启用**：这个开关是**代码基线（开源脚手架）的默认值，不是本项目刻意的架构选择**。本地开发不需要定时任务框架，排除自动配置可以少建 11 张 `QRTZ_*` 表、少一个建库前置步骤；`dev` 为了联调 `infra_job` 里的任务（`tokenCleanJob`、三个日志清理 Job）才启用。
- **本项目的采集定时任务不走 Quartz**（`ScheduledCrawlTask` 是 Spring 原生 `@Scheduled`），所以**不导入本文件也不影响定时采集**。将来新增 `prod` profile 时必须显式决定 Quartz 启用与否，详见 [`03-系统架构.md`](../技术/03-系统架构.md) §六「为什么 Quartz『仅 dev 启用』」。
- 说明：`infra_job` 只是**任务定义表**，Quartz 的实际调度依赖本文件的表。两者配合使用。

> 补充：本项目的**采集定时任务不在这套体系内** —— `ScheduledCrawlTask` 是 Spring 原生 `@Scheduled`，不受 Quartz 与 `infra_job` 管理，因此它**不会出现在管理端「定时任务」页面**，只能通过 `tender.crawler.schedule-*` 配置项控制。详见 [`10-运行手册.md`](10-运行手册.md) §4.3。

---

## 三、执行顺序（一次性初始化）

```bash
# 1. 建库（一次性）
mysql -uroot -p -e "CREATE DATABASE tender DEFAULT CHARACTER SET utf8mb4"

# 2. 框架基础（29 表 + 最小种子）
mysql -uroot -p tender < backend/sql/mysql/tender.sql

# 3. 业务全量（4 表 + 菜单/站点种子）
mysql -uroot -p tender < backend/sql/mysql/notice.sql

# 4. 仅 dev/test 等启用 Quartz 的环境
mysql -uroot -p tender < backend/sql/mysql/quartz.sql
```

**顺序约束**：`tender.sql` → `notice.sql` 为**固定顺序**（后者依赖前者的 `system_menu` 表）；`quartz.sql` 与业务链无关，任意时机执行均可。

---

## 四、历史文件说明（已删除 / 已归档）

| 文件 | 说明 |
|---|---|
| `demo-crawler-bid.sql`、`demo-crawler-bid-seed.sql`、`demo-crawler-admin.sql` | 内容已合并入 `notice.sql`（建表 / 站点种子 / 菜单种子），作为独立增量脚本已删除 |
| `member_user.sql` | 内容已并入 `tender.sql` 末尾（演示会员），独立文件已删除 |
| `ai-trimmed.sql` | AI 套件 10 张表 —— 随单租赁化裁剪删除（`tender-module-ai` 模块已删）；若后续恢复 AI 功能需重建 |
| `trim-admin-menus-20260908.sql`、`tender.sql.bak` | 裁剪过程的补丁与备份，已完成使命 |
| `doc/archive/system_menu_backup_20260908.sql`、`doc/archive/tender.sql.bak-20260908-trim2.sql`、`doc/archive/{clean,trim}_tender_sql.py` | 裁剪过程脚本与备份，归档保留 |

---

## 五、维护约定

| 场景 | 做法 |
|---|---|
| 新增业务表 | 直接改 `notice.sql`（业务表都在这），补上 DDL 与必要种子；**不要新建增量脚本** |
| 修改已有表结构 | 改 `notice.sql` 中的建表语句；开发期直接重建库，不写 `ALTER` |
| 生产环境变更 | ⚠️ 当前脚本体系**不支持增量迁移**，生产变更需要另行编写 `ALTER` 脚本并单独管理 |
| 新增管理端页面 | 需向 `system_menu` 插一行（可写进 `notice.sql` 的种子段，用 `ON DUPLICATE KEY UPDATE` 保持幂等） |
| 新增采集站点 | 向 `crawler_site` 插一行（`code` 对应新的 `SourceAdapter` 实现），详见 [`06-数据采集设计.md`](../技术/06-数据采集设计.md) |

> 注意：`backend/README.md` 中的旧版「数据库」章节（含 7 步清单）已过时，**以本文档为准**。

---

## 相关文档

- 表的完整字段与设计说明 → [`07-数据模型-现状.md`](../数据/07-数据模型-现状.md)
- 规划中的 15 张表 → [`08-数据模型-规划.md`](../数据/08-数据模型-规划.md)
- 完整启动流程 → [`10-运行手册.md`](10-运行手册.md)
