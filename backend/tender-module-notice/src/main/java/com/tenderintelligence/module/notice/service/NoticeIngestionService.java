package com.tenderintelligence.module.notice.service;

import com.tenderintelligence.framework.common.util.object.BeanUtils;
import com.tenderintelligence.module.notice.api.notice.dto.NoticeAttachmentDTO;
import com.tenderintelligence.module.notice.api.notice.dto.NoticeDTO;
import com.tenderintelligence.module.notice.api.notice.dto.NoticeUpsertRespDTO;
import com.tenderintelligence.module.notice.dal.dataobject.BaseAreaDO;
import com.tenderintelligence.module.notice.dal.dataobject.CrawlMergeLogDO;
import com.tenderintelligence.module.notice.dal.dataobject.EnterpriseDO;
import com.tenderintelligence.module.notice.dal.dataobject.EnterpriseRoleDO;
import com.tenderintelligence.module.notice.dal.dataobject.NoticeAttachmentDO;
import com.tenderintelligence.module.notice.dal.dataobject.NoticeEnterpriseDO;
import com.tenderintelligence.module.notice.dal.dataobject.NoticePortalDO;
import com.tenderintelligence.module.notice.dal.dataobject.NoticeQualityDO;
import com.tenderintelligence.module.notice.dal.dataobject.ProjectDO;
import com.tenderintelligence.module.notice.dal.dataobject.TenderLotDO;
import com.tenderintelligence.module.notice.dal.dataobject.TenderProjectDO;
import com.tenderintelligence.module.notice.dal.mysql.BaseAreaMapper;
import com.tenderintelligence.module.notice.dal.mysql.CrawlMergeLogMapper;
import com.tenderintelligence.module.notice.dal.mysql.EnterpriseMapper;
import com.tenderintelligence.module.notice.dal.mysql.EnterpriseRoleMapper;
import com.tenderintelligence.module.notice.dal.mysql.NoticeAttachmentMapper;
import com.tenderintelligence.module.notice.dal.mysql.NoticeEnterpriseMapper;
import com.tenderintelligence.module.notice.dal.mysql.NoticePortalMapper;
import com.tenderintelligence.module.notice.dal.mysql.NoticeQualityMapper;
import com.tenderintelligence.module.notice.dal.mysql.ProjectMapper;
import com.tenderintelligence.module.notice.dal.mysql.TenderLotMapper;
import com.tenderintelligence.module.notice.dal.mysql.TenderProjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * 公告采集入库服务（新模型写路径）
 *
 * <p>一次 {@link #upsert(NoticeDTO)} 完成：
 * <ol>
 *   <li>{@code notice} 按 {@code source_url} 去重写入（仅写 P0 国标列）；</li>
 *   <li>确保 {@code project} / {@code tender_project} / {@code tender_lot} 四层骨架；</li>
 *   <li>确保 {@code enterprise} / {@code enterprise_role} / {@code notice_enterprise} 主体关联；</li>
 *   <li>写入 {@code notice_quality} 缺失字段与 {@code crawl_merge_log} 归并记录。</li>
 * </ol>
 *
 * <p>这是 P0 之后唯一的采集写入口，替代旧的 {@code NoticeModelSyncService} JdbcTemplate 兼容层。
 */
@Service
@Slf4j
public class NoticeIngestionService {

    private static final String DEFAULT_LOT_NO = "DEFAULT";
    private static final String CREATOR = "crawler";
    private static final String DEFAULT_PARSER_VERSION = "CRAWL-V2";
    private static final String STATUS_ACTIVE = "ACTIVE";

    @Resource
    private NoticePortalMapper noticePortalMapper;
    @Resource
    private NoticeAttachmentMapper noticeAttachmentMapper;
    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private TenderProjectMapper tenderProjectMapper;
    @Resource
    private TenderLotMapper tenderLotMapper;
    @Resource
    private EnterpriseMapper enterpriseMapper;
    @Resource
    private EnterpriseRoleMapper enterpriseRoleMapper;
    @Resource
    private NoticeEnterpriseMapper noticeEnterpriseMapper;
    @Resource
    private NoticeQualityMapper noticeQualityMapper;
    @Resource
    private CrawlMergeLogMapper crawlMergeLogMapper;
    @Resource
    private BaseAreaMapper baseAreaMapper;

    /**
     * 按 {@code sourceUrl} 去重入库，并同步四层模型。
     */
    @Transactional(rollbackFor = Exception.class)
    public NoticeUpsertRespDTO upsert(NoticeDTO dto) {
        if (dto == null || isBlank(dto.getSourceUrl())) {
            throw new IllegalArgumentException("sourceUrl 不能为空");
        }
        NoticePortalDO existing = noticePortalMapper.selectBySourceUrl(dto.getSourceUrl());
        RegionCodes region = resolveRegion(dto);
        boolean created = false;
        boolean updated = false;
        NoticePortalDO notice;
        if (existing == null) {
            notice = new NoticePortalDO();
            applyDto(dto, notice);
            noticePortalMapper.insert(notice);
            saveAttachments(notice.getId(), dto.getAttachments());
            created = true;
        } else {
            notice = existing;
            if (isChanged(existing, dto, region)) {
                applyDto(dto, notice);
                notice.setUpdateTime(LocalDateTime.now());
                noticePortalMapper.updateById(notice);
                saveAttachments(notice.getId(), dto.getAttachments());
                updated = true;
            }
        }

        // 新公告、内容有变、或四层骨架不完整时才同步，避免每轮采集重复写关联表
        boolean modelIncomplete = notice.getProjectId() == null
                || notice.getTenderProjectId() == null || notice.getLotId() == null;
        if (created || updated || modelIncomplete) {
            syncModel(notice, dto, region);
        }
        return new NoticeUpsertRespDTO(notice.getId(), created, updated);
    }

    // ------------------------------------------------------------------
    // 四层模型同步
    // ------------------------------------------------------------------

    private void syncModel(NoticePortalDO notice, NoticeDTO dto, RegionCodes region) {
        Long tendererId = ensureEnterprise(dto.getTendererName(), "TENDERER", notice.getId(), dto.getSourceUrl());
        Long agencyId = ensureEnterprise(dto.getAgencyName(), "AGENCY", notice.getId(), dto.getSourceUrl());

        ProjectDO project = ensureProject(notice, dto, region, tendererId, agencyId);
        Long projectId = project == null ? null : project.getId();
        TenderProjectDO tenderProject = ensureTenderProject(notice, dto, projectId, region, tendererId, agencyId);
        Long tenderProjectId = tenderProject == null ? null : tenderProject.getId();
        TenderLotDO lot = ensureLot(notice, dto, projectId, tenderProjectId, region);

        linkEnterprise(notice.getId(), tendererId, "TENDERER", dto.getTendererName());
        linkEnterprise(notice.getId(), agencyId, "AGENCY", dto.getAgencyName());

        updateNoticeNewColumns(notice, dto, region, projectId, tenderProjectId,
                lot == null ? null : lot.getId(), tendererId, agencyId);

        if (projectId != null) {
            projectMapper.refreshStats(projectId);
        }
        if (tenderProjectId != null) {
            tenderProjectMapper.refreshLotCount(tenderProjectId);
        }
        writeMergeLog(notice.getId(), projectId, tenderProjectId,
                lot == null ? null : lot.getId(), dto, project);
    }

    private ProjectDO ensureProject(NoticePortalDO notice, NoticeDTO dto, RegionCodes region,
                                    Long tendererId, Long agencyId) {
        String projectKey = buildProjectKey(dto);
        ProjectDO project = projectMapper.selectByProjectKey(projectKey);
        if (project == null) {
            project = new ProjectDO();
            project.setProjectKey(projectKey);
            project.setUnifiedDealCode(dto.getUnifiedDealCode());
            project.setProjectNo(safe(dto.getProjectNo()));
            project.setProjectName(firstNonBlank(notice.getTitle(), safe(dto.getProjectNo())));
            project.setTradeCategoryCode(safe(dto.getTradeCategoryCode()));
            project.setRegionCode(region.adminRegionCode);
            project.setProvinceCode(region.provinceCode);
            project.setCityCode(region.cityCode);
            project.setDistrictCode(region.districtCode);
            project.setBudget(dto.getBudget());
            project.setFundingSourceCode(safe(dto.getFundingSourceCode()));
            project.setTendererEnterpriseId(tendererId);
            project.setAgencyEnterpriseId(agencyId);
            project.setStatus(STATUS_ACTIVE);
            project.setFirstPublishTime(notice.getPublishTime());
            project.setLastPublishTime(notice.getPublishTime());
            project.setNoticeCount(1);
            project.setLotCount(1);
            project.setSourceCount(1);
            project.setDataQualityStatus("PARTIAL");
            try {
                projectMapper.insert(project);
            } catch (DuplicateKeyException ex) {
                project = projectMapper.selectByProjectKey(projectKey);
                if (project == null && !isBlank(dto.getUnifiedDealCode())) {
                    project = projectMapper.selectOne(ProjectDO::getUnifiedDealCode, dto.getUnifiedDealCode());
                }
            }
        } else {
            boolean dirty = false;
            if (!Objects.equals(project.getUnifiedDealCode(), dto.getUnifiedDealCode())
                    && !isBlank(dto.getUnifiedDealCode())) {
                project.setUnifiedDealCode(dto.getUnifiedDealCode());
                dirty = true;
            }
            if (project.getBudget() == null && dto.getBudget() != null) {
                project.setBudget(dto.getBudget());
                dirty = true;
            }
            if (project.getTendererEnterpriseId() == null && tendererId != null) {
                project.setTendererEnterpriseId(tendererId);
                dirty = true;
            }
            if (project.getAgencyEnterpriseId() == null && agencyId != null) {
                project.setAgencyEnterpriseId(agencyId);
                dirty = true;
            }
            if (project.getFirstPublishTime() == null
                    || (notice.getPublishTime() != null && notice.getPublishTime().isBefore(project.getFirstPublishTime()))) {
                project.setFirstPublishTime(notice.getPublishTime());
                dirty = true;
            }
            if (project.getLastPublishTime() == null
                    || (notice.getPublishTime() != null && notice.getPublishTime().isAfter(project.getLastPublishTime()))) {
                project.setLastPublishTime(notice.getPublishTime());
                dirty = true;
            }
            if (dirty) {
                try {
                    projectMapper.updateById(project);
                } catch (DuplicateKeyException ex) {
                    // 统一交易标识码被另一个项目占用时，不让整条采集失败
                    log.warn("[ingestion] 项目#{} 更新冲突(可能统一交易标识码重复): {}",
                            project.getId(), ex.getMessage());
                }
            }
        }
        return project;
    }

    private TenderProjectDO ensureTenderProject(NoticePortalDO notice, NoticeDTO dto, Long projectId,
                                                RegionCodes region, Long tendererId, Long agencyId) {
        if (projectId == null) {
            return null;
        }
        TenderProjectDO tenderProject = tenderProjectMapper.selectFirstByProjectId(projectId);
        if (tenderProject == null) {
            tenderProject = new TenderProjectDO();
            tenderProject.setProjectId(projectId);
            tenderProject.setTenderProjectNo(blankToNull(firstNonBlank(dto.getTenderProjectNo(), dto.getProjectNo())));
            tenderProject.setTenderProjectName(firstNonBlank(dto.getTenderProjectNo(), notice.getTitle()));
            tenderProject.setProcurementMethodCode(safe(dto.getProcurementMethodCode()));
            tenderProject.setBudget(dto.getBudget());
            tenderProject.setPriceCeiling(dto.getPriceCeiling());
            tenderProject.setFundingSourceCode(safe(dto.getFundingSourceCode()));
            tenderProject.setLotCount(1);
            tenderProject.setEvaluationMethodCode(safe(dto.getEvaluationMethodCode()));
            tenderProject.setTendererEnterpriseId(tendererId);
            tenderProject.setAgencyEnterpriseId(agencyId);
            tenderProject.setRegionCode(region.adminRegionCode);
            tenderProject.setPublishTime(notice.getPublishTime());
            tenderProject.setStatus(STATUS_ACTIVE);
            try {
                tenderProjectMapper.insert(tenderProject);
            } catch (DuplicateKeyException ex) {
                tenderProject = tenderProjectMapper.selectFirstByProjectId(projectId);
            }
        } else {
            boolean dirty = false;
            if (isBlank(tenderProject.getTenderProjectNo()) && !isBlank(dto.getTenderProjectNo())) {
                tenderProject.setTenderProjectNo(dto.getTenderProjectNo());
                dirty = true;
            }
            if (tenderProject.getBudget() == null && dto.getBudget() != null) {
                tenderProject.setBudget(dto.getBudget());
                dirty = true;
            }
            if (tenderProject.getTendererEnterpriseId() == null && tendererId != null) {
                tenderProject.setTendererEnterpriseId(tendererId);
                dirty = true;
            }
            if (tenderProject.getAgencyEnterpriseId() == null && agencyId != null) {
                tenderProject.setAgencyEnterpriseId(agencyId);
                dirty = true;
            }
            if (dirty) {
                try {
                    tenderProjectMapper.updateById(tenderProject);
                } catch (DuplicateKeyException ex) {
                    log.warn("[ingestion] 招标项目#{} 更新冲突: {}", tenderProject.getId(), ex.getMessage());
                }
            }
        }
        return tenderProject;
    }

    private TenderLotDO ensureLot(NoticePortalDO notice, NoticeDTO dto, Long projectId,
                                  Long tenderProjectId, RegionCodes region) {
        if (tenderProjectId == null || projectId == null) {
            return null;
        }
        String lotNo = firstNonBlank(dto.getLotNo(), DEFAULT_LOT_NO);
        TenderLotDO lot = tenderLotMapper.selectByTenderProjectIdAndLotNo(tenderProjectId, lotNo);
        if (lot == null) {
            lot = new TenderLotDO();
            lot.setTenderProjectId(tenderProjectId);
            lot.setProjectId(projectId);
            lot.setLotNo(lotNo);
            lot.setLotName(firstNonBlank(dto.getLotNo(), notice.getTitle()));
            lot.setLotContent(dto.getQualificationRequirement());
            lot.setBudget(dto.getBudget());
            lot.setPriceCeiling(dto.getPriceCeiling());
            lot.setDepositAmount(dto.getDepositAmount());
            lot.setQualificationRequirement(dto.getQualificationRequirement());
            lot.setSpecificQualification(dto.getSpecificQualification());
            lot.setConsortiumAllowed(dto.getConsortiumAllowed());
            lot.setPerformanceRequirement(dto.getPerformanceRequirement());
            lot.setQualificationLevelCode(safe(dto.getQualificationLevelCode()));
            lot.setEvaluationMethodCode(safe(dto.getEvaluationMethodCode()));
            lot.setRegionCode(region.adminRegionCode);
            lot.setStatus(STATUS_ACTIVE);
            try {
                tenderLotMapper.insert(lot);
            } catch (DuplicateKeyException ex) {
                lot = tenderLotMapper.selectByTenderProjectIdAndLotNo(tenderProjectId, lotNo);
            }
        } else {
            boolean dirty = false;
            if (lot.getBudget() == null && dto.getBudget() != null) {
                lot.setBudget(dto.getBudget());
                dirty = true;
            }
            if (lot.getDepositAmount() == null && dto.getDepositAmount() != null) {
                lot.setDepositAmount(dto.getDepositAmount());
                dirty = true;
            }
            if (dirty) {
                tenderLotMapper.updateById(lot);
            }
        }
        return lot;
    }

    // ------------------------------------------------------------------
    // 企业主体
    // ------------------------------------------------------------------

    private Long ensureEnterprise(String rawName, String roleCode, Long noticeId, String sourceUrl) {
        String name = rawName == null ? "" : rawName.trim();
        if (name.isBlank()) {
            return null;
        }
        String normalized = normalizeName(name);
        String hash = sha256Hex(normalized);
        EnterpriseDO enterprise = enterpriseMapper.selectFirstByNameHash(hash);
        if (enterprise == null) {
            enterprise = new EnterpriseDO();
            enterprise.setName(name);
            enterprise.setNormalizedName(normalized);
            enterprise.setNameHash(hash);
            enterprise.setSourceType("NOTICE");
            enterprise.setSourceUrl(safe(sourceUrl));
            enterprise.setStatus(STATUS_ACTIVE);
            try {
                enterpriseMapper.insert(enterprise);
            } catch (DuplicateKeyException ex) {
                enterprise = enterpriseMapper.selectFirstByNameHash(hash);
            }
        }
        if (enterprise == null || enterprise.getId() == null) {
            return null;
        }
        ensureEnterpriseRole(enterprise.getId(), roleCode, noticeId);
        return enterprise.getId();
    }

    private void ensureEnterpriseRole(Long enterpriseId, String roleCode, Long noticeId) {
        EnterpriseRoleDO role = enterpriseRoleMapper.selectByEnterpriseIdAndRoleCode(enterpriseId, roleCode);
        if (role == null) {
            role = new EnterpriseRoleDO();
            role.setEnterpriseId(enterpriseId);
            role.setRoleCode(roleCode);
            role.setFirstSeenNoticeId(noticeId);
            role.setLastSeenNoticeId(noticeId);
            role.setStatus(STATUS_ACTIVE);
            try {
                enterpriseRoleMapper.insert(role);
            } catch (DuplicateKeyException ignored) {
                // 唯一键 (enterprise_id, role_code) 已存在
            }
        } else if (!Objects.equals(role.getLastSeenNoticeId(), noticeId)) {
            role.setLastSeenNoticeId(noticeId);
            enterpriseRoleMapper.updateById(role);
        }
    }

    private void linkEnterprise(Long noticeId, Long enterpriseId, String roleCode, String sourceText) {
        if (noticeId == null || enterpriseId == null) {
            return;
        }
        NoticeEnterpriseDO link = noticeEnterpriseMapper
                .selectOneByNoticeEnterpriseRole(noticeId, enterpriseId, roleCode);
        if (link == null) {
            link = new NoticeEnterpriseDO();
            link.setNoticeId(noticeId);
            link.setEnterpriseId(enterpriseId);
            link.setRoleCode(roleCode);
            link.setSourceText(safe(sourceText));
            link.setConfidence(BigDecimal.ONE);
            link.setStatus("AUTO");
            try {
                noticeEnterpriseMapper.insert(link);
            } catch (DuplicateKeyException ignored) {
                // 唯一键 (notice_id, enterprise_id, role_code) 已存在
            }
        } else if (!Objects.equals(link.getSourceText(), safe(sourceText))) {
            link.setSourceText(safe(sourceText));
            noticeEnterpriseMapper.updateById(link);
        }
    }

    // ------------------------------------------------------------------
    // notice 新列 / 质量 / 归并日志
    // ------------------------------------------------------------------

    private void updateNoticeNewColumns(NoticePortalDO notice, NoticeDTO dto, RegionCodes region,
                                        Long projectId, Long tenderProjectId, Long lotId,
                                        Long tendererId, Long agencyId) {
        notice.setProjectId(projectId);
        notice.setTenderProjectId(tenderProjectId);
        notice.setLotId(lotId);
        notice.setNoticeTypeCode(firstNonBlank(dto.getNoticeTypeCode(), notice.getNoticeTypeCode(), "tender"));
        notice.setNoticeSubtypeCode(firstNonBlank(dto.getNoticeSubtypeCode(), notice.getNoticeSubtypeCode()));
        notice.setTenderProjectNo(firstNonBlank(dto.getTenderProjectNo(), dto.getProjectNo(),
                notice.getTenderProjectNo()));
        notice.setLotNo(firstNonBlank(dto.getLotNo(), notice.getLotNo(), DEFAULT_LOT_NO));
        notice.setNoticeNo(firstNonBlank(dto.getNoticeNo(), dto.getProjectNo(), notice.getNoticeNo()));
        notice.setUnifiedDealCode(firstNonBlank(dto.getUnifiedDealCode(), notice.getUnifiedDealCode()));
        notice.setSourceSiteCode(firstNonBlank(dto.getSourceSiteCode(), mapSourceSite(dto.getSourcePlatform()),
                notice.getSourceSiteCode()));
        notice.setSourcePlatform(firstNonBlank(dto.getSourcePlatform(),
                notice.getSourcePlatform()));
        notice.setFileGetStartTime(firstNonNull(dto.getFileGetStartTime(), notice.getFileGetStartTime()));
        notice.setFileGetEndTime(firstNonNull(dto.getFileGetEndTime(), notice.getFileGetEndTime()));
        notice.setQaDeadline(firstNonNull(dto.getQaDeadline(), notice.getQaDeadline()));
        notice.setSignDeadline(firstNonNull(dto.getSignDeadline(), notice.getSignDeadline()));
        notice.setBidDeadline(firstNonNull(dto.getBidDeadline(), notice.getBidDeadline()));
        notice.setOpenPlace(firstNonBlank(dto.getOpenPlace(), notice.getOpenPlace()));
        notice.setPublicityStartTime(firstNonNull(dto.getPublicityStartTime(), notice.getPublicityStartTime()));
        notice.setPublicityEndTime(firstNonNull(dto.getPublicityEndTime(), notice.getPublicityEndTime()));
        notice.setPriceCeiling(firstNonNull(dto.getPriceCeiling(), notice.getPriceCeiling()));
        notice.setDepositAmount(firstNonNull(dto.getDepositAmount(), notice.getDepositAmount()));
        notice.setFilePrice(firstNonNull(dto.getFilePrice(), notice.getFilePrice()));
        notice.setWinAmount(firstNonNull(dto.getWinAmount(), notice.getWinAmount()));
        notice.setProcurementMethodCode(firstNonBlank(dto.getProcurementMethodCode(),
                notice.getProcurementMethodCode()));
        notice.setFundingSourceCode(firstNonBlank(dto.getFundingSourceCode(), notice.getFundingSourceCode()));
        notice.setEvaluationMethodCode(firstNonBlank(dto.getEvaluationMethodCode(),
                notice.getEvaluationMethodCode()));
        notice.setQualificationRequirement(firstNonBlank(dto.getQualificationRequirement(),
                notice.getQualificationRequirement()));
        notice.setSpecificQualification(firstNonBlank(dto.getSpecificQualification(),
                notice.getSpecificQualification()));
        notice.setConsortiumAllowed(firstNonNull(dto.getConsortiumAllowed(), notice.getConsortiumAllowed()));
        notice.setPerformanceRequirement(firstNonBlank(dto.getPerformanceRequirement(),
                notice.getPerformanceRequirement()));
        notice.setQualificationLevelCode(firstNonBlank(dto.getQualificationLevelCode(),
                notice.getQualificationLevelCode()));
        notice.setTradeCategoryCode(firstNonBlank(dto.getTradeCategoryCode(), notice.getTradeCategoryCode()));
        notice.setProvinceCode(firstNonBlank(region.provinceCode, notice.getProvinceCode()));
        notice.setCityCode(firstNonBlank(region.cityCode, notice.getCityCode()));
        notice.setDistrictCode(firstNonBlank(region.districtCode, notice.getDistrictCode()));
        notice.setTendererEnterpriseId(firstNonNull(tendererId, notice.getTendererEnterpriseId()));
        notice.setAgencyEnterpriseId(firstNonNull(agencyId, notice.getAgencyEnterpriseId()));
        notice.setIndustryCode(firstNonBlank(dto.getIndustryCode(), notice.getIndustryCode()));
        notice.setHasAttachment(dto.getAttachments() != null && !dto.getAttachments().isEmpty());
        notice.setStatus(STATUS_ACTIVE);
        notice.setParserVersion(firstNonBlank(dto.getParserVersion(), notice.getParserVersion(),
                DEFAULT_PARSER_VERSION));

        List<String> missing = computeMissingFields(notice);
        notice.setMissingFields(toJsonArray(missing));
        if (isBlank(notice.getTitle())) {
            notice.setDataQualityStatus("BROKEN");
        } else {
            notice.setDataQualityStatus(missing.isEmpty() ? "OK" : "PARTIAL");
        }
        notice.setUpdateTime(LocalDateTime.now());
        noticePortalMapper.updateById(notice);

        writeQuality(notice, missing);
    }

    private List<String> computeMissingFields(NoticePortalDO notice) {
        List<String> missing = new ArrayList<>();
        if (isBlank(notice.getTitle())) {
            missing.add("title");
        }
        addIfBlank(missing, "unified_deal_code", notice.getUnifiedDealCode());
        addIfBlank(missing, "trade_category_code", notice.getTradeCategoryCode());
        addIfBlank(missing, "procurement_method_code", notice.getProcurementMethodCode());
        addIfBlank(missing, "qualification_requirement", notice.getQualificationRequirement());
        if (notice.getDepositAmount() == null) {
            missing.add("deposit_amount");
        }
        if (notice.getFileGetStartTime() == null) {
            missing.add("file_get_start_time");
        }
        if (notice.getFileGetEndTime() == null) {
            missing.add("file_get_end_time");
        }
        return missing;
    }

    private void writeQuality(NoticePortalDO notice, List<String> missingFields) {
        noticeQualityMapper.deleteOpenByNoticeId(notice.getId());
        for (String field : missingFields) {
            NoticeQualityDO quality = new NoticeQualityDO();
            quality.setNoticeId(notice.getId());
            quality.setIssueType("title".equals(field) ? "PARSE_ERROR" : "MISSING_FIELD");
            quality.setFieldName(field);
            quality.setSeverity("title".equals(field) ? "ERROR" : "WARN");
            quality.setDetail("title".equals(field) ? "公告标题解析为空" : "采集/解析后该字段为空");
            quality.setSourceUrl(safe(notice.getSourceUrl()));
            quality.setParserVersion(safe(notice.getParserVersion()));
            quality.setStatus("OPEN");
            noticeQualityMapper.insert(quality);
        }
    }

    private void writeMergeLog(Long noticeId, Long projectId, Long tenderProjectId, Long lotId,
                               NoticeDTO dto, ProjectDO project) {
        CrawlMergeLogDO mergeLog = new CrawlMergeLogDO();
        mergeLog.setSourceNoticeId(noticeId);
        mergeLog.setTargetProjectId(projectId);
        mergeLog.setTargetTenderProjectId(tenderProjectId);
        mergeLog.setTargetLotId(lotId);
        mergeLog.setMatchType(!isBlank(dto.getUnifiedDealCode()) ? "UNIFIED_CODE"
                : !isBlank(dto.getProjectNo()) ? "PROJECT_NO_NOTICE_ID" : "SOURCE_URL");
        mergeLog.setStatus("AUTO");
        mergeLog.setOperatedAt(LocalDateTime.now());
        if (project != null) {
            mergeLog.setDetail("{\"projectKey\":\"" + safe(project.getProjectKey()) + "\"}");
        }
        crawlMergeLogMapper.insert(mergeLog);
    }

    // ------------------------------------------------------------------
    // 附件 / DTO 映射
    // ------------------------------------------------------------------

    private void saveAttachments(Long noticeId, List<NoticeAttachmentDTO> attachments) {
        noticeAttachmentMapper.deleteByNoticeId(noticeId);
        if (attachments == null || attachments.isEmpty()) {
            return;
        }
        List<NoticeAttachmentDO> rows = BeanUtils.toBean(attachments, NoticeAttachmentDO.class);
        for (NoticeAttachmentDO row : rows) {
            row.setId(null);
            row.setNoticeId(noticeId);
        }
        noticeAttachmentMapper.insertBatch(rows);
    }

    private void applyDto(NoticeDTO dto, NoticePortalDO notice) {
        notice.setTitle(dto.getTitle());
        notice.setProjectNo(dto.getProjectNo());
        notice.setIndustry(dto.getIndustry());
        notice.setBudget(dto.getBudget());
        notice.setContact(dto.getContact());
        notice.setContactPhone(dto.getContactPhone());
        notice.setContent(dto.getContent());
        notice.setSourceUrl(dto.getSourceUrl());
        notice.setPublishTime(dto.getPublishTime());
        notice.setOpenTime(dto.getOpenTime());
        notice.setRegionCode(dto.getRegionCode());
        // 其余国标列在 syncModel 中统一写回
    }

    private boolean isChanged(NoticePortalDO existing, NoticeDTO dto, RegionCodes region) {
        if (!equalsSafe(existing.getTitle(), dto.getTitle())
                || !equalsSafe(existing.getProjectNo(), dto.getProjectNo())
                || !equalsSafe(existing.getIndustry(), dto.getIndustry())
                || !equalsSafe(existing.getBudget(), dto.getBudget())
                || !equalsSafe(existing.getContact(), dto.getContact())
                || !equalsSafe(existing.getContactPhone(), dto.getContactPhone())
                || !equalsSafe(existing.getContent(), dto.getContent())
                || !equalsSafe(existing.getOpenTime(), dto.getOpenTime())
                || !equalsSafe(existing.getPublishTime(), dto.getPublishTime())
                || !equalsSafe(existing.getRegionCode(), dto.getRegionCode())
                || !equalsSafe(existing.getNoticeTypeCode(),
                        firstNonBlank(dto.getNoticeTypeCode(), existing.getNoticeTypeCode()))
                || !equalsSafe(existing.getBidDeadline(),
                        firstNonNull(dto.getBidDeadline(), existing.getBidDeadline()))
                || !equalsSafe(existing.getSourcePlatform(),
                        firstNonBlank(dto.getSourcePlatform(), existing.getSourcePlatform()))
                || !equalsSafe(existing.getProvinceCode(),
                        firstNonBlank(region.provinceCode, existing.getProvinceCode()))
                || !equalsSafe(existing.getCityCode(),
                        firstNonBlank(region.cityCode, existing.getCityCode()))) {
            return true;
        }
        // 国标列：只有解析器明确产出时才参与变更判断，避免 null 覆盖库中已有值
        if ((dto.getUnifiedDealCode() != null && !equalsSafe(existing.getUnifiedDealCode(), dto.getUnifiedDealCode()))
                || (dto.getPriceCeiling() != null && !equalsSafe(existing.getPriceCeiling(), dto.getPriceCeiling()))
                || (dto.getDepositAmount() != null && !equalsSafe(existing.getDepositAmount(), dto.getDepositAmount()))
                || (notBlankAndDifferent(existing.getProcurementMethodCode(), dto.getProcurementMethodCode()))
                || (notBlankAndDifferent(existing.getQualificationRequirement(), dto.getQualificationRequirement()))) {
            return true;
        }
        return attachmentsChanged(existing.getId(), dto.getAttachments());
    }

    private static boolean notBlankAndDifferent(String existing, String incoming) {
        return incoming != null && !incoming.isBlank() && !equalsSafe(existing, incoming);
    }

    private boolean attachmentsChanged(Long noticeId, List<NoticeAttachmentDTO> incoming) {
        List<NoticeAttachmentDO> existing = noticeAttachmentMapper.selectListByNoticeId(noticeId);
        List<NoticeAttachmentDTO> fresh = incoming == null ? List.of() : incoming;
        if (existing.size() != fresh.size()) {
            return true;
        }
        for (int i = 0; i < existing.size(); i++) {
            NoticeAttachmentDO before = existing.get(i);
            NoticeAttachmentDTO after = fresh.get(i);
            if (!equalsSafe(before.getFileUrl(), after.getFileUrl())
                    || !equalsSafe(before.getFileName(), after.getFileName())
                    || !equalsSafe(before.getFileSize(), after.getFileSize())) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    // 地区 / 工具
    // ------------------------------------------------------------------

    private RegionCodes resolveRegion(NoticeDTO dto) {
        String provinceCode = firstNonBlank(dto.getProvinceCode(), lookupProvinceCode(dto.getProvinceName()));
        String cityCode = firstNonBlank(dto.getCityCode(), lookupCityCode(provinceCode, dto.getCityName()));
        String districtCode = safe(dto.getDistrictCode());
        String adminRegionCode = firstNonBlank(cityCode, provinceCode, dto.getRegionCode());
        return new RegionCodes(provinceCode, cityCode, districtCode, adminRegionCode);
    }

    private String lookupProvinceCode(String province) {
        BaseAreaDO area = baseAreaMapper.selectProvinceByName(province);
        return area == null ? "" : area.getCode();
    }

    private String lookupCityCode(String provinceCode, String city) {
        if (isBlank(city)) {
            return "";
        }
        BaseAreaDO area = baseAreaMapper.selectCityByName(provinceCode, city);
        if (area != null) {
            return area.getCode();
        }
        BaseAreaDO municipality = baseAreaMapper.selectProvinceByCityName(city);
        return municipality == null ? "" : municipality.getCode();
    }

    private String buildProjectKey(NoticeDTO dto) {
        // 必须与 P0-3 回填 SQL 的公式一致：SHA256(项目编号或标题 | notice.region_code | 项目编号)
        String base = isBlank(dto.getProjectNo()) ? safe(dto.getTitle()) : dto.getProjectNo();
        return sha256Hex(base + "|" + safe(dto.getRegionCode()) + "|" + safe(dto.getProjectNo()));
    }

    private String mapSourceSite(String source) {
        if (source == null) {
            return "";
        }
        String lower = source.toLowerCase(Locale.ROOT);
        if (lower.contains("ccgp") || lower.contains("中国政府采购")) {
            return "ccgp";
        }
        if (lower.contains("ggzy") || lower.contains("公共资源")) {
            return "ggzy";
        }
        if (lower.contains("ztb") || lower.contains("贵州")) {
            return "ztb_gz";
        }
        return "";
    }

    /**
     * 企业名称归一化，必须与 P0-2 回填 SQL 保持一致，否则同一条公告会建出重复企业。
     * SQL：TRIM → 去掉全角空格/半角空格/换行/回车/制表 → 全角括号转半角 → LEFT(...,256)
     */
    private static String normalizeName(String name) {
        String normalized = name.trim()
                .replace("　", "")
                .replace(" ", "")
                .replace("（", "(")
                .replace("）", ")")
                .replace("\n", "")
                .replace("\r", "")
                .replace("\t", "");
        return normalized.length() <= 256 ? normalized : normalized.substring(0, 256);
    }

    private static String sha256Hex(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                builder.append(Character.forDigit((b >> 4) & 0xF, 16));
                builder.append(Character.forDigit(b & 0xF, 16));
            }
            return builder.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    private static String toJsonArray(List<String> values) {
        if (values.isEmpty()) {
            return "[]";
        }
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append('"').append(values.get(i)).append('"');
        }
        return builder.append(']').toString();
    }

    private static void addIfBlank(List<String> missing, String field, String value) {
        if (isBlank(value)) {
            missing.add(field);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String blankToNull(String value) {
        return isBlank(value) ? null : value;
    }

    private static boolean equalsSafe(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }

    private static <T> T firstNonNull(T first, T second) {
        return first != null ? first : second;
    }

    private static <T> T firstNonNull(T first, T second, T third) {
        return first != null ? first : (second != null ? second : third);
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private record RegionCodes(String provinceCode, String cityCode, String districtCode,
                               String adminRegionCode) {
    }
}
