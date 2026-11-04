package com.tenderintelligence.module.notice.service;

import com.tenderintelligence.framework.common.exception.util.ServiceExceptionUtil;
import com.tenderintelligence.framework.common.pojo.PageResult;
import com.tenderintelligence.framework.common.util.object.BeanUtils;
import com.tenderintelligence.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.tenderintelligence.module.notice.controller.admin.notice.vo.ProjectDetailRespVO;
import com.tenderintelligence.module.notice.controller.admin.notice.vo.ProjectPageReqVO;
import com.tenderintelligence.module.notice.controller.admin.notice.vo.ProjectRespVO;
import com.tenderintelligence.module.notice.controller.admin.notice.vo.TenderLotRespVO;
import com.tenderintelligence.module.notice.controller.admin.notice.vo.TenderProjectRespVO;
import com.tenderintelligence.module.notice.dal.dataobject.BaseAreaDO;
import com.tenderintelligence.module.notice.dal.dataobject.EnterpriseDO;
import com.tenderintelligence.module.notice.dal.dataobject.NoticeEnterpriseDO;
import com.tenderintelligence.module.notice.dal.dataobject.NoticePortalDO;
import com.tenderintelligence.module.notice.dal.dataobject.ProjectDO;
import com.tenderintelligence.module.notice.dal.dataobject.TenderLotDO;
import com.tenderintelligence.module.notice.dal.dataobject.TenderProjectDO;
import com.tenderintelligence.module.notice.dal.mysql.NoticeEnterpriseMapper;
import com.tenderintelligence.module.notice.dal.mysql.NoticePortalMapper;
import com.tenderintelligence.module.notice.dal.mysql.ProjectMapper;
import com.tenderintelligence.module.notice.dal.mysql.TenderLotMapper;
import com.tenderintelligence.module.notice.dal.mysql.TenderProjectMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.tenderintelligence.module.notice.enums.ErrorCodeConstants.PROJECT_NOT_EXISTS;

/**
 * 项目（project-centric）读服务实现
 */
@Service
public class ProjectServiceImpl implements ProjectService {

    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private NoticePortalMapper noticePortalMapper;
    @Resource
    private TenderProjectMapper tenderProjectMapper;
    @Resource
    private TenderLotMapper tenderLotMapper;
    @Resource
    private NoticeEnterpriseMapper noticeEnterpriseMapper;
    @Resource
    private NoticeViewAssembler noticeViewAssembler;

    @Override
    public PageResult<ProjectRespVO> getProjectPage(ProjectPageReqVO pageReqVO) {
        LambdaQueryWrapperX<ProjectDO> wrapper = new LambdaQueryWrapperX<ProjectDO>()
                .eqIfPresent(ProjectDO::getProvinceCode, pageReqVO.getProvinceCode())
                .eqIfPresent(ProjectDO::getCityCode, pageReqVO.getCityCode())
                .betweenIfPresent(ProjectDO::getBudget, pageReqVO.getBudgetMin(), pageReqVO.getBudgetMax())
                .geIfPresent(ProjectDO::getLastPublishTime, pageReqVO.getPublishTimeGreaterThan())
                .ltIfPresent(ProjectDO::getLastPublishTime, pageReqVO.getPublishTimeLessThan())
                .orderByDesc(ProjectDO::getLastPublishTime)
                .orderByDesc(ProjectDO::getId);
        if (pageReqVO.getKeyword() != null && !pageReqVO.getKeyword().isBlank()) {
            String keyword = pageReqVO.getKeyword().trim();
            wrapper.and(w -> w.like(ProjectDO::getProjectName, keyword)
                    .or().like(ProjectDO::getProjectNo, keyword));
        }
        if (pageReqVO.getNoticeTypeCode() != null && !pageReqVO.getNoticeTypeCode().isBlank()) {
            wrapper.apply("id IN (SELECT project_id FROM notice WHERE deleted = 0 AND notice_type_code = {0})",
                    pageReqVO.getNoticeTypeCode());
        }
        if (pageReqVO.getIndustry() != null && !pageReqVO.getIndustry().isBlank()) {
            wrapper.apply("id IN (SELECT project_id FROM notice WHERE deleted = 0 AND industry = {0})",
                    pageReqVO.getIndustry());
        }

        PageResult<ProjectDO> page = projectMapper.selectPage(pageReqVO, wrapper);
        List<ProjectRespVO> list = buildProjectList(page.getList(), pageReqVO.getNoticeTypeCode());
        return new PageResult<>(list, page.getTotal());
    }

