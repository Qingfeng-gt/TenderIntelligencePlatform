package com.tenderintelligence.module.system.api.user;

import com.tenderintelligence.framework.common.util.object.BeanUtils;
import com.tenderintelligence.framework.datapermission.core.annotation.DataPermission;
import com.tenderintelligence.framework.datapermission.core.util.DataPermissionUtils;
import com.tenderintelligence.module.system.api.user.dto.AdminUserRespDTO;
import com.tenderintelligence.module.system.dal.dataobject.user.AdminUserDO;
import com.tenderintelligence.module.system.service.user.AdminUserService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * Admin 用户 API 实现类
 *
 * @author Tender Intelligence
 */
@Service
public class AdminUserApiImpl implements AdminUserApi {

    @Resource
    private AdminUserService userService;

    @Override
    @DataPermission(enable = false) // 忽略数据权限，避免因为过滤，导致无法查询用户。类似：https://github.com/YunaiV/ruoyi-vue-pro/issues/1051
    public AdminUserRespDTO getUser(Long id) {
        AdminUserDO user = userService.getUser(id);
        return BeanUtils.toBean(user, AdminUserRespDTO.class);
    }

    @Override
    @DataPermission(enable = false) // 忽略数据权限，指定手机号的 API 查询用于跨模块数据拼接
    public AdminUserRespDTO getUserByMobile(String mobile) {
        AdminUserDO user = userService.getUserByMobile(mobile);
        return BeanUtils.toBean(user, AdminUserRespDTO.class);
    }

    @Override
    public List<AdminUserRespDTO> getUserList(Collection<Long> ids) {
        return DataPermissionUtils.executeIgnore(() -> { // 禁用数据权限。原因是，一般基于指定 id 的 API 查询，都是数据拼接为主
            List<AdminUserDO> users = userService.getUserList(ids);
            return BeanUtils.toBean(users, AdminUserRespDTO.class);
        });
    }

    @Override
    public List<AdminUserRespDTO> getUserListByNickname(String nickname) {
        List<AdminUserDO> users = userService.getUserListByNickname(nickname);
        return BeanUtils.toBean(users, AdminUserRespDTO.class);
    }

    @Override
    public void validateUserList(Collection<Long> ids) {
        userService.validateUserList(ids);
    }

}
