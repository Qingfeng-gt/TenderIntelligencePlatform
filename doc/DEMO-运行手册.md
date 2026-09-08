# DEMO 运行手册 —— 数据采集 + 用户招投标

> 环境: WSL(Ubuntu) 内 MySQL + Java17 + Maven; Windows 上 Node(vite 前端; user-web 可直接在 Windows 跑,代理到 WSL 后端)
> 目录: 后端 `backend/`(tender-server 多模块); 前端 `user-web/`

## 一、数据库(一次性)

```bash
# WSL 内执行(库: ruoyi-vue-pro-jdk8)
cd /mnt/d/MyProjects/TenderIntelligencePlatform/backend/sql/mysql
mysql -uroot -p123456 ruoyi-vue-pro-jdk8 < demo-crawler-bid.sql      # 建表 + notice 扩展字段
mysql -uroot -p123456 ruoyi-vue-pro-jdk8 < demo-crawler-bid-seed.sql # 爬虫站点种子(幂等)
```

## 二、启动后端(WSL)

```bash
cd /mnt/d/MyProjects/TenderIntelligencePlatform/backend
# 首次/改代码后: 全链安装(新模块 crawler/bid 必须 install,否则 server 打包解析不到依赖)
mvn -DskipTests install
# 只重打包 server(jar 被运行进程占用时会失败,先 fuser -k 48080/tcp 停服务再打包)
mvn -pl tender-server -am -DskipTests package
java -jar tender-server/target/tender-server.jar --spring.profiles.active=local
# 验证: http://127.0.0.1:48080/admin-api/notice/list
```

> 注意事项(WSL drvfs 跨盘文件系统): Maven 增量编译判定偶发失效,改源码后建议 `mvn -DskipTests install` 全链重装;
> 服务运行中 jar 被文件锁,`package` 会报 `tender-server.jar.original` 错误 —— 先停服务再打包。

## 三、触发爬虫(任选其一)

```bash
# 1) 采集所有启用站点(默认仅 ccgp 中国政府采购网)
curl -X POST "http://127.0.0.1:48080/admin-api/crawler/run"
# 2) 指定站点
curl -X POST "http://127.0.0.1:48080/admin-api/crawler/run?siteCode=ccgp"
# 3) 查询任务进度
curl "http://127.0.0.1:48080/admin-api/crawler/task/latest?siteCode=ccgp"
```

任务说明: 接口立即返回任务号,后台异步执行(约 3~5 分钟,受详情页 3s 限速影响);
采集 4 个频道(公开招标/中标/更正/询价)各 1 页,约 80~100 条公告。

## 四、启动前端(Windows 或 WSL,Node 24)

```bash
cd user-web
npm install   # 首次
npm run dev   # http://localhost:3001  → vite 代理 /admin-api → 127.0.0.1:48080
```

## 五、演示路径

1. 首页/标讯大厅: 列表含爬虫真实数据(来源: 中国政府采购网,约 111 条)
2. 招标公告详情: 项目编号/投标截止时间/预算金额等为**爬虫实测提取字段**; 点【发起投标】
3. 弹窗填写拟投标金额、文件名称 → 确认递交 → 进入「我的投标」
4. 我的投标: 状态流转 已递交→标记开标→待开标→确认中标/未中标/放弃(状态机校验,终态不可再变)

> 采集频次: 手动触发(接口/后台脚本), 4 频道各抓最新 1 页(约 80 条); 想抓更早的公告把 crawler_site.channels 对应频道 `pageCount` 调大即可(翻页 index_N.htm)。

## 六、数据源与扩充

- 当前源站: 中国政府采购网 `www.ccgp.gov.cn`(配置在 `crawler_site` 表,适配器 `tender-module-crawler` 的 `CcgpSourceAdapter`)
- 扩充新源站: 实现 `SourceAdapter` + 在 `crawler_site` 加一行配置(频道 JSON 支持多页 `pageCount`、`index_N.htm` 翻页)
- 实测结论: 千里马详情页(华为云 WAF 419)、政府采购网搜索接口(IP 限流)、全国平台(JS 二次加载)、CEB(人机验证)均不可直接用 HTTP 抓取,已在 `doc/平台设计-数据采集与投标平台.md` 记录,二期可接 headless 浏览器
