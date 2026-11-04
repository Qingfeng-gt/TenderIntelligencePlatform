package com.tenderintelligence.module.notice.dal.mysql;

import com.tenderintelligence.framework.mybatis.core.mapper.BaseMapperX;
import com.tenderintelligence.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.tenderintelligence.module.notice.dal.dataobject.BaseAreaDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * 行政区划 Mapper
 */
@Mapper
public interface BaseAreaMapper extends BaseMapperX<BaseAreaDO> {

    default List<BaseAreaDO> selectListByCodes(Collection<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<BaseAreaDO>()
                .in(BaseAreaDO::getCode, codes));
    }

    default List<BaseAreaDO> selectProvinces() {
        return selectList(new LambdaQueryWrapperX<BaseAreaDO>()
                .eq(BaseAreaDO::getLevel, 1)
                .eq(BaseAreaDO::getEnabled, true)
                .orderByAsc(BaseAreaDO::getSort)
                .orderByAsc(BaseAreaDO::getCode));
    }

    default BaseAreaDO selectProvinceByName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<BaseAreaDO>()
                .eq(BaseAreaDO::getLevel, 1)
                .and(w -> w.eq(BaseAreaDO::getShortName, name.trim())
                        .or().eq(BaseAreaDO::getName, name.trim()))
                .last("LIMIT 1"));
    }

    default BaseAreaDO selectCityByName(String provinceCode, String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<BaseAreaDO>()
                .eq(BaseAreaDO::getLevel, 2)
                .eq(provinceCode != null && !provinceCode.isBlank(), BaseAreaDO::getParentCode, provinceCode)
                .and(w -> w.eq(BaseAreaDO::getShortName, name.trim())
                        .or().eq(BaseAreaDO::getName, name.trim()))
                .last("LIMIT 1"));
    }

    /**
     * 直辖市/省直辖市级名称按省级再查一次（如「北京」既没有地市级行）
     */
    default BaseAreaDO selectProvinceByCityName(String name) {
        return selectProvinceByName(name);
    }
}
