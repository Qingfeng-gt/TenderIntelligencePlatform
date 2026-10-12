package com.tenderintelligence.module.notice.api.notice.dto;

import com.tenderintelligence.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 公告查询条件 DTO
 *
 * <p>给丁（M4 检索召回）、戊（M5 批量打标）直接读库用，不走 HTTP。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class NoticeQueryDTO extends PageParam {

    /**
     * 关键词（当前实现是标题模糊匹配）
     */
    private String keyword;

    /**
     * 公告类型：tender 招标 / win 中标 / change 变更 / explore 采购
     */
    private String type;

    /**
     * 省份
     */
    private String province;

    /**
     * 行业分类
     *
     * <p>⚠️ 口径见 doc/毕设 §6 第 8 项：这个字段是 crawler 用关键词词典推导的<b>规则抽取结果</b>，
     * 不是权威标签。权威标签源是戊的 {@code tag} 表，多维筛选请改用 {@link #tagIds}。
     */
    private String industry;

    /**
     * 地区（省或市，模糊匹配）
     */
    private String region;

    /**
     * 预算上限（万元）
     */
    private BigDecimal budgetMax;

    /**
     * 标签筛选（戊 M5 的标签联动）
     *
     * <p>⚠️ 已于第 1 周末随本接口一起冻结签名，但<b>实际过滤要等第 5 周戊的 tag 表落地后才生效</b>；
     * 在那之前传了会被忽略（而不是报错）。
     */
    private List<Long> tagIds;

    /**
     * 发布时间 ≥（含）
     */
    private LocalDateTime publishTimeGreaterThan;

    /**
     * 发布时间 &lt;（不含）
     */
    private LocalDateTime publishTimeLessThan;

}
