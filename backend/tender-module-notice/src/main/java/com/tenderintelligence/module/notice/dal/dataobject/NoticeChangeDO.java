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
 * 公告澄清修改
 *
 * <p>对应表 {@code notice_change}。
 */
@TableName("notice_change")
@KeySequence("notice_change_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class NoticeChangeDO extends BaseDO {

    @TableId
    private Long id;
    /**
     * 原公告 ID
     */
    private Long noticeId;
    /**
     * 变更类型代码：澄清 / 修改 / 终止
     */
    private String changeTypeCode;
    /**
     * 变更标题
     */
    private String changeTitle;
    /**
     * 变更内容
     */
    private String changeContent;
    /**
     * 变更时间
     */
    private LocalDateTime changeTime;
    /**
     * 变更公告 URL
     */
    private String sourceUrl;
    /**
     * 原始快照 ID
     */
    private Long rawId;
}
