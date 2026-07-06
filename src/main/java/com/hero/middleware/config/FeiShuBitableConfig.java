package com.hero.middleware.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "feishu.bitable")
public class FeiShuBitableConfig {

    private String appToken;

    private String paymentTableId;

    private String receiptTableId;

    private String archiveTableId;

    private String taxTableId;
}
