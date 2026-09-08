package com.tenderintelligence.module.bid.controller.app.bid;

import com.tenderintelligence.framework.common.pojo.CommonResult;
import com.tenderintelligence.framework.tenant.core.aop.TenantIgnore;
import com.tenderintelligence.module.bid.controller.admin.bid.vo.BidProjectCreateReqVO;
import com.tenderintelligence.module.bid.controller.admin.bid.vo.BidProjectRespVO;
import com.tenderintelligence.module.bid.controller.admin.bid.vo.BidProjectStatusReqVO;
import com.tenderintelligence.module.bid.service.BidProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
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
import static com.tenderintelligence.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

/**
 * 投标项目 - 用户端门户接口(需登录)
 *
 * 会员身份从 token 中获取,不允许通过参数伪造 userId
 */
@Tag(name = "用户端 - 招投标")
@RestController
@TenantIgnore
@RequestMapping("/bid-project")
@Validated
public class BidPortalController {

    @Resource
    private BidProjectService bidProjectService;

    @PostMapping("/create")
    @Operation(summary = "用户针对公告发起投标")
    public CommonResult<Long> create(@Valid @RequestBody BidProjectCreateReqVO reqVO) {
        return success(bidProjectService.createBidProject(getLoginUserId(), reqVO));
    }

    @GetMapping("/list")
    @Operation(summary = "查询当前用户投标项目列表")
    @Parameter(name = "status", description = "状态过滤,不传则全部")
    public CommonResult<List<BidProjectRespVO>> getList(
            @RequestParam(value = "status", required = false) String status) {
        return success(toBean(bidProjectService.getBidProjectList(getLoginUserId(), status), BidProjectRespVO.class));
    }

    @GetMapping("/get")
    @Operation(summary = "查询投标项目详情")
    @Parameter(name = "id", description = "投标项目编号", required = true, example = "1024")
    public CommonResult<BidProjectRespVO> get(@RequestParam("id") Long id) {
        return success(toBean(bidProjectService.getBidProject(id, getLoginUserId()), BidProjectRespVO.class));
    }

    @PostMapping("/status")
    @Operation(summary = "投标项目状态流转")
    public CommonResult<Boolean> updateStatus(@Valid @RequestBody BidProjectStatusReqVO reqVO) {
        bidProjectService.updateStatus(reqVO.getId(), reqVO.getStatus(), getLoginUserId());
        return success(true);
    }

}
