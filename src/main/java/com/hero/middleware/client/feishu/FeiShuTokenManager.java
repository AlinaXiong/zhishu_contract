package com.hero.middleware.client.feishu;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSON;
import com.hero.middleware.client.feishu.request.FeiShuTokenRequest;
import com.hero.middleware.client.feishu.response.FeiShuTokenResponse;
import com.hero.middleware.config.FeiShuApiConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Component
public class FeiShuTokenManager {

    private static final String TOKEN_URL = "/open-apis/auth/v3/tenant_access_token/internal";

    private static final int TOKEN_EXPIRE_BUFFER = 300;

    @Autowired
    private FeiShuApiConfig feiShuApiConfig;

    private volatile String cachedToken;

    private volatile long tokenExpireTime;

    private final ReentrantLock lock = new ReentrantLock();

    public String getAccessToken() {
        if (isValidToken()) {
            log.info("使用缓存的飞书访问令牌");
            return cachedToken;
        }

        lock.lock();
        try {
            if (isValidToken()) {
                log.info("使用缓存的飞书访问令牌");
                return cachedToken;
            }
            log.info("飞书访问令牌已过期或不存在，开始获取新令牌");
            return fetchNewToken();
        } finally {
            lock.unlock();
        }
    }

    private boolean isValidToken() {
        return cachedToken != null && System.currentTimeMillis() < tokenExpireTime;
    }

    private String fetchNewToken() {
        String url = feiShuApiConfig.getBaseUrl() + TOKEN_URL;
        if (feiShuApiConfig.getAppId() == null || feiShuApiConfig.getAppId().trim().isEmpty()
                || feiShuApiConfig.getAppSecret() == null || feiShuApiConfig.getAppSecret().trim().isEmpty()) {
            throw new RuntimeException("飞书应用appId或appSecret未配置");
        }
        FeiShuTokenRequest request = new FeiShuTokenRequest(
                feiShuApiConfig.getAppId(),
                feiShuApiConfig.getAppSecret()
        );
        log.info("请求飞书访问令牌, URL: {}", url);
        log.info("飞书appId: {}, appSecret: {}", feiShuApiConfig.getAppId(),
                feiShuApiConfig.getAppSecret() != null ? "******" : "null");
        try {
            log.info("开始请求飞书访问令牌");
            HttpResponse response = HttpRequest.post(url)
                    .header("Content-Type", "application/json")
                    .body(JSON.toJSONString(request))
                    .timeout(feiShuApiConfig.getTimeout())
                    .execute();
            log.info("飞书访问令牌请求完成，准备读取响应内容");
            String responseBody = response.body();
            log.info("飞书访问令牌响应状态码: {}", response.getStatus());
            log.info("飞书访问令牌响应: {}", responseBody);

            FeiShuTokenResponse tokenResponse = JSON.parseObject(responseBody, FeiShuTokenResponse.class);
            if (tokenResponse == null || !tokenResponse.isSuccess()) {
                throw new RuntimeException("获取飞书访问令牌失败: "
                        + (tokenResponse != null ? tokenResponse.getMsg() : "响应为空"));
            }

            cachedToken = tokenResponse.getTenant_access_token();
            int expireSeconds = tokenResponse.getExpire() != null ? tokenResponse.getExpire() : 7200;
            tokenExpireTime = System.currentTimeMillis() + (expireSeconds - TOKEN_EXPIRE_BUFFER) * 1000L;
            log.info("飞书访问令牌获取成功，有效期: {}秒", expireSeconds);
            return cachedToken;
        } catch (Exception e) {
            log.error("获取飞书访问令牌异常: {}", e.getMessage(), e);
            throw new RuntimeException("获取飞书访问令牌异常: " + e.getMessage());
        }
    }

    public void invalidateToken() {
        lock.lock();
        try {
            log.info("手动失效飞书访问令牌缓存");
            cachedToken = null;
            tokenExpireTime = 0;
        } finally {
            lock.unlock();
        }
    }
}
