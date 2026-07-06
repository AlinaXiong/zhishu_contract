package com.hero.middleware.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiLogExecutorConfigTest {

    @Test
    void shouldCreateSingleThreadExecutorsWithExpectedQueueCapacity() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(ApiLogExecutorConfig.class)) {
            ThreadPoolTaskExecutor logExecutor =
                    context.getBean("apiLogExecutor", ThreadPoolTaskExecutor.class);
            ThreadPoolTaskExecutor alertExecutor =
                    context.getBean("apiAlertExecutor", ThreadPoolTaskExecutor.class);

            assertEquals(1, logExecutor.getCorePoolSize());
            assertEquals(1, logExecutor.getMaxPoolSize());
            assertEquals(1000, logExecutor.getThreadPoolExecutor().getQueue().remainingCapacity());
            assertEquals(200, alertExecutor.getThreadPoolExecutor().getQueue().remainingCapacity());
        }
    }

    @Test
    void shouldFinishSubmittedTaskWhenContextCloses() throws Exception {
        AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext(ApiLogExecutorConfig.class);
        ThreadPoolTaskExecutor executor =
                context.getBean("apiLogExecutor", ThreadPoolTaskExecutor.class);
        CountDownLatch completed = new CountDownLatch(1);

        executor.execute(() -> {
            try {
                Thread.sleep(100);
                completed.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        context.close();

        assertTrue(completed.await(1, TimeUnit.SECONDS));
    }
}
