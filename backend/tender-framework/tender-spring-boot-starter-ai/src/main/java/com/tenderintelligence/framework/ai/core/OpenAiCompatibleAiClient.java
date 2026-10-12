package com.tenderintelligence.framework.ai.core;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.RateLimiter;
import com.tenderintelligence.framework.ai.config.TenderAiProperties;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * OpenAI 兼容协议的 LLM 客户端实现
 *
 * <p>只依赖 okhttp + jackson（都已在 BOM 里），刻意不引 Spring AI / LangChain4j。
 * 端点路径由本类拼：{@code {base-url}/chat/completions} 与 {@code {base-url}/embeddings}，
 * 所以 {@code tender.ai.base-url} 必须含 {@code /v1}。
 */
@Slf4j
public class OpenAiCompatibleAiClient implements AiClient {

    private static final String PATH_CHAT = "/chat/completions";
    private static final String PATH_EMBEDDING = "/embeddings";

    private static final MediaType CONTENT_TYPE_JSON = MediaType.parse("application/json; charset=utf-8");

    /**
     * 出错时打进日志/异常里的响应体截断长度
     */
    private static final int ERROR_BODY_MAX_LENGTH = 500;

    private final TenderAiProperties properties;
    private final AiResponseCache cache;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OkHttpClient httpClient;

    /**
     * 全局限流器；{@code rate-limit-qps} 配成 0 或负数时为 null（不限流）
     */
    private final RateLimiter rateLimiter;

    public OpenAiCompatibleAiClient(TenderAiProperties properties, AiResponseCache cache) {
        this.properties = properties;
        this.cache = cache;
        this.rateLimiter = properties.getRateLimitQps() != null && properties.getRateLimitQps() > 0
                ? RateLimiter.create(properties.getRateLimitQps()) : null;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(properties.getTimeout())
                .readTimeout(properties.getTimeout())
                .writeTimeout(properties.getTimeout())
                .build();
        if (StrUtil.isBlank(properties.getApiKey())) {
            log.warn("[ai] 未配置 API key —— 请设置环境变量 DEEPSEEK_API_KEY（或 tender.ai.api-key），"
                    + "否则 chat(..)/embed(..) 调用会失败");
        }
        if (StrUtil.isBlank(properties.getEmbeddingModel())) {
            log.info("[ai] 未配置 tender.ai.embedding-model，embed(..) 不可用（DeepSeek 不提供 embedding 接口）");
        }
    }

    // ==================== 对话 ====================

    @Override
    public String chat(String prompt) {
        return chat(List.of(AiMessage.user(prompt)), null);
    }

    @Override
    public String chat(List<AiMessage> messages) {
        return chat(messages, null);
    }

    @Override
    public String chat(List<AiMessage> messages, Double temperature) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", properties.getModel());
        body.put("stream", false);
        ArrayNode array = body.putArray("messages");
        for (AiMessage message : messages) {
            ObjectNode node = array.addObject();
            node.put("role", message.getRole());
            node.put("content", message.getContent());
        }
        if (temperature != null) {
            body.put("temperature", temperature);
        }