    @Override
    public ProjectDetailRespVO getProjectDetail(Long id) {
        ProjectDO project = projectMapper.selectById(id);
        if (project == null) {
            throw ServiceExceptionUtil.exception(PROJECT_NOT_EXISTS);
        }
        List<NoticePortalDO> notices = noticePortalMapper.selectListByProjectId(id);
        NoticePortalDO latest = notices.isEmpty() ? null : notices.get(0);

        ProjectDetailRespVO vo = BeanUtils.toBean(project, ProjectDetailRespVO.class);
        Map<Long, EnterpriseDO> enterpriseMap = enterpriseMapOfProjects(List.of(project));
        Map<String, BaseAreaDO> areaMap = noticeViewAssembler.areaMap(
                areaCodesOfProjects(List.of(project)));
        Map<String, String> tradeCategoryNames = noticeViewAssembler.codeNameMap("trade_category");
        Map<String, String> noticeTypeNames = noticeViewAssembler.codeNameMap("notice_type");
        fillProjectFields(vo, project, latest, enterpriseMap, areaMap, tradeCategoryNames, noticeTypeNames);

        List<TenderProjectDO> tenderProjects = tenderProjectMapper.selectListByProjectId(id);
        vo.setTenderProjects(buildTenderProjectList(tenderProjects, enterpriseMap));
        List<TenderLotDO> lots = tenderLotMapper.selectListByProjectId(id);
        vo.setLots(buildTenderLotList(lots));
        vo.setNotices(noticeViewAssembler.buildNoticeList(notices, false));

        Set<Long> noticeIds = new HashSet<>();
        notices.forEach(n -> noticeIds.add(n.getId()));
        List<NoticeEnterpriseDO> links = noticeEnterpriseMapper.selectListByNoticeIds(noticeIds);
        vo.setEnterprises(noticeViewAssembler.buildEnterpriseList(links,
                project.getTendererEnterpriseId(), noticeViewAssembler.enterpriseName(enterpriseMap, project.getTendererEnterpriseId()),
                project.getAgencyEnterpriseId(), noticeViewAssembler.enterpriseName(enterpriseMap, project.getAgencyEnterpriseId()),
                null, null));
        return vo;
    }

    private List<ProjectRespVO> buildProjectList(List<ProjectDO> projects, String noticeTypeCode) {
        if (projects.isEmpty()) {
            return List.of();
        }
        List<Long> projectIds = projects.stream().map(ProjectDO::getId).toList();
        List<NoticePortalDO> notices = noticePortalMapper.selectListByProjectIds(projectIds);
        Map<Long, List<NoticePortalDO>> noticesByProject = new HashMap<>();
        for (NoticePortalDO notice : notices) {
            if (notice.getProjectId() == null) {
                continue;
            }
            if (noticeTypeCode != null && !noticeTypeCode.isBlank()
                    && !noticeTypeCode.equals(notice.getNoticeTypeCode())) {
                continue;
            }
            noticesByProject.computeIfAbsent(notice.getProjectId(), k -> new ArrayList<>()).add(notice);
        }
        Map<Long, EnterpriseDO> enterpriseMap = enterpriseMapOfProjects(projects);
        Map<String, BaseAreaDO> areaMap = noticeViewAssembler.areaMap(areaCodesOfProjects(projects));
        Map<String, String> tradeCategoryNames = noticeViewAssembler.codeNameMap("trade_category");
        Map<String, String> noticeTypeNames = noticeViewAssembler.codeNameMap("notice_type");

        List<ProjectRespVO> result = new ArrayList<>(projects.size());
        for (ProjectDO project : projects) {
            List<NoticePortalDO> projectNotices = noticesByProject.getOrDefault(project.getId(), List.of());
            NoticePortalDO latest = projectNotices.isEmpty() ? null : projectNotices.get(0);
            ProjectRespVO vo = new ProjectRespVO();
            fillProjectFields(vo, project, latest, enterpriseMap, areaMap, tradeCategoryNames, noticeTypeNames);
            result.add(vo);
        }
        return result;
    }

