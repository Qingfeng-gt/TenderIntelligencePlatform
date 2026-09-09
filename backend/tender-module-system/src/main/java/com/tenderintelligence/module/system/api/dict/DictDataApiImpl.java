package com.tenderintelligence.module.system.api.dict;

import com.tenderintelligence.framework.common.util.object.BeanUtils;
import com.tenderintelligence.framework.common.biz.system.dict.dto.DictDataRespDTO;
import com.tenderintelligence.module.system.dal.dataobject.dict.DictDataDO;
import com.tenderintelligence.module.system.service.dict.DictDataService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * 字典数据 API 实现类
 *
 * @author Tender Intelligence
 */
@Service
public class DictDataApiImpl implements DictDataApi {

    @Resource
    private DictDataService dictDataService;

    @Override
    public void validateDictDataList(String dictType, Collection<String> values) {
        dictDataService.validateDictDataList(dictType, values);
    }

    @Override
    public List<DictDataRespDTO> getDictDataList(String dictType) {
        List<DictDataDO> list = dictDataService.getDictDataListByDictType(dictType);
        return BeanUtils.toBean(list, DictDataRespDTO.class);
    }

}
