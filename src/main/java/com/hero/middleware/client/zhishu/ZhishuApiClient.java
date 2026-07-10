package com.hero.middleware.client.zhishu;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hero.middleware.config.ZhishuApiConfig;
import com.hero.middleware.dto.ApiLogEvent;
import com.hero.middleware.service.ApiLogService;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URI;
import java.nio.charset.StandardCharsets;
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

    @Autowired
    @Qualifier("zhishuHttpClient")
    private CloseableHttpClient zhishuHttpClient;

    public String doGet(String path, Map<String, Object> params) {
        return doGet("智书接口调用", path, params);
    }

    public String doGet(String action, String path, Map<String, Object> params) {
        String url = zhishuApiConfig.getBaseUrl() + path;
        return executeAndRecord(action, "GET", url, JSON.toJSONString(params), () -> {
            String token = zhishuTokenManager.getAccessToken();
            HttpGet request = new HttpGet(buildGetUri(url, params));
            request.setHeader("Authorization", "Bearer " + token);
            return request;
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
            HttpPost request = new HttpPost(url);
            request.setHeader("Content-Type", "application/json");
            request.setHeader("Authorization", "Bearer " + token);
            request.setEntity(new StringEntity(bodyJson, StandardCharsets.UTF_8));
            return request;
        });
    }

    public String doPostWithoutBody(String action, String path) {
        String url = zhishuApiConfig.getBaseUrl() + path;
        return executeAndRecord(action, "POST", url, "", () -> {
            String token = zhishuTokenManager.getAccessToken();
            HttpPost request = new HttpPost(url);
            request.setHeader("Authorization", "Bearer " + token);
            return request;
        });
    }

    public String doDelete(String action, String path) {
        String url = zhishuApiConfig.getBaseUrl() + path;
        return executeAndRecord(action, "DELETE", url, "", () -> {
            String token = zhishuTokenManager.getAccessToken();
            HttpDelete request = new HttpDelete(url);
            request.setHeader("Authorization", "Bearer " + token);
            return request;
        });
    }

    public String doPostMultipart(String path, Map<String, Object> formData) {
        String url = zhishuApiConfig.getBaseUrl() + path;
        String requestParams = buildMultipartLogData(formData);
        return executeAndRecord("智书接口调用", "POST", url, requestParams, () -> {
            String token = zhishuTokenManager.getAccessToken();
            HttpPost request = new HttpPost(url);
            request.setHeader("Authorization", "Bearer " + token);
            request.setEntity(buildMultipartEntity(formData));
            return request;
        });
    }

    private String buildMultipartLogData(Map<String, Object> formData) {
        JSONObject logData = new JSONObject();
        if (formData == null) {
            return logData.toJSONString();
        }
        for (Map.Entry<String, Object> entry : formData.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof File) {
                File file = (File) value;
                logData.put(entry.getKey(), file.getAbsolutePath() + "(" + file.length() + " bytes)");
            } else {
                logData.put(entry.getKey(), value);
            }
        }
        return logData.toJSONString();
    }

    private org.apache.http.HttpEntity buildMultipartEntity(Map<String, Object> formData) {
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        if (formData == null) {
            return builder.build();
        }
        for (Map.Entry<String, Object> entry : formData.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof File) {
                File file = (File) value;
                builder.addBinaryBody(key, file, ContentType.DEFAULT_BINARY, file.getName());
            } else {
                builder.addTextBody(key, value == null ? "" : String.valueOf(value),
                        ContentType.TEXT_PLAIN.withCharset(StandardCharsets.UTF_8));
            }
        }
        return builder.build();
    }

    public String doPut(String path, Object body) {
        return doPut("智书接口调用", path, body);
    }

    public String doPut(String action, String path, Object body) {
        String url = zhishuApiConfig.getBaseUrl() + path;
        String bodyJson = JSON.toJSONString(body);
        return executeAndRecord(action, "PUT", url, bodyJson, () -> {
            String token = zhishuTokenManager.getAccessToken();
            HttpPut request = new HttpPut(url);
            request.setHeader("Content-Type", "application/json");
            request.setHeader("Authorization", "Bearer " + token);
            request.setEntity(new StringEntity(bodyJson, StandardCharsets.UTF_8));
            return request;
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
                                    Supplier<HttpUriRequest> requestSupplier) {
        long startTime = System.currentTimeMillis();
        Integer httpStatus = null;
        String responseBody = null;
        String exceptionMessage = null;
        if (log.isDebugEnabled()) {
            log.debug("智书API请求，action={}，method={}，url={}，requestSize={}",
                    action, httpMethod, url, requestParams == null ? 0 : requestParams.length());
        }
        try {
            HttpUriRequest request = requestSupplier.get();
            try (CloseableHttpResponse response = zhishuHttpClient.execute(request)) {
                httpStatus = response.getStatusLine().getStatusCode();
                responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                if (log.isDebugEnabled()) {
                    log.debug("智书API响应，action={}，method={}，url={}，status={}，durationMs={}，responseSize={}",
                            action, httpMethod, url, httpStatus, System.currentTimeMillis() - startTime,
                            responseBody == null ? 0 : responseBody.length());
                }
                return responseBody;
            }
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

    private URI buildGetUri(String url, Map<String, Object> params) {
        try {
            URIBuilder builder = new URIBuilder(url);
            if (params != null) {
                for (Map.Entry<String, Object> entry : params.entrySet()) {
                    if (entry.getKey() != null && entry.getValue() != null) {
                        builder.addParameter(entry.getKey(), String.valueOf(entry.getValue()));
                    }
                }
            }
            return builder.build();
        } catch (Exception e) {
            throw new IllegalArgumentException("构建智书GET请求地址失败：" + url, e);
        }
    }

}
