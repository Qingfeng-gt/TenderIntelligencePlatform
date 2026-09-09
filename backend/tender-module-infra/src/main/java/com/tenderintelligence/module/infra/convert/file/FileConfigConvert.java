package com.tenderintelligence.module.infra.convert.file;

import com.tenderintelligence.module.infra.controller.admin.file.vo.config.FileConfigSaveReqVO;
import com.tenderintelligence.module.infra.dal.dataobject.file.FileConfigDO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * 文件配置 Convert
 *
 * @author Tender Intelligence
 */
@Mapper
public interface FileConfigConvert {

    FileConfigConvert INSTANCE = Mappers.getMapper(FileConfigConvert.class);

    @Mapping(target = "config", ignore = true)
    FileConfigDO convert(FileConfigSaveReqVO bean);

}
