package com.tenderintelligence.module.ai.framework.ai.config;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.tenderintelligence.module.ai.framework.ai.core.model.AiModelFactory;
import com.tenderintelligence.module.ai.framework.ai.core.model.AiModelFactoryImpl;
import com.tenderintelligence.module.ai.framework.ai.core.model.baichuan.BaiChuanChatModel;
import com.tenderintelligence.module.ai.framework.ai.core.model.doubao.DouBaoChatModel;
import com.tenderintelligence.module.ai.framework.ai.core.model.gemini.GeminiChatModel;
import com.tenderintelligence.module.ai.framework.ai.core.model.grok.GrokChatModel;
import com.tenderintelligence.module.ai.framework.ai.core.model.hunyuan.HunYuanChatModel;
import com.tenderintelligence.module.ai.framework.ai.core.model.minimax.MiniMaxChatModel;
import com.tenderintelligence.module.ai.framework.ai.core.model.moonshot.MoonshotChatModel;
import com.tenderintelligence.module.ai.framework.ai.core.model.siliconflow.SiliconFlowApiConstants;
import com.tenderintelligence.module.ai.framework.ai.core.model.siliconflow.SiliconFlowChatModel;
import com.tenderintelligence.module.ai.framework.ai.core.model.stepfun.StepFunChatModel;
import com.tenderintelligence.module.ai.framework.ai.core.model.xinghuo.XingHuoChatModel;
import com.tenderintelligence.module.ai.framework.ai.core.model.yiyan.YiYanChatModel;
import com.tenderintelligence.module.ai.framework.ai.core.model.zhipu.ZhiPuChatModel;
import com.tenderintelligence.module.ai.framework.ai.core.webserch.AiWebSearchClient;
import com.tenderintelligence.module.ai.framework.ai.core.webserch.bocha.AiBoChaWebSearchClient;
import com.tenderintelligence.module.ai.tool.method.PersonService;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tokenizer.JTokkitTokenCountEstimator;
import org.springframework.ai.tokenizer.TokenCountEstimator;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.vectorstore.milvus.autoconfigure.MilvusServiceClientProperties;
import org.springframework.ai.vectorstore.milvus.autoconfigure.MilvusVectorStoreProperties;
import org.springframework.ai.vectorstore.qdrant.autoconfigure.QdrantVectorStoreProperties;
import org.springframework.ai.vectorstore.redis.autoconfigure.RedisVectorStoreProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * AI 自动配置
 *
 * @author fansili
 */
@Configuration
@EnableConfigurationProperties({ TenderAiProperties.class,
        QdrantVectorStoreProperties.class, // 解析 Qdrant 配置
        RedisVectorStoreProperties.class, // 解析 Redis 配置
        MilvusVectorStoreProperties.class, MilvusServiceClientProperties.class // 解析 Milvus 配置
})
@Slf4j
public class AiAutoConfiguration {

    @Bean
    public AiModelFactory aiModelFactory() {
        return new AiModelFactoryImpl();
    }

    @Bean
    @ConditionalOnMissingBean
    public ObservationRegistry observationRegistry() {
        // 特殊：兜底有 ObservationRegistry Bean，避免相关的 ChatModel 创建报错。相关 issue：https://t.zsxq.com/CuPu4
        return ObservationRegistry.NOOP;
    }

    // ========== 各种 AI Client 创建 ==========

    @Bean
    @ConditionalOnProperty(value = "tender.ai.gemini.enable", havingValue = "true")
    public GeminiChatModel geminiChatModel(TenderAiProperties tenderAiProperties) {
        TenderAiProperties.Gemini properties = tenderAiProperties.getGemini();
        return buildGeminiChatClient(properties);
    }

