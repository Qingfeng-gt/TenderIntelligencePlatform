package com.tenderintelligence.module.notice.service;

import com.tenderintelligence.framework.common.util.object.BeanUtils;
import com.tenderintelligence.module.notice.controller.admin.notice.vo.NoticeAttachmentRespVO;
import com.tenderintelligence.module.notice.controller.admin.notice.vo.NoticeChangeRespVO;
import com.tenderintelligence.module.notice.controller.admin.notice.vo.NoticeEnterpriseRespVO;
import com.tenderintelligence.module.notice.controller.admin.notice.vo.NoticeRespVO;
import com.tenderintelligence.module.notice.dal.dataobject.BaseAreaDO;
import com.tenderintelligence.module.notice.dal.dataobject.BaseCodeDO;
import com.tenderintelligence.module.notice.dal.dataobject.EnterpriseDO;
import com.tenderintelligence.module.notice.dal.dataobject.NoticeChangeDO;
import com.tenderintelligence.module.notice.dal.dataobject.NoticeEnterpriseDO;
import com.tenderintelligence.module.notice.dal.dataobject.NoticePortalDO;
import com.tenderintelligence.module.notice.dal.dataobject.ProjectDO;
import com.tenderintelligence.module.notice.dal.dataobject.TenderLotDO;
import com.tenderintelligence.module.notice.dal.dataobject.TenderProjectDO;
import com.tenderintelligence.module.notice.dal.mysql.BaseAreaMapper;
import com.tenderintelligence.module.notice.dal.mysql.BaseCodeMapper;
import com.tenderintelligence.module.notice.dal.mysql.EnterpriseMapper;
import com.tenderintelligence.module.notice.dal.mysql.NoticeAttachmentMapper;
import com.tenderintelligence.module.notice.dal.mysql.NoticeChangeMapper;
import com.tenderintelligence.module.notice.dal.mysql.NoticeEnterpriseMapper;
import com.tenderintelligence.module.notice.dal.mysql.ProjectMapper;
import com.tenderintelligence.module.notice.dal.mysql.TenderLotMapper;
import com.tenderintelligence.module.notice.dal.mysql.TenderProjectMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 公告/项目视图组装器
 *
 * <p>把四层模型拆开的主表一次性批量取回再组装，避免逐条查询（N+1）。
 * 旧扁平列只作为地区名、招标人/代理机构名的兜底，不再作为主要读源。
 */
@Component
public class NoticeViewAssembler {

    private static final String CODE_SET_NOTICE_TYPE = "notice_type";
    private static final String CODE_SET_PROCUREMENT_METHOD = "procurement_method";
    private static final String CODE_SET_QUALIFICATION_LEVEL = "qualification_level";
    private static final String CODE_SET_TRADE_CATEGORY = "trade_category";
    private static final String CODE_SET_ENTERPRISE_ROLE = "enterprise_role";
    private static final String CODE_SET_EVALUATION_METHOD = "evaluation_method";

    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private TenderProjectMapper tenderProjectMapper;
    @Resource
    private TenderLotMapper tenderLotMapper;
    @Resource
    private EnterpriseMapper enterpriseMapper;
    @Resource
    private NoticeAttachmentMapper noticeAttachmentMapper;
    @Resource
    private NoticeEnterpriseMapper noticeEnterpriseMapper;
    @Resource
    private NoticeChangeMapper noticeChangeMapper;
    @Resource
    private BaseAreaMapper baseAreaMapper;
    @Resource
    private BaseCodeMapper baseCodeMapper;

