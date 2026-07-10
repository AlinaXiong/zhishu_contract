package com.hero.middleware.config;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class ZhishuHttpClientConfig {

    @Bean(name = "zhishuHttpConnectionManager", destroyMethod = "shutdown")
    public PoolingHttpClientConnectionManager zhishuHttpConnectionManager(
            @Qualifier("zhishuApiConfig") ZhishuApiConfig config) {
        int maxTotalConnections = positiveValue(config.getMaxTotalConnections(), 30);
        int maxConnectionsPerRoute = Math.min(maxTotalConnections,
                positiveValue(config.getMaxConnectionsPerRoute(), 12));

        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(maxTotalConnections);
        connectionManager.setDefaultMaxPerRoute(maxConnectionsPerRoute);
        return connectionManager;
    }

    @Bean(name = "zhishuHttpClient", destroyMethod = "close")
    public CloseableHttpClient zhishuHttpClient(
            @Qualifier("zhishuHttpConnectionManager") PoolingHttpClientConnectionManager connectionManager,
            @Qualifier("zhishuApiConfig") ZhishuApiConfig config) {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(positiveValue(config.getConnectionRequestTimeout(), 5000))
                .setConnectTimeout(positiveValue(config.getConnectTimeout(), 5000))
                .setSocketTimeout(positiveValue(config.getTimeout(), 30000))
                .build();

        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setConnectionManagerShared(true)
                .setDefaultRequestConfig(requestConfig)
                .disableAutomaticRetries()
                .evictExpiredConnections()
                .evictIdleConnections(60, TimeUnit.SECONDS)
                .build();
    }

    private int positiveValue(Integer value, int defaultValue) {
        return value == null || value <= 0 ? defaultValue : value;
    }
}
