# 生产环境（prod）部署

> 适用：Linux + systemd + Nginx + MySQL 8 + Redis。
> 本目录提供模板，实际域名、证书、密码必须替换。

## 一、目录约定

```text
/opt/tender/
├── tender-server.jar
└── uploads/                # 如后续启用本地文件存储

/etc/tender/
├── tender.env              # 环境变量（权限 600）
└── application-prod.yaml   # 可选：外部覆盖配置

/var/www/tender/
├── admin-web/              # admin-web build:prod 产物
├── user-web/               # user-web build 产物
└── h5/                     # tender-app build:h5 产物

/var/log/tender/            # 应用日志
```

## 二、初始化数据库

```bash
# 1. 框架基础（自动 CREATE DATABASE + USE tender）
mysql -uroot -p < backend/sql/mysql/tender.sql

# 2. 业务最终结构 + 基础/站点/菜单种子
mysql -uroot -p < backend/sql/mysql/notice.sql

# 3. 仅当启用 Quartz 时
mysql -uroot -p < backend/sql/mysql/quartz.sql

# 4. 生产不要执行 demo-data.sql
```

初始化后立即处理：

- 修改 `admin` 默认密码；
- 禁用或删除演示会员 `demo123`；
- 替换 OAuth2 客户端密钥、数据库/Redis 密码；
- 确认 `notice` 无旧列、`crawler_channel` 有 35 条。

## 三、构建

> 以下命令在仓库根目录执行。

```bash
# 后端
(cd backend && mvn clean package -DskipTests)
# 产物：backend/tender-server/target/tender-server.jar

# 前端
(cd admin-web && pnpm install && pnpm build:prod)   # dist-prod
(cd user-web && pnpm install && pnpm build)         # dist
(cd tender-app && pnpm install && pnpm build:h5)    # dist/build/h5
```

## 四、配置

```bash
sudo mkdir -p /opt/tender /etc/tender /var/log/tender
sudo useradd -r -s /sbin/nologin tender 2>/dev/null || true
sudo chown -R tender:tender /opt/tender /var/log/tender

# 复制 jar
sudo cp backend/tender-server/target/tender-server.jar /opt/tender/

# 环境变量
sudo cp deploy/env.prod.example /etc/tender/tender.env
sudo chmod 600 /etc/tender/tender.env
sudo editor /etc/tender/tender.env
```

`application-prod.yaml` 已打包在 jar 内，所有敏感项通过 `/etc/tender/tender.env` 注入。  
如需外部覆盖，可在启动参数加：

```text
--spring.config.additional-location=file:/etc/tender/
```

## 五、启动

```bash
sudo cp deploy/systemd/tender-server.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now tender-server
sudo systemctl status tender-server
sudo journalctl -u tender-server -f
```

## 六、Nginx

```bash
sudo cp deploy/nginx/tender.conf /etc/nginx/conf.d/tender.conf
sudo editor /etc/nginx/conf.d/tender.conf   # 改域名/证书/静态目录
sudo nginx -t
sudo systemctl reload nginx
```

模板包含：

- 用户端 `tender.example.com`
- 管理端 `admin.tender.example.com`
- HTTP → HTTPS 跳转
- `/admin-api/`、`/app-api/`、`/infra/ws` 反向代理
- 静态资源缓存、SPA fallback、基础安全头

## 七、验证

```bash
# 健康检查（本机）
curl -fsS http://127.0.0.1:48080/actuator/health

# 通过 Nginx
curl -fsS https://tender.example.com/admin-api/notice/overview
curl -fsS https://admin.tender.example.com/

# 登录
# 管理端：https://admin.tender.example.com
# 用户端：https://tender.example.com
```

## 八、上线检查清单

- [ ] `SPRING_PROFILES_ACTIVE=prod`
- [ ] MySQL/Redis 为生产地址，账号最小权限
- [ ] `DB_PASSWORD` / `REDIS_PASSWORD` / `DEEPSEEK_API_KEY` 不在仓库中
- [ ] 默认管理员密码已修改
- [ ] 演示会员 `demo123` 已删除或停用
- [ ] 验证码开启、mock 关闭
- [ ] Actuator 只暴露 `health,info`
- [ ] Druid 控制台关闭
- [ ] Swagger/Knife4j 关闭
- [ ] HTTPS 证书有效，HTTP 已跳转
- [ ] Nginx 静态目录权限正确
- [ ] 日志目录可写、轮转已配置
- [ ] 数据库备份任务已配置
- [ ] 采集调度只在一个实例开启（多实例时）
- [ ] `notice` 旧列为 0，`crawler_channel` 为 35
- [ ] 健康检查与门户接口通过

## 九、多实例 / Quartz

- 默认单实例：`application-prod.yaml` 排除了 `QuartzAutoConfiguration`，不需要 `QRTZ_*` 表。
- 若需要框架定时任务或多实例 Quartz：
  1. 执行 `quartz.sql`；
  2. 删除 `application-prod.yaml` 中的 `spring.autoconfigure.exclude`；
  3. 配置 `spring.quartz.job-store-type=jdbc`、`isClustered=true`。
- 采集是 Spring `@Scheduled` + 单线程执行器：**多实例部署时只能让一个实例开启 `CRAWLER_SCHEDULE_ENABLED=true`**，否则会重复采集。

## 十、升级与回滚

```bash
# 备份
mysqldump -uroot -p --single-transaction --routines --triggers tender \
  > /backup/tender_$(date +%Y%m%d_%H%M).sql

# 升级
sudo systemctl stop tender-server
sudo cp tender-server-new.jar /opt/tender/tender-server.jar
sudo systemctl start tender-server
curl -fsS http://127.0.0.1:48080/actuator/health

# 回滚
sudo systemctl stop tender-server
sudo cp tender-server-old.jar /opt/tender/tender-server.jar
sudo systemctl start tender-server
# 如数据库有变更，使用备份恢复
```

## 十一、安全建议

- 数据库/Redis 不对公网开放，仅内网或本机访问。
- Nginx 只暴露 80/443；后端 48080 只监听 `127.0.0.1` 或内网。
- 使用独立系统用户 `tender` 运行，禁止 root。
- 日志中不输出密码/Token；定期审计 `infra_api_access_log`。
- 采集遵守源站 robots 与限速，不绕过验证码/登录。