    private void fillProjectFields(ProjectRespVO vo, ProjectDO project, NoticePortalDO latest,
                                   Map<Long, EnterpriseDO> enterpriseMap,
                                   Map<String, BaseAreaDO> areaMap,
                                   Map<String, String> tradeCategoryNames,
                                   Map<String, String> noticeTypeNames) {
        if (vo.getId() == null) {
            vo.setId(project.getId());
            vo.setProjectKey(project.getProjectKey());
            vo.setUnifiedDealCode(project.getUnifiedDealCode());
            vo.setProjectNo(project.getProjectNo());
            vo.setProjectName(project.getProjectName());
            vo.setTradeCategoryCode(project.getTradeCategoryCode());
            vo.setProvinceCode(project.getProvinceCode());
            vo.setCityCode(project.getCityCode());
            vo.setBudget(project.getBudget());
            vo.setTendererEnterpriseId(project.getTendererEnterpriseId());
            vo.setAgencyEnterpriseId(project.getAgencyEnterpriseId());
            vo.setStatus(project.getStatus());
            vo.setFirstPublishTime(project.getFirstPublishTime());
            vo.setLastPublishTime(project.getLastPublishTime());
            vo.setNoticeCount(project.getNoticeCount());
            vo.setLotCount(project.getLotCount());
            vo.setSourceCount(project.getSourceCount());
            vo.setDataQualityStatus(project.getDataQualityStatus());
        }
        vo.setProvinceName(NoticeViewAssembler.areaName(areaMap.get(project.getProvinceCode()), null));
        vo.setCityName(NoticeViewAssembler.areaName(areaMap.get(project.getCityCode()), null));
        vo.setTradeCategoryName(NoticeViewAssembler.tradeCategoryName(
                tradeCategoryNames, project.getTradeCategoryCode()));
        vo.setTendererName(noticeViewAssembler.enterpriseName(enterpriseMap, project.getTendererEnterpriseId()));
        vo.setAgencyName(noticeViewAssembler.enterpriseName(enterpriseMap, project.getAgencyEnterpriseId()));
        if (latest != null) {
            vo.setLatestNoticeId(latest.getId());
            vo.setLatestNoticeTypeCode(latest.getNoticeTypeCode());
            vo.setLatestNoticeTypeName(noticeTypeNames.getOrDefault(
                    latest.getNoticeTypeCode(), latest.getNoticeTypeCode()));
            vo.setLatestNoticeTitle(latest.getTitle());
            vo.setLatestNoticePublishTime(latest.getPublishTime());
        }
    }

    private List<TenderProjectRespVO> buildTenderProjectList(List<TenderProjectDO> tenderProjects,
                                                             Map<Long, EnterpriseDO> enterpriseMap) {
        List<TenderProjectRespVO> result = BeanUtils.toBean(tenderProjects, TenderProjectRespVO.class);
        Map<String, String> procurementNames = noticeViewAssembler.codeNameMap("procurement_method");
        Map<String, String> evaluationNames = noticeViewAssembler.codeNameMap("evaluation_method");
        for (int i = 0; i < tenderProjects.size(); i++) {
            TenderProjectDO source = tenderProjects.get(i);
            TenderProjectRespVO target = result.get(i);
            target.setProcurementMethodName(NoticeViewAssembler.procurementMethodName(
                    procurementNames, source.getProcurementMethodCode()));
            target.setEvaluationMethodName(NoticeViewAssembler.evaluationMethodName(
                    evaluationNames, source.getEvaluationMethodCode()));
            target.setTendererName(noticeViewAssembler.enterpriseName(enterpriseMap, source.getTendererEnterpriseId()));
            target.setAgencyName(noticeViewAssembler.enterpriseName(enterpriseMap, source.getAgencyEnterpriseId()));
        }
        return result;
    }

    private List<TenderLotRespVO> buildTenderLotList(List<TenderLotDO> lots) {
        List<TenderLotRespVO> result = BeanUtils.toBean(lots, TenderLotRespVO.class);
        Map<String, String> qualificationNames = noticeViewAssembler.codeNameMap("qualification_level");
        Map<String, String> evaluationNames = noticeViewAssembler.codeNameMap("evaluation_method");
        for (int i = 0; i < lots.size(); i++) {
            TenderLotDO source = lots.get(i);
            TenderLotRespVO target = result.get(i);
            target.setQualificationLevelName(qualificationNames.get(source.getQualificationLevelCode()));
            target.setEvaluationMethodName(NoticeViewAssembler.evaluationMethodName(
                    evaluationNames, source.getEvaluationMethodCode()));
        }
        return result;
    }

    private Map<Long, EnterpriseDO> enterpriseMapOfProjects(Collection<ProjectDO> projects) {
        Set<Long> ids = new HashSet<>();
        for (ProjectDO project : projects) {
            if (project.getTendererEnterpriseId() != null) {
                ids.add(project.getTendererEnterpriseId());
            }
            if (project.getAgencyEnterpriseId() != null) {
                ids.add(project.getAgencyEnterpriseId());
            }
        }
        return noticeViewAssembler.enterpriseMap(ids);
    }

    private Set<String> areaCodesOfProjects(Collection<ProjectDO> projects) {
        Set<String> codes = new HashSet<>();
        for (ProjectDO project : projects) {
            if (project.getProvinceCode() != null && !project.getProvinceCode().isBlank()) {
                codes.add(project.getProvinceCode());
            }
            if (project.getCityCode() != null && !project.getCityCode().isBlank()) {
                codes.add(project.getCityCode());
            }
        }
        return codes;
    }
}
