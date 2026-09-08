package com.tenderintelligence.module.member.dal.dataobject;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tenderintelligence.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 会员用户 DO
 *
 * 面向用户端门户(user-web),与后台 system_users 账号相互独立
 */
@TableName("member_user")
@KeySequence("member_user_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class MemberUserDO extends BaseDO {

    /**
     * 会员编号
     */
    @TableId
    private Long id;
    /**
     * 账号
     */
    private String username;
    /**
     * 密码(BCrypt 加密)
     */
    private String password;
    /**
     * 昵称
     */
    private String nickname;
    /**
     * 手机号码
     */
    private String mobile;
    /**
     * 头像地址
     */
    private String avatar;
    /**
     * 帐号状态(0 正常 1 停用)
     */
    private Integer status;
    /**
     * 最后登录 IP
     */
    private String loginIp;
    /**
     * 最后登录时间
     */
    private LocalDateTime loginDate;

}
