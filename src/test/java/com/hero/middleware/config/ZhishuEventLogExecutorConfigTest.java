package com.hero.middleware.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZhishuEventLogExecutorConfigTest {

    @Test
    void shouldCreateSingleThreadExecutorWithExpectedQueueCapacity() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(ZhishuEventLogExecutorConfig.class)) {
            ThreadPoolTaskExecutor executor =
                    context.getBean("zhishuEventLogExecutor", ThreadPoolTaskExecutor.class);

            assertEquals(1, executor.getCorePoolSize());
            assertEquals(1, executor.getMaxPoolSize());
            assertEquals(500, executor.getThreadPoolExecutor().getQueue().remainingCapacity());
        }
    }

    @Test
    void shouldFinishSubmittedTaskWhenContextCloses() throws Exception {
        AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext(ZhishuEventLogExecutorConfig.class);
        ThreadPoolTaskExecutor executor =
                context.getBean("zhishuEventLogExecutor", ThreadPoolTaskExecutor.class);
        CountDownLatch completed = new CountDownLatch(1);
        executor.execute(completed::countDown);

        context.close();

        assertTrue(completed.await(1, TimeUnit.SECONDS));
    }
}
