package com.hero.middleware.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class ApiLogExecutorConfig {

    @Bean(name = "apiLogExecutor")
    public ThreadPoolTaskExecutor apiLogExecutor() {
        return createSingleThreadExecutor("api-log-", 1000);
    }

    @Bean(name = "apiAlertExecutor")
    public ThreadPoolTaskExecutor apiAlertExecutor() {
        return createSingleThreadExecutor("api-alert-", 200);
    }

    private ThreadPoolTaskExecutor createSingleThreadExecutor(String threadNamePrefix, int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(120);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }
}
