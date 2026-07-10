package com.hero.middleware.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "zhishu.api")
public class ZhishuApiConfig {

    private String baseUrl;

    private String appId;

    private String appSecret;

    private Integer timeout;

    private Integer connectionRequestTimeout = 5000;

    private Integer connectTimeout = 5000;

    private Integer maxTotalConnections = 30;

    private Integer maxConnectionsPerRoute = 12;

    private String draftPageUrl;

    private String detailPageUrl;

}
