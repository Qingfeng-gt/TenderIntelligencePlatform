package com.tenderintelligence.framework.ai.core;

/**
 * AI 调用异常
 *
 * <p>刻意不继承框架的 ServiceException：AI 调用失败不是业务错误，调用方（甲/丁/戊）
 * 自己决定是降级、重试还是抛出——例如乙的 LLM 抽取兜底失败时应当退回规则抽取的结果。
 */
public class AiException extends RuntimeException {

    public AiException(String message) {
        super(message);
    }

    public AiException(String message, Throwable cause) {
        super(message, cause);
    }

}
