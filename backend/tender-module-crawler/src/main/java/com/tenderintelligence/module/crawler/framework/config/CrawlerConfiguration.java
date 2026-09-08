package com.tenderintelligence.module.crawler.framework.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 爬虫模块配置
 */
@Configuration
@EnableConfigurationProperties(CrawlerProperties.class)
public class CrawlerConfiguration {

}
