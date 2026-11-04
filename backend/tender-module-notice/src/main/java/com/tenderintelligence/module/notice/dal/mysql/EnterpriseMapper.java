package com.tenderintelligence.module.notice.dal.mysql;

import com.tenderintelligence.framework.mybatis.core.mapper.BaseMapperX;
import com.tenderintelligence.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.tenderintelligence.module.notice.dal.dataobject.EnterpriseDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * 企业主体 Mapper
 */
@Mapper
public interface EnterpriseMapper extends BaseMapperX<EnterpriseDO> {

    default List<EnterpriseDO> selectListByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return selectBatchIds(ids);
    }

    default EnterpriseDO selectFirstByNameHash(String nameHash) {
        return selectOne(new LambdaQueryWrapperX<EnterpriseDO>()
                .eq(EnterpriseDO::getNameHash, nameHash)
                .last("LIMIT 1"));
    }
}