    /**
     * 批量组装公告视图
     *
     * @param notices     公告列表
     * @param withContent 是否带公告正文（列表页不需要，避免大字段传输）
     */
    public List<NoticeRespVO> buildNoticeList(Collection<NoticePortalDO> notices, boolean withContent) {
        if (notices == null || notices.isEmpty()) {
            return List.of();
        }
        Map<Long, ProjectDO> projectMap = toMap(projectMapper.selectListByIds(
                collect(notices, NoticePortalDO::getProjectId)), ProjectDO::getId);
        Map<Long, TenderProjectDO> tenderProjectMap = toMap(tenderProjectMapper.selectListByProjectIds(
                collect(notices, NoticePortalDO::getProjectId)), TenderProjectDO::getId);
        Map<Long, TenderLotDO> lotMap = toMap(tenderLotMapper.selectListByProjectIds(
                collect(notices, NoticePortalDO::getProjectId)), TenderLotDO::getId);
        Set<Long> enterpriseIds = new HashSet<>();
        notices.forEach(n -> {
            addIfNotNull(enterpriseIds, n.getTendererEnterpriseId());
            addIfNotNull(enterpriseIds, n.getAgencyEnterpriseId());
            addIfNotNull(enterpriseIds, n.getWinnerEnterpriseId());
        });
        Map<Long, EnterpriseDO> enterpriseMap = toMap(
                enterpriseMapper.selectListByIds(enterpriseIds), EnterpriseDO::getId);
        Set<String> areaCodes = new HashSet<>();
        notices.forEach(n -> {
            addIfNotBlank(areaCodes, n.getProvinceCode());
            addIfNotBlank(areaCodes, n.getCityCode());
        });
        Map<String, BaseAreaDO> areaMap = toMap(
                baseAreaMapper.selectListByCodes(areaCodes), BaseAreaDO::getCode);
        Map<String, String> noticeTypeNames = codeNameMap(CODE_SET_NOTICE_TYPE);
        Map<String, String> procurementMethodNames = codeNameMap(CODE_SET_PROCUREMENT_METHOD);
        Map<String, String> qualificationLevelNames = codeNameMap(CODE_SET_QUALIFICATION_LEVEL);

        List<NoticeRespVO> result = new ArrayList<>(notices.size());
        for (NoticePortalDO notice : notices) {
            ProjectDO project = notice.getProjectId() == null ? null : projectMap.get(notice.getProjectId());
            TenderProjectDO tenderProject = notice.getTenderProjectId() == null
                    ? null : tenderProjectMap.get(notice.getTenderProjectId());
            TenderLotDO lot = notice.getLotId() == null ? null : lotMap.get(notice.getLotId());
            NoticeRespVO vo = new NoticeRespVO();
            vo.setId(notice.getId());
            vo.setProjectId(notice.getProjectId());
            vo.setTenderProjectId(notice.getTenderProjectId());
            vo.setLotId(notice.getLotId());
            vo.setTitle(notice.getTitle());
            vo.setNoticeTypeCode(notice.getNoticeTypeCode());
            vo.setNoticeTypeName(noticeTypeNames.getOrDefault(notice.getNoticeTypeCode(), notice.getNoticeTypeCode()));
            vo.setNoticeNo(notice.getNoticeNo());
            vo.setProjectNo(notice.getProjectNo());
            vo.setTenderProjectNo(notice.getTenderProjectNo());
            vo.setLotNo(notice.getLotNo());
            vo.setUnifiedDealCode(notice.getUnifiedDealCode());

            if (project != null) {
                vo.setProjectName(project.getProjectName());
                vo.setProjectBudget(project.getBudget());
                vo.setProjectNoticeCount(project.getNoticeCount());
            }
            if (tenderProject != null) {
                vo.setTenderProjectName(tenderProject.getTenderProjectName());
                vo.setProcurementMethodCode(tenderProject.getProcurementMethodCode());
            } else {
                vo.setProcurementMethodCode(notice.getProcurementMethodCode());
            }
            vo.setProcurementMethodName(procurementMethodNames.getOrDefault(
                    vo.getProcurementMethodCode(), vo.getProcurementMethodCode()));

            vo.setProvinceCode(notice.getProvinceCode());
            vo.setCityCode(notice.getCityCode());
            vo.setProvinceName(areaName(areaMap.get(notice.getProvinceCode()), notice.getProvince()));
            vo.setCityName(areaName(areaMap.get(notice.getCityCode()), notice.getCity()));
            vo.setIndustry(notice.getIndustry());
            vo.setIndustryCode(notice.getIndustryCode());

            vo.setBudget(firstNonNull(notice.getBudget(), project == null ? null : project.getBudget()));
            vo.setPriceCeiling(firstNonNull(notice.getPriceCeiling(), lot == null ? null : lot.getPriceCeiling()));
            vo.setDepositAmount(firstNonNull(notice.getDepositAmount(), lot == null ? null : lot.getDepositAmount()));
            vo.setWinAmount(notice.getWinAmount());

            vo.setTendererEnterpriseId(notice.getTendererEnterpriseId());
            vo.setTendererName(firstNonBlank(enterpriseName(enterpriseMap, notice.getTendererEnterpriseId()),
                    notice.getTenderPerson()));
            vo.setAgencyEnterpriseId(notice.getAgencyEnterpriseId());
            vo.setAgencyName(firstNonBlank(enterpriseName(enterpriseMap, notice.getAgencyEnterpriseId()),
                    notice.getAgency()));
            vo.setWinnerEnterpriseId(notice.getWinnerEnterpriseId());
            vo.setWinnerName(enterpriseName(enterpriseMap, notice.getWinnerEnterpriseId()));

            vo.setContact(notice.getContact());
            vo.setContactPhone(notice.getContactPhone());
            vo.setContent(withContent ? notice.getContent() : null);
            vo.setSourceSiteCode(notice.getSourceSiteCode());
            vo.setSourcePlatform(firstNonBlank(notice.getSourcePlatform(), notice.getSource()));
            vo.setSourceUrl(notice.getSourceUrl());

            vo.setPublishTime(notice.getPublishTime());
            vo.setBidDeadline(notice.getBidDeadline());
            vo.setOpenTime(notice.getOpenTime());
            vo.setFileGetStartTime(notice.getFileGetStartTime());
            vo.setFileGetEndTime(notice.getFileGetEndTime());
            vo.setQaDeadline(notice.getQaDeadline());
            vo.setSignDeadline(notice.getSignDeadline());
            vo.setPublicityStartTime(notice.getPublicityStartTime());
            vo.setPublicityEndTime(notice.getPublicityEndTime());
            vo.setOpenPlace(notice.getOpenPlace());

            vo.setQualificationRequirement(firstNonBlank(notice.getQualificationRequirement(),
                    lot == null ? null : lot.getQualificationRequirement()));
            vo.setSpecificQualification(firstNonBlank(notice.getSpecificQualification(),
                    lot == null ? null : lot.getSpecificQualification()));
            vo.setPerformanceRequirement(firstNonBlank(notice.getPerformanceRequirement(),
                    lot == null ? null : lot.getPerformanceRequirement()));
            vo.setConsortiumAllowed(firstNonNull(notice.getConsortiumAllowed(),
                    lot == null ? null : lot.getConsortiumAllowed()));
            vo.setQualificationLevelCode(firstNonBlank(notice.getQualificationLevelCode(),
                    lot == null ? null : lot.getQualificationLevelCode()));
            vo.setQualificationLevelName(qualificationLevelNames.get(vo.getQualificationLevelCode()));

            vo.setHasAttachment(notice.getHasAttachment());
            vo.setViewCount(notice.getViewCount());
            vo.setStatus(notice.getStatus());
            vo.setDataQualityStatus(notice.getDataQualityStatus());
            result.add(vo);
        }
        return result;
    }

