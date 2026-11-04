package com.tenderintelligence.module.notice.controller.admin.notice;

import com.tenderintelligence.framework.common.pojo.CommonResult;
import com.tenderintelligence.framework.common.pojo.PageResult;
import com.tenderintelligence.framework.tenant.core.aop.TenantIgnore;
import com.tenderintelligence.module.notice.controller.admin.notice.vo.NoticePageReqVO;
import com.tenderintelligence.module.notice.controller.admin.notice.vo.NoticeRespVO;
import com.tenderintelligence.module.notice.controller.admin.notice.vo.NoticeStatsRespVO;
import com.tenderintelligence.module.notice.controller.admin.notice.vo.ProjectDetailRespVO;
import com.tenderintelligence.module.notice.controller.admin.notice.vo.ProjectPageReqVO;
import com.tenderintelligence.module.notice.controller.admin.notice.vo.ProjectRespVO;
import com.tenderintelligence.module.notice.controller.admin.notice.vo.RegionRespVO;
import com.tenderintelligence.module.notice.service.NoticeService;
import com.tenderintelligence.module.notice.service.ProjectService;
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

import java.util.List;

import static com.tenderintelligence.framework.common.pojo.CommonResult.success;

/**
 * 招投标公告（标讯）用户端门户查询接口，匿名可访问
 *
 * <p>读路径已整体切到 P0 四层模型：{@code notice} 新列 + {@code project} /
 * {@code tender_project} / {@code tender_lot} / {@code enterprise} / {@code base_area}。
 * 旧扁平接口（/list、/get、/stats）不再保留。
 */
@Tag(name = "用户端 - 招投标公告")
@RestController
@TenantIgnore
@RequestMapping("/notice")
@Validated
public class NoticePortalController {

    @Resource
    private NoticeService noticeService;
    @Resource
    private ProjectService projectService;

    @GetMapping("/page")
    @PermitAll
    @Operation(summary = "分页查询公告（四层模型）")
    public CommonResult<PageResult<NoticeRespVO>> getNoticePage(@Valid NoticePageReqVO pageReqVO) {
        return success(noticeService.getNoticePage(pageReqVO));
    }

    @GetMapping("/detail")
    @PermitAll
    @Operation(summary = "获取公告详情（含附件、澄清修改、关联企业）")
    @Parameter(name = "id", description = "公告编号", required = true, example = "1")
    public CommonResult<NoticeRespVO> getNoticeDetail(@RequestParam("id") Long id) {
        return success(noticeService.getNoticeDetail(id));
    }

    @GetMapping("/overview")
    @PermitAll
    @Operation(summary = "首页统计信息")
    public CommonResult<NoticeStatsRespVO> getOverview() {
        return success(noticeService.getStats());
    }

    @GetMapping("/provinces")
    @PermitAll
    @Operation(summary = "省级行政区划选项")
    public CommonResult<List<RegionRespVO>> getProvinces() {
        return success(noticeService.getProvinces());
    }

    @GetMapping("/industries")
    @PermitAll
    @Operation(summary = "行业选项")
    public CommonResult<List<String>> getIndustries() {
        return success(noticeService.getIndustries());
    }

    @GetMapping("/project/page")
    @PermitAll
    @Operation(summary = "分页查询项目（project-centric）")
    public CommonResult<PageResult<ProjectRespVO>> getProjectPage(@Valid ProjectPageReqVO pageReqVO) {
        return success(projectService.getProjectPage(pageReqVO));
    }

    @GetMapping("/project/detail")
    @PermitAll
    @Operation(summary = "获取项目详情（含招标项目、标段、公告、企业）")
    @Parameter(name = "id", description = "项目编号", required = true, example = "1")
    public CommonResult<ProjectDetailRespVO> getProjectDetail(@RequestParam("id") Long id) {
        return success(projectService.getProjectDetail(id));
    }
}
