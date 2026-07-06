package com.hero.middleware.client.yuecai;

import com.alibaba.fastjson.JSONObject;
import com.hero.middleware.client.yuecai.request.*;
import com.hero.middleware.client.yuecai.response.*;
import com.hero.middleware.common.Result;
import com.hero.middleware.enums.MasterDataTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class YuecaiContractClient {

    @Autowired
    private YuecaiApiClient yuecaiApiClient;
    //同步合同信息
    private static final String SYN_CONTRACT_INFO = "/hitf/v2p/rest/invoke/SFpFUk86SEZJTlMuUFJFOmhmaW5zLmFwaS1jb24tY29udHJhY3QtaGVhZGVyLnN5bmNDb250";
    //获取主数据
    private static final String GET_MASTER_DATA = "/hitf/v2p/rest/invoke/SFpFUk86SEZJTlMuQkFTRTpoZmlucy1iYXNlLm1hc3Rlci1kYXRhLmluY3JlbWVudFF1ZXJ5";
    //获取项目订单信息
    private static final String GET_ORDER_INFO = "/hitf/v2p/rest/invoke/SFpFUk86SEZJTlM6aGZpbnMucHJqLW1kbS5pbmNyZW1lbnRRdWVyeQ==";
    //获取主播卡片
    private static final String GET_ANCHOR_CARD = "/hitf/v2p/rest/invoke/SFpFUk86SEZJTlMuUFJFOmhmaW5zLmdldEV4cFJlcVF1ZXJ5UmVzdWx0";
    //获取采购申请
    private static final String GET_PROCUREMENT_REQ = "/hitf/v2p/rest/invoke/SFpFUk86SEZJTlMuUFJFOmhmaW5zLmFwaS1leHAtcmVxdWlzaXRpb24taGVhZGVyLmdldEV4cFJlcVF1ZXJ5UmVzdWx0";
    //更新主播卡片
    private static final String UPDATE_ANCHOR_CARD = "/hitf/v2p/rest/invoke/SFpFUk86SEZJTlMuUFJFOmhmaW5zLnN5bmNBbmNob3JQcm9maWxlTGlzdExpbmVJbmZv";
    //同步流程
    private static final String SYNC_FLOW_INFO = "/hitf/v2p/rest/invoke/SFpFUk86SEZJTlMuUFJFOmhmaW5zLmFwaS1jb24tY29udHJhY3QtaGVhZGVyLnN5bmNXb3JrZmxvdw==";
    //同步任务
    private static final String SYNC_INSTANCE_INFO = "/hitf/v2p/rest/invoke/SFpFUk86SEZJTlMuUFJFOmhmaW5zLmFwaS1jb24tY29udHJhY3QtaGVhZGVyLnN5bmNJbnN0YW5jZQ==";
    private int reNum = 0;

    public DocumentListResponse getDocumentList(Map<String, Object> params) {
        String response = yuecaiApiClient.doGet("查询业财单据列表", "/api/document/list", params);
        return parseResponse(response, DocumentListResponse.class);
    }

    public YuecaiResponse syncContract(ContSyncRequest request) {
        log.info("同步合同信息-入参：{}", JSONObject.toJSONString(Collections.singletonList(request)));
        String response = yuecaiApiClient.doPost("同步业财合同信息", SYN_CONTRACT_INFO, Arrays.asList(request));
        log.info("同步合同信息-返回参数：{}", response);
        return parseResponse(response, YuecaiResponse.class);
    }

    public YuecaiResponse syncContractStatus(SyncContractStatusRequest request) {
        String response = yuecaiApiClient.doPost("同步业财合同状态", "/api/contract/status/sync", request);
        return parseResponse(response, YuecaiResponse.class);
    }

    public YuecaiResponse updateContract(String contractId, YuecaiUpdateContractRequest request) {
        String response = yuecaiApiClient.doPut("更新业财合同", "/api/contract/" + contractId, request);
        return parseResponse(response, YuecaiResponse.class);
    }

    public MasterDataRes getMasterData(Map<String, Object> params) {
        log.info("开始获取主数据{}信息：{}",params.get("dataType"),JSONObject.toJSONString(params));
//        if(MasterDataTypeEnum.BANK.getCode().equals(params.get("dataType"))){
//            params.put("page", 0);
//            params.put("size", 500);
//        }
        String response = yuecaiApiClient.doGetNoLog("查询业财主数据", GET_MASTER_DATA, params);
        if(!MasterDataTypeEnum.EMPLOYEE.getCode().equals(params.get("dataType"))){
            log.info("获取主数据{}信息：{}",params.get("dataType"),response);
            JSONObject jsonObject = JSONObject.parseObject(response);
            String code = jsonObject.getString("code");
            if("error.permission.accessTokenExpired".equals(code)&&reNum<2){
                reNum++;
                log.info("token失效，重新获取数据信息");
                return getMasterData(params);
            }
        }
        return parseResponse(response, MasterDataRes.class);
    }

    public MasterDataRes getOrderInfo(Map<String, Object> params) {
        log.info("获取订单信息-入参：{}", JSONObject.toJSONString(params));
        String response = yuecaiApiClient.doGet("查询业财订单信息", GET_ORDER_INFO, params);
//        log.info("获取订单信息-返回参数：{}", response);
        return parseResponse(response, MasterDataRes.class);
    }

    public MasterDataRes getAnchorCard(Map<String, Object> params,String documentNumber,String fieldStr) {
        JSONObject body = new JSONObject();
        JSONObject paramObj = new JSONObject();
        paramObj.put("organizationId",0);
        body.put("pathVariableMap",paramObj);
        body.put("payload",new JSONObject());
        if(documentNumber!=null){
            body.put(fieldStr,documentNumber);
        }
        log.info("获取主播卡片-入参：params {} body {}", params, body);
        String response = yuecaiApiClient.doGet("查询业财主播卡片", GET_ANCHOR_CARD, params,body);
        log.info("获取主播卡片-返回参数：{}", response);
        return parseResponse(response, MasterDataRes.class);
    }

    public Map<String,Object> updateAnchorCard(UpdateAnchorCardRequest request) {
        Map<String,Object> result = new HashMap<>();
        log.info("修改主播信息-入参：{}", JSONObject.toJSONString(request));
        String response = yuecaiApiClient.doPost("更新业财主播卡片", UPDATE_ANCHOR_CARD, request);
        log.info("修改主播信息-返回参数：{}", response);
        JSONObject  resultJson = JSONObject.parseObject(response);
        String code = resultJson.getString("code");
        if(code!=null){
            resultJson.getString("message");
            result.put("code","-1");
            result.put("message",resultJson.getString("message"));
        }else{
            result.put("code","0");
            result.put("message",response);
        }
        return result;
    }

    public MasterDataRes getProcurement(Map<String, Object> params,String documentNumber) {
        JSONObject body = new JSONObject();
        JSONObject paramObj = new JSONObject();
        paramObj.put("organizationId",0);
        body.put("pathVariableMap",paramObj);
        body.put("payload",new JSONObject());
        if(documentNumber!=null){
//            body.put("expRequisitionNumber",documentNumber);
            body.put("keyword",documentNumber);
        }
        log.info("获取采购申请信息-入参：params {} body {}", params, body);
        String response = yuecaiApiClient.doGet("查询业财采购申请", GET_PROCUREMENT_REQ, params,body);
        log.info("获取采购申请信息-返回参数：{}", response);
        return parseResponse(response, MasterDataRes.class);
    }

    public String syncFlow(ProcessDefinitionRequest request){
        log.info("开始同步合同流程信息-入参：{}",JSONObject.toJSONString(request));
        String response = yuecaiApiClient.doPost("同步业财合同流程", SYNC_FLOW_INFO, request);
        log.info("同步合同流程信息-返回参数：{}",response);
        return response;
    }

    public String syncInstance(ProcessInstanceRequest request){
        log.info("开始同步合同任务信息-入参：{}",JSONObject.toJSONString(request));
        String response = yuecaiApiClient.doPost("同步业财合同任务", SYNC_INSTANCE_INFO, request);
        log.info("同步合同任务信息-返回参数：{}",response);
        return response;
    }

    private <T> T parseResponse(String response, Class<T> clazz) {
        return com.alibaba.fastjson.JSON.parseObject(response, clazz);
    }

}
