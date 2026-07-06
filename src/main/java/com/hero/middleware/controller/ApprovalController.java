package com.hero.middleware.controller;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hero.middleware.annotation.ZhishuEventLog;
import com.hero.middleware.client.yuecai.request.ApprovalCallbackRequest;
import com.hero.middleware.client.zhishu.request.BaseEventRequest;
import com.hero.middleware.client.zhishu.request.ContractEventRequest;
import com.hero.middleware.common.Result;
import com.hero.middleware.dto.BatchApprovalDTO;
import com.hero.middleware.dto.BatchApprovalResultDTO;
import com.hero.middleware.dto.ContractSyncDTO;
import com.hero.middleware.service.ApprovalService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Api(tags = "合同审批")
@Slf4j
@RestController
@RequestMapping("/api/approval")
public class ApprovalController {

    @Autowired
    private ApprovalService approvalService;

    @ApiOperation("合同批量审批")
    @PostMapping("/batch")
    public Result<BatchApprovalResultDTO> batchApproval(@Validated @RequestBody BatchApprovalDTO dto) {
        log.info("接收批量审批请求: {}", dto);
        BatchApprovalResultDTO result = approvalService.batchApproval(dto);
        return Result.success(result);
    }

    @PostMapping("/event")
    @ZhishuEventLog
    public JSONObject callback(@RequestBody String json) {
        log.info("审批事件监听入参：{}", json);
        BaseEventRequest dto = JSONUtil.toBean(json, BaseEventRequest.class);
        approvalService.callback(dto);
        return JSONUtil.parseObj(json);
    }

    @PostMapping("/approvalCallBack")
    public JSONObject approvalCallBack(@RequestBody String json) {
        log.info("审批事件回调监听入参：{}", json);
        ApprovalCallbackRequest request = JSONUtil.toBean(json, ApprovalCallbackRequest.class);
        approvalService.approvalCallBack(request);
        return JSONUtil.parseObj(json);
    }
}