        String raw = execute(PATH_CHAT, body.toString());
        JsonNode content = readTree(raw).path("choices").path(0).path("message").path("content");
        if (content.isMissingNode() || content.isNull()) {
            throw new AiException("AI 对话响应里没有 choices[0].message.content: "
                    + StrUtil.maxLength(raw, ERROR_BODY_MAX_LENGTH));
        }
        return content.asText();
    }

    // ==================== 向量化 ====================

    @Override
    public boolean isEmbeddingAvailable() {
        return properties.isEnabled() && StrUtil.isNotBlank(properties.getEmbeddingModel());
    }

    @Override
    public List<Double> embed(String text) {
        return embed(List.of(text)).get(0);
    }

    @Override
    public List<List<Double>> embed(List<String> texts) {
        if (!isEmbeddingAvailable()) {
            throw new AiException("未配置向量化模型（tender.ai.embedding-model），embed(..) 不可用");
        }
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", properties.getEmbeddingModel());
        ArrayNode input = body.putArray("input");
        texts.forEach(input::add);

        JsonNode data = readTree(execute(PATH_EMBEDDING, body.toString())).path("data");
        if (!data.isArray() || data.size() != texts.size()) {
            throw new AiException("AI 向量化响应条数与入参不符: 期望 " + texts.size()
                    + ", 实际 " + (data.isArray() ? data.size() : -1));
        }
        // 服务端不保证返回顺序与入参一致，按 index 重排
        List<IndexedVector> vectors = new ArrayList<>(data.size());
        for (JsonNode item : data) {
            List<Double> vector = new ArrayList<>();
            item.path("embedding").forEach(value -> vector.add(value.asDouble()));
            vectors.add(new IndexedVector(item.path("index").asInt(vectors.size()), vector));
        }
        vectors.sort(Comparator.comparingInt(IndexedVector::index));
        return vectors.stream().map(IndexedVector::vector).toList();
    }

    // ==================== 请求执行：缓存 → 限流 → 重试 ====================

    private String execute(String path, String requestBody) {
        if (!properties.isEnabled()) {
            throw new AiException("AI 能力已关闭（tender.ai.enabled=false）");
        }
        if (StrUtil.isBlank(properties.getApiKey())) {
            throw new AiException("未配置 API key：请设置环境变量 DEEPSEEK_API_KEY");
        }
        String url = StrUtil.removeSuffix(properties.getBaseUrl(), "/") + path;
        String cacheKey = AiResponseCache.key(properties.getProvider(), properties.getModel(), url, requestBody);
        Optional<String> cached = cache.get(cacheKey);
        if (cached.isPresent()) {
            log.debug("[ai] 响应缓存命中 {}", cacheKey);
            return cached.get();
        }

        int maxRetries = properties.getMaxRetries() == null ? 0 : Math.max(0, properties.getMaxRetries());
        AiException lastFailure = null;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            if (attempt > 0) {
                sleepBackoff(attempt);
                log.warn("[ai] 第 {}/{} 次重试: {}", attempt, maxRetries, url);
            }
            if (rateLimiter != null) {
                rateLimiter.acquire();
            }
            try {
                Request request = new Request.Builder()
                        .url(url)
                        .addHeader("Authorization", "Bearer " + properties.getApiKey())
                        .post(RequestBody.create(requestBody, CONTENT_TYPE_JSON))
                        .build();
                try (Response response = httpClient.newCall(request).execute()) {
                    String body = response.body() == null ? "" : response.body().string();
                    if (response.isSuccessful()) {
                        cache.put(cacheKey, body);
                        return body;
                    }
                    AiException failure = new AiException("AI 调用失败 HTTP " + response.code() + ": "
                            + StrUtil.maxLength(body, ERROR_BODY_MAX_LENGTH));
                    // 429 与 5xx 值得重试；4xx 是请求本身有问题，重试没意义
                    if (isRetryable(response.code())) {
                        lastFailure = failure;
                        continue;
                    }
                    throw failure;
                }
            } catch (IOException e) {
                lastFailure = new AiException("AI 调用 IO 失败: " + e.getMessage(), e);
            }
        }
        throw lastFailure != null ? lastFailure : new AiException("AI 调用失败: " + url);
    }

    private boolean isRetryable(int httpStatus) {
        return httpStatus == 429 || httpStatus >= 500;
    }

    private void sleepBackoff(int attempt) {
        try {
            // 500ms, 1000ms, 2000ms ...
            Thread.sleep(500L << (attempt - 1));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiException("等待重试时被中断", e);
        }
    }

    private JsonNode readTree(String raw) {
        try {
            return objectMapper.readTree(raw);
        } catch (JsonProcessingException e) {
            throw new AiException("AI 响应不是合法 JSON: " + StrUtil.maxLength(raw, ERROR_BODY_MAX_LENGTH), e);
        }
    }

    /**
     * 向量 + 服务端返回的下标，用于重排
     */
    private record IndexedVector(int index, List<Double> vector) {
    }

}
