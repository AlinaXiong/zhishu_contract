package com.hero.middleware.aspect;

import com.hero.middleware.annotation.ZhishuEventLog;
import com.hero.middleware.component.ZhishuEventLogRecorder;
import com.hero.middleware.context.ApiLogTaskContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ZhishuEventLogAspectTest {

    private ZhishuEventLogRecorder recorder;

    private TestController controller;

    private ZhishuEventLogRecorder.PreparedEvent event;

    @AfterEach
    void clearContext() {
        while (ApiLogTaskContext.currentTaskId() != null) {
            ApiLogTaskContext.exit();
        }
    }

    @BeforeEach
    void setUp() {
        recorder = mock(ZhishuEventLogRecorder.class);
        event = mock(ZhishuEventLogRecorder.PreparedEvent.class);
        when(event.getTaskId()).thenReturn("event-task");
        when(recorder.capture(any(Object[].class))).thenReturn(event);
        AspectJProxyFactory proxyFactory = new AspectJProxyFactory(new TestController());
        proxyFactory.addAspect(new ZhishuEventLogAspect(recorder));
        controller = proxyFactory.getProxy();
    }

    @Test
    void shouldRecordSuccessWhenAnnotatedMethodReturnsNormally() {
        assertEquals("event-task", controller.success("{\"schema\":\"2.0\"}"));

        verify(recorder).record(event, true);
        assertNull(ApiLogTaskContext.currentTaskId());
    }

    @Test
    void shouldRecordFailureAndRethrowWhenAnnotatedMethodThrows() {
        assertThrows(IllegalStateException.class,
                () -> controller.failure("{\"schema\":\"2.0\"}"));

        verify(recorder).record(event, false);
        assertNull(ApiLogTaskContext.currentTaskId());
    }

    static class TestController {

        @ZhishuEventLog
        public String success(String json) {
            return ApiLogTaskContext.currentTaskId();
        }

        @ZhishuEventLog
        public String failure(String json) {
            throw new IllegalStateException("test");
        }
    }
}
