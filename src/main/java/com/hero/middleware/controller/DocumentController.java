package com.hero.middleware.controller;

import com.alibaba.fastjson.JSONObject;
import com.hero.middleware.client.yuecai.response.DocumentListResponse;
import com.hero.middleware.client.zhishu.request.PrecedingDocRequest;
import com.hero.middleware.client.zhishu.response.ResultResponse;
import com.hero.middleware.common.Result;
import com.hero.middleware.dto.DocumentQueryDTO;
import com.hero.middleware.service.DocumentService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Api(tags = "单据管理")
@Slf4j
@RestController
@RequestMapping("/api/document")
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    @ApiOperation("关联前置单据接口")
    @GetMapping("/list")
    public Result<DocumentListResponse> getDocumentList(DocumentQueryDTO dto) {
        log.info("获取业财系统前置单据列表: {}", dto);
        DocumentListResponse response = documentService.getDocumentList(dto);
        return Result.success(response);
    }

    @ApiOperation("关联前置单据接口-主播卡片")
    @PostMapping("/getAnchorCardInfo")
    public ResultResponse getAnchorCardInfo(@RequestBody PrecedingDocRequest request) {
        log.info("获取业财系统前置单据-主播卡片列表-入参: {}", JSONObject.toJSON(request));
        ResultResponse anchorCardInfo = documentService.getAnchorCardInfo(request);
        log.info("获取业财系统前置单据-主播卡片列表-返回参数: {}", JSONObject.toJSON(anchorCardInfo));
        return anchorCardInfo;
    }

    @ApiOperation("关联前置单据接口-采购申请信息")
    @PostMapping("/getProcurementInfo")
    public ResultResponse getProcurementInfo(@RequestBody PrecedingDocRequest request) {
        log.info("获取业财系统前置单据-采购申请列表-入参: {}", JSONObject.toJSON(request));
        ResultResponse procurementInfo = documentService.getProcurementInfo(request);
        log.info("获取业财系统前置单据-采购申请列表-返回参数: {}", JSONObject.toJSON(procurementInfo));
        return procurementInfo;
    }

    @ApiOperation("关联前置单据接口-订单信息")
    @PostMapping("/getOrderInfo")
    public ResultResponse getOrderInfo(@RequestBody PrecedingDocRequest request) {
        log.info("获取业财系统前置单据-订单信息列表-入参: {}", JSONObject.toJSON(request));
        ResultResponse orderInfo = documentService.getOrderInfo(request);
        log.info("获取业财系统前置单据-订单信息列表-返回参数: {}", JSONObject.toJSON(orderInfo));
        return orderInfo;
    }

    @ApiOperation("关联前置单据接口-订单详情信息")
    @PostMapping("/getOrderDetail")
    public Map<String,Object> getOrderDetail(@RequestBody Map<String,Object> paramMap){
        log.info("获取业财系统前置单据-订单详情信息-入参: {}", JSONObject.toJSON(paramMap));
        Map<String, Object> orderDetail = documentService.getOrderDetail(paramMap);
        log.info("获取业财系统前置单据-订单详情信息-返回参数: {}", JSONObject.toJSON(orderDetail));
        return orderDetail;
    }

    @ApiOperation("关联前置单据接口-主播卡片详情信息")
    @PostMapping("/getAnchorCardDetail")
    public Map<String,Object> getAnchorCardDetail(@RequestBody Map<String,Object> paramMap){
        log.info("获取业财系统前置单据-主播卡片详情信息-入参: {}", JSONObject.toJSON(paramMap));
        Map<String, Object> anchorCardDetail = documentService.getAnchorCardDetail(paramMap);
        log.info("获取业财系统前置单据-主播卡片详情信息-返回参数: {}", JSONObject.toJSON(anchorCardDetail));
        return anchorCardDetail;
    }

    @ApiOperation("关联前置单据接口-采购申请详情信息")
    @PostMapping("/getProcurementDetail")
    public Map<String,Object> getProcurementDetail(@RequestBody Map<String,Object> paramMap){
        log.info("获取业财系统前置单据-采购申请详情信息-入参: {}", JSONObject.toJSON(paramMap));
        Map<String, Object> procurementDetail = documentService.getProcurementDetail(paramMap);
        log.info("获取业财系统前置单据-采购申请详情信息-返回参数: {}", JSONObject.toJSON(procurementDetail));
        return procurementDetail;
    }

    @ApiOperation("关联前置单据接口-专项分类下拉选项")
    @PostMapping("/getspecCategoryList")
    public Map<String,Object> getspecCategoryList(@RequestBody Map<String,Object> paramMap){
        log.info("获取业财系统前置单据-专项分类下拉选项-入参: {}", JSONObject.toJSON(paramMap));
        Map<String, Object> specCategoryList = documentService.getspecCategoryList(paramMap);
        log.info("获取业财系统前置单据-专项分类下拉选项-返回参数: {}", JSONObject.toJSON(specCategoryList));
        return specCategoryList;
    }
}
