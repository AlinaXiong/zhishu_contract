package com.hero.middleware.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "api-log")
public class ApiLogProperties {

    private boolean enabled = false;

    private String apiAppToken;

    private String apiTableId;

    private int maxContentLength = 20000;

    private Alert alert = new Alert();

    @Data
    public static class Alert {

        private boolean enabled = false;

        private List<String> receiveChatIds = new ArrayList<>();

        private List<String> receiveUserIds = new ArrayList<>();

        private long dedupWindowSeconds = 300;

        private int maxErrorSummaryLength = 500;
    }
}
