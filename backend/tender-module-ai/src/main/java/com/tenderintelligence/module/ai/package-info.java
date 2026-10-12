/**
 * ai 模块，大模型语义模糊查找（M4）
 *
 * <p>口语化查询 → LLM 查询理解 → 向量 + BM25 + 结构化过滤 三路召回 → Rerank → 评测。
 *
 * <p>建议的包结构（由丁落地）：
 * <ul>
 *   <li>{@code understand/} —— QueryUnderstandService：口语化 → 结构化条件 + 语义 query</li>
 *   <li>{@code embedding/} —— EmbeddingService：向量化 + 存储</li>
 *   <li>{@code search/} —— VectorSearcher / Bm25Searcher / StructuredFilter / HybridSearchService</li>
 *   <li>{@code rerank/} —— RerankService</li>
 *   <li>{@code eval/} —— EvalService：Recall@10 / MRR / NDCG@10</li>
 *   <li>{@code controller/{app,admin}/}、{@code dal/}、{@code enums/}</li>
 * </ul>
 *
 * <p>两条已定的跨模块口径（doc/毕设 §6）：
 * <ol>
 *   <li><b>向量存储</b>：MySQL 8 没有 VECTOR 类型，向量存 JSON 列、应用层算余弦。
 *       数据量 500~1000 条时全量暴力检索是毫秒级，不要搭 pgvector/ES 向量集群。</li>
 *   <li><b>BM25 与 ES 的边界</b>：ES 基础设施归丙（M3，tender-module-notice）；
 *       本模块的 Bm25Searcher 只负责调用它并做融合，不自己实现 BM25。</li>
 * </ol>
 *
 * <p>读公告一律走 {@code com.tenderintelligence.module.notice.api.NoticeApi}，不直接连 notice 表。
 */
package com.tenderintelligence.module.ai;
