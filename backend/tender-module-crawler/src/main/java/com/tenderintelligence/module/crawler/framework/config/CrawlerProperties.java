package com.tenderintelligence.module.crawler.framework.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 爬虫采集配置(定时触发)
 */
@ConfigurationProperties(prefix = "tender.crawler")
@Data
public class CrawlerProperties {

    /**
     * 定时采集开关。
     * cron 设为空/OFF 会导致启动报错,关闭定时请用本开关
     */
    private boolean scheduleEnabled = true;

    /** 采集 cron(Spring 6 位、秒在前),默认每 2 小时 */
    private String scheduleCron = "0 0 */2 * * ?";

}
