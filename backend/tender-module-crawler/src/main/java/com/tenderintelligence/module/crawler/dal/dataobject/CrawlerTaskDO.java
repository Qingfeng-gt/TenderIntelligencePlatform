package com.tenderintelligence.module.crawler.dal.dataobject;

import com.tenderintelligence.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 爬虫采集任务日志
 */
@TableName("crawler_task")
@KeySequence("crawler_task_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CrawlerTaskDO extends BaseDO {

    /** 任务编号 */
    @TableId
    private Long id;
    /** 站点标识 */
    private String siteCode;
    /** 开始时间 */
    private LocalDateTime startTime;
    /** 结束时间 */
    private LocalDateTime endTime;
    /** 状态:RUNNING/SUCCESS/FAILED */
    private String status;
    /** 列表页抓取数 */
    private Integer listFetched;
    /** 列表解析出公告数 */
    private Integer listParsed;
    /** 详情页抓取成功数 */
    private Integer detailFetched;
    /** 详情页抓取失败数 */
    private Integer detailFailed;
    /** 新增入库数 */
    private Integer newInsert;
    /** 变更更新数 */
    private Integer updated;
    /** 失败原因 */
    private String errorMsg;
}
