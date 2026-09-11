package com.tenderintelligence.module.crawler.service;

import com.tenderintelligence.module.notice.dal.dataobject.NoticePortalDO;
import com.tenderintelligence.module.notice.dal.mysql.NoticePortalMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 标讯入库去重: 各 SourceAdapter 共用的持久化终点
 *
 * 去重依据 {@code notice.uk_source_url} 唯一键:
 * 1. 新记录 INSERT, {@link CrawlStats#newInsert} +1
 * 2. 已存在且关键字段有变则 UPDATE, {@link CrawlStats#updated} +1
 * 3. 已存在且无变更则不动(避免无效写入)
 *
 * 变更比对只覆盖会被源站改写的字段(title/deadline/budget/content/province/city/industry);
 * 其余字段仅在判定为"有变更"时一并回写, 作为顺带刷新。
 *
 * 接入新源时无需再复制本逻辑 —— 这是它从两个适配器中上提到此的原因。
 */
@Component
public class NoticeUpsertService {

    @Resource
    private NoticePortalMapper noticePortalMapper;

    /**
     * 按 sourceUrl 去重入库
     *
     * @param notice 适配器解析出的标讯(须已设置 sourceUrl)
     * @param stats  采集统计, 本方法会就地累加 newInsert / updated
     */
    public void upsert(NoticePortalDO notice, CrawlStats stats) {
        NoticePortalDO existing = noticePortalMapper.selectBySourceUrl(notice.getSourceUrl());
        if (existing == null) {
            noticePortalMapper.insert(notice);
            stats.setNewInsert(stats.getNewInsert() + 1);
            return;
        }
        if (!isChanged(existing, notice)) {
            return;
        }
        applyChanges(existing, notice);
        noticePortalMapper.updateById(existing);
        stats.setUpdated(stats.getUpdated() + 1);
    }

    /** 关键字段是否发生变更(决定是否需要 UPDATE) */
    private boolean isChanged(NoticePortalDO existing, NoticePortalDO incoming) {
        return !equalsSafe(existing.getTitle(), incoming.getTitle())
                || !equalsSafe(existing.getDeadline(), incoming.getDeadline())
                || !equalsSafe(existing.getBudget(), incoming.getBudget())
                || !equalsSafe(existing.getContent(), incoming.getContent())
                || !equalsSafe(existing.getProvince(), incoming.getProvince())
                || !equalsSafe(existing.getCity(), incoming.getCity())
                || !equalsSafe(existing.getIndustry(), incoming.getIndustry());
    }

    /** 将源站最新字段回写到已存在的记录 */
    private void applyChanges(NoticePortalDO existing, NoticePortalDO incoming) {
        existing.setTitle(incoming.getTitle());
        existing.setContent(incoming.getContent());
        existing.setDeadline(incoming.getDeadline());
        existing.setOpenTime(incoming.getOpenTime());
        existing.setBudget(incoming.getBudget());
        existing.setTenderPerson(incoming.getTenderPerson());
        existing.setAgency(incoming.getAgency());
        existing.setContact(incoming.getContact());
        existing.setContactPhone(incoming.getContactPhone());
        existing.setProjectNo(incoming.getProjectNo());
        existing.setPublishTime(incoming.getPublishTime());
        existing.setProvince(incoming.getProvince());
        existing.setCity(incoming.getCity());
        existing.setIndustry(incoming.getIndustry());
    }

    private boolean equalsSafe(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }
}
