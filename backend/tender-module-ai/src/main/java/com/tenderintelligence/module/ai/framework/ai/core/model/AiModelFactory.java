package com.tenderintelligence.module.ai.framework.ai.core.model;

import com.tenderintelligence.module.ai.enums.model.AiPlatformEnum;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.Map;

/**
 * AI Model 模型工厂的接口类
 *
 * @author fansili
 */
public interface AiModelFactory {

    /**
     * 基于指定配置，获得 ChatModel 对象
     *
     * 如果不存在，则进行创建
     *
     * @param platform 平台
     * @param apiKey API KEY
     * @param url API URL
     * @return ChatModel 对象
     */
    ChatModel getOrCreateChatModel(AiPlatformEnum platform, String apiKey, String url);

    /**
     * 基于默认配置，获得 ChatModel 对象
     *
     * 默认配置，指的是在 application.yaml 配置文件中的 spring.ai 相关的配置
     *
     * @param platform 平台
     * @return ChatModel 对象
     */
    ChatModel getDefaultChatModel(AiPlatformEnum platform);

    /**
     * 基于指定配置，获得 EmbeddingModel 对象
     *
     * 如果不存在，则进行创建
     *
     * @param platform 平台
     * @param apiKey   API KEY
     * @param url      API URL
     * @param model     模型
     * @return ChatModel 对象
     */
    EmbeddingModel getOrCreateEmbeddingModel(AiPlatformEnum platform, String apiKey, String url, String model);

    /**
     * 基于指定配置，获得 VectorStore 对象
     *
     * 如果不存在，则进行创建
     *
     * @param type           向量存储类型
     * @param embeddingModel 向量模型
     * @param metadataFields 元数据字段
     * @return VectorStore 对象
     */
    VectorStore getOrCreateVectorStore(Class<? extends VectorStore> type,
                                       EmbeddingModel embeddingModel,
                                       Map<String, Class<?>> metadataFields);

}
