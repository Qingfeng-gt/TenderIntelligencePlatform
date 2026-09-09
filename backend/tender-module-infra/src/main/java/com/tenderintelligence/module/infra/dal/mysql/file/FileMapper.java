package com.tenderintelligence.module.infra.dal.mysql.file;

import com.tenderintelligence.framework.common.pojo.PageResult;
import com.tenderintelligence.framework.mybatis.core.mapper.BaseMapperX;
import com.tenderintelligence.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.tenderintelligence.module.infra.controller.admin.file.vo.file.FilePageReqVO;
import com.tenderintelligence.module.infra.dal.dataobject.file.FileDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件操作 Mapper
 *
 * @author Tender Intelligence
 */
@Mapper
public interface FileMapper extends BaseMapperX<FileDO> {

    default PageResult<FileDO> selectPage(FilePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<FileDO>()
                .likeIfPresent(FileDO::getPath, reqVO.getPath())
                .likeIfPresent(FileDO::getType, reqVO.getType())
                .betweenIfPresent(FileDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(FileDO::getId));
    }

    default FileDO selectLatestByConfigIdAndPath(Long configId, String path) {
        return selectLastOne(new LambdaQueryWrapperX<FileDO>()
                .eq(FileDO::getConfigId, configId)
                .eq(FileDO::getPath, path)
                .orderByAsc(FileDO::getId));
    }

}
