package com.tenderintelligence.module.crawler.dal.dataobject;

import com.tenderintelligence.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 爬虫站点配置
 *
 * 一个站点对应一个 SourceAdapter 实现,channels 为频道JSON配置
 */
@TableName("crawler_site")
@KeySequence("crawler_site_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CrawlerSiteDO extends BaseDO {

    /** 站点编号 */
    @TableId
    private Long id;
    /** 站点名称 */
    private String name;
    /** 站点标识,对应 SourceAdapter 实现 */
    private String code;
    /** 是否启用 */
    private Boolean enabled;
    /**
     * 频道JSON:[{path,name,type,pageCount}]
     */
    private String channels;
    /** 详情页请求间隔(ms,防反爬) */
    private Long intervalMs;
    /** 抓取配置JSON(UA/首访URL等) */
    private String config;
}
