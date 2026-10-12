package com.tenderintelligence.module.notice.api.notice;

import com.tenderintelligence.framework.common.pojo.PageResult;
import com.tenderintelligence.module.notice.api.notice.dto.NoticeDTO;
import com.tenderintelligence.module.notice.api.notice.dto.NoticeQueryDTO;
import com.tenderintelligence.module.notice.api.notice.dto.NoticeRespDTO;
import com.tenderintelligence.module.notice.api.notice.dto.NoticeUpsertRespDTO;

/**
 * 招投标公告 API 接口
 *
 * <p><b>契约状态：第 1 周末冻结，冻结后只加不改。</b>
 * 变更必须同步乙、丁、戊，并登记到 doc/毕设 §5 接口契约表。
 *
 * <p>⚠️ 本接口<b>只定签名、暂不提供实现</b>（插槽阶段）。消费方先在自己模块里写 mock 实现，
 * 第 4 周由丙提供真实现。
 *
 * @author Tender Intelligence
 */
public interface NoticeApi {

    // ==================== 读：给丁（M4）、戊（M5） ====================

    /**
     * 按主键查询公告（含附件）
     *
     * @param id 公告编号
     * @return 公告；不存在返回 {@code null}
     */
    NoticeRespDTO getNotice(Long id);

    /**
     * 按源站 URL 查询公告。源站 URL 是公告的唯一去重键
     *
     * @param sourceUrl 源站详情 URL
     * @return 公告；不存在返回 {@code null}
     */
    NoticeRespDTO getNoticeBySourceUrl(String sourceUrl);

    /**
     * 分页查询公告
     *
     * <p>给丁的检索召回、戊的批量打标用——他们直接读库，不走 HTTP。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    PageResult<NoticeRespDTO> getNoticePage(NoticeQueryDTO query);

    // ==================== 写：给乙（M2） ====================

    /**
     * 按 {@code sourceUrl} 去重入库：不存在则新增，存在且关键字段有变则更新，无变化则不动
     *
     * <p>这是乙的 {@code NoticeUpsertService} 的接口化形式。语义必须与现状保持一致：
     * <ul>
     *   <li>去重键是 {@code sourceUrl}</li>
     *   <li>变更比对只覆盖会被源站改写的字段（标题 / 截止 / 预算 / 正文 / 省市 / 行业），
     *       以及附件整组（URL / 文件名 / 大小）</li>
     *   <li>附件<b>整组重写</b>（先删后写），这样源站删掉的附件不会在我们的库里残留</li>
     * </ul>
     *
     * @param notice 公告（必须已设置 {@code sourceUrl}）
     * @return 公告主键 + 本次是新增还是更新（供乙统计 newInsert / updated）
     */
    NoticeUpsertRespDTO upsert(NoticeDTO notice);

}
