package com.tenderintelligence.module.notice.dal.mysql;

import com.tenderintelligence.framework.mybatis.core.mapper.BaseMapperX;
import com.tenderintelligence.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.tenderintelligence.module.notice.dal.dataobject.TenderLotDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * 标段（包）数据集 Mapper
 */
@Mapper
public interface TenderLotMapper extends BaseMapperX<TenderLotDO> {

    default List<TenderLotDO> selectListByProjectIds(Collection<Long> projectIds) {
        if (projectIds == null || projectIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<TenderLotDO>()
                .in(TenderLotDO::getProjectId, projectIds)
                .orderByAsc(TenderLotDO::getTenderProjectId)
                .orderByAsc(TenderLotDO::getLotNo));
    }

    default List<TenderLotDO> selectListByProjectId(Long projectId) {
        return selectList(new LambdaQueryWrapperX<TenderLotDO>()
                .eq(TenderLotDO::getProjectId, projectId)
                .orderByAsc(TenderLotDO::getTenderProjectId)
                .orderByAsc(TenderLotDO::getLotNo));
    }

    default TenderLotDO selectByTenderProjectIdAndLotNo(Long tenderProjectId, String lotNo) {
        return selectOne(new LambdaQueryWrapperX<TenderLotDO>()
                .eq(TenderLotDO::getTenderProjectId, tenderProjectId)
                .eq(TenderLotDO::getLotNo, lotNo)
                .last("LIMIT 1"));
    }
}
