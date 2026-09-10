# 招投标智能平台 —— 后端服务(tender-server)

基于 [ruoyi-vue-pro](https://gitee.com/zhijiantianya/ruoyi-vue-pro) 框架二次开发的招投标智能平台后端,单体多模块 Maven 架构。

> 2026-09 已按产品定位(标讯采集 + 会员 AI 写标书)完成初次裁剪:
> 移除 `tender-module-bpm`(工作流)、`tender-module-pms`(项目管理)、`tender-module-report`(大屏报表);
> AI 模块精简为「模型管理 + 对话 + 知识库 RAG + AI 写作 + 工具」,移除绘图/音乐/思维导图/工作流/代码生成演示。

## 技术栈

| 项 | 版本 / 说明 |
|---|---|
| Spring Boot | 3.5.15 |
| JDK | 17 |
| 数据库 | MySQL 8.0(库名 `tender`)+ Redis |
| 持久层 | MyBatis-Plus + Druid 动态多数据源( master/slave,均指向 `tender` ) |
| 定时任务 | Quartz(JDBC 存储,`qrtz_*` 表) |
| 消息队列 | Redis Stream / RocketMQ / Kafka / RabbitMQ 多实现(可选) |
| AI | Spring AI(多模型:OpenAI/Anthropic/DeepSeek/Qwen/Llama/豆包 等),向量库 Redis/Qdrant/Milvus |
| 文档 | Knife4j / springdoc(@/admin-api 接口) |

## 模块总览

```
tender-server                         # 启动入口(TenderServerApplication,端口 48080)
tender-dependencies                   # 统一 BOM 版本管理(spring.boot.version=3.5.15)
tender-framework/                     # 框架层
  tender-common                       # 通用工具(JSON/Http/Excel/加密等)
  tender-spring-boot-starter-*        # 14 个起步组件(见下表)
tender-module-system                  # 平台基础:认证、RBAC 权限、多租户、日志、短信/邮件/通知
tender-module-infra                   # 平台基础设施:配置、文件、定时任务、API 日志、代码生成
tender-module-notice                  # 标讯门户(公告检索)
tender-module-crawler                 # 标讯数据采集(爬虫)
tender-module-bid                     # 用户招投标项目
tender-module-ai                      # AI 套件(裁剪版:对话/知识库 RAG/写作/模型/工具)
```

业务模块依赖关系(裁剪时注意方向:下层被上层依赖):

```
tender-module-infra  ←  tender-module-system  ←  tender-module-notice  ←  tender-module-crawler
        ↑                     ↑                       ↑
        │                     └───────────────────────┴────  tender-module-bid
        ├─────────────────────────────────────────  tender-module-ai
```

## 一、tender-module-system —— 平台基础(必留)

用户/权限/组织等系统的「地基」,无业务属性,几乎所有模块和前端都依赖它。

- **认证**:账号密码登录、验证码(滑块)、注册、重置密码、短信验证码登录、社交登录(微信/钉钉/企业微信/支付宝)、mock 登录
- **RBAC 权限**:用户、角色、部门、岗位、菜单、权限分配(menus/roles)、数据权限
- **多租户**:租户管理、租户套餐(开启后所有业务表带 `tenant_id`)
- **字典**:字典类型/数据
- **系统消息**:站内通知模板与消息、邮件(账号/模板/日志)、短信(通道/模板/日志)
- **OAuth2**:授权客户端、口令、授权码、用户信息——提供给第三方系统对接
- **社交用户**:第三方社交账号绑定/解绑
- **日志**:登录日志、操作日志
- **其他**:地区(IP 归属)、个人中心(资料/改密码)
- 定时任务:`TokenCleanJob`(清理 OAuth2 令牌)

关联表:`system_*` 共 32 张。

## 二、tender-module-infra —— 平台基础设施(必留)

系统运行的「通用设施」,多为平台配置与运维能力。

- **配置管理**:系统参数
- **文件管理**:文件上传/分片/预签名、存储配置(本地/DB/阿里云 OSS/S3/FTP 等)、App 端上传
- **定时任务**:Quartz 任务配置、任务日志、日志清理 Job
- **API 日志**:访问日志、异常日志
- **代码生成器**:codegen(生成 CRUD 前后端代码)——开发工具,若团队不需要可裁
- **数据源管理**:多数据源配置
- **Redis 监控**:keys 查询/监控指标

> 已裁剪:demo01/demo02/demo03 codegen 示例(`tender_demo*` 5 张表已从库与初始化脚本移除)。

关联表:`infra_*` 11 张 + `qrtz_*` 11 张。

## 三、tender-module-notice —— 标讯门户(业务核心,必留)

招投标公告(标讯)公开查询门户。

- 公告多类型:`tender` 招标 / `win` 中标 / `change` 变更 / `explore` 采购
- 检索维度:标题、省份、城市、行业、预算、发布时间、类型
- 公告原文 HTML 内容、来源网站、招标人/代理机构/联系人
- 门户接口 `/admin-api/notice/**` 免登录公开访问

关联表:`notice`(含爬虫扩展字段)。

## 四、tender-module-crawler —— 标讯采集爬虫(核心)

对接各招投标源站自动采集公告,写入 `notice` 表。

- 站点配置(`crawler_site`)、采集任务(`crawler_task`)
- 适配器抽象 `SourceAdapter` + 实现 `CcgpSourceAdapter`(中国政府采购网)——新站点加一行配置 + 一个 Adapter
- 去重:`notice.source_url` 唯一键
- 触发:手动接口 `POST /admin-api/crawler/run?siteCode=ccgp`

关联表:`crawler_site`、`crawler_task`。

## 五、tender-module-bid —— 用户招投标项目(核心)

向用户端(user-web)提供「我的投标项目」管理基础能力:项目 CRUD + 状态流转。

关联表:`bid_project`(1 张)。

## 六、tender-module-ai —— AI 套件(裁剪版,会员写标书)

> 2026-09 该模块已随单租赁化裁剪**整体删除**(`ai-trimmed.sql`、10 张 AI 表、前端页面同步移除),本节保留作设计备忘;若恢复,需重建模块与表。

- **模型管理**:平台引擎(OpenAI/Azure/Anthropic/Ollama/通义/硅基流动/DeepSeek/豆包/混元/星火/Grok 等)、API Key、多模型 —— `ai_model`、`ai_api_key`
- **对话 Chat**:会话列表、消息记录、聊天角色(Persona)、SSE 流式回复、上下文、联网搜索(WebSearch) —— `ai_chat_conversation`、`ai_chat_message`、`ai_chat_role`
- **知识库 RAG**:知识库、文档上传/切分/抽取、分段向量化、问答检索 —— `ai_knowledge`、`ai_knowledge_document`、`ai_knowledge_segment`
- **AI 写作**:长文/续写/排版生成(招标书撰写的落点功能)—— `ai_write`
- **工具 Tool**:可扩展的工具调用(目录查询/天气查询示范)—— `ai_tool`
- 多平台配置:`application.yaml` 的 `tender.ai.*`(每个平台 enable + api-key)

**会员写标书的建议链路**:爬虫采集 `notice` → 标讯详情作为 Prompt 上下文 → `ai_knowledge` 上传企业资质/标书模板 → `ai_write` 生成投标书 → `ai_chat_conversation` 记录交互历史。

关联表:`ai_*` 共 10 张(均已在库)。

## 七、framework 起步组件(`tender-framework/`)

| 组件 | 作用 |
|---|---|
| `tender-common` | 通用工具:JSON/Http/正则/Excel/加密/日期等 |
| `spring-boot-starter-web` | Web 容器:全局异常、Knife4j 接口文档、Servlet 配置 |
| `spring-boot-starter-security` | 安全认证:Token、RBAC、`@PreAuthorize`、接口加密 |
| `spring-boot-starter-mybatis` | MyBatis-Plus:多数据源、逻辑删除、分页、SQL 日志 |
| `spring-boot-starter-redis` | Redis/Redisson:缓存、分布式锁、限流计数 |
| `spring-boot-starter-mq` | 消息队列抽象(Redis Stream/RocketMQ/Kafka/RabbitMQ) |
| `spring-boot-starter-job` | Quartz 定时任务(JobHandler 接口) |
| `spring-boot-starter-monitor` | 监控:Actuator、Spring Boot Admin、日志 |
| `spring-boot-starter-protection` | 防护:参数校验、幂等、限流、XSS/验证码 |
| `spring-boot-starter-excel` | EasyExcel 导入导出 |
| `spring-boot-starter-biz-ip` | IP 归属地区查询(ip2region/纯真) |
| `spring-boot-starter-biz-tenant` | 多租户基建(租户上下文/JOB 切租户) |
| `spring-boot-starter-biz-data-permission` | 数据权限过滤 |
| `spring-boot-starter-websocket` | WebSocket 集成(通知/消息推送) |
| `spring-boot-starter-test` | 单测:H2 内存库 + 测试基类 |

## 启动与运行

```bash
# 全链安装(新模块必须先 install)
mvn -DskipTests install
# 打包 server 并启动
mvn -pl tender-server -am -DskipTests package
java -jar tender-server/target/tender-server.jar --spring.profiles.active=local
# 管理端:  http://127.0.0.1:48080/admin-api (Knife4j: /doc.html)
#  用户端:  http://127.0.0.1:48080/app-api   /  标讯公开: /admin-api/notice/list
```

- 环境配置:`application.yaml`(公共)+ `application-local.yaml`(本地开发)+ `application-dev.yaml`(测试)
- 数据源:`jdbc:mysql://127.0.0.1:3306/tender` / Redis `127.0.0.1:6379`
- 注意事项:local 环境排除了 Quartz 自动配置,定时任务只在 dev/生产启用
- Flowable(BPM)配置与依赖已移除

## 数据库

- 库:`tender`(master + slave 指向同一库,slave 为懒加载模拟)
- 初始化脚本 `backend/sql/mysql/`,按序导入 `tender` 库,**一次性初始化**(均为全量 DROP+CREATE,详解见 `doc/SQL脚本说明.md`):

```
1. tender.sql            # 框架基础 29 张表:infra_/system_/member_user(含种子数据,仅初始化)
2. notice.sql            # 业务全量:标讯/爬虫/投标 4 张表 + 菜单与站点种子(仅初始化)
3. quartz.sql            # QRTZ_* 11 张定时任务表(仅启用 Quartz 的 dev/test 环境)
```

> **2026-09 裁剪现状**:库内共 33 张表(29 张基础表 + notice/crawler_site/crawler_task/bid_project),AI 模块已整体裁剪(`tender-module-ai` 与 ai-trimmed.sql、10 张 AI 表一起删除)。全部有代码引用、无缺表,`mvn compile` 通过。若后续要加回功能(如工作流审批),需重新导入对应模块并补建表。
> **2026-09-09 种子清理**:框架自带的演示/测试/已裁模块(MES、IoT、HRM、商城、工作流、CMS 等)种子数据已清除,收敛为系统必需最小集合 —— 仅 admin 超管、15 个字典类型、45 条菜单、4 条定时任务、1 条数据库存储配置、1 条平台公告,另保留演示数据(demo123 会员 + 20 条演示标讯)。清理脚本:`doc/archive/clean_tender_sql.py`。

## 后续裁剪方向(备忘)

- **必留**:system、infra、notice、crawler、bid、ai(精简版)——当前产品定位所需
- **可再裁**:
  - `infra` 的代码生成器(codegen)、多数据源、Redis 监控(运维工具,java 有依赖需细查)
  - `system` 的 OAuth2 第三方对接(无第三方系统时可删)、社交登录(仅保留需要的平台)
  - framework starter:不用消息队列/WebSocket/数据权限时,从各模块 pom 移除
- **删除联动清单**:① 删 module 目录并去 `tender-server/pom.xml` 依赖;② 删 `application.yaml` 对应配置;③ 删数据库表与 `sql/mysql` 脚本片段;④ 删前端 admin-web/user-web 路由与 API;⑤ 删除相关菜单(`system_menu`)与 `role_menu` 关联