    /**
     * 填充详情专属字段：附件、澄清修改、关联企业
     */
    public void fillDetail(NoticeRespVO vo) {
        if (vo == null || vo.getId() == null) {
            return;
        }
        vo.setAttachments(BeanUtils.toBean(
                noticeAttachmentMapper.selectListByNoticeId(vo.getId()), NoticeAttachmentRespVO.class));
        vo.setChanges(BeanUtils.toBean(
                noticeChangeMapper.selectListByNoticeId(vo.getId()), NoticeChangeRespVO.class));
        vo.setEnterprises(buildNoticeEnterprises(vo));
    }

    private List<NoticeEnterpriseRespVO> buildNoticeEnterprises(NoticeRespVO vo) {
        return buildEnterpriseList(noticeEnterpriseMapper.selectListByNoticeId(vo.getId()),
                vo.getTendererEnterpriseId(), vo.getTendererName(),
                vo.getAgencyEnterpriseId(), vo.getAgencyName(),
                vo.getWinnerEnterpriseId(), vo.getWinnerName());
    }

    /**
     * 组装企业角色列表（公告详情、项目详情共用）
     */
    public List<NoticeEnterpriseRespVO> buildEnterpriseList(List<NoticeEnterpriseDO> links,
                                                            Long tendererId, String tendererName,
                                                            Long agencyId, String agencyName,
                                                            Long winnerId, String winnerName) {
        Set<Long> enterpriseIds = links.stream().map(NoticeEnterpriseDO::getEnterpriseId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        addIfNotNull(enterpriseIds, tendererId);
        addIfNotNull(enterpriseIds, agencyId);
        addIfNotNull(enterpriseIds, winnerId);
        Map<Long, EnterpriseDO> enterpriseMap = toMap(
                enterpriseMapper.selectListByIds(enterpriseIds), EnterpriseDO::getId);
        Map<String, String> roleNames = codeNameMap(CODE_SET_ENTERPRISE_ROLE);

        List<NoticeEnterpriseRespVO> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (NoticeEnterpriseDO link : links) {
            NoticeEnterpriseRespVO item = new NoticeEnterpriseRespVO();
            item.setEnterpriseId(link.getEnterpriseId());
            item.setEnterpriseName(firstNonBlank(enterpriseName(enterpriseMap, link.getEnterpriseId()),
                    link.getSourceText()));
            item.setRoleCode(link.getRoleCode());
            item.setRoleName(roleNames.getOrDefault(link.getRoleCode(), link.getRoleCode()));
            item.setSourceText(link.getSourceText());
            item.setConfidence(link.getConfidence());
            item.setStatus(link.getStatus());
            result.add(item);
            seen.add(link.getEnterpriseId() + "|" + link.getRoleCode());
        }
        addSyntheticEnterprise(result, seen, roleNames, tendererId, "TENDERER", tendererName, enterpriseMap);
        addSyntheticEnterprise(result, seen, roleNames, agencyId, "AGENCY", agencyName, enterpriseMap);
        addSyntheticEnterprise(result, seen, roleNames, winnerId, "WINNER", winnerName, enterpriseMap);
        return result;
    }

    private void addSyntheticEnterprise(List<NoticeEnterpriseRespVO> result, Set<String> seen,
                                        Map<String, String> roleNames, Long enterpriseId, String roleCode,
                                        String fallbackName, Map<Long, EnterpriseDO> enterpriseMap) {
        if (enterpriseId == null || !seen.add(enterpriseId + "|" + roleCode)) {
            return;
        }
        NoticeEnterpriseRespVO item = new NoticeEnterpriseRespVO();
        item.setEnterpriseId(enterpriseId);
        item.setEnterpriseName(enterpriseName(enterpriseMap, enterpriseId));
        item.setRoleCode(roleCode);
        item.setRoleName(roleNames.getOrDefault(roleCode, roleCode));
        item.setStatus("AUTO");
        if (item.getEnterpriseName() == null) {
            item.setEnterpriseName(fallbackName);
        }
        result.add(item);
    }

    // ------------------------------------------------------------------
    // 公共工具：供 ProjectService 复用
    // ------------------------------------------------------------------

    public Map<String, String> codeNameMap(String codeSet) {
        return baseCodeMapper.selectListByCodeSet(codeSet).stream()
                .collect(Collectors.toMap(BaseCodeDO::getCode, BaseCodeDO::getName, (a, b) -> a));
    }

    public Map<Long, EnterpriseDO> enterpriseMap(Collection<Long> ids) {
        return toMap(enterpriseMapper.selectListByIds(ids), EnterpriseDO::getId);
    }

    public Map<String, BaseAreaDO> areaMap(Collection<String> codes) {
        return toMap(baseAreaMapper.selectListByCodes(codes), BaseAreaDO::getCode);
    }

    public String enterpriseName(Map<Long, EnterpriseDO> enterpriseMap, Long enterpriseId) {
        if (enterpriseId == null) {
            return null;
        }
        EnterpriseDO enterprise = enterpriseMap.get(enterpriseId);
        return enterprise == null ? null : enterprise.getName();
    }

    public static String areaName(BaseAreaDO area, String fallback) {
        if (area == null) {
            return fallback;
        }
        if (area.getShortName() != null && !area.getShortName().isBlank()) {
            return area.getShortName();
        }
        return area.getName();
    }

    public static String tradeCategoryName(Map<String, String> names, String code) {
        return names.getOrDefault(code, code);
    }

    public static String evaluationMethodName(Map<String, String> names, String code) {
        return names.getOrDefault(code, code);
    }

    public static String procurementMethodName(Map<String, String> names, String code) {
        return names.getOrDefault(code, code);
    }

    private static <K, T> Map<K, T> toMap(Collection<T> list, Function<T, K> keyGetter) {
        Map<K, T> map = new HashMap<>();
        for (T item : list) {
            map.put(keyGetter.apply(item), item);
        }
        return map;
    }

    private static Set<Long> collect(Collection<NoticePortalDO> notices, Function<NoticePortalDO, Long> getter) {
        Set<Long> ids = new HashSet<>();
        for (NoticePortalDO notice : notices) {
            addIfNotNull(ids, getter.apply(notice));
        }
        return ids;
    }

    private static void addIfNotNull(Set<Long> ids, Long id) {
        if (id != null) {
            ids.add(id);
        }
    }

    private static void addIfNotBlank(Set<String> values, String value) {
        if (value != null && !value.isBlank()) {
            values.add(value);
        }
    }

    private static <T> T firstNonNull(T first, T second) {
        return first != null ? first : second;
    }

    private static String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }
}
