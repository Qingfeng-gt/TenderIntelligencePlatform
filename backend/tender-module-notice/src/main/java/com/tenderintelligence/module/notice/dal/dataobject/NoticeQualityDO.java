package com.tenderintelligence.module.notice.dal.dataobject;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tenderintelligence.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 公告数据质量
 *
 * <p>对应表 {@code notice_quality}。采集入库时按缺失字段/非法值写入，供运营核对。
 */
@TableName("notice_quality")
@KeySequence("notice_quality_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class NoticeQualityDO extends BaseDO {

    @TableId
    private Long id;
    /** 公告 ID */
    private Long noticeId;
    /** MISSING_FIELD/PARSE_ERROR/INVALID_VALUE/DUPLICATE/OTHER */
    private String issueType;
    /** 字段名 */
    private String fieldName;
    /** INFO/WARN/ERROR */
    private String severity;
    /** 问题描述 */
    private String detail;
    /** 来源 URL */
    private String sourceUrl;
    /** 解析器版本 */
    private String parserVersion;
    /** OPEN/RESOLVED/IGNORED */
    private String status;
    /** 处理人 */
    private String resolvedBy;
    /** 处理时间 */
    private LocalDateTime resolvedTime;
}
