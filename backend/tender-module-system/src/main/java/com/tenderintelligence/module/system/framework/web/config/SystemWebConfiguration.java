package com.tenderintelligence.module.system.framework.web.config;

import com.tenderintelligence.framework.swagger.config.TenderSwaggerAutoConfiguration;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * system 模块的 web 组件的 Configuration
 *
 * @author Tender Intelligence
 */
@Configuration(proxyBeanMethods = false)
public class SystemWebConfiguration {

    /**
     * system 模块的 API 分组
     */
    @Bean
    public GroupedOpenApi systemGroupedOpenApi() {
        return TenderSwaggerAutoConfiguration.buildGroupedOpenApi("system");
    }

}
