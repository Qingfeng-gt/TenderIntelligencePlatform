package com.tenderintelligence.module.crawler.dal.dataobject;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tenderintelligence.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 采集频道配置
 *
 * <p>对应表 {@code crawler_channel}，替代旧 {@code crawler_site.channels} JSON。
 * {@code config} 保存频道级扩展配置（如 ccgp 的 {@code template}）。
 */
@TableName("crawler_channel")
@KeySequence("crawler_channel_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CrawlerChannelDO extends BaseDO {

    @TableId
    private Long id;
    /** 站点 code */
    private String siteCode;
    /** 频道路径或类别编码 */
    private String path;
    /** 频道名 */
    private String name;
    /** 公告类型代码 */
    private String noticeTypeCode;
    /** 每轮最大翻页数 */
    private Integer pageCount;
    /** 频道级调度 */
    private String scheduleCron;
    /** 是否启用 */
    private Boolean enabled;
    /** 排序 */
    private Integer sort;
    /** 频道级扩展配置 JSON（如 template） */
    private String config;
}
