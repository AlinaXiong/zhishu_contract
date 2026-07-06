package com.hero.middleware.client.yuecai;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hero.middleware.config.YuecaiApiConfig;
import com.hero.middleware.dto.ApiLogEvent;
import com.hero.middleware.service.ApiLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

@Slf4j
@Component
public class YuecaiApiClient {

    @Autowired
    private YuecaiApiConfig yuecaiApiConfig;
    @Autowired
    private YuecaiApiClient yuecaiApiClient;

    @Autowired
    private ApiLogService apiLogService;

    private static String ACCESS_TOKEN = null;

    private Map<String,String> getHeader(){
//        if(ACCESS_TOKEN==null){
//        }
        ACCESS_TOKEN = yuecaiApiClient.getAccessToken();
        Map<String,String> header = new HashMap<>();
        header.put("Content-Type","application/json");
        header.put("Authorization",ACCESS_TOKEN);
        return header;
    }

    public String getAccessToken(){
//        String response = yuecaiApiClient.doPost("/oauth/oauth/token", null);
        String url = yuecaiApiConfig.getBaseUrl() + "/oauth/oauth/token";
        Map<String, Object> params = new HashMap<>();
        params.put("grant_type",yuecaiApiConfig.getGrantType());
        params.put("client_id",yuecaiApiConfig.getClientId());
        params.put("client_secret",yuecaiApiConfig.getClientSecret());
        try {
            HttpResponse response = HttpRequest.post(url)
                    .form(params)
                    .timeout(yuecaiApiConfig.getTimeout())
                    .execute();
//            log.info("业财API响应: {}", response.body());
            Map map = JSONObject.parseObject(response.body(), Map.class);
            return String.valueOf(map.get("access_token"));
        } catch (Exception e) {
            log.error("业财API请求异常: {}", e.getMessage(), e);
            throw new RuntimeException("业财API请求失败: " + e.getMessage());
        }
    }

    public String doGet(String path, Map<String, Object> params) {
        return doGet("业财接口调用", path, params);
    }

    public String doGet(String action, String path, Map<String, Object> params) {
        String url = yuecaiApiConfig.getBaseUrl() + path;
        return executeAndRecord(action, "GET", url, JSON.toJSONString(params), () ->
                HttpRequest.get(url)
                    .addHeaders(getHeader())
                    .form(params)
                    .timeout(yuecaiApiConfig.getTimeout())
                    .execute()
        );
    }

    public String doGetNoLog(String path, Map<String, Object> params) {
        return doGetNoLog("业财接口调用", path, params);
    }

    public String doGetNoLog(String action, String path, Map<String, Object> params) {
        String url = yuecaiApiConfig.getBaseUrl() + path;
        return executeWithoutRecord(action, "GET", url, () ->
                HttpRequest.get(url)
                    .addHeaders(getHeader())
                    .form(params)
                    .timeout(yuecaiApiConfig.getTimeout())
                    .execute()
        );
    }

    private String executeWithoutRecord(String action,
                                        String httpMethod,
                                        String url,
                                        Supplier<HttpResponse> requestSupplier) {
        try {
            return requestSupplier.get().body();
        } catch (Exception e) {
            log.error("业财API {}执行异常，方法={}，URL={}，错误={}",
                    action, httpMethod, url, e.getMessage(), e);
            throw new RuntimeException("业财API请求失败: " + e.getMessage(), e);
        }
    }

    public String doGet(String path, Map<String, Object> params, Object body) {
        return doGet("业财接口调用", path, params, body);
    }

    public String doGet(String action, String path, Map<String, Object> params, Object body) {
        String url = yuecaiApiConfig.getBaseUrl() + path;
        if(params!=null&&params.get("size")!=null&&params.get("page")!=null){
            url = yuecaiApiConfig.getBaseUrl() + path + "?size=" + params.get("size").toString()+"&page=" + params.get("page").toString();
        }
        String finalUrl = url;
        JSONObject requestParams = new JSONObject();
        requestParams.put("query", params);
        requestParams.put("body", body);
        return executeAndRecord(action, "GET", finalUrl, requestParams.toJSONString(), () ->
                HttpRequest.get(finalUrl)
                    .addHeaders(getHeader())
                    .form(params)
                    .body(JSON.toJSONString(body))
                    .timeout(yuecaiApiConfig.getTimeout())
                    .execute()
        );
    }

    public String doPost(String path, Object body) {
        return doPost("业财接口调用", path, body);
    }

    public String doPost(String action, String path, Object body) {
        String url = yuecaiApiConfig.getBaseUrl() + path;
        String bodyJson = JSON.toJSONString(body);
        return executeAndRecord(action, "POST", url, bodyJson, () ->
                HttpRequest.post(url)
                    .addHeaders(getHeader())
                    .body(bodyJson)
                    .timeout(yuecaiApiConfig.getTimeout())
                    .execute()
        );
    }

    public String doPut(String path, Object body) {
        return doPut("业财接口调用", path, body);
    }

    public String doPut(String action, String path, Object body) {
        String url = yuecaiApiConfig.getBaseUrl() + path;
        String bodyJson = JSON.toJSONString(body);
        return executeAndRecord(action, "PUT", url, bodyJson, () ->
                HttpRequest.put(url)
                    .addHeaders(getHeader())
                    .body(bodyJson)
                    .timeout(yuecaiApiConfig.getTimeout())
                    .execute()
        );
    }

    private String executeAndRecord(String action,
                                    String httpMethod,
                                    String url,
                                    String requestParams,
                                    Supplier<HttpResponse> requestSupplier) {
        long startTime = System.currentTimeMillis();
        Integer httpStatus = null;
        String responseBody = null;
        String exceptionMessage = null;
        log.info("业财API {}请求: {}", httpMethod, url);
        log.debug("业财API {}请求参数: {}", httpMethod, requestParams);
        try {
            HttpResponse response = requestSupplier.get();
            httpStatus = response.getStatus();
            responseBody = response.body();
            log.info("业财API响应状态码: {}", httpStatus);
            log.debug("业财API响应: {}", responseBody);
            return responseBody;
        } catch (Exception e) {
            exceptionMessage = e.getMessage();
            log.error("业财API {}请求异常, URL: {}, 错误: {}", httpMethod, url, e.getMessage(), e);
            throw new RuntimeException("业财API请求失败: " + e.getMessage(), e);
        } finally {
            try {
                apiLogService.record(ApiLogEvent.builder()
                        .targetSystem("业财")
                        .action(action)
                        .httpMethod(httpMethod)
                        .url(url)
                        .requestParams(requestParams)
                        .responseBody(responseBody)
                        .httpStatus(httpStatus)
                        .exceptionMessage(exceptionMessage)
                        .startTime(startTime)
                        .durationMs(System.currentTimeMillis() - startTime)
                        .build());
            } catch (Exception e) {
                log.error("提交业财API调用日志失败, URL: {}", url, e);
            }
        }
    }

}
