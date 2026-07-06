package com.hero.middleware.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiLogTableContextTest {

    @AfterEach
    void clearContext() {
        while (ApiLogTableContext.isSuppressed()) {
            ApiLogTableContext.exitSuppressed();
        }
    }

    @Test
    void shouldSupportNestedSuppressionAndRestoreState() {
        ApiLogTableContext.enterSuppressed();
        ApiLogTableContext.enterSuppressed();

        ApiLogTableContext.exitSuppressed();
        assertTrue(ApiLogTableContext.isSuppressed());

        ApiLogTableContext.exitSuppressed();
        assertFalse(ApiLogTableContext.isSuppressed());
    }

    @Test
    void shouldPropagateSuppressionToWrappedTaskWithoutLeaking() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            ApiLogTableContext.enterSuppressed();
            Callable<Boolean> wrapped = ApiLogTableContext.wrap(ApiLogTableContext::isSuppressed);
            ApiLogTableContext.exitSuppressed();

            Future<Boolean> suppressedResult = executor.submit(wrapped);
            assertTrue(suppressedResult.get());
            assertFalse(executor.submit(ApiLogTableContext::isSuppressed).get());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void shouldRestoreWorkerContextWhenWrappedTaskFails() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            ApiLogTableContext.enterSuppressed();
            Callable<Void> wrapped = ApiLogTableContext.wrap(() -> {
                throw new IllegalStateException("test");
            });
            ApiLogTableContext.exitSuppressed();

            assertThrows(Exception.class, () -> executor.submit(wrapped).get());
            assertFalse(executor.submit(ApiLogTableContext::isSuppressed).get());
        } finally {
            executor.shutdownNow();
        }
    }
}
