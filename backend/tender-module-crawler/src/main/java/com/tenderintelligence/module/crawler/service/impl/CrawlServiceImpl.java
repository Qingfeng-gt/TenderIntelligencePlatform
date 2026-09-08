package com.tenderintelligence.module.crawler.service.impl;

import com.tenderintelligence.framework.common.pojo.PageParam;
import com.tenderintelligence.framework.common.pojo.PageResult;
import com.tenderintelligence.module.crawler.dal.dataobject.CrawlerSiteDO;
import com.tenderintelligence.module.crawler.dal.dataobject.CrawlerTaskDO;
import com.tenderintelligence.module.crawler.dal.mysql.CrawlerSiteMapper;
import com.tenderintelligence.module.crawler.dal.mysql.CrawlerTaskMapper;
import com.tenderintelligence.module.crawler.enums.CrawlerTaskStatusEnum;
import com.tenderintelligence.module.crawler.enums.ErrorCodeConstants;
import com.tenderintelligence.module.crawler.service.CrawlService;
import com.tenderintelligence.module.crawler.service.CrawlStats;
import com.tenderintelligence.module.crawler.service.SourceAdapter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.tenderintelligence.framework.common.exception.util.ServiceExceptionUtil.exception;

/**
 * 采集服务实现(异步执行: 接口立即返回任务号, 后台串行跑)
 */
@Service
@Slf4j
public class CrawlServiceImpl implements CrawlService {

    /** 采集线程池(单线程串行, 防止并发打爆源站) */
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "crawler-run");
        thread.setDaemon(true);
        return thread;
    });

    @Resource
    private CrawlerSiteMapper crawlSiteMapper;
    @Resource
    private CrawlerTaskMapper crawlTaskMapper;
    /** 当前已注册的源适配器(Spring 自动注入) */
    private final List<SourceAdapter> adapters;

    public CrawlServiceImpl(List<SourceAdapter> adapters) {
        this.adapters = adapters;
    }

    @Override
    public Long run(String siteCode) {
        // 每轮先回收卡死的 RUNNING 任务(进程崩溃后遗留,避免永久挡住该站点)
        crawlTaskMapper.failStaleTasks(Duration.ofHours(4));
        List<CrawlerSiteDO> sites;
        if (siteCode != null && !siteCode.isEmpty()) {
            CrawlerSiteDO site = crawlSiteMapper.selectByCode(siteCode);
            if (site == null) {
                throw exception(ErrorCodeConstants.CRAWLER_SITE_NOT_EXISTS);
            }
            if (Boolean.FALSE.equals(site.getEnabled())) {
                throw exception(ErrorCodeConstants.CRAWLER_SITE_DISABLED);
            }
            sites = List.of(site);
        } else {
            sites = crawlSiteMapper.selectList(null).stream()
                    .filter(site -> !Boolean.FALSE.equals(site.getEnabled()))
                    .toList();
        }
        Long firstTaskId = null;
        for (CrawlerSiteDO site : sites) {
            // 防重入: 该站点已有执行中的任务时跳过(定时 + 手动并发)
            if (crawlTaskMapper.selectRunningCount(site.getCode()) > 0) {
                log.warn("[crawler] 站点[{}]已有 RUNNING 任务, 跳过本次提交", site.getCode());
                continue;
            }
            CrawlerTaskDO task = createTask(site.getCode());
            if (firstTaskId == null) {
                firstTaskId = task.getId();
            }
            EXECUTOR.submit(() -> runSite(site, task));
        }
        return firstTaskId;
    }

    private CrawlerTaskDO createTask(String siteCode) {
        CrawlerTaskDO task = new CrawlerTaskDO();
        task.setSiteCode(siteCode);
        task.setStartTime(LocalDateTime.now());
        task.setStatus(CrawlerTaskStatusEnum.RUNNING.getStatus());
        crawlTaskMapper.insert(task);
        return task;
    }

    private void runSite(CrawlerSiteDO site, CrawlerTaskDO task) {
        SourceAdapter adapter = findAdapter(site.getCode());
        if (adapter == null) {
            task.setStatus(CrawlerTaskStatusEnum.FAILED.getStatus());
            task.setErrorMsg("无对应适配器: " + site.getCode());
            task.setEndTime(LocalDateTime.now());
            crawlTaskMapper.updateById(task);
            return;
        }
        log.info("[crawler] 开始采集站点[{}] 任务#{}", site.getName(), task.getId());
        CrawlStats stats = new CrawlStats();
        try {
            adapter.crawl(site, stats);
            task.setStatus(CrawlerTaskStatusEnum.SUCCESS.getStatus());
            task.setErrorMsg("");
        } catch (Exception ex) {
            task.setStatus(CrawlerTaskStatusEnum.FAILED.getStatus());
            task.setErrorMsg(ex.getMessage() == null ? ex.getClass().getSimpleName()
                    : ex.getMessage().substring(0, Math.min(500, ex.getMessage().length())));
            log.error("[crawler] 采集站点[{}] 失败: {}", site.getName(), task.getErrorMsg(), ex);
        }
        task.setEndTime(LocalDateTime.now());
        task.setListFetched(stats.getListFetched());
        task.setListParsed(stats.getListParsed());
        task.setDetailFetched(stats.getDetailFetched());
        task.setDetailFailed(stats.getDetailFailed());
        task.setNewInsert(stats.getNewInsert());
        task.setUpdated(stats.getUpdated());
        crawlTaskMapper.updateById(task);
        log.info("[crawler] 站点[{}] 任务#{} 完成: status={} new={} updated={}",
                site.getName(), task.getId(), task.getStatus(), stats.getNewInsert(), stats.getUpdated());
    }

    private SourceAdapter findAdapter(String code) {
        return adapters.stream().filter(adapter -> adapter.code().equals(code)).findFirst().orElse(null);
    }

    @Override
    public List<CrawlerSiteDO> getSiteList() {
        return crawlSiteMapper.selectList(null);
    }

    @Override
    public PageResult<CrawlerTaskDO> getTaskPage(PageParam pageParam, String siteCode) {
        return crawlTaskMapper.selectPageBySite(pageParam, siteCode);
    }

    @Override
    public CrawlerTaskDO getLatestTask(String siteCode) {
        return crawlTaskMapper.selectBySiteCode(siteCode);
    }
}
