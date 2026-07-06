package com.hero.middleware.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "feishu.api")
public class FeiShuApiConfig {

    private String baseUrl = "https://open.feishu.cn";

    private String appId;

    private String appSecret;

    private Integer timeout = 30000;
}
