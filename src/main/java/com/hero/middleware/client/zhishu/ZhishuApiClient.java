package com.hero.middleware.client.zhishu;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hero.middleware.config.ZhishuApiConfig;
import com.hero.middleware.dto.ApiLogEvent;
import com.hero.middleware.service.ApiLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Map;
import java.util.function.Supplier;

@Slf4j
@Component
public class ZhishuApiClient {

    @Autowired
    private ZhishuApiConfig zhishuApiConfig;

    @Autowired
    private ZhishuTokenManager zhishuTokenManager;

    @Autowired
    private ApiLogService apiLogService;

    public String doGet(String path, Map<String, Object> params) {
        return doGet("智书接口调用", path, params);
    }

    public String doGet(String action, String path, Map<String, Object> params) {
        String url = zhishuApiConfig.getBaseUrl() + path;
        return executeAndRecord(action, "GET", url, JSON.toJSONString(params), () -> {
            String token = zhishuTokenManager.getAccessToken();
            return HttpRequest.get(url)
                    .header("Authorization", "Bearer " + token)
                    .form(params)
                    .timeout(zhishuApiConfig.getTimeout())
                    .execute();
        });
    }

    public String doPost(String path, Object body) {
        return doPost("智书接口调用", path, body);
    }

    public String doPost(String action, String path, Object body) {
        String url = zhishuApiConfig.getBaseUrl() + path;
        String bodyJson = JSON.toJSONString(body);
        return executeAndRecord(action, "POST", url, bodyJson, () -> {
            String token = zhishuTokenManager.getAccessToken();
            return HttpRequest.post(url)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .body(bodyJson)
                    .timeout(zhishuApiConfig.getTimeout())
                    .execute();
        });
    }

    public String doPostWithoutBody(String action, String path) {
        String url = zhishuApiConfig.getBaseUrl() + path;
        return executeAndRecord(action, "POST", url, "", () -> {
            String token = zhishuTokenManager.getAccessToken();
            return HttpRequest.post(url)
                    .header("Authorization", "Bearer " + token)
                    .timeout(zhishuApiConfig.getTimeout())
                    .execute();
        });
    }

    public String doDelete(String action, String path) {
        String url = zhishuApiConfig.getBaseUrl() + path;
        return executeAndRecord(action, "DELETE", url, "", () -> {
            String token = zhishuTokenManager.getAccessToken();
            return HttpRequest.delete(url)
                    .header("Authorization", "Bearer " + token)
                    .timeout(zhishuApiConfig.getTimeout())
                    .execute();
        });
    }

    public String doPostMultipart(String path, Map<String, Object> formData) {
        String url = zhishuApiConfig.getBaseUrl() + path;
        log.info("智书API multipart POST请求: {}", url);
        try {
            String token = zhishuTokenManager.getAccessToken();
            log.info("获取到的token: {}", token != null ? "******" : "null");
            logMultipartForm(formData);

            HttpResponse response = HttpRequest.post(url)
                    .header("Authorization", "Bearer " + token)
                    .form(formData)
                    .timeout(zhishuApiConfig.getTimeout())
                    .execute();

            log.info("智书API响应状态码: {}", response.getStatus());
            String responseBody = response.body();
            log.info("智书API响应内容: {}", responseBody);
            return responseBody;
        } catch (Exception e) {
            log.error("智书API multipart POST请求异常, URL: {}, 错误: {}", url, e.getMessage(), e);
            throw new RuntimeException("智书API请求失败: " + e.getMessage());
        }
    }

    private void logMultipartForm(Map<String, Object> formData) {
        if (formData == null) {
            log.info("智书API multipart POST请求参数: null");
            return;
        }
        JSONObject logData = new JSONObject();
        for (Map.Entry<String, Object> entry : formData.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof File) {
                File file = (File) value;
                logData.put(entry.getKey(), file.getAbsolutePath() + "(" + file.length() + " bytes)");
            } else {
                logData.put(entry.getKey(), value);
            }
        }
        log.info("智书API multipart POST请求参数: {}", logData.toJSONString());
    }

    public String doPut(String path, Object body) {
        return doPut("智书接口调用", path, body);
    }

    public String doPut(String action, String path, Object body) {
        String url = zhishuApiConfig.getBaseUrl() + path;
        String bodyJson = JSON.toJSONString(body);
        return executeAndRecord(action, "PUT", url, bodyJson, () -> {
            String token = zhishuTokenManager.getAccessToken();
            return HttpRequest.put(url)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .body(bodyJson)
                    .timeout(zhishuApiConfig.getTimeout())
                    .execute();
        });
    }

    public String buildDraftPageUrl(String contractId) {
        String template = zhishuApiConfig.getDraftPageUrl();
        if (template == null || template.isEmpty()) {
            log.warn("草稿页URL模板未配置");
            return null;
        }
        String url = template.replace("{contractId}", contractId);
        log.info("生成草稿页链接: {}", url);
        return url;
    }

    public String buildDetailPageUrl(String contractId) {
        String template = zhishuApiConfig.getDetailPageUrl();
        if (template == null || template.isEmpty()) {
            log.warn("合同页URL模板未配置");
            return null;
        }
        String url = template.replace("{contractId}", contractId);
        log.info("生成合同页链接: {}", url);
        return url;
    }

    /**
     * 同步付款记录
     * https://open.qfei.cn/open-apis/contract/v1/payment/notify?user_id_type=user_id
     *
     * HTTP Method
     * POST
     */
    public String paymentSave(Map<String, Object> formMap, JSONObject reqBody){
        log.info("rpc 同步付款记录 in {} {}", formMap, reqBody);
        String body = doPost(
                "同步智书付款记录",
                "/open-apis/contract/v1/payment/notify?user_id_type=user_id",
                reqBody
        );
        log.info("rpc 同步付款记录 out {}", body);
        return body;
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
        log.info("智书API {}请求: {}", httpMethod, url);
        log.debug("智书API {}请求参数: {}", httpMethod, requestParams);
        try {
            HttpResponse response = requestSupplier.get();
            httpStatus = response.getStatus();
            responseBody = response.body();
            log.info("智书API响应状态码: {}", httpStatus);
            log.debug("智书API响应内容: {}", responseBody);
            return responseBody;
        } catch (Exception e) {
            exceptionMessage = e.getMessage();
            log.error("智书API {}请求异常, URL: {}, 错误: {}", httpMethod, url, e.getMessage(), e);
            throw new RuntimeException("智书API请求失败: " + e.getMessage(), e);
        } finally {
            try {
                apiLogService.record(ApiLogEvent.builder()
                        .targetSystem("智书")
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
                log.error("提交智书API调用日志失败, URL: {}", url, e);
            }
        }
    }

}
