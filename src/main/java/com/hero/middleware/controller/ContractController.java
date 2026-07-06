package com.hero.middleware.controller;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.hero.middleware.annotation.ApiLog;
import com.hero.middleware.annotation.ZhishuEventLog;
import com.hero.middleware.client.zhishu.request.BaseEventRequest;
import com.hero.middleware.client.zhishu.request.ContractEventRequest;
import com.hero.middleware.common.Result;
import com.hero.middleware.dto.ContractSyncDTO;
import com.hero.middleware.dto.CreateContractDTO;
import com.hero.middleware.dto.CreateContractResultDTO;
import com.hero.middleware.service.ApprovalService;
import com.hero.middleware.service.ContractService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Api(tags = "合同管理")
@Slf4j
@RestController
@RequestMapping("/api/contract")
public class ContractController {

    @Autowired
    private ContractService contractService;

    @Autowired
    private ApprovalService approvalService;

    @ApiOperation("创建合同接口 - 业财系统创建单据后调用，返回智书合同草稿页链接")
    @ApiLog(value = "创建合同", description = "业财系统创建单据后调用，返回智书合同草稿页链接")
    @PostMapping("/create")
    public Result<CreateContractResultDTO> createContract(@Validated @RequestBody CreateContractDTO dto) {
        log.info("接收业财系统创建合同请求, 单据编号: {}, 单据类型: {}, 创建人ID: {}",
                dto.getDocumentNumber(), dto.getDocumentType(), dto.getCreateUserId());
        CreateContractResultDTO result = contractService.createContract(dto);
        log.info("合同创建成功, 返回草稿页链接: {}", result.getDraftUrl());
        if(result.getErrMessage()!=null&& !result.getErrMessage().isEmpty()){
            return Result.error(result.getErrMessage());
        }else{
            return Result.success("合同创建成功", result);
        }
    }

    @PostMapping("/event")
    @ZhishuEventLog
    public JSONObject callback(@RequestBody String json) {
        BaseEventRequest dto = JSONUtil.toBean(json, BaseEventRequest.class);
        ContractSyncDTO syncDTO = new ContractSyncDTO();
        ContractEventRequest event = JSONUtil.toBean(dto.getEvent().toString(), ContractEventRequest.class);
        if(event!=null&&event.getContractId()!=null&&!event.getContractId().isEmpty()){
            log.info("监听智书合同状态变更同步业财-入参：{}",json);
        }
        syncDTO.setContractId(event.getContractId());
        try{
            contractService.syncContractFromZhishu(syncDTO);
        }catch (Exception e){
            contractService.saveSyncLog(event.getContractId(), "SYNC", "ZHISHU_TO_YUECAI", "FAIL",
                    JSON.toJSONString(dto), JSON.toJSONString(e.getMessage()), null);
        }
//        approvalService.callback(dto);
        return JSONUtil.parseObj(json);
    }

    @PostMapping("/getTaxItem")
    public Map<String,Object> getTaxItem(@RequestBody Map<String,Object> paramMap){
        log.info("获取多维表格-税率税目信息-入参: {}", com.alibaba.fastjson.JSONObject.toJSON(paramMap));
        Map<String, Object> taxItem = null;
        try {
            taxItem = contractService.getTaxData(paramMap);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        log.info("获取多维表格-税率税目信息-返回参数: {}", com.alibaba.fastjson.JSONObject.toJSON(taxItem));
        return taxItem;
    }
}
