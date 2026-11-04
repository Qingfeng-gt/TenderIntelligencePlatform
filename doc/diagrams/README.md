# 图表源文件

本目录的图由 Mermaid 源文件生成。**修改图表请改 `.mmd`，不要直接改 `.svg`。**

| 源文件 | 渲染文件 | 用途 |
|---|---|---|
| `architecture.mmd` | `architecture.svg` | 总体架构 |
| `module-dependency.mmd` | `module-dependency.svg` | Maven 模块依赖 |
| `deployment.mmd` | `deployment.svg` | 部署拓扑 |
| `crawl-flow.mmd` | `crawl-flow.svg` | 采集流程时序 |
| `notice-read-flow.mmd` | `notice-read-flow.svg` | 公告查询时序 |
| `bid-state.mmd` | `bid-state.svg` | 投标状态机 |
| `er-core.mmd` | `er-core.svg` | 核心 ER 图 |

## 重新生成

```bash
cd doc/diagrams

# 单个图
npx -y @mermaid-js/mermaid-cli -i architecture.mmd -o architecture.svg

# 全部
for f in *.mmd; do
  npx -y @mermaid-js/mermaid-cli -i "$f" -o "${f%.mmd}.svg"
done
```

如果本机代理未启动，先临时移除代理环境变量：

```bash
env -u HTTP_PROXY -u HTTPS_PROXY -u ALL_PROXY \
  npx -y @mermaid-js/mermaid-cli -i architecture.mmd -o architecture.svg
```

> Mermaid CLI 首次运行会下载 Chromium，耗时较长。SVG 为矢量图，适合提交到仓库；需要位图时可加 `-o architecture.png`。
