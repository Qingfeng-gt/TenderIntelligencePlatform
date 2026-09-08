package com.tenderintelligence.module.crawler.controller.admin.crawler;

import com.tenderintelligence.framework.common.pojo.CommonResult;
import com.tenderintelligence.framework.common.pojo.PageParam;
import com.tenderintelligence.framework.common.pojo.PageResult;
import com.tenderintelligence.module.crawler.controller.admin.crawler.vo.CrawlerSiteRespVO;
import com.tenderintelligence.module.crawler.controller.admin.crawler.vo.CrawlerTaskRespVO;
import com.tenderintelligence.module.crawler.dal.dataobject.CrawlerSiteDO;
import com.tenderintelligence.module.crawler.dal.dataobject.CrawlerTaskDO;
import com.tenderintelligence.module.crawler.service.CrawlService;
import com.tenderintelligence.framework.tenant.core.aop.TenantIgnore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.tenderintelligence.framework.common.pojo.CommonResult.success;
import static com.tenderintelligence.framework.common.util.object.BeanUtils.toBean;

/**
 * 招投标数据采集 - 管理接口(DEMO: PermitAll 匿名可调,正式版需管理员鉴权)
 */
@Tag(name = "数据采集中心")
@RestController
@TenantIgnore
@RequestMapping("/crawler")
@Validated
public class CrawlerController {

    @Resource
    private CrawlService crawlService;

    @PostMapping("/run")
    @PermitAll
    @Operation(summary = "手动触发采集(异步执行,返回任务编号)")
    @Parameter(name = "siteCode", description = "站点标识,缺省采集所有启用站点", example = "ccgp")
    public CommonResult<Long> run(@RequestParam(value = "siteCode", required = false) String siteCode) {
        return success(crawlService.run(siteCode));
    }

    @GetMapping("/site/list")
    @PermitAll
    @Operation(summary = "查询站点配置列表")
    public CommonResult<List<CrawlerSiteRespVO>> getSiteList() {
        return success(toBean(crawlService.getSiteList(), CrawlerSiteRespVO.class));
    }

    @GetMapping("/task/page")
    @PermitAll
    @Operation(summary = "分页查询采集任务日志")
    public CommonResult<PageResult<CrawlerTaskRespVO>> getTaskPage(
            @Validated PageParam pageParam,
            @RequestParam(value = "siteCode", required = false) String siteCode) {
        return success(toBean(crawlService.getTaskPage(pageParam, siteCode), CrawlerTaskRespVO.class));
    }

    @GetMapping("/task/latest")
    @PermitAll
    @Operation(summary = "查询最近一次采集任务")
    @Parameter(name = "siteCode", description = "站点标识,必填", example = "ccgp")
    public CommonResult<CrawlerTaskRespVO> getLatestTask(@RequestParam("siteCode") String siteCode) {
        return success(toBean(crawlService.getLatestTask(siteCode), CrawlerTaskRespVO.class));
    }
}
