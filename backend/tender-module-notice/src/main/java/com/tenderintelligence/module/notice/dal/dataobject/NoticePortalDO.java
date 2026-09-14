package com.tenderintelligence.module.notice.dal.dataobject;

import com.tenderintelligence.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 招投标公告（标讯）DO
 *
 * 与用户端门户 user-web 的 Notice 结构对齐;数据由爬虫采集入库
 */
@TableName("notice")
@KeySequence("notice_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class NoticePortalDO extends BaseDO {

    /**
     * 公告编号
     */
    @TableId
    private Long id;
    /**
     * 公告标题
     */
    private String title;
    /**
     * 项目编号(源站)
     */
    private String projectNo;
    /**
     * 公告类型:tender 招标 / win 中标 / change 变更 / explore 采购
     *
     * 枚举 {@link com.tenderintelligence.module.notice.enums.NoticeTypeEnum}
     */
    private String type;
    /**
     * 省份
     */
    private String province;
    /**
     * 城市
     */
    private String city;
    /**
     * 行业
     */
    private String industry;
    /**
     * 预算金额(万元)
     */
    private BigDecimal budget;
    /**
     * 招标人 / 采购人
     */
    private String tenderPerson;
    /**
     * 代理机构
     */
    private String agency;
    /**
     * 联系人
     */
    private String contact;
    /**
     * 联系电话
     */
    private String contactPhone;
    /**
     * 公告原文(HTML)
     */
    private String content;
    /**
     * 来源网站
     */
    private String source;
    /**
     * 源站详情URL(唯一去重键)
     */
    private String sourceUrl;
    /**
     * 发布时间
     */
    private LocalDateTime publishTime;
    /**
     * 投标截止时间
     */
    private LocalDateTime deadline;
    /**
     * 开标时间
     */
    private LocalDateTime openTime;
    /**
     * 源站地区代码(如 610000)
     */
    private String regionCode;
    /**
     * 附件列表(源站附件直链与元信息)
     *
     * 不是 notice 表的字段, 而来自子表 {@link NoticeAttachmentDO}(notice_attachment):
     * 爬虫在解析详情页时填充, 由 NoticeUpsertService 一并入库; 查询详情时由 Service 回填。
     * 故需显式声明 exist=false, 否则 MyBatis-Plus 会把它当成 notice 表的列。
     */
    @TableField(exist = false)
    private List<NoticeAttachmentDO> attachments;
}
