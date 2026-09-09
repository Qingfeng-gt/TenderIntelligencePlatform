package com.tenderintelligence.module.infra.framework.file.config;

import com.tenderintelligence.module.infra.framework.file.core.client.FileClientFactory;
import com.tenderintelligence.module.infra.framework.file.core.client.FileClientFactoryImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 文件配置类
 *
 * @author Tender Intelligence
 */
@Configuration(proxyBeanMethods = false)
public class TenderFileAutoConfiguration {

    @Bean
    public FileClientFactory fileClientFactory() {
        return new FileClientFactoryImpl();
    }

}
