package com.tenderintelligence.module.system.convert.user;

import com.tenderintelligence.framework.common.util.collection.CollectionUtils;
import com.tenderintelligence.framework.common.util.object.BeanUtils;
import com.tenderintelligence.module.system.controller.admin.permission.vo.role.RoleSimpleRespVO;
import com.tenderintelligence.module.system.controller.admin.user.vo.profile.UserProfileRespVO;
import com.tenderintelligence.module.system.controller.admin.user.vo.user.UserRespVO;
import com.tenderintelligence.module.system.controller.admin.user.vo.user.UserSimpleRespVO;
import com.tenderintelligence.module.system.dal.dataobject.permission.RoleDO;
import com.tenderintelligence.module.system.dal.dataobject.user.AdminUserDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface UserConvert {

    UserConvert INSTANCE = Mappers.getMapper(UserConvert.class);

    default List<UserRespVO> convertList(List<AdminUserDO> list) {
        return CollectionUtils.convertList(list, this::convert);
    }

    default UserRespVO convert(AdminUserDO user) {
        return BeanUtils.toBean(user, UserRespVO.class);
    }

    default List<UserSimpleRespVO> convertSimpleList(List<AdminUserDO> list) {
        return CollectionUtils.convertList(list, user -> BeanUtils.toBean(user, UserSimpleRespVO.class));
    }

    default UserProfileRespVO convert(AdminUserDO user, List<RoleDO> userRoles) {
        UserProfileRespVO userVO = BeanUtils.toBean(user, UserProfileRespVO.class);
        userVO.setRoles(BeanUtils.toBean(userRoles, RoleSimpleRespVO.class));
        return userVO;
    }

}
