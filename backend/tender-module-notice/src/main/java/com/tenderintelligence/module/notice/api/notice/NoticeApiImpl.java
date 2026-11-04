package com.tenderintelligence.module.notice.api.notice;

import com.tenderintelligence.framework.common.pojo.PageResult;
import com.tenderintelligence.framework.common.util.object.BeanUtils;
import com.tenderintelligence.module.notice.api.notice.dto.NoticeAttachmentRespDTO;
import com.tenderintelligence.module.notice.api.notice.dto.NoticeDTO;
import com.tenderintelligence.module.notice.api.notice.dto.NoticeQueryDTO;
import com.tenderintelligence.module.notice.api.notice.dto.NoticeRespDTO;
import com.tenderintelligence.module.notice.api.notice.dto.NoticeUpsertRespDTO;
import com.tenderintelligence.module.notice.controller.admin.notice.vo.NoticePageReqVO;
import com.tenderintelligence.module.notice.controller.admin.notice.vo.NoticeRespVO;
import com.tenderintelligence.module.notice.dal.dataobject.NoticePortalDO;
import com.tenderintelligence.module.notice.dal.mysql.NoticePortalMapper;
import com.tenderintelligence.module.notice.service.NoticeIngestionService;
import com.tenderintelligence.module.notice.service.NoticeService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 公告 API 实现
 *
 * <p>读：走四层模型组装后的 {@link NoticeService}；
 * 写：走新的 {@link NoticeIngestionService}（notice + project + tender_project + tender_lot + enterprise + 治理表）。
 */
@Service
public class NoticeApiImpl implements NoticeApi {

    @Resource
    private NoticeService noticeService;
    @Resource
    private NoticeIngestionService noticeIngestionService;
    @Resource
    private NoticePortalMapper noticePortalMapper;

    @Override
    public NoticeRespDTO getNotice(Long id) {
        if (id == null) {
            return null;
        }
        NoticePortalDO notice = noticePortalMapper.selectById(id);
        if (notice == null) {
            return null;
        }
        return toRespDTO(noticeService.getNoticeDetail(id));
    }

    @Override
    public NoticeRespDTO getNoticeBySourceUrl(String sourceUrl) {
        if (sourceUrl == null || sourceUrl.isBlank()) {
            return null;
        }
        NoticePortalDO notice = noticePortalMapper.selectBySourceUrl(sourceUrl);
        return notice == null ? null : toRespDTO(noticeService.getNoticeDetail(notice.getId()));
    }

    @Override
    public PageResult<NoticeRespDTO> getNoticePage(NoticeQueryDTO query) {
        NoticePageReqVO reqVO = new NoticePageReqVO();
        reqVO.setPageNo(query.getPageNo());
        reqVO.setPageSize(query.getPageSize());
        reqVO.setKeyword(query.getKeyword());
        reqVO.setNoticeTypeCode(query.getType());
        reqVO.setProvinceCode(query.getProvinceCode());
        reqVO.setCityCode(query.getCityCode());
        reqVO.setIndustry(query.getIndustry());
        reqVO.setBudgetMin(query.getBudgetMin());
        reqVO.setBudgetMax(query.getBudgetMax());
        reqVO.setPublishTimeGreaterThan(query.getPublishTimeGreaterThan());
        reqVO.setPublishTimeLessThan(query.getPublishTimeLessThan());
        PageResult<NoticeRespVO> page = noticeService.getNoticePage(reqVO);
        List<NoticeRespDTO> list = page.getList().stream().map(this::toRespDTO).toList();
        return new PageResult<>(list, page.getTotal());
    }

    @Override
    public NoticeUpsertRespDTO upsert(NoticeDTO notice) {
        return noticeIngestionService.upsert(notice);
    }

    private NoticeRespDTO toRespDTO(NoticeRespVO vo) {
        NoticeRespDTO dto = BeanUtils.toBean(vo, NoticeRespDTO.class);
        dto.setType(vo.getNoticeTypeCode());
        dto.setProvince(vo.getProvinceName());
        dto.setCity(vo.getCityName());
        dto.setTenderPerson(vo.getTendererName());
        dto.setAgency(vo.getAgencyName());
        dto.setDeadline(vo.getBidDeadline());
        dto.setSource(vo.getSourcePlatform());
        dto.setAttachments(BeanUtils.toBean(vo.getAttachments(), NoticeAttachmentRespDTO.class));
        return dto;
    }
}
