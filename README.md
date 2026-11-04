# 招投标情报平台（Tender Intelligence Platform）

面向政府采购与公共资源交易场景的**多源标讯采集、四层项目归并、门户检索与本企业投标台账**平台。

- 采集公开源站：中国政府采购网（ccgp）、全国公共资源交易平台（ggzy）
- 数据模型：`project → tender_project → tender_lot → notice` 四层
- 三端：管理端 `admin-web`、用户端 `user-web`、移动端 `tender-app`
- 后端：Spring Boot 3.5 + Java 17 + MyBatis-Plus + MySQL 8 + Redis

> 当前状态：P0 数据模型重构完成，旧列已删除，门户读路径与采集写链路已全量切换。项目未上线。

---

## 仓库结构

```text
.
├── admin-web/           # 管理端（Vue 3 + Vite + Element Plus）
├── user-web/            # 用户端（Vue 3 + Vite + Element Plus）
├── tender-app/          # 移动端（UniApp，H5 / App）
├── backend/             # 后端（Maven 多模块 Spring Boot）
├── doc/                 # 项目开发文档（架构 / 数据 / 接口 / 规范）
├── wsl-setup/           # WSL 环境自检与源站探测脚本
└── .github/workflows/   # CI：后端 + 三端前端构建
```

---

## 快速开始

### 1. 环境

JDK 17、Maven 3.9+、MySQL 8、Redis 6+、Node.js 24、pnpm 12。

### 2. 初始化数据库

```bash
# 自动创建并使用 tender 库；notice.sql 直接创建 P0 终态结构
mysql -uroot -p < backend/sql/mysql/tender.sql
mysql -uroot -p < backend/sql/mysql/notice.sql

# 可选：20 条演示公告 + 四层关联，用于门户联调
mysql -uroot -p < backend/sql/mysql/demo-data.sql
```

> `notice.sql` 已包含四层标讯、企业主体、基础数据、采集治理、投标台账等**最终表结构**，但业务表为空，等待采集填充。**不需要再执行 `refactor/P0__run_all.sh`**。  
> `demo-data.sql` 是可选演示数据；`refactor/` 仅保留作已存在库的历史迁移记录。

### 3. 启动后端

```bash
cd backend
mvn -pl tender-server -am -DskipTests package
java -jar tender-server/target/tender-server.jar --spring.profiles.active=local
```

- 后端：`http://127.0.0.1:48080`
- 接口文档：`http://127.0.0.1:48080/doc.html`

### 4. 启动前端

```bash
cd user-web && pnpm install && pnpm dev      # http://127.0.0.1:3001
cd admin-web && pnpm install && pnpm dev     # http://127.0.0.1:80
cd tender-app && pnpm install && pnpm dev:h5 # H5
```

### 5. 演示账号

| 端 | 账号 | 密码 |
|---|---|---|
| 管理端 | `admin` | 以 `backend/sql/mysql/tender.sql` 种子为准 |
| 用户端 | `demo123` | `123456` |

---

## 文档

完整开发文档从 [`doc/README.md`](doc/README.md) 开始。

| 主题 | 文档 |
|---|---|
| 项目概述 | [doc/01-项目概述.md](doc/01-项目概述.md) |
| 需求与功能 | [doc/02-需求与功能.md](doc/02-需求与功能.md) |
| 系统架构 / 架构图 | [doc/03-系统架构.md](doc/03-系统架构.md) |
| 后端设计 / 接口清单 | [doc/04-后端设计.md](doc/04-后端设计.md) |
| 数据模型 / ER 图 | [doc/05-数据模型与ER图.md](doc/05-数据模型与ER图.md) |
| 数据采集设计 / 流程图 | [doc/06-数据采集设计.md](doc/06-数据采集设计.md) |
| 前端设计 | [doc/07-前端设计.md](doc/07-前端设计.md) |
| 接口设计规范 | [doc/08-接口设计规范.md](doc/08-接口设计规范.md) |
| 数据库设计规范 | [doc/09-数据库设计规范.md](doc/09-数据库设计规范.md) |
| 编码规范 | [doc/10-编码规范.md](doc/10-编码规范.md) |
| 测试与 CI | [doc/11-测试与CI.md](doc/11-测试与CI.md) |
| 部署与运维 | [doc/12-部署与运维.md](doc/12-部署与运维.md) |
| 安全与合规 | [doc/13-安全与合规.md](doc/13-安全与合规.md) |
| 开发指南 / FAQ | [doc/14-开发指南.md](doc/14-开发指南.md) |

---

## 关键设计

- **四层标讯模型**：项目 / 招标项目 / 标段 / 公告，公告是事实记录，项目是聚合对象。
- **适配器采集**：新增源站只需实现 `SourceAdapter` + 配置 `crawler_site` / `crawler_channel`。
- **唯一写入口**：适配器产出 `NoticeDTO`，统一走 `NoticeApi.upsert`。
- **幂等**：`source_url` 去重、`project_key` 归并、`name_hash` 企业归一。
- **可追溯**：`crawler_task` / `crawler_fetch_log` / `crawl_raw` / `notice_quality`。

---

## 合规边界

- 只采政府公开发布的公告页面/公开接口。
- 不模拟登录、不绕过验证码、不采非公开数据。
- 附件只存源站直链与元信息，不下载、不再分发。
- 采集限速、遵守 robots。

详见 [doc/13-安全与合规.md](doc/13-安全与合规.md)。
