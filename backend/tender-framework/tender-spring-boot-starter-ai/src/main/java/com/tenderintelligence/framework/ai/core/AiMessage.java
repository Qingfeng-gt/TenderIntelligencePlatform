package com.tenderintelligence.framework.ai.core;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 一条对话消息（OpenAI chat 协议的 {@code messages[]} 元素）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiMessage {

    public static final String ROLE_SYSTEM = "system";
    public static final String ROLE_USER = "user";
    public static final String ROLE_ASSISTANT = "assistant";

    /**
     * 角色：system / user / assistant
     */
    private String role;

    /**
     * 内容
     */
    private String content;

    public static AiMessage system(String content) {
        return new AiMessage(ROLE_SYSTEM, content);
    }

    public static AiMessage user(String content) {
        return new AiMessage(ROLE_USER, content);
    }

    public static AiMessage assistant(String content) {
        return new AiMessage(ROLE_ASSISTANT, content);
    }

}
