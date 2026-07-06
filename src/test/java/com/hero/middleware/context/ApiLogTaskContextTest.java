package com.hero.middleware.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ApiLogTaskContextTest {

    @AfterEach
    void clearContext() {
        while (ApiLogTaskContext.currentTaskId() != null) {
            ApiLogTaskContext.exit();
        }
    }

    @Test
    void shouldSupportNestedTaskIdsAndRestoreParent() {
        ApiLogTaskContext.enter("outer-task");
        ApiLogTaskContext.enter("inner-task");

        assertEquals("inner-task", ApiLogTaskContext.currentTaskId());
        ApiLogTaskContext.exit();
        assertEquals("outer-task", ApiLogTaskContext.currentTaskId());
        ApiLogTaskContext.exit();
        assertNull(ApiLogTaskContext.currentTaskId());
    }

    @Test
    void shouldPropagateTaskIdToWorkerWithoutLeaking() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            ApiLogTaskContext.enter("event-task");
            Callable<String> wrapped = ApiLogTaskContext.wrap(ApiLogTaskContext::currentTaskId);
            ApiLogTaskContext.exit();

            Future<String> taskId = executor.submit(wrapped);
            assertEquals("event-task", taskId.get());
            assertNull(executor.submit(ApiLogTaskContext::currentTaskId).get());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void shouldRestoreWorkerContextWhenWrappedTaskFails() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            ApiLogTaskContext.enter("event-task");
            Callable<Void> wrapped = ApiLogTaskContext.wrap(() -> {
                throw new IllegalStateException("test");
            });
            ApiLogTaskContext.exit();

            assertThrows(Exception.class, () -> executor.submit(wrapped).get());
            assertNull(executor.submit(ApiLogTaskContext::currentTaskId).get());
        } finally {
            executor.shutdownNow();
        }
    }
}
