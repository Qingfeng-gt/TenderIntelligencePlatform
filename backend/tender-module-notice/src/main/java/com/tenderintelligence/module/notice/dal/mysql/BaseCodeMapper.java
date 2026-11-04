package com.tenderintelligence.module.notice.dal.mysql;

import com.tenderintelligence.framework.mybatis.core.mapper.BaseMapperX;
import com.tenderintelligence.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.tenderintelligence.module.notice.dal.dataobject.BaseCodeDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 标准编码集 Mapper
 */
@Mapper
public interface BaseCodeMapper extends BaseMapperX<BaseCodeDO> {

    default List<BaseCodeDO> selectListByCodeSet(String codeSet) {
        return selectList(new LambdaQueryWrapperX<BaseCodeDO>()
                .eq(BaseCodeDO::getCodeSet, codeSet)
                .eq(BaseCodeDO::getEnabled, true)
                .orderByAsc(BaseCodeDO::getSort));
    }
}
