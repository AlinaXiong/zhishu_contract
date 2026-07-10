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
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Component
public class YuecaiApiClient {

    @Autowired
    private YuecaiApiConfig yuecaiApiConfig;
    @Autowired
    private ApiLogService apiLogService;

    private static final long DEFAULT_ACCESS_TOKEN_TTL_MILLIS = TimeUnit.MINUTES.toMillis(5);
    private static final long ACCESS_TOKEN_REFRESH_AHEAD_MILLIS = TimeUnit.SECONDS.toMillis(60);

    private final Object accessTokenLock = new Object();
    private volatile AccessTokenCache accessTokenCache;

    private Map<String,String> getHeader(){
        Map<String,String> header = new HashMap<>();
        header.put("Content-Type","application/json");
        header.put("Authorization", getAccessToken());
        return header;
    }

    public String getAccessToken(){
        AccessTokenCache cachedToken = accessTokenCache;
        if (cachedToken != null && cachedToken.isUsable()) {
            return cachedToken.getToken();
        }
        synchronized (accessTokenLock) {
            cachedToken = accessTokenCache;
            if (cachedToken != null && cachedToken.isUsable()) {
                return cachedToken.getToken();
            }
            return requestNewAccessToken();
        }
    }

    private String requestNewAccessToken() {
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
            Object accessToken = map == null ? null : map.get("access_token");
            String token = accessToken == null ? null : String.valueOf(accessToken).trim();
            if (token == null || token.isEmpty() || "null".equalsIgnoreCase(token)) {
                throw new RuntimeException("业财OAuth响应缺少access_token");
            }
            long expiresInMillis = resolveAccessTokenTtlMillis(map.get("expires_in"));
            accessTokenCache = new AccessTokenCache(token, System.currentTimeMillis() + expiresInMillis);
            log.info("业财OAuth token已刷新，有效期={}ms", expiresInMillis);
            return token;
        } catch (Exception e) {
            log.error("业财API请求异常: {}", e.getMessage(), e);
            throw new RuntimeException("业财API请求失败: " + e.getMessage());
        }
    }

    private long resolveAccessTokenTtlMillis(Object expiresIn) {
        if (expiresIn == null) {
            return DEFAULT_ACCESS_TOKEN_TTL_MILLIS;
        }
        try {
            long seconds = Long.parseLong(String.valueOf(expiresIn));
            if (seconds <= 0) {
                return DEFAULT_ACCESS_TOKEN_TTL_MILLIS;
            }
            long ttlMillis = TimeUnit.SECONDS.toMillis(seconds);
            long refreshAheadMillis = Math.min(ACCESS_TOKEN_REFRESH_AHEAD_MILLIS, ttlMillis / 10);
            return Math.max(TimeUnit.SECONDS.toMillis(1), ttlMillis - refreshAheadMillis);
        } catch (NumberFormatException ignored) {
            return DEFAULT_ACCESS_TOKEN_TTL_MILLIS;
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

    private static class AccessTokenCache {
        private final String token;
        private final long expiresAtMillis;

        private AccessTokenCache(String token, long expiresAtMillis) {
            this.token = token;
            this.expiresAtMillis = expiresAtMillis;
        }

        private String getToken() {
            return token;
        }

        private boolean isUsable() {
            return System.currentTimeMillis() < expiresAtMillis;
        }
    }

}
