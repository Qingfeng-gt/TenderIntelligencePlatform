package com.tenderintelligence.module.crawler.service;

import com.tenderintelligence.framework.tenant.core.aop.TenantIgnore;
import com.tenderintelligence.module.crawler.framework.config.CrawlerProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时采集任务: 每 2 小时自动触发一轮全站点采集(可配置)
 *
 * 默认触发时间由 tender.crawler.schedule-cron 控制;
 * 关闭定时请用 tender.crawler.schedule-enabled=false(cron 设为空会导致启动报错)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledCrawlTask {

    private final CrawlService crawlService;
    private final CrawlerProperties crawlerProperties;

    @Scheduled(cron = "${tender.crawler.schedule-cron:0 0 */2 * * ?}")
    @TenantIgnore // 定时线程非 HTTP 请求, 需显式忽略租户上下文(与 TokenCleanJob 一致)
    public void runAllSites() {
        if (!crawlerProperties.isScheduleEnabled()) {
            log.info("[crawler] 定时采集已关闭(tender.crawler.schedule-enabled=false),跳过本轮");
            return;
        }
        log.info("[crawler] 定时采集开始(每 2 小时一轮)");
        // 复用 run(null): 自动排除停用站点; 内部并发防重入(已有 RUNNING 任务则跳过)
        Long firstTaskId = crawlService.run(null);
        if (firstTaskId == null) {
            log.info("[crawler] 定时采集: 所有启用站点均在采集中,无新任务提交");
        }
    }

}
