package com.tenderintelligence.module.notice.dal.mysql;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tenderintelligence.framework.common.pojo.PageResult;
import com.tenderintelligence.framework.mybatis.core.mapper.BaseMapperX;
import com.tenderintelligence.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.tenderintelligence.module.notice.controller.admin.notice.vo.NoticePageReqVO;
import com.tenderintelligence.module.notice.dal.dataobject.NoticePortalDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 招投标公告 Mapper（四层模型读路径）
 */
@Mapper
public interface NoticePortalMapper extends BaseMapperX<NoticePortalDO> {

    default PageResult<NoticePortalDO> selectPage(NoticePageReqVO reqVO) {
        LambdaQueryWrapperX<NoticePortalDO> wrapper = new LambdaQueryWrapperX<NoticePortalDO>()
                .eqIfPresent(NoticePortalDO::getNoticeTypeCode, reqVO.getNoticeTypeCode())
                .eqIfPresent(NoticePortalDO::getProvinceCode, reqVO.getProvinceCode())
                .eqIfPresent(NoticePortalDO::getCityCode, reqVO.getCityCode())
                .eqIfPresent(NoticePortalDO::getIndustry, reqVO.getIndustry())
                .betweenIfPresent(NoticePortalDO::getBudget, reqVO.getBudgetMin(), reqVO.getBudgetMax())
                .geIfPresent(NoticePortalDO::getPublishTime, reqVO.getPublishTimeGreaterThan())
                .ltIfPresent(NoticePortalDO::getPublishTime, reqVO.getPublishTimeLessThan())
                .orderByDesc(NoticePortalDO::getPublishTime)
                .orderByDesc(NoticePortalDO::getId);
        if (reqVO.getKeyword() != null && !reqVO.getKeyword().isBlank()) {
            String keyword = reqVO.getKeyword().trim();
            wrapper.and(w -> w.like(NoticePortalDO::getTitle, keyword)
                    .or().like(NoticePortalDO::getProjectNo, keyword)
                    .or().like(NoticePortalDO::getNoticeNo, keyword));
        }
        return selectPage(reqVO, wrapper);
    }

    /**
     * 按项目批量取公告（项目详情用）
     */
    default List<NoticePortalDO> selectListByProjectIds(Collection<Long> projectIds) {
        if (projectIds == null || projectIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<NoticePortalDO>()
                .in(NoticePortalDO::getProjectId, projectIds)
                .orderByDesc(NoticePortalDO::getPublishTime)
                .orderByDesc(NoticePortalDO::getId));
    }

    default List<NoticePortalDO> selectListByProjectId(Long projectId) {
        return selectList(new LambdaQueryWrapper<NoticePortalDO>()
                .eq(NoticePortalDO::getProjectId, projectId)
                .orderByDesc(NoticePortalDO::getPublishTime)
                .orderByDesc(NoticePortalDO::getId));
    }

    default Long selectCountByPublishTime(LocalDateTime time) {
        return selectCount(new LambdaQueryWrapperX<NoticePortalDO>()
                .ge(NoticePortalDO::getPublishTime, time));
    }

    /**
     * 公告总数（未删除）
     */
    default Long selectTotalCount() {
        return selectCount(new LambdaQueryWrapperX<>());
    }

    /**
     * 覆盖省份数量（取新模型 province_code）
     */
    @Select("SELECT COUNT(DISTINCT province_code) FROM notice WHERE deleted = 0 AND province_code IS NOT NULL AND province_code <> ''")
    Long selectProvinceCodeCount();

    /**
     * 覆盖省份数量（旧列兜底）
     */
    @Select("SELECT COUNT(DISTINCT province) FROM notice WHERE deleted = 0 AND province IS NOT NULL AND province <> '' AND province != '其他'")
    Long selectProvinceCount();

    /**
     * 行业分类数量（去重，排除空值）
     */
    @Select("SELECT COUNT(DISTINCT industry) FROM notice WHERE deleted = 0 AND industry IS NOT NULL AND industry <> ''")
    Long selectIndustryCount();

    /**
     * 行业列表（去重）
     */
    @Select("SELECT DISTINCT industry FROM notice WHERE deleted = 0 AND industry IS NOT NULL AND industry <> '' ORDER BY industry")
    List<String> selectDistinctIndustries();

    /**
     * 按源站详情 URL 查询（爬虫去重）
     */
    default NoticePortalDO selectBySourceUrl(String sourceUrl) {
        return selectOne(NoticePortalDO::getSourceUrl, sourceUrl);
    }
}
