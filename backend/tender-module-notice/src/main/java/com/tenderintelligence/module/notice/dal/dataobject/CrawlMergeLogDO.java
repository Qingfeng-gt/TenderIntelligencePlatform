package com.tenderintelligence.module.notice.dal.dataobject;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tenderintelligence.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 判重 / 归并日志
 *
 * <p>对应表 {@code crawl_merge_log}。采集入库时记录公告归并到哪个项目/招标项目/标段。
 */
@TableName("crawl_merge_log")
@KeySequence("crawl_merge_log_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CrawlMergeLogDO extends BaseDO {

    @TableId
    private Long id;
    /** 源公告 ID */
    private Long sourceNoticeId;
    /** 归并到的项目 */
    private Long targetProjectId;
    /** 归并到的招标项目 */
    private Long targetTenderProjectId;
    /** 归并到的标段 */
    private Long targetLotId;
    /** UNIFIED_CODE/LOT_NO/PROJECT_NO_NOTICE_ID/SOURCE_URL/MANUAL */
    private String matchType;
    /** 匹配分 */
    private BigDecimal matchScore;
    /** AUTO/MANUAL/CONFLICT */
    private String status;
    /** 匹配依据（JSON） */
    private String detail;
    /** 操作时间 */
    private LocalDateTime operatedAt;
}
