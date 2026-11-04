package com.tenderintelligence.module.notice.dal.mysql;

import com.tenderintelligence.framework.mybatis.core.mapper.BaseMapperX;
import com.tenderintelligence.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.tenderintelligence.module.notice.dal.dataobject.TenderProjectDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.List;

/**
 * 招标项目数据集 Mapper
 */
@Mapper
public interface TenderProjectMapper extends BaseMapperX<TenderProjectDO> {

    default List<TenderProjectDO> selectListByProjectIds(Collection<Long> projectIds) {
        if (projectIds == null || projectIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<TenderProjectDO>()
                .in(TenderProjectDO::getProjectId, projectIds)
                .orderByAsc(TenderProjectDO::getId));
    }

    default List<TenderProjectDO> selectListByProjectId(Long projectId) {
        return selectList(new LambdaQueryWrapperX<TenderProjectDO>()
                .eq(TenderProjectDO::getProjectId, projectId)
                .orderByAsc(TenderProjectDO::getId));
    }

    default TenderProjectDO selectFirstByProjectId(Long projectId) {
        return selectOne(new LambdaQueryWrapperX<TenderProjectDO>()
                .eq(TenderProjectDO::getProjectId, projectId)
                .orderByAsc(TenderProjectDO::getId)
                .last("LIMIT 1"));
    }

    @Update("UPDATE tender_project tp SET " +
            "lot_count = (SELECT COUNT(*) FROM tender_lot l WHERE l.tender_project_id = tp.id AND l.deleted = 0) " +
            "WHERE tp.id = #{tenderProjectId}")
    int refreshLotCount(@Param("tenderProjectId") Long tenderProjectId);
}
