package com.hero.middleware.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "zhishu-event-log")
public class ZhishuEventLogProperties {

    private boolean enabled = false;

    private String tableId;

    private int maxContentLength = 20000;
}
