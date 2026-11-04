package com.tenderintelligence.module.notice.dal.mysql;

import com.tenderintelligence.framework.mybatis.core.mapper.BaseMapperX;
import com.tenderintelligence.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.tenderintelligence.module.notice.dal.dataobject.EnterpriseRoleDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 企业角色 Mapper
 */
@Mapper
public interface EnterpriseRoleMapper extends BaseMapperX<EnterpriseRoleDO> {

    default EnterpriseRoleDO selectByEnterpriseIdAndRoleCode(Long enterpriseId, String roleCode) {
        return selectOne(new LambdaQueryWrapperX<EnterpriseRoleDO>()
                .eq(EnterpriseRoleDO::getEnterpriseId, enterpriseId)
                .eq(EnterpriseRoleDO::getRoleCode, roleCode)
                .last("LIMIT 1"));
    }
}
