package com.tenderintelligence.module.crawler.service;

import com.tenderintelligence.module.notice.dal.dataobject.NoticeAttachmentDO;
import com.tenderintelligence.module.notice.dal.dataobject.NoticePortalDO;
import com.tenderintelligence.module.notice.dal.mysql.NoticeAttachmentMapper;
import com.tenderintelligence.module.notice.dal.mysql.NoticePortalMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 入库去重与附件同步({@link NoticeUpsertService})测试
 *
 * 用 mock 的 Mapper 覆盖三条分支(新增 / 有变更 / 无变更), 以及容易被忽略的两点:
 * 附件变更也要触发更新、更新时必须显式推进 {@code updateTime}。
 */
class NoticeUpsertServiceTest {

    private static final String SOURCE_URL = "http://www.ccgp.gov.cn/cggg/dfgg/gkzb/202609/t20260913_1.htm";

    private NoticeUpsertService service;
    private NoticePortalMapper noticePortalMapper;
    private NoticeAttachmentMapper noticeAttachmentMapper;

    @BeforeEach
    void setUp() {
        service = new NoticeUpsertService();
        noticePortalMapper = mock(NoticePortalMapper.class);
        noticeAttachmentMapper = mock(NoticeAttachmentMapper.class);
        ReflectionTestUtils.setField(service, "noticePortalMapper", noticePortalMapper);
        ReflectionTestUtils.setField(service, "noticeAttachmentMapper", noticeAttachmentMapper);
    }

    @Test
    @DisplayName("新增: 写公告并把附件挂到回填的主键上")
    void insertsNoticeAndAttachments() {
        when(noticePortalMapper.selectBySourceUrl(SOURCE_URL)).thenReturn(null);
        // 模拟 MyBatis-Plus 回填自增主键
        when(noticePortalMapper.insert(any(NoticePortalDO.class))).thenAnswer(inv -> {
            inv.getArgument(0, NoticePortalDO.class).setId(888L);
            return 1;
        });

        NoticePortalDO notice = notice(SOURCE_URL, "标题");
        notice.setAttachments(List.of(attachment("https://x.gov.cn/a.pdf")));
        CrawlStats stats = new CrawlStats();
        service.upsert(notice, stats);

        assertThat(stats.getNewInsert()).isEqualTo(1);
        assertThat(stats.getUpdated()).isZero();
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<NoticeAttachmentDO>> captor = ArgumentCaptor.forClass(List.class);
        verify(noticeAttachmentMapper).insertBatch(captor.capture());
        assertThat(captor.getValue()).singleElement()
                .satisfies(a -> {
                    assertThat(a.getNoticeId()).isEqualTo(888L);
                    assertThat(a.getId()).isNull(); // 解析产物没有主键, 不能把插入变成更新
                });
    }

    @Test
    @DisplayName("无变更: 不写公告、不碰附件表")
    void skipsWhenNothingChanged() {
        NoticePortalDO existing = notice(SOURCE_URL, "标题");
        existing.setId(1L);
        when(noticePortalMapper.selectBySourceUrl(SOURCE_URL)).thenReturn(existing);
        when(noticeAttachmentMapper.selectListByNoticeId(1L)).thenReturn(List.of());

        service.upsert(notice(SOURCE_URL, "标题"), new CrawlStats());

        verify(noticePortalMapper, never()).updateById(any(NoticePortalDO.class));
        verify(noticeAttachmentMapper, never()).deleteByNoticeId(anyLong());
    }

    @Test
    @DisplayName("正文有变: 更新公告 + 整组重写附件 + 推进 updateTime")
    void updatesAndRewritesAttachments() {
        NoticePortalDO existing = notice(SOURCE_URL, "旧标题");
        existing.setId(1L);
        existing.setUpdateTime(LocalDateTime.of(2026, 9, 11, 15, 32));
        when(noticePortalMapper.selectBySourceUrl(SOURCE_URL)).thenReturn(existing);

        NoticePortalDO incoming = notice(SOURCE_URL, "新标题");
        incoming.setAttachments(List.of(attachment("https://x.gov.cn/b.pdf")));
        CrawlStats stats = new CrawlStats();
        service.upsert(incoming, stats);

        assertThat(stats.getUpdated()).isEqualTo(1);
        // 更新时间必须往前走: 框架只在 updateTime 为空时填当前时间, 带旧值更新会把旧值写回库
        ArgumentCaptor<NoticePortalDO> captor = ArgumentCaptor.forClass(NoticePortalDO.class);
        verify(noticePortalMapper).updateById(captor.capture());
        assertThat(captor.getValue().getUpdateTime()).isAfter(LocalDateTime.of(2026, 9, 11, 15, 32));
        verify(noticeAttachmentMapper).deleteByNoticeId(1L);
        verify(noticeAttachmentMapper).insertBatch(any());
    }

    @Test
    @DisplayName("只有附件变(正文一字未动)也要更新 —— ztb_gz 的附件来自详情 JSON, 不在 content 里")
    void updatesWhenOnlyAttachmentsChanged() {
        NoticePortalDO existing = notice(SOURCE_URL, "标题");
        existing.setId(1L);
        existing.setUpdateTime(LocalDateTime.of(2026, 9, 11, 15, 32));
        when(noticePortalMapper.selectBySourceUrl(SOURCE_URL)).thenReturn(existing);
        // 库里存的是旧附件
        when(noticeAttachmentMapper.selectListByNoticeId(1L))
                .thenReturn(List.of(attachment("https://x.gov.cn/old.pdf")));

        NoticePortalDO incoming = notice(SOURCE_URL, "标题"); // 标量字段全一致
        incoming.setAttachments(List.of(attachment("https://x.gov.cn/new.pdf")));
        CrawlStats stats = new CrawlStats();
        service.upsert(incoming, stats);

        assertThat(stats.getUpdated()).isEqualTo(1);
        verify(noticeAttachmentMapper).deleteByNoticeId(1L);
        verify(noticeAttachmentMapper).insertBatch(any());
    }

    @Test
    @DisplayName("附件被源站删光时, 库里的附件也要清掉")
    void clearsAttachmentsWhenSourceRemovedThem() {
        NoticePortalDO existing = notice(SOURCE_URL, "旧标题");
        existing.setId(1L);
        when(noticePortalMapper.selectBySourceUrl(SOURCE_URL)).thenReturn(existing);

        NoticePortalDO incoming = notice(SOURCE_URL, "新标题");
        incoming.setAttachments(List.of());
        CrawlStats stats = new CrawlStats();
        service.upsert(incoming, stats);

        verify(noticeAttachmentMapper).deleteByNoticeId(1L);
        verify(noticeAttachmentMapper, never()).insertBatch(any());
    }

    private NoticePortalDO notice(String sourceUrl, String title) {
        NoticePortalDO notice = new NoticePortalDO();
        notice.setSourceUrl(sourceUrl);
        notice.setTitle(title);
        notice.setContent("<p>正文</p>");
        notice.setProvince("贵州");
        notice.setCity("贵阳市");
        notice.setIndustry("软件服务");
        return notice;
    }

    private NoticeAttachmentDO attachment(String url) {
        NoticeAttachmentDO attachment = new NoticeAttachmentDO();
        attachment.setFileName("文件.pdf");
        attachment.setFileUrl(url);
        attachment.setFileType("pdf");
        attachment.setFileSize("");
        attachment.setSort(0);
        return attachment;
    }

}
