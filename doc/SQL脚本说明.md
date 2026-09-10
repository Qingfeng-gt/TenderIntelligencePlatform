# SQL 脚本说明

> 说明 `backend/sql/mysql/` 下每个 SQL 文件的作用与执行顺序。
> 适用环境:MySQL 8.x;目标库 `tender`(与 `application.yaml` 数据源一致),字符集 utf8mb4。
> 执行方式:`mysql -uroot -p tender < 文件.sql`
>
> 结构约定:所有文件均为**全量初始化脚本(DROP + CREATE)**——只在系统初始化时执行一次,
> 面向"一次性建好库",不为已存在的库提供增量/演进片段(需要改动直接改这里,重新初始化)。

## 一、文件总览(仅 3 个)

| 文件 | 作用 | 执行优先级 |
|---|---|---|
| `tender.sql` | 框架基础 29 张表(infra_/system_/member_user)+ 种子数据 | ① 必执行 |
| `notice.sql` | 业务全量:标讯/爬虫/投标 4 张表 + 菜单与站点种子 | ② 必执行 |
| `quartz.sql` | Quartz 定时任务 QRTZ_* 11 张表 | ③ 启用 Quartz 的环境执行 |

## 二、各文件详解

### 1. `tender.sql` —— 框架基础(必执行)

- Navicat 导出脚本,须在已创建好的 `tender` 库执行(脚本本身不建库)。
- 共 **29 张表**:
  - `infra_*`(11 张):API 访问/异常日志、代码生成器、配置、数据源、文件、文件存储、定时任务定义、任务日志
  - `system_*`(17 张):字典、登录日志、菜单、网站公告、租户通知、oauth2 相关(5 张)、操作日志、角色、角色菜单、用户角色、用户
  - `member_user`(1 张,含演示会员 demo123/123456,与用户端演示投标数据 user_id=1 对应)
- **种子数据已收敛为系统必需最小集合**(2026-09-09 清理,脚本 `doc/archive/clean_tender_sql.py`,删除 1810 行、保留 120 行):
  - 用户/角色:仅 admin(绑定超级管理员 super_admin,权限旁路不依赖 role_menu)
  - 字典:仅 15 个类型(后端 `DictTypeConstants` + 前端 `admin-web/src/utils/dict.ts` 实际引用的类型,含补回的 system_menu_type/system_data_scope 两条缺失 type 行)
  - 菜单:45 条完整树(系统管理/基础设施两大目录,component 均对应 `admin-web/src/views` 真实页面),孤儿按钮与已裁模块权限已删
  - 定时任务:仅 4 条框架自带的日志清理任务(accessLogCleanJob/errorLogCleanJob/jobLogCleanJob + 补回缺失的 tokenCleanJob)
  - 系统参数:2 条(system.user.init-password / register-enabled);文件存储:仅 1 条数据库存储(无密钥、master);OAuth2 客户端:仅 1 条平台客户端;平台公告:1 条干净公告
  - 保留的演示业务数据:会员 demo123(及 notice.sql 中 20 条演示标讯);爬虫站点配置在 notice.sql
  - 已删除的历史垃圾:约 266 个已裁模块字典+1300 行字典数据、20 个测试用户、6 个多余角色、92 行 role_menu(24 行为悬空引用)、82 条自测站内消息、2 条测试公告、10 套示例文件存储(假密钥)、24 条无实现 handler 的定时任务
- ⚠️ 每张表先 `DROP TABLE IF EXISTS`,只用于新库初始化或整体重置。

### 2. `notice.sql` —— 业务全量(必执行,须在 tender.sql 之后)

由原 notice / demo-crawler-bid(含 seed)/ demo-crawler-admin 等 4 个脚本**合并而成**,一次执行全部就绪,
共 4 张业务表 + 2 类种子:

1. **`notice`** 标讯表:公告类型(tender 招标/win 中标/change 变更/explore 采购)、省份/城市/行业、
   预算金额、招标人/代理机构、联系人、正文 HTML、来源网站、发布时间,**已内建爬虫扩展字段**
   (`project_no` 项目编号、`source_url` 源站详情 URL 唯一去重键、`deadline` 投标截止时间、
   `open_time` 开标时间、`region_code` 源站地区代码),以及 4 个业务索引 + `AUTO_INCREMENT=100`。
   → 含 20 条演示种子(与前端 mock 一致,供开发联调;正式数据由爬虫写入)。
