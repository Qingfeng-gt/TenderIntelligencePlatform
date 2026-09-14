package com.tenderintelligence.module.crawler.service;

import com.tenderintelligence.module.notice.dal.dataobject.NoticeAttachmentDO;
import com.tenderintelligence.module.notice.dal.dataobject.NoticePortalDO;
import com.tenderintelligence.module.notice.dal.mysql.NoticeAttachmentMapper;
import com.tenderintelligence.module.notice.dal.mysql.NoticePortalMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

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
 * 附件({@code notice_attachment})随公告一起落库, 整组重写(见 {@link #saveAttachments})。
 *
 * 接入新源时无需再复制本逻辑 —— 这是它从两个适配器中上提到此的原因。
 */
@Component
public class NoticeUpsertService {

    @Resource
    private NoticePortalMapper noticePortalMapper;
    @Resource
    private NoticeAttachmentMapper noticeAttachmentMapper;

    /**
     * 按 sourceUrl 去重入库
     *
     * @param notice 适配器解析出的标讯(须已设置 sourceUrl, 附件在 {@code notice.attachments})
     * @param stats  采集统计, 本方法会就地累加 newInsert / updated
     */
    public void upsert(NoticePortalDO notice, CrawlStats stats) {
        NoticePortalDO existing = noticePortalMapper.selectBySourceUrl(notice.getSourceUrl());
        if (existing == null) {
            noticePortalMapper.insert(notice);
            saveAttachments(notice.getId(), notice.getAttachments());
            stats.setNewInsert(stats.getNewInsert() + 1);
            return;
        }
        if (!isChanged(existing, notice)) {
            return;
        }
        applyChanges(existing, notice);
        noticePortalMapper.updateById(existing);
        saveAttachments(existing.getId(), notice.getAttachments());
        stats.setUpdated(stats.getUpdated() + 1);
    }

    /** 关键字段是否发生变更(决定是否需要 UPDATE) */
    private boolean isChanged(NoticePortalDO existing, NoticePortalDO incoming) {
        if (!equalsSafe(existing.getTitle(), incoming.getTitle())
                || !equalsSafe(existing.getDeadline(), incoming.getDeadline())
                || !equalsSafe(existing.getBudget(), incoming.getBudget())
                || !equalsSafe(existing.getContent(), incoming.getContent())
                || !equalsSafe(existing.getProvince(), incoming.getProvince())
                || !equalsSafe(existing.getCity(), incoming.getCity())
                || !equalsSafe(existing.getIndustry(), incoming.getIndustry())) {
            return true;
        }
        // 上面全一致时附件仍可能单独变: ztb_gz 的附件取自详情 JSON 的 UploadFile/PdfFile,
        // 并不在 content 里 —— 源站只换附件不换正文时, 只比上面几项会永远发现不了。
        // 放在短路的最后一步, 正文有变时就不必再查一次附件表。
        return attachmentsChanged(existing.getId(), incoming.getAttachments());
    }

    /** 附件是否变化(比 URL/文件名/大小: 三者都会影响展示) */
    private boolean attachmentsChanged(Long noticeId, List<NoticeAttachmentDO> incoming) {
        List<NoticeAttachmentDO> existing = noticeAttachmentMapper.selectListByNoticeId(noticeId);
        List<NoticeAttachmentDO> fresh = incoming == null ? List.of() : incoming;
        if (existing.size() != fresh.size()) {
            return true;
        }
        for (int i = 0; i < existing.size(); i++) {
            NoticeAttachmentDO before = existing.get(i);
            NoticeAttachmentDO after = fresh.get(i);
            if (!equalsSafe(before.getFileUrl(), after.getFileUrl())
                    || !equalsSafe(before.getFileName(), after.getFileName())
                    || !equalsSafe(before.getFileSize(), after.getFileSize())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 整组重写某公告的附件
     *
     * 先删后写而非逐条 diff: 附件没有天然唯一键(同一 URL 在源站可被重复列出, 同名不同 URL 也常见),
     * 且单轮内同一公告只会写一次(采集按站点串行), 不存在并发写同一公告的情况。
     * 这样源站删掉的附件不会在我们库里残留 —— 逐条 upsert 做不到这点。
     */
    private void saveAttachments(Long noticeId, List<NoticeAttachmentDO> attachments) {
        noticeAttachmentMapper.deleteByNoticeId(noticeId);
        if (attachments == null || attachments.isEmpty()) {
            return;
        }
        for (NoticeAttachmentDO attachment : attachments) {
            // 解析产物不带主键; 显式清空, 避免调用方复用对象时把写入变成改名更新
            attachment.setId(null);
            attachment.setNoticeId(noticeId);
        }
        noticeAttachmentMapper.insertBatch(attachments);
    }

    /** 将源站最新字段回写到已存在的记录 */
    private void applyChanges(NoticePortalDO existing, NoticePortalDO incoming) {
        // 显式推进更新时间: 框架的 DefaultDBFieldHandler 只在 updateTime 为空时才填当前时间,
        // 而 existing 是从库里读出来的、带着旧的 updateTime, 直接 updateById 会把旧值原样写回
        // (UPDATE 语句显式赋值该列, 列上的 ON UPDATE CURRENT_TIMESTAMP 也就不会生效)。
        // 2026-09-13 实测: 75 条公告当天被重采更新, update_time 却仍停在 2026-09-11。
        existing.setUpdateTime(LocalDateTime.now());
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
