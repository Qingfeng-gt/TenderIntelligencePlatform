package com.tenderintelligence.framework.ai.core;

import java.util.List;

/**
 * LLM 客户端：只暴露 chat 与 embed 两个能力
 *
 * <p>甲（M1 判站）、丁（M4 查询理解 / Embedding / Rerank）、戊（M5 行业打标）共用。
 * 所有调用都经过统一的限流与响应缓存（见 {@link com.tenderintelligence.framework.ai.config.TenderAiProperties}）。
 */
public interface AiClient {

    /**
     * 单轮对话，等价于 {@code chat(List.of(AiMessage.user(prompt)))}
     *
     * @param prompt 用户输入
     * @return 模型回复的文本
     */
    String chat(String prompt);

    /**
     * 多轮对话
     *
     * @param messages 消息列表，通常第一条是 system
     * @return 模型回复的文本
     */
    String chat(List<AiMessage> messages);

    /**
     * 多轮对话，指定采样温度
     *
     * @param messages    消息列表
     * @param temperature 采样温度；传 null 用服务端默认
     * @return 模型回复的文本
     */
    String chat(List<AiMessage> messages, Double temperature);

    /**
     * 向量化是否可用（取决于是否配置了 {@code tender.ai.embedding-model}）
     */
    boolean isEmbeddingAvailable();

    /**
     * 单条文本向量化
     *
     * @param text 文本
     * @return 向量
     */
    List<Double> embed(String text);

    /**
     * 批量向量化
     *
     * <p>返回顺序与入参一一对应（内部按响应里的 index 重排，不依赖服务端返回顺序）
     *
     * @param texts 文本列表
     * @return 向量列表，与入参等长
     */
    List<List<Double>> embed(List<String> texts);

}
