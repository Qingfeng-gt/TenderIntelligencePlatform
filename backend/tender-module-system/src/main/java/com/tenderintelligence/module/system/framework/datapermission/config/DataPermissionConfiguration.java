package com.tenderintelligence.module.system.framework.datapermission.config;

import com.tenderintelligence.module.system.dal.dataobject.user.AdminUserDO;
import com.tenderintelligence.framework.datapermission.core.rule.dept.DeptDataPermissionRuleCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * system 模块的数据权限 Configuration
 *
 * @author Tender Intelligence
 */
@Configuration(proxyBeanMethods = false)
public class DataPermissionConfiguration {

    @Bean
    public DeptDataPermissionRuleCustomizer sysDeptDataPermissionRuleCustomizer() {
        return rule -> {
            // 单租户：数据权限恒为 ALL，仅保留用户表映射以备扩展
            rule.addUserColumn(AdminUserDO.class, "id");
        };
    }

}
