package com.hero.middleware.config;

import com.hero.middleware.context.ApiLogTaskContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsyncExecutorConfigTest {

    @AfterEach
    void clearContext() {
        while (ApiLogTaskContext.currentTaskId() != null) {
            ApiLogTaskContext.exit();
        }
    }

    @Test
    void shouldExposeApprovalTaskExecutor() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(AsyncExecutorConfig.class)) {
            ThreadPoolTaskExecutor executor =
                    context.getBean("approvalTaskExecutor", ThreadPoolTaskExecutor.class);

            assertNotNull(executor);
        }
    }

    @Test
    void shouldPropagateAndClearEventTaskId() throws Exception {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(AsyncExecutorConfig.class)) {
            ThreadPoolTaskExecutor executor =
                    context.getBean("approvalTaskExecutor", ThreadPoolTaskExecutor.class);
            AtomicReference<String> propagatedTaskId = new AtomicReference<>();
            AtomicReference<String> leakedTaskId = new AtomicReference<>();
            CountDownLatch firstTask = new CountDownLatch(1);
            CountDownLatch secondTask = new CountDownLatch(1);

            ApiLogTaskContext.enter("event-task");
            executor.execute(() -> {
                propagatedTaskId.set(ApiLogTaskContext.currentTaskId());
                firstTask.countDown();
            });
            ApiLogTaskContext.exit();

            assertTrue(firstTask.await(1, TimeUnit.SECONDS));
            executor.execute(() -> {
                leakedTaskId.set(ApiLogTaskContext.currentTaskId());
                secondTask.countDown();
            });
            assertTrue(secondTask.await(1, TimeUnit.SECONDS));
            assertEquals("event-task", propagatedTaskId.get());
            assertNull(leakedTaskId.get());
        }
    }
}
