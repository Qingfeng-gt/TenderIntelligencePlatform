package com.tenderintelligence.framework.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * AI 配置项，前缀 {@code tender.ai}
 *
 * <p>对应 application.yaml 里插槽 6 预置的那一段（见 doc/毕设 §7）。
 */
@ConfigurationProperties("tender.ai")
@Data
public class TenderAiProperties {

    /**
     * AI 能力开关。关闭时不阻断启动，只让 {@code chat(..)} / {@code embed(..)} 抛异常
     */
    private boolean enabled = true;

    /**
     * 提供方标识，仅用于日志与缓存 key，不参与协议
     */
    private String provider = "deepseek";

    /**
     * OpenAI 兼容接口的 base-url，<b>要含 {@code /v1}</b>（端点路径由客户端拼）
     */
    private String baseUrl = "https://api.deepseek.com/v1";

    /**
     * API key。⚠️ 绝不写死进 yaml —— 用 {@code ${DEEPSEEK_API_KEY:}} 走环境变量
     */
    private String apiKey;

    /**
     * 对话模型
     */
    private String model = "deepseek-chat";

    /**
     * 向量化模型。留空则 embed(..) 不可用（DeepSeek 无 embedding 接口，需另配如 bge-m3 本地服务）
     */
    private String embeddingModel;

    /**
     * 向量维度，仅作记录与长度校验用
     */
    private Integer embeddingDimensions = 1024;

    /**
     * 单次请求超时
     */
    private Duration timeout = Duration.ofSeconds(60);

    /**
     * 失败重试次数（仅对 IO 失败与 HTTP 429/5xx 重试，指数退避）
     */
    private Integer maxRetries = 2;

    /**
     * 全局限流 QPS。避免甲/丁/戊 三处并发打爆配额，0 或负数表示不限流
     */
    private Integer rateLimitQps = 5;

    /**
     * 响应缓存
     */
    private Cache cache = new Cache();

    @Data
    public static class Cache {

        /**
         * 是否开启响应缓存。开启后同样的请求不会再打网络
         */
        private boolean enabled = true;

        /**
         * 缓存目录（相对启动工作目录）。已加进 .gitignore
         */
        private String dir = ".ai-cache";

    }

}
