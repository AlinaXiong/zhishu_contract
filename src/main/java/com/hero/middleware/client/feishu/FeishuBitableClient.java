package com.hero.middleware.client.feishu;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonParser;
import com.hero.middleware.config.FeiShuBitableConfig;
import com.lark.oapi.Client;
import com.lark.oapi.core.request.RequestOptions;
import com.lark.oapi.core.utils.Jsons;
import com.lark.oapi.service.bitable.v1.model.*;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class FeishuBitableClient {

    @Autowired
    private FeiShuTokenManager feiShuTokenManager;

    @Autowired
    private FeiShuBitableConfig feiShuBitableConfig;
    private Integer pageSize=20;

    public void atchCreateAppTableRecordSample(JSONObject jsonObject, String tableId) throws Exception {
        batchCreateAppTableRecordSample(jsonObject, getAppToken(), tableId);
    }

    public void batchCreateAppTableRecordSample(JSONObject jsonObject, String appToken, String tableId) throws Exception {
        checkNotBlank(appToken, "feishu.bitable.app-token");
        checkNotBlank(tableId, "tableId");

        Client client = Client.newBuilder("YOUR_APP_ID", "YOUR_APP_SECRET").disableTokenCache().build();
        BatchCreateAppTableRecordReq req = BatchCreateAppTableRecordReq.newBuilder()
                .appToken(appToken)
                .tableId(tableId)
                .batchCreateAppTableRecordReqBody(JSON.parseObject(jsonObject.toJSONString(), BatchCreateAppTableRecordReqBody.class))
                .build();

        BatchCreateAppTableRecordResp resp = client.bitable().v1().appTableRecord().batchCreate(req, RequestOptions.newBuilder()
                .tenantAccessToken(feiShuTokenManager.getAccessToken())
                .build());

        if (!resp.success()) {
            log.error("Feishu bitable batch create failed, code:{}, msg:{}, requestId:{} >>>>> {}", resp.getCode(), resp.getMsg(), resp.getRequestId(),
                    Jsons.createGSON(true, false).toJson(JsonParser.parseString(new String(resp.getRawResponse().getBody(), StandardCharsets.UTF_8))));
            throw new Exception("Feishu bitable batch create failed");
        }
    }

    public void createAppTableRecordSample(JSONObject jsonObject, String tableId) throws Exception {
        createAppTableRecordSample(jsonObject, getAppToken(), tableId);
    }

    public void createAppTableRecordSample(JSONObject jsonObject, String appToken, String tableId) throws Exception {
        checkNotBlank(appToken, "feishu.bitable.app-token");
        checkNotBlank(tableId, "tableId");

        Client client = Client.newBuilder("YOUR_APP_ID", "YOUR_APP_SECRET").disableTokenCache().build();
        CreateAppTableRecordReq req = CreateAppTableRecordReq.newBuilder()
                .appToken(appToken)
                .tableId(tableId)
                .appTableRecord(AppTableRecord.newBuilder()
                        .fields(jsonObject)
                        .build())
                .build();

        CreateAppTableRecordResp resp = client.bitable().v1().appTableRecord().create(req, RequestOptions.newBuilder()
                .tenantAccessToken(feiShuTokenManager.getAccessToken())
                .build());

        if (!resp.success()) {
            log.error("Feishu bitable create failed, code:{}, msg:{}, requestId:{} >>>>> {}", resp.getCode(), resp.getMsg(), resp.getRequestId(),
                    Jsons.createGSON(true, false).toJson(JsonParser.parseString(new String(resp.getRawResponse().getBody(), StandardCharsets.UTF_8))));
            throw new Exception("Feishu bitable create failed");
        }
    }

    /**
     * 获取条目码
     * @param jsonObject 查询数据
     * @param tableId 表单码
     * @return
     * @throws Exception
     */
    public String searchRecordId(JSONObject jsonObject, String tableId) throws Exception {
        AppTableRecord[] records= searchAppTableRecordSample(jsonObject, getAppToken(), tableId,pageSize);
        String recordId = "";
        for (AppTableRecord item : records) {
            recordId = item.getRecordId();
        }
        return recordId;
    }
    public AppTableRecord[] searchAppTableRecordSample(JSONObject jsonObject, String tableId) throws Exception {
        return searchAppTableRecordSample(jsonObject, getAppToken(), tableId,pageSize);
    }
    public AppTableRecord[] searchAppTableRecordSample(JSONObject jsonObject, String appToken, String tableId,Integer pageSize) throws Exception {
        // 构建client
        Client client = Client.newBuilder("YOUR_APP_ID", "YOUR_APP_SECRET").disableTokenCache().build();
        SearchAppTableRecordReq req = null;
        // 创建请求对象
        if(jsonObject==null){
            req = SearchAppTableRecordReq.newBuilder()
                    .appToken(appToken)
                    .tableId(tableId)
                    .pageSize(pageSize)
                    .searchAppTableRecordReqBody(SearchAppTableRecordReqBody.newBuilder()
                            .build())
                    .build();
        }else{
            req = SearchAppTableRecordReq.newBuilder()
                    .appToken(appToken)
                    .tableId(tableId)
                    .pageSize(pageSize)
                    .searchAppTableRecordReqBody(SearchAppTableRecordReqBody.newBuilder()
                            .filter(JSONObject.parseObject(jsonObject.toJSONString(),FilterInfo.class))
                            .build())
                    .build();
        }

        // 发起请求
        SearchAppTableRecordResp resp = client.bitable().v1().appTableRecord().search(req, RequestOptions.newBuilder()
                .tenantAccessToken(feiShuTokenManager.getAccessToken())
                .build());

        // 处理服务端错误
        if (!resp.success()) {
            log.error("Feishu bitable search failed, code:{}, msg:{}, requestId:{} >>>>> {}", resp.getCode(), resp.getMsg(), resp.getRequestId(),
                    Jsons.createGSON(true, false).toJson(JsonParser.parseString(new String(resp.getRawResponse().getBody(), StandardCharsets.UTF_8))));
            throw new Exception("Feishu bitable search failed");
        }

        return resp.getData().getItems();
    }


    public void updateAppTableRecordSample(JSONObject jsonObject, String tableId, String recordId) throws Exception {
        updateAppTableRecordSample(jsonObject, getAppToken(), tableId, recordId);
    }
    public void updateAppTableRecordSample(JSONObject jsonObject, String appToken, String tableId, String recordId) throws Exception {

        // 构建client
        Client client = Client.newBuilder("YOUR_APP_ID", "YOUR_APP_SECRET").disableTokenCache().build();

        // 创建请求对象
        UpdateAppTableRecordReq req = UpdateAppTableRecordReq.newBuilder()
                .appToken(appToken)
                .tableId(tableId)
                .recordId(recordId)
                .appTableRecord(AppTableRecord.newBuilder()
                        .fields(jsonObject)
                        .build())
                .build();

        // 发起请求
        UpdateAppTableRecordResp resp = client.bitable().v1().appTableRecord().update(req, RequestOptions.newBuilder()
                .tenantAccessToken(feiShuTokenManager.getAccessToken())
                .build());

        // 处理服务端错误
        if (!resp.success()) {
            log.error("Feishu bitable update failed, code:{}, msg:{}, requestId:{} >>>>> {}", resp.getCode(), resp.getMsg(), resp.getRequestId(),
                    Jsons.createGSON(true, false).toJson(JsonParser.parseString(new String(resp.getRawResponse().getBody(), StandardCharsets.UTF_8))));
            throw new Exception("Feishu bitable update failed");
        }
    }

    private String getAppToken() {
        return feiShuBitableConfig.getAppToken();
    }

    private void checkNotBlank(String value, String propertyName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException(propertyName + " is not configured");
        }
    }
}
