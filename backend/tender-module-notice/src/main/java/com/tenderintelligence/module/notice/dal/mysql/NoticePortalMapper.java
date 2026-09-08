package com.tenderintelligence.module.notice.dal.mysql;

import com.tenderintelligence.framework.common.pojo.PageResult;
import com.tenderintelligence.framework.mybatis.core.mapper.BaseMapperX;
import com.tenderintelligence.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.tenderintelligence.module.notice.controller.admin.notice.vo.NoticePageReqVO;
import com.tenderintelligence.module.notice.controller.admin.notice.vo.NoticeRespVO;
import com.tenderintelligence.module.notice.dal.dataobject.NoticePortalDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

/**
 * 招投标公告 Mapper
 */
@Mapper
public interface NoticePortalMapper extends BaseMapperX<NoticePortalDO> {

    default PageResult<NoticePortalDO> selectPage(NoticePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<NoticePortalDO>()
                .likeIfPresent(NoticePortalDO::getTitle, reqVO.getKeyword())
                .eqIfPresent(NoticePortalDO::getType, reqVO.getType())
                .eqIfPresent(NoticePortalDO::getProvince, reqVO.getProvince())
                .eqIfPresent(NoticePortalDO::getIndustry, reqVO.getIndustry())
                .geIfPresent(NoticePortalDO::getPublishTime, reqVO.getPublishTimeGreaterThan())
                .orderByDesc(NoticePortalDO::getPublishTime));
    }

    default Long selectCountByPublishTime(LocalDateTime time) {
        return selectCount(new LambdaQueryWrapperX<NoticePortalDO>()
                .ge(NoticePortalDO::getPublishTime, time));
    }

    /**
     * 公告总数(未删除)
     */
    default Long selectTotalCount() {
        return selectCount(new LambdaQueryWrapperX<>());
    }

    /**
     * 覆盖省份数量(去重,排除空值)
     */
    @Select("SELECT COUNT(DISTINCT province) FROM notice WHERE deleted = 0 AND province IS NOT NULL AND province != ''")
    Long selectProvinceCount();

    /**
     * 行业分类数量(去重,排除空值)
     */
    @Select("SELECT COUNT(DISTINCT industry) FROM notice WHERE deleted = 0 AND industry IS NOT NULL AND industry != ''")
    Long selectIndustryCount();

    /**
     * 按源站详情 URL 查询(爬虫去重)
     */
    default NoticePortalDO selectBySourceUrl(String sourceUrl) {
        return selectOne(NoticePortalDO::getSourceUrl, sourceUrl);
    }
}
