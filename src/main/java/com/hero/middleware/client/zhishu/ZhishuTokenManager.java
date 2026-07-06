package com.hero.middleware.client.zhishu;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSON;
import com.hero.middleware.client.zhishu.request.ZhishuTokenRequest;
import com.hero.middleware.client.zhishu.response.ZhishuTokenResponse;
import com.hero.middleware.config.ZhishuApiConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Component
public class ZhishuTokenManager {

    private static final String TOKEN_URL = "/open-apis/auth/v3/tenant_access_token/internal";

    private static final int TOKEN_EXPIRE_BUFFER = 300;

    @Autowired
    private ZhishuApiConfig zhishuApiConfig;

    private volatile String cachedToken;

    private volatile long tokenExpireTime;

    private final ReentrantLock lock = new ReentrantLock();

    public String getAccessToken() {
//        log.info("获取智书访问令牌开始");
//        log.info("缓存令牌: {}", cachedToken != null ? "存在" : "不存在");
//        log.info("令牌过期时间: {}, 当前时间: {}",
//                new java.util.Date(tokenExpireTime),
//                new java.util.Date(System.currentTimeMillis()));

        if (isValidToken()) {
            log.info("使用缓存的智书访问令牌");
            return cachedToken;
        }

        lock.lock();
        try {
            if (isValidToken()) {
                log.info("使用缓存的智书访问令牌");
                return cachedToken;
            }

            log.info("智书访问令牌已过期或不存在，开始获取新令牌");
            return fetchNewToken();
        } finally {
            lock.unlock();
        }
    }

    private boolean isValidToken() {
        return cachedToken != null && System.currentTimeMillis() < tokenExpireTime;
    }

    private String fetchNewToken() {
        validateTokenConfig();
        int timeout = zhishuApiConfig.getTimeout() == null ? 30000 : zhishuApiConfig.getTimeout();
        String url = zhishuApiConfig.getBaseUrl() + TOKEN_URL;
        log.info("Token request timeout: {} ms", timeout);
        log.info("请求智书访问令牌, URL: {}", url);
        log.info("appId: {}, appSecret: {}",
                zhishuApiConfig.getAppId(),
                zhishuApiConfig.getAppSecret() != null ? "******" : "null");

        ZhishuTokenRequest request = new ZhishuTokenRequest(
                zhishuApiConfig.getAppId(),
                zhishuApiConfig.getAppSecret()
        );

        log.info("令牌请求参数: {}", JSON.toJSONString(request));

        try {
            log.info("发起HTTP请求获取令牌...");
            log.info("Before token HTTP execute");
            HttpResponse response = HttpRequest.post(url)
                    .header("Content-Type", "application/json")
                    .body(JSON.toJSONString(request))
                    .timeout(timeout)
                    .execute();
            log.info("After token HTTP execute");

            log.info("令牌请求响应状态码: {}", response.getStatus());
            String responseBody = response.body();
            log.info("智书访问令牌响应: {}", responseBody);

            ZhishuTokenResponse tokenResponse = JSON.parseObject(responseBody, ZhishuTokenResponse.class);
            log.info("解析后的令牌响应: {}", tokenResponse != null ? JSON.toJSONString(tokenResponse) : "null");

            if (tokenResponse == null || !tokenResponse.isSuccess()) {
                log.error("获取智书访问令牌失败, 响应: {}", responseBody);
                throw new RuntimeException("获取智书访问令牌失败: " +
                        (tokenResponse != null ? tokenResponse.getMsg() : "响应为空"));
            }

            cachedToken = tokenResponse.getTenant_access_token();
            log.info("获取到的令牌: {}", cachedToken != null ? "******" : "null");

            int expireSeconds = tokenResponse.getExpire() != null ? tokenResponse.getExpire() : 7200;
            tokenExpireTime = System.currentTimeMillis() + (expireSeconds - TOKEN_EXPIRE_BUFFER) * 1000L;

            log.info("智书访问令牌获取成功, 有效期: {}秒, 缓存过期时间: {}", expireSeconds,
                    new java.util.Date(tokenExpireTime));

            return cachedToken;

        } catch (Exception e) {
            log.error("获取智书访问令牌异常: {}", e.getMessage(), e);
            throw new RuntimeException("获取智书访问令牌异常: " + e.getMessage());
        }
    }

    private void validateTokenConfig() {
        if (isBlank(zhishuApiConfig.getBaseUrl())) {
            throw new IllegalArgumentException("zhishu.api.base-url is blank");
        }
        if (isBlank(zhishuApiConfig.getAppId())) {
            throw new IllegalArgumentException("zhishu.api.app-id is blank");
        }
        if (isBlank(zhishuApiConfig.getAppSecret())) {
            throw new IllegalArgumentException("zhishu.api.app-secret is blank");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public void invalidateToken() {
        lock.lock();
        try {
            log.info("手动失效智书访问令牌缓存");
            cachedToken = null;
            tokenExpireTime = 0;
        } finally {
            lock.unlock();
        }
    }

}
