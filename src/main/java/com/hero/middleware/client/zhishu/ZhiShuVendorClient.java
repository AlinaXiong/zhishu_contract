package com.hero.middleware.client.zhishu;

import cn.hutool.core.util.URLUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hero.middleware.client.zhishu.request.CreateVendorRequest;
import com.hero.middleware.client.zhishu.request.UpdateFixedExchangeRateRequest;
import com.hero.middleware.client.zhishu.response.CreateVendorResponse;
import com.hero.middleware.client.zhishu.response.QueryAllVendorResponse;
import com.hero.middleware.client.zhishu.response.QueryFixedExchangeRateResponse;
import com.hero.middleware.client.zhishu.response.VendorInfoResponse;
import com.hero.middleware.config.YeCaiDataConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class ZhiShuVendorClient {
    private static final String VENDOR_NOT_FOUND_CODE = "1640032";

    @Autowired
    private ZhishuApiClient zhishuApiClient;
    @Autowired
    private YeCaiDataConfig yeCaiDataConfig;
    //创建交易方
    private static final String CREATE_VENDOR_URL = "/open-apis/mdm/v1/vendors?user_id_type=user_id&user_id=:user_id";
    //修改交易方
    private static final String UPDATE_VENDOR_URL = "/open-apis/mdm/v1/vendors/:vendor_id?user_id_type=user_id&user_id=:user_id";
    //批量获取交易方
    private static final String GET_VENDOR_ALL_URL = "/open-apis/mdm/v1/vendors/list_all?user_id_type=user_id";
    private static final String GET_VENDOR_BY_CODE_URL = "/open-apis/mdm/v1/vendors";
    //获取单个交易方信息
    private static final String GET_VENDOR_V2_URL = "/open-apis/mdm/v1/vendors/v2/:vendor_id?user_id_type=user_id";
    private static final String GET_VENDOR_BY_CERTIFICATION_ID_URL = "/open-apis/mdm/v1/vendors/query_vendors?certification_id=:certification_id&ad_country=CN";
    //查询固定费率
    private static final String FIXED_EXCHANGE_RATE_URL = "/open-apis/mdm/v1/fixed_exchange_rate";

    public CreateVendorResponse createVendor(CreateVendorRequest request) {
        String url = CREATE_VENDOR_URL.replace(":user_id",yeCaiDataConfig.getUserId());
        log.info("创建交易方信息-请求信息：{}", request);
        String response = zhishuApiClient.doPost("创建智书交易方", url, request);
        JSONObject resultRes = JSONObject.parseObject(response);
        log.info("创建交易方信息-返回信息：{}", response);
        String code = resultRes.getString("code");
        if("0".equals(code)){
            return parseResponse(resultRes.getString("data"), CreateVendorResponse.class);
        }else{
            return null;
        }
    }

    public CreateVendorResponse updateVendor(CreateVendorRequest request, String vendorId) {
        String url = UPDATE_VENDOR_URL.replace(":user_id",yeCaiDataConfig.getUserId())
                .replace(":vendor_id",vendorId);
        log.info("修改交易方信息-请求信息：{}", JSONObject.toJSONString(request));
        String response = zhishuApiClient.doPut("更新智书交易方", url, request);
        JSONObject resultRes = JSONObject.parseObject(response);
        log.info("修改交易方信息-返回信息：{}", response);
        String code = resultRes.getString("code");
        if("0".equals(code)){
            return parseResponse(resultRes.getString("data"), CreateVendorResponse.class);
        }else{
            return null;
        }
    }

    public QueryAllVendorResponse getVendorAll(String pageToken, String pageSize) {
        String url = GET_VENDOR_ALL_URL;
        if(pageToken!=null){
            url+="&pageToken="+pageToken;
        }
        if(pageSize!=null){
            url+="&pageSize="+pageSize;
        }
        log.info("获取交易方全部信息-请求信息：{} {}", pageToken, pageSize);
        String response = zhishuApiClient.doGet("查询智书全部交易方", url, null);
        JSONObject resultRes = JSONObject.parseObject(response);
        log.info("获取交易方全部信息-返回信息：{}", response);
        String code = resultRes.getString("code");
        if("0".equals(code)){
            return parseResponse(resultRes.getString("data"), QueryAllVendorResponse.class);
        }else{
            throw new RuntimeException("调用获取交易方全部信息方失败："+response);
        }
    }

    public QueryAllVendorResponse getVendorByCode(String vendorCode) {
        QueryAllVendorResponse emptyResponse = new QueryAllVendorResponse();
        emptyResponse.setItems(Collections.emptyList());
        emptyResponse.setHasMore(false);
        if (vendorCode == null || vendorCode.trim().isEmpty()) {
            log.info("按交易方编码查询交易方信息-交易方编码为空，跳过查询");
            return emptyResponse;
        }

        Map<String, Object> params = new HashMap<>();
        params.put("vendor", vendorCode.trim());
        params.put("page_size", 10);
        params.put("user_id_type", "user_id");
        log.info("按交易方编码查询交易方信息-请求信息：{}", vendorCode);
        String response = zhishuApiClient.doGet("按交易方编码查询智书交易方", GET_VENDOR_BY_CODE_URL, params);
        JSONObject resultRes = JSONObject.parseObject(response);
        log.info("按交易方编码查询交易方信息-返回信息：{}", response);
        String code = resultRes.getString("code");
        if ("0".equals(code)) {
            String data = resultRes.getString("data");
            if (data == null || data.isEmpty()) {
                return emptyResponse;
            }
            QueryAllVendorResponse vendorResponse = parseResponse(data, QueryAllVendorResponse.class);
            if (vendorResponse == null) {
                return emptyResponse;
            }
            if (vendorResponse.getItems() == null) {
                vendorResponse.setItems(Collections.emptyList());
            }
            return vendorResponse;
        }
        if (VENDOR_NOT_FOUND_CODE.equals(code)) {
            log.info("按交易方编码未查询到交易方信息，交易方编码：{}，返回信息：{}", vendorCode, response);
            return emptyResponse;
        }
        throw new RuntimeException("按交易方编码查询交易方接口调用失败：" + response);
    }

    public JSONObject updateFixedExchangeRate(UpdateFixedExchangeRateRequest request){
        log.info("修改固定汇率信息-请求信息：{}", JSONObject.toJSONString(request));
        String response = zhishuApiClient.doPut("更新智书固定汇率", FIXED_EXCHANGE_RATE_URL, request);
        JSONObject resultRes = JSONObject.parseObject(response);
        log.info("修改固定汇率信息-返回信息：{}", response);
        return resultRes;
    }

    public QueryFixedExchangeRateResponse getFixedExchangeRate(Map<String, Object> params) {
        log.info("获取固定汇率信息-请求信息：{}", JSONObject.toJSONString(params));
        String response = zhishuApiClient.doGet("查询智书固定汇率", FIXED_EXCHANGE_RATE_URL, params);
        JSONObject resultRes = JSONObject.parseObject(response);
        log.info("获取固定汇率信息-返回信息：{}", response);
        String code = resultRes.getString("code");
        if("0".equals(code)){
            return parseResponse(resultRes.getString("data"), QueryFixedExchangeRateResponse.class);
        }else{
            throw new RuntimeException("调用获取固定汇率信息方失败："+response);
        }
    }

    public VendorInfoResponse getVendorV2(String id) {
        String url = GET_VENDOR_V2_URL.replace(":vendor_id",id);
        log.info("获取交易方详情信息信息-请求信息：{}", id);
        String response = zhishuApiClient.doGet("查询智书交易方详情", url, null);
        JSONObject resultRes = JSONObject.parseObject(response);
        log.info("获取交易方详情信息信息-返回信息：{}", response);
        String code = resultRes.getString("code");
        if("0".equals(code)){
            return parseResponse(resultRes.getString("data"), VendorInfoResponse.class);
        }else{
            throw new RuntimeException("调用获取交易方详情信息信息失败："+response);
        }
    }


    public VendorInfoResponse getVendorByCertificationId(String certificationId) {
        String url = GET_VENDOR_BY_CERTIFICATION_ID_URL.replace(":certification_id", URLUtil.encode(certificationId));
        log.info("根据证件编码查询交易方信息-请求信息：{}", certificationId);
        String response = zhishuApiClient.doGet("按证件号查询智书交易方", url, null);
        JSONObject resultRes = JSONObject.parseObject(response);
        log.info("根据证件编码查询交易方信息-返回信息：{}", response);
        String code = resultRes.getString("code");
        if("0".equals(code)){
//            JSONObject data = resultRes.getJSONObject("data");
            JSONArray data = resultRes.getJSONArray("data");
//            if (data != null && data.getJSONObject("vendor") != null) {
//                return parseResponse(data.getString("vendor"), VendorInfoResponse.class);
//            }
            if(data!=null&& !data.isEmpty()){
                return parseResponse(data.getString(0),VendorInfoResponse.class);
            }
            log.info("根据证件编码未查询到交易方信息，证件编码：{}", certificationId);
            return null;
        }else{
            return null;
        }
    }

    private <T> T parseResponse(String response, Class<T> clazz) {
        return com.alibaba.fastjson.JSON.parseObject(response, clazz);
    }
}
