package com.tenderintelligence.module.crawler.service;

import com.tenderintelligence.module.notice.dal.dataobject.NoticePortalDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 新标讯模型同步（P0 过渡层）
 *
 * <p>在旧 {@code notice} 入库/更新后，把数据补齐到新模型：
 * <ul>
 *   <li>{@code project} / {@code tender_project} / {@code tender_lot}：四层归并骨架</li>
 *   <li>{@code enterprise} / {@code enterprise_role} / {@code notice_enterprise}：招标人/代理机构</li>
 *   <li>{@code notice} 新增国标列：project_id、notice_type_code、地区代码、企业 ID 等</li>
 * </ul>
 *
 * <p>为什么用 JdbcTemplate 而不是 DO/Mapper：
 * 这是 P0 的**兼容写入层**，先让新表开始积累数据，不引入新模块/新 DO，避免大范围改动；
 * P0 稳定后再替换成领域模块的 Service。
 *
 * <p>失败策略：同步失败只记 WARN，不阻断旧链路入库。新模型缺口由
 * {@code P0-3__notice_backfill.sql} 或后续补偿任务修复。
 */
@Service
@Slf4j
public class NoticeModelSyncService {

    private static final String DEFAULT_LOT_NO = "DEFAULT";
    private static final String CREATOR = "crawler";
    private static final String PARSER_VERSION = "CRAWL-V1";

    private final JdbcTemplate jdbcTemplate;

    public NoticeModelSyncService(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    /**
     * 同步一条公告到新模型。必须在 notice 已入库（id 非空）后调用。
     */
    public void sync(NoticePortalDO notice) {
        if (notice == null || notice.getId() == null) {
            return;
        }
        try {
            String provinceCode = lookupProvinceCode(notice.getProvince());
            String cityCode = lookupCityCode(provinceCode, notice.getCity());
            String districtCode = "";
            String adminRegionCode = firstNonBlank(cityCode, provinceCode, notice.getRegionCode());
            String sourceRegionCode = safe(notice.getRegionCode());

            Long tendererId = resolveEnterprise(notice.getTenderPerson(), "TENDERER");
            Long agencyId = resolveEnterprise(notice.getAgency(), "AGENCY");

            String projectKey = buildProjectKey(notice, sourceRegionCode);
            Long projectId = ensureProject(notice, projectKey, adminRegionCode,
                    provinceCode, cityCode, districtCode, tendererId, agencyId);
            Long tenderProjectId = ensureTenderProject(notice, projectId, adminRegionCode, tendererId, agencyId);
            Long lotId = ensureLot(notice, tenderProjectId, projectId, adminRegionCode);

            linkNoticeEnterprise(notice.getId(), tendererId, "TENDERER", notice.getTenderPerson());
            linkNoticeEnterprise(notice.getId(), agencyId, "AGENCY", notice.getAgency());

            String sourceSiteCode = mapSourceSite(notice.getSource());
            updateNoticeColumns(notice, projectId, tenderProjectId, lotId, sourceSiteCode,
                    provinceCode, cityCode, districtCode, tendererId, agencyId);

            if (projectId != null) {
                refreshProjectStats(projectId);
            }
            if (tenderProjectId != null) {
                refreshTenderProjectStats(tenderProjectId);
            }
        } catch (Exception e) {
            // 不阻断旧链路：新模型失败可由回填/补偿任务修复
            log.warn("同步标讯新模型失败, noticeId={}, sourceUrl={}", notice.getId(), notice.getSourceUrl(), e);
        }
    }

    // ------------------------------------------------------------------
    // 企业
    // ------------------------------------------------------------------

    private Long resolveEnterprise(String rawName, String roleCode) {
        String normalized = normalizeName(rawName);
        if (isBlank(normalized)) {
            return null;
        }
        String nameHash = sha256Hex(normalized);
        Long enterpriseId = queryLong(
                "SELECT id FROM enterprise WHERE deleted=0 AND name_hash=? LIMIT 1", nameHash);
        if (enterpriseId == null) {
            try {
                jdbcTemplate.update(
                        "INSERT INTO enterprise (name, normalized_name, name_hash, source_type, status, " +
                                "creator, create_time, updater, update_time, deleted, tenant_id) " +
                                "VALUES (?,?,?,?,?,?,NOW(),?,NOW(),0,0)",
                        truncate(safe(rawName).trim(), 256), truncate(normalized, 256), nameHash,
                        "NOTICE", "ACTIVE", CREATOR, CREATOR);
            } catch (DuplicateKeyException ignored) {
                // name_hash 没有唯一键；这里只防并发重复，不保证强一致
            }
            enterpriseId = queryLong(
                    "SELECT id FROM enterprise WHERE deleted=0 AND name_hash=? LIMIT 1", nameHash);
        }
        if (enterpriseId == null) {
            return null;
        }
        Integer roleCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM enterprise_role WHERE deleted=0 AND enterprise_id=? AND role_code=?",
                Integer.class, enterpriseId, roleCode);
        if (roleCount == null || roleCount == 0) {
            try {
                jdbcTemplate.update(
                        "INSERT INTO enterprise_role (enterprise_id, role_code, status, " +
                                "creator, create_time, updater, update_time, deleted, tenant_id) " +
                                "VALUES (?,?,?,?,NOW(),?,NOW(),0,0)",
                        enterpriseId, roleCode, "ACTIVE", CREATOR, CREATOR);
            } catch (DuplicateKeyException ignored) {
                // 唯一键 (enterprise_id, role_code) 已存在
            }
        }
        return enterpriseId;
    }

