/**
 * discovery 模块，招投标站点发现（M1）
 *
 * <p>范围已按 doc/毕设 降级到「产品够用」：输入关键词 → 搜索引擎发现候选 → 抓首页 → LLM 判站
 * → 人工审核 → 写入 crawler_site。砍掉了 ReAct 全流程留痕、适配器骨架生成、四维评分模型、
 * 20 关键词跑批实验（M1 不出论文，论文主题让给 M6 移动端 App）。
 *
 * <p>建议的包结构（由甲落地）：
 * <ul>
 *   <li>{@code agent/} —— DiscoveryAgent：搜索 → 抓首页 → LLM 判站</li>
 *   <li>{@code agent/tool/} —— SearchTool / FetchTool / SiteJudgeTool（3 个，Profile 合并进 Judge）</li>
 *   <li>{@code service/}、{@code dal/}、{@code controller/admin/discovery/}</li>
 *   <li>{@code enums/ErrorCodeConstants}、{@code framework/config/DiscoveryProperties}</li>
 * </ul>
 *
 * <p>与其他模块的边界：本模块 <b>不依赖 tender-module-crawler 的任何 Java 类</b>，
 * 自己定义只做 insert/update 的 DiscoveredSiteDO 映射 crawler_site 表。
 */
package com.tenderintelligence.module.discovery;
