package com.hero.middleware.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "yuecai.api")
public class YuecaiApiConfig {

    private String baseUrl;

    private String grantType;

    private String clientId;

    private String clientSecret;

    private Integer timeout;

}
