package com.tenderintelligence.module.notice.dal.mysql;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tenderintelligence.framework.mybatis.core.mapper.BaseMapperX;
import com.tenderintelligence.module.notice.dal.dataobject.ProjectDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.List;

/**
 * 项目数据集 Mapper
 */
@Mapper
public interface ProjectMapper extends BaseMapperX<ProjectDO> {

    default List<ProjectDO> selectListByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return selectBatchIds(ids);
    }

    default Long selectTotalCount() {
        return selectCount(new LambdaQueryWrapper<>());
    }

    default ProjectDO selectByProjectKey(String projectKey) {
        return selectOne(ProjectDO::getProjectKey, projectKey);
    }

    /**
     * 刷新项目冗余统计（公告数 / 标段数 / 来源数 / 首末发布时间）
     */
    @Update("UPDATE project p SET " +
            "notice_count = (SELECT COUNT(*) FROM notice n WHERE n.project_id = p.id AND n.deleted = 0), " +
            "lot_count = (SELECT COUNT(*) FROM tender_lot l WHERE l.project_id = p.id AND l.deleted = 0), " +
            "source_count = (SELECT COUNT(DISTINCT n.source_site_code) FROM notice n WHERE n.project_id = p.id AND n.deleted = 0), " +
            "first_publish_time = (SELECT MIN(n.publish_time) FROM notice n WHERE n.project_id = p.id AND n.deleted = 0), " +
            "last_publish_time = (SELECT MAX(n.publish_time) FROM notice n WHERE n.project_id = p.id AND n.deleted = 0) " +
            "WHERE p.id = #{projectId}")
    int refreshStats(@Param("projectId") Long projectId);
}
