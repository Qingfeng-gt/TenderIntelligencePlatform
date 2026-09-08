package com.tenderintelligence.module.bid.controller.admin.bid;

import com.tenderintelligence.framework.common.pojo.CommonResult;
import com.tenderintelligence.module.bid.controller.admin.bid.vo.BidProjectCreateReqVO;
import com.tenderintelligence.module.bid.controller.admin.bid.vo.BidProjectRespVO;
import com.tenderintelligence.module.bid.controller.admin.bid.vo.BidProjectStatusReqVO;
import com.tenderintelligence.module.bid.service.BidProjectService;
import com.tenderintelligence.framework.tenant.core.aop.TenantIgnore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.tenderintelligence.framework.common.pojo.CommonResult.success;
import static com.tenderintelligence.framework.common.util.object.BeanUtils.toBean;

/**
 * 投标项目 - 管理后台接口(需登录,userId 由请求参数指定)
 *
 * 正式用户端走 {@link com.tenderintelligence.module.bid.controller.app.bid.BidPortalController},身份取 token
 */
@Tag(name = "管理后台 - 招投标")
@RestController
@TenantIgnore
@RequestMapping("/bid-project")
@Validated
public class BidProjectController {

    @Resource
    private BidProjectService bidProjectService;

    @PostMapping("/create")
    @Operation(summary = "针对公告发起投标")
    public CommonResult<Long> create(@RequestParam("userId") Long userId,
                                     @Valid @RequestBody BidProjectCreateReqVO reqVO) {
        return success(bidProjectService.createBidProject(userId, reqVO));
    }

    @GetMapping("/list")
    @Operation(summary = "查询投标项目列表")
    @Parameter(name = "userId", description = "用户编号", required = true, example = "1")
    public CommonResult<List<BidProjectRespVO>> getList(
            @RequestParam("userId") Long userId,
            @RequestParam(value = "status", required = false) String status) {
        return success(toBean(bidProjectService.getBidProjectList(userId, status), BidProjectRespVO.class));
    }

    @GetMapping("/get")
    @Operation(summary = "查询投标项目详情")
    @Parameter(name = "id", description = "投标项目编号", required = true, example = "1024")
    public CommonResult<BidProjectRespVO> get(@RequestParam("id") Long id,
                                              @RequestParam("userId") Long userId) {
        return success(toBean(bidProjectService.getBidProject(id, userId), BidProjectRespVO.class));
    }

    @PostMapping("/status")
    @Operation(summary = "投标项目状态流转")
    public CommonResult<Boolean> updateStatus(@RequestParam("userId") Long userId,
                                              @Valid @RequestBody BidProjectStatusReqVO reqVO) {
        bidProjectService.updateStatus(reqVO.getId(), reqVO.getStatus(), userId);
        return success(true);
    }
}
