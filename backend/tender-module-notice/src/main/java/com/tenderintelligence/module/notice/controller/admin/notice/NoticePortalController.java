package com.tenderintelligence.module.notice.controller.admin.notice;

import com.tenderintelligence.framework.common.pojo.CommonResult;
import com.tenderintelligence.framework.common.pojo.PageResult;
import com.tenderintelligence.module.notice.controller.admin.notice.vo.NoticePageReqVO;
import com.tenderintelligence.module.notice.controller.admin.notice.vo.NoticeRespVO;
import com.tenderintelligence.module.notice.dal.dataobject.NoticePortalDO;
import com.tenderintelligence.module.notice.service.NoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

import static com.tenderintelligence.framework.common.pojo.CommonResult.success;
import static com.tenderintelligence.framework.common.util.object.BeanUtils.toBean;

import com.tenderintelligence.framework.tenant.core.aop.TenantIgnore;

/**
 * 招投标公告(标讯) - 用户端门户查询接口,匿名可访问
 */
@Tag(name = "用户端 - 招投标公告")
@RestController
@TenantIgnore
@RequestMapping("/notice")
@Validated
public class NoticePortalController {

    @Resource
    private NoticeService noticeService;

    @GetMapping("/list")
    @PermitAll
    @Operation(summary = "分页查询公告")
    public CommonResult<PageResult<NoticeRespVO>> getNoticePage(@Valid NoticePageReqVO pageReqVO) {
        return success(toBean(noticeService.getNoticePage(pageReqVO), NoticeRespVO.class));
    }

    @GetMapping("/get")
    @PermitAll
    @Operation(summary = "获取公告详情")
    @Parameter(name = "id", description = "公告编号", required = true, example = "1")
    public CommonResult<NoticeRespVO> getNotice(@RequestParam("id") Long id) {
        return success(toBean(noticeService.getNotice(id), NoticeRespVO.class));
    }

    @GetMapping("/stats")
    @PermitAll
    @Operation(summary = "首页统计信息")
    public CommonResult<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("todayCount", noticeService.getTodayCount());
        stats.put("totalCount", noticeService.getTotalCount());
        stats.put("provinces", noticeService.getProvinceCount());
        stats.put("industries", noticeService.getIndustryCount());
        return success(stats);
    }
}
