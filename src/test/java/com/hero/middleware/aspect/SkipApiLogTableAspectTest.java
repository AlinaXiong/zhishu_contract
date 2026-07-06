package com.hero.middleware.aspect;

import com.hero.middleware.annotation.SkipApiLogTable;
import com.hero.middleware.context.ApiLogTableContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkipApiLogTableAspectTest {

    @AfterEach
    void clearContext() {
        while (ApiLogTableContext.isSuppressed()) {
            ApiLogTableContext.exitSuppressed();
        }
    }

    @Test
    void shouldSuppressAnnotatedClassAndRestoreAfterCall() {
        AnnotatedClass target = proxy(new AnnotatedClass());

        assertTrue(target.isSuppressed());
        assertFalse(ApiLogTableContext.isSuppressed());
    }

    @Test
    void shouldSuppressAnnotatedMethodAndRestoreAfterException() {
        AnnotatedMethod target = proxy(new AnnotatedMethod());

        assertThrows(IllegalStateException.class, target::failWhenSuppressed);
        assertFalse(ApiLogTableContext.isSuppressed());
    }

    private <T> T proxy(T target) {
        AspectJProxyFactory proxyFactory = new AspectJProxyFactory(target);
        proxyFactory.addAspect(new SkipApiLogTableAspect());
        return proxyFactory.getProxy();
    }

    @SkipApiLogTable
    static class AnnotatedClass {

        public boolean isSuppressed() {
            return ApiLogTableContext.isSuppressed();
        }
    }

    static class AnnotatedMethod {

        @SkipApiLogTable
        public void failWhenSuppressed() {
            if (ApiLogTableContext.isSuppressed()) {
                throw new IllegalStateException("test");
            }
        }
    }
}
