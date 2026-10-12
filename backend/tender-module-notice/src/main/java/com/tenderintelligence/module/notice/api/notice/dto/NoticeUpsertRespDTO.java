package com.tenderintelligence.module.notice.api.notice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 公告入库结果 DTO
 *
 * <p>乙采集时按站点累计 {@code newInsert} 与 {@code updated} 两个统计值
 * （见现状的 {@code CrawlStats}），所以 {@code upsert(..)} 必须把「这次是新增还是更新」回传，
 * 否则统计口径会变。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NoticeUpsertRespDTO {

    /**
     * 公告主键
     */
    private Long id;

    /**
     * true = 本次是新增；false = 已存在且被更新，或已存在且无变化（看 {@link #updated}）
     */
    private Boolean created;

    /**
     * true = 已存在且关键字段（或附件）有变更、执行了 UPDATE
     *
     * <p>{@code created=false && updated=false} 表示命中了已有记录但内容无变化、未产生写入
     */
    private Boolean updated;

}
