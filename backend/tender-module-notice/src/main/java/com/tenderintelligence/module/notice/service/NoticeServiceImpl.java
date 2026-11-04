package com.tenderintelligence.module.notice.service;

import com.tenderintelligence.framework.common.exception.util.ServiceExceptionUtil;
import com.tenderintelligence.framework.common.pojo.PageResult;
import com.tenderintelligence.framework.common.util.object.BeanUtils;
import com.tenderintelligence.module.notice.controller.admin.notice.vo.NoticePageReqVO;
import com.tenderintelligence.module.notice.controller.admin.notice.vo.NoticeRespVO;
import com.tenderintelligence.module.notice.controller.admin.notice.vo.NoticeStatsRespVO;
import com.tenderintelligence.module.notice.controller.admin.notice.vo.RegionRespVO;
import com.tenderintelligence.module.notice.dal.dataobject.NoticePortalDO;
import com.tenderintelligence.module.notice.dal.mysql.BaseAreaMapper;
import com.tenderintelligence.module.notice.dal.mysql.NoticePortalMapper;
import com.tenderintelligence.module.notice.dal.mysql.ProjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

import static com.tenderintelligence.module.notice.enums.ErrorCodeConstants.NOTICE_NOT_EXISTS;

/**
 * 招投标公告 Service 实现（四层模型读路径）
 */
@Service("noticePortalService")
@Slf4j
public class NoticeServiceImpl implements NoticeService {

    @Resource
    private NoticePortalMapper noticePortalMapper;
    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private BaseAreaMapper baseAreaMapper;
    @Resource
    private NoticeViewAssembler noticeViewAssembler;

    @Override
    public PageResult<NoticeRespVO> getNoticePage(NoticePageReqVO pageReqVO) {
        PageResult<NoticePortalDO> page = noticePortalMapper.selectPage(pageReqVO);
        // 列表不返回正文与附件：正文是大字段，附件逐条查询会产生 N+1
        List<NoticeRespVO> list = noticeViewAssembler.buildNoticeList(page.getList(), false);
        return new PageResult<>(list, page.getTotal());
    }

    @Override
    public NoticeRespVO getNotice(Long id) {
        NoticePortalDO notice = noticePortalMapper.selectById(id);
        if (notice == null) {
            throw ServiceExceptionUtil.exception(NOTICE_NOT_EXISTS);
        }
        return noticeViewAssembler.buildNoticeList(List.of(notice), false).get(0);
    }

    @Override
    public NoticeRespVO getNoticeDetail(Long id) {
        NoticePortalDO notice = noticePortalMapper.selectById(id);
        if (notice == null) {
            throw ServiceExceptionUtil.exception(NOTICE_NOT_EXISTS);
        }
        NoticeRespVO vo = noticeViewAssembler.buildNoticeList(List.of(notice), true).get(0);
        noticeViewAssembler.fillDetail(vo);
        return vo;
    }

    @Override
    public NoticeStatsRespVO getStats() {
        NoticeStatsRespVO stats = new NoticeStatsRespVO();
        stats.setTodayCount(noticePortalMapper.selectCountByPublishTime(LocalDate.now().atStartOfDay()));
        stats.setTotalCount(noticePortalMapper.selectTotalCount());
        Long provinceCount = noticePortalMapper.selectProvinceCodeCount();
        if (provinceCount == null || provinceCount == 0) {
            provinceCount = noticePortalMapper.selectProvinceCount();
        }
        stats.setProvinces(provinceCount);
        stats.setIndustries(noticePortalMapper.selectIndustryCount());
        stats.setProjects(projectMapper.selectTotalCount());
        return stats;
    }

    @Override
    public List<RegionRespVO> getProvinces() {
        return BeanUtils.toBean(baseAreaMapper.selectProvinces(), RegionRespVO.class);
    }

    @Override
    public List<String> getIndustries() {
        return noticePortalMapper.selectDistinctIndustries();
    }
}