    private void linkNoticeEnterprise(Long noticeId, Long enterpriseId, String roleCode, String sourceText) {
        if (noticeId == null || enterpriseId == null) {
            return;
        }
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notice_enterprise WHERE deleted=0 AND notice_id=? AND enterprise_id=? AND role_code=?",
                Integer.class, noticeId, enterpriseId, roleCode);
        if (count != null && count > 0) {
            return;
        }
        try {
            jdbcTemplate.update(
                    "INSERT INTO notice_enterprise (notice_id, enterprise_id, role_code, source_text, " +
                            "confidence, status, creator, create_time, updater, update_time, deleted, tenant_id) " +
                            "VALUES (?,?,?,?,?,?,?,NOW(),?,NOW(),0,0)",
                    noticeId, enterpriseId, roleCode, truncate(safe(sourceText), 512),
                    1.0, "AUTO", CREATOR, CREATOR);
        } catch (DuplicateKeyException ignored) {
            // 唯一键 (notice_id, enterprise_id, role_code) 已存在
        }
    }

    // ------------------------------------------------------------------
    // 项目 / 招标项目 / 标段
    // ------------------------------------------------------------------

    private String buildProjectKey(NoticePortalDO notice, String sourceRegionCode) {
        String base = isBlank(notice.getProjectNo()) ? safe(notice.getTitle()) : notice.getProjectNo();
        return sha256Hex(base + "|" + safe(sourceRegionCode) + "|" + safe(notice.getProjectNo()));
    }

    private Long ensureProject(NoticePortalDO notice, String projectKey, String regionCode,
                               String provinceCode, String cityCode, String districtCode,
                               Long tendererId, Long agencyId) {
        Long projectId = queryLong(
                "SELECT id FROM project WHERE deleted=0 AND project_key=? LIMIT 1", projectKey);
        if (projectId != null) {
            return projectId;
        }
        try {
            jdbcTemplate.update(
                    "INSERT INTO project (project_key, project_no, project_name, region_code, " +
                            "province_code, city_code, district_code, budget, tenderer_enterprise_id, " +
                            "agency_enterprise_id, status, first_publish_time, last_publish_time, " +
                            "notice_count, lot_count, source_count, data_quality_status, " +
                            "creator, create_time, updater, update_time, deleted, tenant_id) " +
                            "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,NOW(),?,NOW(),0,0)",
                    projectKey, safe(notice.getProjectNo()), safe(notice.getTitle()), safe(regionCode),
                    safe(provinceCode), safe(cityCode), safe(districtCode), notice.getBudget(),
                    tendererId, agencyId, "ACTIVE", notice.getPublishTime(), notice.getPublishTime(),
                    1, 1, 1, "PARTIAL", CREATOR, CREATOR);
        } catch (DuplicateKeyException ignored) {
            // 并发下另一个线程已插入
        }
        return queryLong("SELECT id FROM project WHERE deleted=0 AND project_key=? LIMIT 1", projectKey);
    }

    private Long ensureTenderProject(NoticePortalDO notice, Long projectId, String regionCode,
                                     Long tendererId, Long agencyId) {
        if (projectId == null) {
            return null;
        }
        Long tenderProjectId = queryLong(
                "SELECT id FROM tender_project WHERE deleted=0 AND project_id=? LIMIT 1", projectId);
        if (tenderProjectId != null) {
            return tenderProjectId;
        }
        try {
            jdbcTemplate.update(
                    "INSERT INTO tender_project (project_id, tender_project_no, tender_project_name, " +
                            "procurement_method_code, budget, region_code, publish_time, status, " +
                            "creator, create_time, updater, update_time, deleted, tenant_id) " +
                            "VALUES (?,?,?,?,?,?,?,?,?,NOW(),?,NOW(),0,0)",
                    projectId, isBlank(notice.getProjectNo()) ? null : notice.getProjectNo(),
                    safe(notice.getTitle()), "", notice.getBudget(), safe(regionCode),
                    notice.getPublishTime(), "ACTIVE", CREATOR, CREATOR);
        } catch (DuplicateKeyException ignored) {
            // 唯一键 (project_id, tender_project_no) 已存在
        }
        return queryLong("SELECT id FROM tender_project WHERE deleted=0 AND project_id=? LIMIT 1", projectId);
    }

    private Long ensureLot(NoticePortalDO notice, Long tenderProjectId, Long projectId, String regionCode) {
        if (tenderProjectId == null || projectId == null) {
            return null;
        }
        Long lotId = queryLong(
                "SELECT id FROM tender_lot WHERE deleted=0 AND tender_project_id=? AND lot_no=? LIMIT 1",
                tenderProjectId, DEFAULT_LOT_NO);
        if (lotId != null) {
            return lotId;
        }
        try {
            jdbcTemplate.update(
                    "INSERT INTO tender_lot (tender_project_id, project_id, lot_no, lot_name, lot_content, " +
                            "budget, region_code, status, creator, create_time, updater, update_time, deleted, tenant_id) " +
                            "VALUES (?,?,?,?,?,?,?,?,?,NOW(),?,NOW(),0,0)",
                    tenderProjectId, projectId, DEFAULT_LOT_NO, safe(notice.getTitle()), null,
                    notice.getBudget(), safe(regionCode), "ACTIVE", CREATOR, CREATOR);
        } catch (DuplicateKeyException ignored) {
            // 唯一键 (tender_project_id, lot_no) 已存在
        }
        return queryLong(
                "SELECT id FROM tender_lot WHERE deleted=0 AND tender_project_id=? AND lot_no=? LIMIT 1",
                tenderProjectId, DEFAULT_LOT_NO);
    }

    // ------------------------------------------------------------------
    // notice 新列
    // ------------------------------------------------------------------

    private void updateNoticeColumns(NoticePortalDO notice, Long projectId, Long tenderProjectId, Long lotId,
                                     String sourceSiteCode, String provinceCode, String cityCode,
                                     String districtCode, Long tendererId, Long agencyId) {
        String noticeTypeCode = isBlank(notice.getType()) ? "tender" : notice.getType();
        String tenderProjectNo = safe(notice.getProjectNo());
        String noticeNo = safe(notice.getProjectNo());
        String lotNo = DEFAULT_LOT_NO;
        int hasAttachment = notice.getAttachments() != null && !notice.getAttachments().isEmpty() ? 1 : 0;

        jdbcTemplate.update(
                "UPDATE notice SET project_id=?, tender_project_id=?, lot_id=?, tender_project_no=?, " +
                        "lot_no=?, notice_no=?, notice_type_code=?, source_site_code=?, source_platform=?, " +
                        "bid_deadline=?, province_code=?, city_code=?, district_code=?, " +
                        "tenderer_enterprise_id=?, agency_enterprise_id=?, has_attachment=?, " +
                        "status='ACTIVE', parser_version=?, data_quality_status='PARTIAL', " +
                        "missing_fields=JSON_ARRAY('unified_deal_code','trade_category_code'," +
                        "'procurement_method_code','deposit_amount','qualification_requirement'," +
                        "'file_get_start_time','file_get_end_time'), update_time=NOW() " +
                        "WHERE id=?",
                projectId, tenderProjectId, lotId, tenderProjectNo,
                lotNo, noticeNo, noticeTypeCode, safe(sourceSiteCode), safe(notice.getSource()),
                notice.getDeadline(), safe(provinceCode), safe(cityCode), safe(districtCode),
                tendererId, agencyId, hasAttachment,
                PARSER_VERSION, notice.getId());
    }

    private void refreshProjectStats(Long projectId) {
        jdbcTemplate.update(
                "UPDATE project p SET " +
                        "notice_count=(SELECT COUNT(*) FROM notice n WHERE n.project_id=p.id AND n.deleted=0), " +
                        "lot_count=(SELECT COUNT(*) FROM tender_lot l WHERE l.project_id=p.id AND l.deleted=0), " +
                        "source_count=(SELECT COUNT(DISTINCT n.source_site_code) FROM notice n WHERE n.project_id=p.id AND n.deleted=0), " +
                        "first_publish_time=(SELECT MIN(n.publish_time) FROM notice n WHERE n.project_id=p.id AND n.deleted=0), " +
                        "last_publish_time=(SELECT MAX(n.publish_time) FROM notice n WHERE n.project_id=p.id AND n.deleted=0) " +
                        "WHERE p.id=?",
                projectId);
    }

    private void refreshTenderProjectStats(Long tenderProjectId) {
        jdbcTemplate.update(
                "UPDATE tender_project tp SET " +
                        "lot_count=(SELECT COUNT(*) FROM tender_lot l WHERE l.tender_project_id=tp.id AND l.deleted=0) " +
                        "WHERE tp.id=?",
                tenderProjectId);
    }

    // ------------------------------------------------------------------
    // 地区 / 工具方法
    // ------------------------------------------------------------------

    private String lookupProvinceCode(String province) {
        if (isBlank(province)) {
            return "";
        }
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT code FROM base_area WHERE deleted=0 AND level=1 AND (short_name=? OR name=?) LIMIT 1",
                    String.class, province.trim(), province.trim());
        } catch (EmptyResultDataAccessException e) {
            return "";
        }
    }

    private String lookupCityCode(String provinceCode, String city) {
        if (isBlank(city)) {
            return "";
        }
        if (!isBlank(provinceCode)) {
            try {
                return jdbcTemplate.queryForObject(
                        "SELECT code FROM base_area WHERE deleted=0 AND level=2 AND parent_code=? " +
                                "AND (short_name=? OR name=?) LIMIT 1",
                        String.class, provinceCode, city.trim(), city.trim());
            } catch (EmptyResultDataAccessException e) {
                // 下面继续按城市名兜底
            }
        }
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT code FROM base_area WHERE deleted=0 AND level=2 " +
                            "AND (short_name=? OR name=?) LIMIT 1",
                    String.class, city.trim(), city.trim());
        } catch (EmptyResultDataAccessException e) {
            // 可能是直辖市名，按省级再查一次
        }
        return isMunicipalityCity(city) ? lookupProvinceCode(city) : "";
    }

    private boolean isMunicipalityCity(String city) {
        String c = city == null ? "" : city.trim();
        return "北京".equals(c) || "北京市".equals(c)
                || "上海".equals(c) || "上海市".equals(c)
                || "天津".equals(c) || "天津市".equals(c)
                || "重庆".equals(c) || "重庆市".equals(c);
    }

    private String mapSourceSite(String source) {
        String s = source == null ? "" : source.trim();
        if ("中国政府采购网".equals(s)) {
            return "ccgp";
        }
        if ("全国公共资源交易平台".equals(s)) {
            return "ggzy";
        }
        if ("贵州省招标投标公共服务平台".equals(s)) {
            return "ztb_gz";
        }
        return "";
    }

    /**
     * 与 P0-2__enterprise_backfill.sql 的归一化保持一致：
     * 去全角空格/普通空格/换行/制表符，全角括号转半角。
     */
    private String normalizeName(String name) {
        if (name == null) {
            return "";
        }
        return name.trim()
                .replace("　", "")
                .replace(" ", "")
                .replace("（", "(")
                .replace("）", ")")
                .replace("\n", "")
                .replace("\r", "")
                .replace("\t", "");
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private Long queryLong(String sql, Object... args) {
        try {
            return jdbcTemplate.queryForObject(sql, Long.class, args);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (!isBlank(value)) {
                return value;
            }
        }
        return "";
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }
}