2. **`crawler_site`** 爬虫站点配置:`code` 唯一(对应 SourceAdapter 实现)、`enabled`、`channels` 频道 JSON、
   `interval_ms` 详情页请求间隔(防反爬)、`config`(UA/首访 URL 等)。
   → 含站点种子:ccgp 中国政府采购网(4 频道)、ztb_gz 贵州省招标投标公共服务平台(JSON API 源站;
   该站 2026-09-08 实测 search 列表接口返回 0 条,GetDetail 可用,源站恢复后自动采到)。
3. **`crawler_task`** 采集任务日志:起止时间、状态(RUNNING/SUCCESS/FAILED)、列表/详情抓取数、新增/更新数、失败原因。
4. **`bid_project`** 用户投标项目:`user_id`(演示默认 1,正式接会员)、`notice_id` 源公告、拟投标金额、
   投标文件名、状态机(SUBMITTED 已递交 / OTB 待开标 / WON 中标 / LOST 未中标 / ABANDONED 放弃)、备注。
5. **后台「数据采集中心」菜单**(id=12800,path=`/crawler`,component=`crawler/index`)——依赖 tender.sql 的
   `system_menu` 表;超级管理员自动拥有全部菜单,无需 `system_role_menu`。

- 依赖:`tender.sql`(system_menu 表)。
- ⚠️ 每张业务表先 `DROP TABLE IF EXISTS`,整体重置时对 notice 的演示数据、crawler_site 的采集配置、
  推送进 crawler_task/`bid_project` 的运行数据会一并清空 —— 需要保留数据时请勿整库重跑。

### 3. `quartz.sql` —— 定时任务表(按环境)

- 标准 QRTZ_* **11 张表**:BLOB_TRIGGERS、CALENDARS、CRON_TRIGGERS、FIRED_TRIGGERS、JOB_DETAILS、
  LOCKS、PAUSED_TRIGGER_GRPS、SCHEDULER_STATE、SIMPLE_TRIGGERS、SIMPROP_TRIGGERS、TRIGGERS。
- 依赖:无(独立于业务表),可在任意顺序执行。
- ⚠️ DROP + 重建,重复执行会重置定时任务状态。
- 仅在**启用 Quartz 的环境(dev/test)**需要;`local` 环境排除了 Quartz 自动配置,不需要。
  说明:`infra_job` 只是任务定义表,Quartz 实际调度依赖本文件的表。

## 三、执行顺序(一次性初始化)

```bash
# 1. 建库(一次性)
mysql -uroot -p -e "CREATE DATABASE tender DEFAULT CHARACTER SET utf8mb4"

# 2. 导入基础框架(29 表 + 种子)
mysql -uroot -p tender < backend/sql/mysql/tender.sql

# 3. 导入业务全量(标讯/爬虫/投标 4 表 + 菜单/站点种子)
mysql -uroot -p tender < backend/sql/mysql/notice.sql

# 4. (仅 dev/test 启用 Quartz 的环境)定时任务表
mysql -uroot -p tender < backend/sql/mysql/quartz.sql
```

`tender.sql` → `notice.sql`(依赖基础表)为固定顺序;`quartz.sql` 与业务链无关,任意时执行。

## 四、历史文件说明(已删除/归档)

| 文件 | 说明 |
|---|---|
| `demo-crawler-bid.sql` `demo-crawler-bid-seed.sql` `demo-crawler-admin.sql` | 内容已合并入 `notice.sql`(建表/站点种子/菜单种子),作为增量脚本已删除 |
| `member_user.sql` | 内容已并入 `tender.sql` 末尾(演示会员),独立文件已删除 |
| `ai-trimmed.sql` | AI 套件 10 张表 —— 随单租赁化裁剪删除(`tender-module-ai` 模块已删);若后续恢复 AI 功能需重建 |
| `trim-admin-menus-20260908.sql` / `tender.sql.bak` / `doc/archive/system_menu_backup_20260908.sql` | 裁剪过程备份/补丁,已完成使命,归档于 `doc/archive` |

> 注意:`backend/README.md` 中旧版「数据库」章节(含 7 步清单)已过时,以本文档为准。