    public GeminiChatModel buildGeminiChatClient(TenderAiProperties.Gemini properties) {
        if (StrUtil.isEmpty(properties.getModel())) {
            properties.setModel(GeminiChatModel.MODEL_DEFAULT);
        }
        OpenAiChatModel openAiChatModel = OpenAiChatModel.builder()
                .openAiApi(OpenAiApi.builder()
                        .baseUrl(GeminiChatModel.BASE_URL)
                        .completionsPath(GeminiChatModel.COMPLETE_PATH)
                        .apiKey(properties.getApiKey())
                        .build())
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(properties.getModel())
                        .temperature(properties.getTemperature())
                        .maxTokens(properties.getMaxTokens())
                        .topP(properties.getTopP())
                        .build())
                .toolCallingManager(getToolCallingManager())
                .build();
        return new GeminiChatModel(openAiChatModel);
    }

    @Bean
    @ConditionalOnProperty(value = "tender.ai.doubao.enable", havingValue = "true")
    public DouBaoChatModel douBaoChatClient(TenderAiProperties tenderAiProperties) {
        TenderAiProperties.DouBao properties = tenderAiProperties.getDoubao();
        return buildDouBaoChatClient(properties);
    }

    public DouBaoChatModel buildDouBaoChatClient(TenderAiProperties.DouBao properties) {
        if (StrUtil.isEmpty(properties.getModel())) {
            properties.setModel(DouBaoChatModel.MODEL_DEFAULT);
        }
        DeepSeekChatModel openAiChatModel = DeepSeekChatModel.builder()
                .deepSeekApi(DeepSeekApi.builder()
                        .baseUrl(DouBaoChatModel.BASE_URL)
                        .completionsPath(DouBaoChatModel.COMPLETE_PATH)
                        .apiKey(properties.getApiKey())
                        .build())
                .defaultOptions(DeepSeekChatOptions.builder()
                        .model(properties.getModel())
                        .temperature(properties.getTemperature())
                        .maxTokens(properties.getMaxTokens())
                        .topP(properties.getTopP())
                        .build())
                .toolCallingManager(getToolCallingManager())
                .build();
        return new DouBaoChatModel(openAiChatModel);
    }

    @Bean
    @ConditionalOnProperty(value = "tender.ai.siliconflow.enable", havingValue = "true")
    public SiliconFlowChatModel siliconFlowChatClient(TenderAiProperties tenderAiProperties) {
        TenderAiProperties.SiliconFlow properties = tenderAiProperties.getSiliconflow();
        return buildSiliconFlowChatClient(properties);
    }

    public SiliconFlowChatModel buildSiliconFlowChatClient(TenderAiProperties.SiliconFlow properties) {
        if (StrUtil.isEmpty(properties.getModel())) {
            properties.setModel(SiliconFlowApiConstants.MODEL_DEFAULT);
        }
        DeepSeekChatModel openAiChatModel = DeepSeekChatModel.builder()
                .deepSeekApi(DeepSeekApi.builder()
                        .baseUrl(SiliconFlowApiConstants.DEFAULT_BASE_URL)
                        .apiKey(properties.getApiKey())
                        .build())
                .defaultOptions(DeepSeekChatOptions.builder()
                        .model(properties.getModel())
                        .temperature(properties.getTemperature())
                        .maxTokens(properties.getMaxTokens())
                        .topP(properties.getTopP())
                        .build())
                .toolCallingManager(getToolCallingManager())
                .build();
        return new SiliconFlowChatModel(openAiChatModel);
    }

    @Bean
    @ConditionalOnProperty(value = "tender.ai.hunyuan.enable", havingValue = "true")
    public HunYuanChatModel hunYuanChatClient(TenderAiProperties tenderAiProperties) {
        TenderAiProperties.HunYuan properties = tenderAiProperties.getHunyuan();
        return buildHunYuanChatClient(properties);
    }

    public HunYuanChatModel buildHunYuanChatClient(TenderAiProperties.HunYuan properties) {
        if (StrUtil.isEmpty(properties.getModel())) {
            properties.setModel(HunYuanChatModel.MODEL_DEFAULT);
        }
        if (StrUtil.isEmpty(properties.getBaseUrl())) {
            properties.setBaseUrl(HunYuanChatModel.BASE_URL);
        }
        // 创建 DeepSeekChatModel、HunYuanChatModel 对象
        DeepSeekChatModel openAiChatModel = DeepSeekChatModel.builder()
                .deepSeekApi(DeepSeekApi.builder()
                        .baseUrl(properties.getBaseUrl())
                        .completionsPath(HunYuanChatModel.COMPLETE_PATH)
                        .apiKey(properties.getApiKey())
                        .build())
                .defaultOptions(DeepSeekChatOptions.builder()
                        .model(properties.getModel())
                        .temperature(properties.getTemperature())
                        .maxTokens(properties.getMaxTokens())
                        .topP(properties.getTopP())
                        .build())
                .toolCallingManager(getToolCallingManager())
                .build();
        return new HunYuanChatModel(openAiChatModel);
    }

    @Bean
    @ConditionalOnProperty(value = "tender.ai.xinghuo.enable", havingValue = "true")
    public XingHuoChatModel xingHuoChatClient(TenderAiProperties tenderAiProperties) {
        TenderAiProperties.XingHuo properties = tenderAiProperties.getXinghuo();
        return buildXingHuoChatClient(properties);
    }

    public XingHuoChatModel buildXingHuoChatClient(TenderAiProperties.XingHuo properties) {
        if (StrUtil.isEmpty(properties.getModel())) {
            properties.setModel(XingHuoChatModel.MODEL_DEFAULT);
        }
        return XingHuoChatModel.builder()
                .apiKey(properties.getApiKey())
                .options(DeepSeekChatOptions.builder()
                        .model(properties.getModel())
                        .temperature(properties.getTemperature())
                        .maxTokens(properties.getMaxTokens())
                        .topP(properties.getTopP())
                        .build())
                .build();
    }

    @Bean
    @ConditionalOnProperty(value = "tender.ai.baichuan.enable", havingValue = "true")
    public BaiChuanChatModel baiChuanChatClient(TenderAiProperties tenderAiProperties) {
        TenderAiProperties.BaiChuan properties = tenderAiProperties.getBaichuan();
        return buildBaiChuanChatClient(properties);
    }

    public BaiChuanChatModel buildBaiChuanChatClient(TenderAiProperties.BaiChuan properties) {
        if (StrUtil.isEmpty(properties.getModel())) {
            properties.setModel(BaiChuanChatModel.MODEL_DEFAULT);
        }
        DeepSeekChatModel deepSeekChatModel = DeepSeekChatModel.builder()
                .deepSeekApi(DeepSeekApi.builder()
                        .baseUrl(BaiChuanChatModel.BASE_URL)
                        .apiKey(properties.getApiKey())
                        .completionsPath(BaiChuanChatModel.COMPLETE_PATH)
                        .build())
                .defaultOptions(DeepSeekChatOptions.builder()
                        .model(properties.getModel())
                        .temperature(properties.getTemperature())
                        .maxTokens(properties.getMaxTokens())
                        .topP(properties.getTopP())
                        .build())
                .toolCallingManager(getToolCallingManager())
                .build();
        return new BaiChuanChatModel(deepSeekChatModel);
    }

    @Bean
    @ConditionalOnProperty(value = "tender.ai.yiyan.enable", havingValue = "true")
    public YiYanChatModel yiYanChatClient(TenderAiProperties tenderAiProperties) {
        TenderAiProperties.YiYan properties = tenderAiProperties.getYiyan();
        return buildYiYanChatClient(properties);
    }

    public YiYanChatModel buildYiYanChatClient(TenderAiProperties.YiYan properties) {
        if (StrUtil.isEmpty(properties.getModel())) {
            properties.setModel(YiYanChatModel.MODEL_DEFAULT);
        }
        return new YiYanChatModel(buildDeepSeekCompatibleChatModel(
                StrUtil.blankToDefault(properties.getBaseUrl(), YiYanChatModel.BASE_URL),
                null, properties.getApiKey(), properties.getModel(), properties.getTemperature(),
                properties.getMaxTokens(), properties.getTopP()));
    }

    @Bean
    @ConditionalOnProperty(value = "tender.ai.zhipu.enable", havingValue = "true")
    public ZhiPuChatModel zhiPuChatClient(TenderAiProperties tenderAiProperties) {
        TenderAiProperties.ZhiPu properties = tenderAiProperties.getZhipu();
        return buildZhiPuChatClient(properties);
    }

    public ZhiPuChatModel buildZhiPuChatClient(TenderAiProperties.ZhiPu properties) {
        if (StrUtil.isEmpty(properties.getModel())) {
            properties.setModel(ZhiPuChatModel.MODEL_DEFAULT);
        }
        DeepSeekChatModel deepSeekChatModel = DeepSeekChatModel.builder()
                .deepSeekApi(DeepSeekApi.builder()
                        .baseUrl(StrUtil.blankToDefault(properties.getBaseUrl(), ZhiPuChatModel.BASE_URL))
                        .apiKey(properties.getApiKey())
                        .build())
                .defaultOptions(DeepSeekChatOptions.builder()
                        .model(properties.getModel())
                        .temperature(properties.getTemperature())
                        .maxTokens(properties.getMaxTokens())
                        .topP(properties.getTopP())
                        .build())
                .build();
        return new ZhiPuChatModel(deepSeekChatModel);
    }

    @Bean
    @ConditionalOnProperty(value = "tender.ai.minimax.enable", havingValue = "true")
    public MiniMaxChatModel miniMaxChatClient(TenderAiProperties tenderAiProperties) {
        TenderAiProperties.MiniMax properties = tenderAiProperties.getMinimax();
        return buildMiniMaxChatClient(properties);
    }

    public MiniMaxChatModel buildMiniMaxChatClient(TenderAiProperties.MiniMax properties) {
        if (StrUtil.isEmpty(properties.getModel())) {
            properties.setModel(MiniMaxChatModel.MODEL_DEFAULT);
        }
        return new MiniMaxChatModel(buildDeepSeekCompatibleChatModel(
                StrUtil.blankToDefault(properties.getBaseUrl(), MiniMaxChatModel.BASE_URL),
                null, properties.getApiKey(), properties.getModel(), properties.getTemperature(),
                properties.getMaxTokens(), properties.getTopP()));
    }

    @Bean
    @ConditionalOnProperty(value = "tender.ai.moonshot.enable", havingValue = "true")
    public MoonshotChatModel moonshotChatClient(TenderAiProperties tenderAiProperties) {
        TenderAiProperties.Moonshot properties = tenderAiProperties.getMoonshot();
        return buildMoonshotChatClient(properties);
    }

    public MoonshotChatModel buildMoonshotChatClient(TenderAiProperties.Moonshot properties) {
        if (StrUtil.isEmpty(properties.getModel())) {
            properties.setModel(MoonshotChatModel.MODEL_DEFAULT);
        }
        return new MoonshotChatModel(buildDeepSeekCompatibleChatModel(
                StrUtil.blankToDefault(properties.getBaseUrl(), MoonshotChatModel.BASE_URL),
                MoonshotChatModel.COMPLETE_PATH, properties.getApiKey(), properties.getModel(),
                properties.getTemperature(), properties.getMaxTokens(), properties.getTopP()));
    }

    @Bean
    @ConditionalOnProperty(value = "tender.ai.stepfun.enable", havingValue = "true")
    public StepFunChatModel stepFunChatClient(TenderAiProperties tenderAiProperties) {
        TenderAiProperties.StepFun properties = tenderAiProperties.getStepfun();
        return buildStepFunChatClient(properties);
    }

    public StepFunChatModel buildStepFunChatClient(TenderAiProperties.StepFun properties) {
        if (StrUtil.isEmpty(properties.getModel())) {
            properties.setModel(StepFunChatModel.MODEL_DEFAULT);
        }
        return new StepFunChatModel(buildDeepSeekCompatibleChatModel(
                StrUtil.blankToDefault(properties.getBaseUrl(), StepFunChatModel.BASE_URL),
                StepFunChatModel.COMPLETE_PATH, properties.getApiKey(), properties.getModel(),
                properties.getTemperature(), properties.getMaxTokens(), properties.getTopP()));
    }

    private static DeepSeekChatModel buildDeepSeekCompatibleChatModel(String baseUrl, String completionsPath,
                                                                     String apiKey, String model,
                                                                     Double temperature, Integer maxTokens,
                                                                     Double topP) {
        DeepSeekApi.Builder apiBuilder = DeepSeekApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey);
        if (StrUtil.isNotEmpty(completionsPath)) {
            apiBuilder.completionsPath(completionsPath);
        }
        return DeepSeekChatModel.builder()
                .deepSeekApi(apiBuilder.build())
                .defaultOptions(DeepSeekChatOptions.builder()
                        .model(model)
                        .temperature(temperature)
                        .maxTokens(maxTokens)
                        .topP(topP)
                        .build())
                .toolCallingManager(getToolCallingManager())
                .build();
    }

    @Bean
    @ConditionalOnProperty(value = "tender.ai.grok.enable", havingValue = "true")
    public GrokChatModel grokChatClient(TenderAiProperties tenderAiProperties) {
        TenderAiProperties.Grok properties = tenderAiProperties.getGrok();
        return buildGrokChatClient(properties);
    }

    public GrokChatModel buildGrokChatClient(TenderAiProperties.Grok properties) {
        if (StrUtil.isEmpty(properties.getModel())) {
            properties.setModel(GrokChatModel.MODEL_DEFAULT);
        }
        OpenAiChatModel openAiChatModel = OpenAiChatModel.builder()
                .openAiApi(OpenAiApi.builder()
                        .baseUrl(StrUtil.blankToDefault(properties.getBaseUrl(), GrokChatModel.BASE_URL))
                        .completionsPath(GrokChatModel.COMPLETE_PATH)
                        .apiKey(properties.getApiKey())
                        .build())
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(properties.getModel())
                        .temperature(properties.getTemperature())
                        .maxTokens(properties.getMaxTokens())
                        .topP(properties.getTopP())
                        .build())
                .toolCallingManager(getToolCallingManager())
                .build();
        return new GrokChatModel(openAiChatModel);
    }

    // ========== RAG 相关 ==========

    @Bean
    public TokenCountEstimator tokenCountEstimator() {
        return new JTokkitTokenCountEstimator();
    }

    @Bean
    public BatchingStrategy batchingStrategy() {
        return new TokenCountBatchingStrategy();
    }

    private static ToolCallingManager getToolCallingManager() {
        return SpringUtil.getBean(ToolCallingManager.class);
    }

    // ========== Web Search 相关 ==========

    @Bean
    @ConditionalOnProperty(value = "tender.ai.web-search.enable", havingValue = "true")
    public AiWebSearchClient webSearchClient(TenderAiProperties tenderAiProperties) {
        return new AiBoChaWebSearchClient(tenderAiProperties.getWebSearch().getApiKey());
    }

    // ========== MCP 相关 ==========

    /**
     * 参考自 <a href="https://docs.spring.io/spring-ai/reference/api/mcp/mcp-client-boot-starter-docs.html">MCP Server Boot Starter</>
     */
    @Bean
    public List<ToolCallback> toolCallbacks(PersonService personService) {
        return List.of(ToolCallbacks.from(personService));
    }

}
