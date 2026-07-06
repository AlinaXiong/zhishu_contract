package com.hero.middleware.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "yuecai.data")
public class YeCaiDataConfig {

    private String startTime;

    private String templateProcurement;

    private String templateOrder;

    private String templateAnchor;

    private String templateFHLXY;

    private String userId;

    private String documentUserId;

    private int pageSize;

    private Boolean vendorSyncMultiThread = true;
}
