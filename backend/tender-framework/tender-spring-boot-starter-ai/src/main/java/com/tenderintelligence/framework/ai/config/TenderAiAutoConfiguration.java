package com.tenderintelligence.framework.ai.config;

import com.tenderintelligence.framework.ai.core.AiClient;
import com.tenderintelligence.framework.ai.core.AiResponseCache;
import com.tenderintelligence.framework.ai.core.OpenAiCompatibleAiClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * AI 组件的自动配置
 *
 * <p>刻意<b>不</b>用 {@code @ConditionalOnProperty(tender.ai.enabled)} 来跳过整个配置类：
 * 那样 {@code tender.ai.enabled=false} 时容器里没有 {@code AiClient} Bean，
 * 甲/丁/戊 的 {@code @Resource AiClient} 会在<b>启动时</b>就失败。
 * 现在改成 Bean 始终存在、调用时才由客户端自己判断开关并抛出可读的异常。
 */
@AutoConfiguration
@EnableConfigurationProperties(TenderAiProperties.class)
public class TenderAiAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AiResponseCache aiResponseCache(TenderAiProperties properties) {
        return new AiResponseCache(properties.getCache().isEnabled(), properties.getCache().getDir());
    }

    @Bean
    @ConditionalOnMissingBean
    public AiClient aiClient(TenderAiProperties properties, AiResponseCache aiResponseCache) {
        return new OpenAiCompatibleAiClient(properties, aiResponseCache);
    }

}
