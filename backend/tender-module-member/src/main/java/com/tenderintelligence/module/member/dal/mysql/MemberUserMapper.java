package com.tenderintelligence.module.member.dal.mysql;

import com.tenderintelligence.framework.mybatis.core.mapper.BaseMapperX;
import com.tenderintelligence.module.member.dal.dataobject.MemberUserDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员用户 Mapper
 */
@Mapper
public interface MemberUserMapper extends BaseMapperX<MemberUserDO> {

    default MemberUserDO selectByUsername(String username) {
        return selectOne(MemberUserDO::getUsername, username);
    }

}
