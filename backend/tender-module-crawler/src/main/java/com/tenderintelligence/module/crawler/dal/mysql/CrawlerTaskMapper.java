package com.tenderintelligence.module.crawler.dal.mysql;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.tenderintelligence.framework.common.pojo.PageParam;
import com.tenderintelligence.framework.common.pojo.PageResult;
import com.tenderintelligence.framework.mybatis.core.mapper.BaseMapperX;
import com.tenderintelligence.module.crawler.dal.dataobject.CrawlerTaskDO;
import com.tenderintelligence.module.crawler.enums.CrawlerTaskStatusEnum;
import org.apache.ibatis.annotations.Mapper;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 采集任务日志 Mapper
 */
@Mapper
public interface CrawlerTaskMapper extends BaseMapperX<CrawlerTaskDO> {

    default PageResult<CrawlerTaskDO> selectPageBySite(PageParam pageParam, String siteCode) {
        return selectPage(pageParam,
                new LambdaQueryWrapper<CrawlerTaskDO>()
                        .eq(siteCode != null && !siteCode.isEmpty(), CrawlerTaskDO::getSiteCode, siteCode)
                        .orderByDesc(CrawlerTaskDO::getId));
    }

    /** 最近一次任务 */
    default CrawlerTaskDO selectBySiteCode(String siteCode) {
        return selectOne(
                new LambdaQueryWrapper<CrawlerTaskDO>()
                        .eq(CrawlerTaskDO::getSiteCode, siteCode)
                        .orderByDesc(CrawlerTaskDO::getId)
                        .last("LIMIT 1"));
    }

    /** 站点进行中任务数(防重入: 手动+定时并发时跳过该站点) */
    default Long selectRunningCount(String siteCode) {
        return selectCount(new LambdaQueryWrapper<CrawlerTaskDO>()
                .eq(CrawlerTaskDO::getSiteCode, siteCode)
                .eq(CrawlerTaskDO::getStatus, CrawlerTaskStatusEnum.RUNNING.getStatus()));
    }

    /** 回收卡死任务: 超时(stale)的 RUNNING 置为 FAILED, 避免僵尸任务永久挡住站点 */
    default Integer failStaleTasks(Duration stale) {
        return update(null, new LambdaUpdateWrapper<CrawlerTaskDO>()
                .eq(CrawlerTaskDO::getStatus, CrawlerTaskStatusEnum.RUNNING.getStatus())
                .lt(CrawlerTaskDO::getStartTime, LocalDateTime.now().minus(stale))
                .set(CrawlerTaskDO::getStatus, CrawlerTaskStatusEnum.FAILED.getStatus())
                .set(CrawlerTaskDO::getErrorMsg, "超时自动回收")
                .set(CrawlerTaskDO::getEndTime, LocalDateTime.now()));
    }
}
