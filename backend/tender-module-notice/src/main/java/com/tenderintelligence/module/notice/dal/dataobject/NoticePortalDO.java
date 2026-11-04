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
     * 行业（规则抽取结果，非权威标签）
     */
    private String industry;
    /**
     * 预算金额(万元)
     */
    private BigDecimal budget;
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
     * 源站详情URL(唯一去重键)
     */
    private String sourceUrl;
    /**
     * 发布时间
     */
    private LocalDateTime publishTime;
    /**
     * 开标时间
     */
    private LocalDateTime openTime;
    /**
     * 行政区划代码
     */
    private String regionCode;

    // ==================== P0 四层模型新增列（只读路径使用） ====================

    /**
     * 项目 ID（project.id）
     */
    private Long projectId;
    /**
     * 招标项目 ID（tender_project.id）
     */
    private Long tenderProjectId;
    /**
     * 标段 ID（tender_lot.id）
     */
    private Long lotId;
    /**
     * 统一交易标识码
     */
    private String unifiedDealCode;
    /**
     * 招标项目编号
     */
    private String tenderProjectNo;
    /**
     * 标段（包）编号
     */
    private String lotNo;
    /**
     * 公告标识/编号
     */
    private String noticeNo;
    /**
     * 公告类型代码：tender / win / change / explore / qualify
     */
    private String noticeTypeCode;
    /**
     * 公告子类型代码
     */
    private String noticeSubtypeCode;
    /**
     * 源站 code
     */
    private String sourceSiteCode;
    /**
     * 来源平台名称
     */
    private String sourcePlatform;
    /**
     * 文件获取开始时间
     */
    private LocalDateTime fileGetStartTime;
    /**
     * 文件获取截止时间
     */
    private LocalDateTime fileGetEndTime;
    /**
     * 答疑/澄清截止时间
     */
    private LocalDateTime qaDeadline;
    /**
     * 报名截止时间
     */
    private LocalDateTime signDeadline;
    /**
     * 投标截止时间（国标列，替代旧 deadline）
     */
    private LocalDateTime bidDeadline;
    /**
     * 开标地点
     */
    private String openPlace;
    /**
     * 公示开始时间
     */
    private LocalDateTime publicityStartTime;
    /**
     * 公示结束时间
     */
    private LocalDateTime publicityEndTime;
    /**
     * 最高限价（万元）
     */
    private BigDecimal priceCeiling;
    /**
     * 保证金（万元）
     */
    private BigDecimal depositAmount;
    /**
     * 文件售价（万元）
     */
    private BigDecimal filePrice;
    /**
     * 中标金额（万元）
     */
    private BigDecimal winAmount;
    /**
     * 采购方式代码
     */
    private String procurementMethodCode;
    /**
     * 资金来源代码
     */
    private String fundingSourceCode;
    /**
     * 评标办法代码
     */
    private String evaluationMethodCode;
    /**
     * 资格要求
     */
    private String qualificationRequirement;
    /**
     * 特定资格
     */
    private String specificQualification;
    /**
     * 是否接受联合体；null 表示未知
     */
    private Boolean consortiumAllowed;
    /**
     * 业绩要求
     */
    private String performanceRequirement;
    /**
     * 资质等级代码
     */
    private String qualificationLevelCode;
    /**
     * 交易分类代码
     */
    private String tradeCategoryCode;
    /**
     * 省级代码
     */
    private String provinceCode;
    /**
     * 市级代码
     */
    private String cityCode;
    /**
     * 区县代码
     */
    private String districtCode;
    /**
     * 招标人企业 ID
     */
    private Long tendererEnterpriseId;
    /**
     * 代理机构企业 ID
     */
    private Long agencyEnterpriseId;
    /**
     * 中标人企业 ID
     */
    private Long winnerEnterpriseId;
    /**
     * 行业代码
     */
    private String industryCode;
    /**
     * 是否有附件
     */
    private Boolean hasAttachment;
    /**
     * 浏览次数
     */
    private Integer viewCount;
    /**
     * 公告状态：ACTIVE / OFFLINE
     */
    private String status;
    /**
     * 缺失字段汇总（JSON）
     */
    private String missingFields;
    /**
     * 数据质量状态：OK / PARTIAL / BROKEN / UNKNOWN
     */
    private String dataQualityStatus;
    /**
     * 原始快照 ID
     */
    private Long rawId;
    /**
     * 解析器版本
     */
    private String parserVersion;

    /**
     * 附件列表(源站附件直链与元信息)
     *
     * 不是 notice 表的字段, 而来自子表 {@link NoticeAttachmentDO}(notice_attachment):
     * 爬虫在解析详情页时填充, 由 NoticeIngestionService 一并入库; 查询详情时由 Service 回填。
     * 故需显式声明 exist=false, 否则 MyBatis-Plus 会把它当成 notice 表的列。
     */
    @TableField(exist = false)
    private List<NoticeAttachmentDO> attachments;
}
