package com.hero.middleware.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "zhishu.base")
public class ZhishuBaseConfig {

    /**
     * 同步业财系统来源名称
     */
    private String sourceSystem;

    /**
     * 审批回调地址
     */
    private String callbackUrl;

}
