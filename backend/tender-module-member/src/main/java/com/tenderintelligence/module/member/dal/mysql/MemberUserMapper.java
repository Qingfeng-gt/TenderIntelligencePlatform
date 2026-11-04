package com.tenderintelligence.module.member.dal.mysql;

import com.tenderintelligence.framework.common.pojo.PageResult;
import com.tenderintelligence.framework.mybatis.core.mapper.BaseMapperX;
import com.tenderintelligence.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.tenderintelligence.module.member.controller.admin.user.vo.MemberUserPageReqVO;
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

    default PageResult<MemberUserDO> selectPage(MemberUserPageReqVO reqVO) {
        LambdaQueryWrapperX<MemberUserDO> wrapper = new LambdaQueryWrapperX<MemberUserDO>()
                .likeIfPresent(MemberUserDO::getUsername, reqVO.getUsername())
                .likeIfPresent(MemberUserDO::getNickname, reqVO.getNickname())
                .likeIfPresent(MemberUserDO::getMobile, reqVO.getMobile())
                .eqIfPresent(MemberUserDO::getStatus, reqVO.getStatus())
                .orderByDesc(MemberUserDO::getId);
        return selectPage(reqVO, wrapper);
    }

}
