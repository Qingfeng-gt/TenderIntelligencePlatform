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
 * 项目数据集（四层模型第一层）
 *
 * <p>一个项目下可以有多个招标项目、多个公告；是跨源站归并后的业务主视角。
 * 对应表 {@code project}，由采集侧 {@code NoticeIngestionService} 与 P0-3 回填脚本维护。
 */
@TableName("project")
@KeySequence("project_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ProjectDO extends BaseDO {

    @TableId
    private Long id;
    /**
     * 本地归并键（规则生成，唯一）
     */
    private String projectKey;
    /**
     * 统一交易标识码
     */
    private String unifiedDealCode;
    /**
     * 项目编号
     */
    private String projectNo;
    /**
     * 项目名称
     */
    private String projectName;
    /**
     * 交易分类代码
     */
    private String tradeCategoryCode;
    /**
     * 项目类别代码
     */
    private String projectCategoryCode;
    /**
     * 行政区划代码
     */
    private String regionCode;
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
     * 预算（万元）
     */
    private BigDecimal budget;
    /**
     * 资金来源代码
     */
    private String fundingSourceCode;
    /**
     * 招标人企业 ID
     */
    private Long tendererEnterpriseId;
    /**
     * 代理机构企业 ID
     */
    private Long agencyEnterpriseId;
    /**
     * 状态：ACTIVE / CLOSED / CANCELLED
     */
    private String status;
    /**
     * 首次公告时间
     */
    private LocalDateTime firstPublishTime;
    /**
     * 最近公告时间
     */
    private LocalDateTime lastPublishTime;
    /**
     * 公告数（冗余统计）
     */
    private Integer noticeCount;
    /**
     * 标段数（冗余统计）
     */
    private Integer lotCount;
    /**
     * 来源站点数（冗余统计）
     */
    private Integer sourceCount;
    /**
     * 缺失字段汇总（JSON）
     */
    private String missingFields;
    /**
     * 数据质量状态：OK / PARTIAL / BROKEN / UNKNOWN
     */
    private String dataQualityStatus;
}
