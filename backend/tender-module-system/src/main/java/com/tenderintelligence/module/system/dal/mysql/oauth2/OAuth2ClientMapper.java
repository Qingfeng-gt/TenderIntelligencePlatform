package com.tenderintelligence.module.system.dal.mysql.oauth2;

import com.tenderintelligence.framework.mybatis.core.mapper.BaseMapperX;
import com.tenderintelligence.module.system.dal.dataobject.oauth2.OAuth2ClientDO;
import org.apache.ibatis.annotations.Mapper;


/**
 * OAuth2 客户端 Mapper
 *
 * @author Tender Intelligence
 */
@Mapper
public interface OAuth2ClientMapper extends BaseMapperX<OAuth2ClientDO> {

    default OAuth2ClientDO selectByClientId(String clientId) {
        return selectOne(OAuth2ClientDO::getClientId, clientId);
    }

}
