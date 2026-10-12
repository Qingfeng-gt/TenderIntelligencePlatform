/**
 * AI 组件，提供统一的 LLM 客户端
 *
 * <p>本包是毕设插槽 7-①（见 doc/毕设 §7）。甲（M1 判站）、丁（M4 查询理解 / Embedding / Rerank）、
 * 戊（M5 行业打标）三方共用，<b>不要再各写一套 HTTP 客户端</b>。
 *
 * <p>用法：
 * <pre>{@code
 * @Resource
 * private AiClient aiClient;
 *
 * String answer = aiClient.chat("判断这个站点是不是招投标站：...");
 * List<Double> vector = aiClient.embed("贵阳市教育局弱电智能化改造项目");
 * }</pre>
 *
 * <p>配置全部在 {@code tender.ai.*} 下（application.yaml 已由插槽 6 预置）。
 * API key 走环境变量 {@code DEEPSEEK_API_KEY}，<b>绝不要写进 yaml</b>。
 *
 * <p>两条容易踩的点：
 * <ul>
 *   <li><b>响应缓存默认开启</b>：key 由「端点 + 模型 + 请求体」算出，所以改 prompt 会自然落到新 key、
 *       不会读到脏值。跑批前把缓存目录备份，实验就能离网重跑。</li>
 *   <li><b>embed(..) 需要单独配 embedding-model</b>：DeepSeek 不提供 embedding 接口，
 *       留空时 {@link com.tenderintelligence.framework.ai.core.AiClient#isEmbeddingAvailable()}
 *       返回 false，调用 embed 会抛异常。</li>
 * </ul>
 */
package com.tenderintelligence.framework.ai;
