package com.tenderintelligence.module.bid.dal.mysql;

import com.tenderintelligence.framework.mybatis.core.mapper.BaseMapperX;
import com.tenderintelligence.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.tenderintelligence.module.bid.dal.dataobject.BidProjectDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 投标项目 Mapper
 */
@Mapper
public interface BidProjectMapper extends BaseMapperX<BidProjectDO> {

    default List<BidProjectDO> selectListByUser(Long userId, String status) {
        return selectList(new LambdaQueryWrapperX<BidProjectDO>()
                .eq(BidProjectDO::getUserId, userId)
                .eqIfPresent(BidProjectDO::getStatus, status)
                .orderByDesc(BidProjectDO::getCreateTime));
    }

    /**
     * 查询用户在指定公告下的在办投标项目(防止重复发起)
     */
    default BidProjectDO selectActiveByUserAndNotice(Long userId, Long noticeId) {
        return selectOne(new LambdaQueryWrapperX<BidProjectDO>()
                .eq(BidProjectDO::getUserId, userId)
                .eq(BidProjectDO::getNoticeId, noticeId)
                .in(BidProjectDO::getStatus, "SUBMITTED", "OTB"));
    }

    default Long selectCountByUser(Long userId) {
        return selectCount(new LambdaQueryWrapperX<BidProjectDO>().eq(BidProjectDO::getUserId, userId));
    }
}
