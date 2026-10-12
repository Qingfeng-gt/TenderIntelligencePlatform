/**
 * tag 模块，标签拓扑查询系统（M5）
 *
 * <p>四层标签本体（行业 / 地区 / 金额 / 采购方式）→ 规则 + LLM 双路打标 → 共现矩阵与 Louvain
 * 社区发现 → 标签推理推荐。
 *
 * <p>建议的包结构（由戊落地）：
 * <ul>
 *   <li>{@code ontology/} —— 标签本体：四层 × 四类 INDUSTRY / REGION / BUDGET / PURCHASE_TYPE</li>
 *   <li>{@code tagging/} —— RuleTaggingService + LlmTaggingService + AutoTaggingService（双路融合与置信度阈值）</li>
 *   <li>{@code graph/} —— CooccurrenceService（共现矩阵）、CommunityDetector（Louvain）</li>
 *   <li>{@code recommend/} —— TagRecommendService</li>
 *   <li>{@code controller/{app,admin}/}、{@code dal/}、{@code enums/}</li>
 * </ul>
 *
 * <p><b>标签归属口径</b>（doc/毕设 §6 第 8 项）：本模块的 {@code tag} 表是<b>唯一权威标签源</b>。
 * {@code notice.industry} 是 crawler 用关键词词典推导出的「规则抽取结果」，仅作为
 * {@code RuleTaggingService} 的输入之一 —— 它由乙产出、且 NoticeRegionExtractor 在乙的私有区，
 * 本模块调不到，只能读 {@code notice.industry} 这个字段。
 *
 * <p>读公告一律走 {@code com.tenderintelligence.module.notice.api.NoticeApi}，不直接连 notice 表。
 */
package com.tenderintelligence.module.tag;
