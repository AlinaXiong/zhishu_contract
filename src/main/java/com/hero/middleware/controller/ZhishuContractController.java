package com.hero.middleware.controller;

import com.alibaba.fastjson.JSONObject;
import com.hero.middleware.common.Result;
import com.hero.middleware.dto.ContractCheckResultDTO;
import com.hero.middleware.dto.ContractSyncDTO;
import com.hero.middleware.service.ContractService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Api(tags = "智书合同同步")
@Slf4j
@RestController
@RequestMapping("/api/zhishu/contract")
public class ZhishuContractController {

    @Autowired
    private ContractService contractService;

    @ApiOperation("智书合同创建同步至业财系统")
    @PostMapping("/sync")
    public Result<Void> syncContractFromZhishu(@RequestBody ContractSyncDTO dto) {
        log.info("接收智书合同创建同步请求: {}", dto);
        contractService.syncContractFromZhishu(dto);
        return Result.success();
    }

    @ApiOperation("智书合同变更同步至业财系统")
    @PutMapping("/sync")
    public Result<Void> updateContractFromZhishu(@RequestBody ContractSyncDTO dto) {
        log.info("接收智书合同变更同步请求: {}", dto);
        contractService.updateContractFromZhishu(dto);
        return Result.success();
    }

    @ApiOperation("智书合同变更履约计划金额分类计算")
    @PostMapping("/calculateAmount")
    public Map<String,Object> calculateAmount(@RequestBody Map<String,Object> paramMap){
        log.info("智书合同变更履约计划金额分类计算-入参: {}", JSONObject.toJSON(paramMap));
        Map<String, Object> orderDetail = contractService.calculateAmount(paramMap);
        log.info("智书合同变更履约计划金额分类计算-返回参数: {}", JSONObject.toJSON(orderDetail));
        return orderDetail;
    }

    @ApiOperation("智书合同-专项分类映射")
    @PostMapping("/specCategoryMapping")
    public Map<String,Object> specCategoryMapping(@RequestBody Map<String,Object> paramMap){
        log.info("智书合同-专项分类映射-入参: {}", JSONObject.toJSON(paramMap));
        Map<String, Object> orderDetail = contractService.specCategoryMapping(paramMap);
        log.info("智书合同-专项分类映射-返回参数: {}", JSONObject.toJSON(orderDetail));
        return orderDetail;
    }

    @ApiOperation("提交校验")
    @PostMapping("/submitCheck")
    public ContractCheckResultDTO submitCheck(@RequestBody Map<String,Object> paramMap){
        log.info("智书合同提交校验-入参: {}", JSONObject.toJSON(paramMap));
        ContractCheckResultDTO checkResultDTO = contractService.submitCheck(paramMap);
        log.info("智书合同提交校验-返回参数: {}", JSONObject.toJSON(checkResultDTO));
        return checkResultDTO;
    }

    @ApiOperation("提交校验交易方")
    @PostMapping("/checkOppositeSide")
    public ContractCheckResultDTO checkOppositeSide(@RequestBody Map<String,Object> paramMap){
        log.info("智书合同提交校验交易方-入参: {}", JSONObject.toJSON(paramMap));
        ContractCheckResultDTO checkResultDTO = contractService.checkOppositeSide(paramMap);
        log.info("智书合同提交校验交易方-返回参数: {}", JSONObject.toJSON(checkResultDTO));
        return checkResultDTO;
    }

}
