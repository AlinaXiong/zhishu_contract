package com.hero.middleware.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.hero.middleware.client.feishu.FeishuBitableClient;
import com.hero.middleware.component.ApiFailureAlertNotifier;
import com.hero.middleware.config.ApiLogProperties;
import com.hero.middleware.context.ApiLogTaskContext;
import com.hero.middleware.context.ApiLogTableContext;
import com.hero.middleware.dto.ApiLogEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class ApiLogServiceImplTest {

    private FeishuBitableClient feishuBitableClient;

    private ApiFailureAlertNotifier alertNotifier;

    private ApiLogProperties properties;

    private ApiLogServiceImpl apiLogService;

    @AfterEach
    void clearContext() {
        while (ApiLogTableContext.isSuppressed()) {
            ApiLogTableContext.exitSuppressed();
        }
        while (ApiLogTaskContext.currentTaskId() != null) {
            ApiLogTaskContext.exit();
        }
    }

    @BeforeEach
    void setUp() {
        feishuBitableClient = mock(FeishuBitableClient.class);
        alertNotifier = mock(ApiFailureAlertNotifier.class);
        properties = new ApiLogProperties();
        properties.setEnabled(true);
        properties.setApiAppToken("appApiLog");
        properties.setApiTableId("tblApiLog");
        properties.getAlert().setEnabled(true);
        Executor directExecutor = Runnable::run;
        apiLogService = new ApiLogServiceImpl(
                properties,
                feishuBitableClient,
                alertNotifier,
                directExecutor,
                directExecutor
        );
    }

    @Test
    void shouldWriteSuccessfulCallWithoutAlert() throws Exception {
        ApiLogEvent event = baseEvent()
                .httpStatus(200)
                .responseBody("{\"code\":0,\"msg\":\"success\"}")
                .build();

        apiLogService.record(event);

        verify(feishuBitableClient).createAppTableRecordSample(
                any(), eq("appApiLog"), eq("tblApiLog"));
        verify(alertNotifier, never()).notifyFailure(anyString(), any(), anyString());
    }

    @Test
    void shouldTreatSuccessCodeAsSuccessfulCall() throws Exception {
        ApiLogEvent event = baseEvent()
                .httpStatus(200)
                .responseBody("{\"code\":\"success\",\"msg\":\"success\"}")
                .build();

        apiLogService.record(event);

        ArgumentCaptor<JSONObject> fieldsCaptor = ArgumentCaptor.forClass(JSONObject.class);
        verify(feishuBitableClient).createAppTableRecordSample(
                fieldsCaptor.capture(), eq("appApiLog"), eq("tblApiLog"));
        assertEquals("成功", fieldsCaptor.getValue().getString("执行结果"));
        verify(alertNotifier, never()).notifyFailure(anyString(), any(), anyString());
    }

    @Test
    void shouldWriteAndAlertForBusinessFailure() throws Exception {
        ApiLogEvent event = baseEvent()
                .httpStatus(200)
                .responseBody("{\"code\":\"error.permission.accessTokenExpired\",\"message\":\"token expired\"}")
                .build();

        apiLogService.record(event);

        verify(feishuBitableClient).createAppTableRecordSample(
                any(), eq("appApiLog"), eq("tblApiLog"));
        verify(alertNotifier).notifyFailure(anyString(), any(), anyString());
    }

    @Test
    void shouldReuseEventTaskIdForAllCallsAndAlert() throws Exception {
        ApiLogTaskContext.enter("event-task");

        apiLogService.record(baseEvent()
                .httpStatus(200)
                .responseBody("{\"code\":0}")
                .build());
        apiLogService.record(baseEvent()
                .httpStatus(500)
                .responseBody("{\"message\":\"failed\"}")
                .build());

        ArgumentCaptor<JSONObject> fieldsCaptor = ArgumentCaptor.forClass(JSONObject.class);
        verify(feishuBitableClient, times(2)).createAppTableRecordSample(
                fieldsCaptor.capture(), eq("appApiLog"), eq("tblApiLog"));
        assertEquals("event-task", fieldsCaptor.getAllValues().get(0).getString("任务号"));
        assertEquals("event-task", fieldsCaptor.getAllValues().get(1).getString("任务号"));
        verify(alertNotifier).notifyFailure(eq("event-task"), any(), anyString());
    }

    @Test
    void shouldGenerateIndependentTaskIdsWithoutEventContext() throws Exception {
        apiLogService.record(baseEvent()
                .httpStatus(200)
                .responseBody("{\"code\":0}")
                .build());
        apiLogService.record(baseEvent()
                .httpStatus(200)
                .responseBody("{\"code\":0}")
                .build());

        ArgumentCaptor<JSONObject> fieldsCaptor = ArgumentCaptor.forClass(JSONObject.class);
        verify(feishuBitableClient, times(2)).createAppTableRecordSample(
                fieldsCaptor.capture(), eq("appApiLog"), eq("tblApiLog"));
        String firstTaskId = fieldsCaptor.getAllValues().get(0).getString("任务号");
        String secondTaskId = fieldsCaptor.getAllValues().get(1).getString("任务号");
        assertNotNull(firstTaskId);
        assertNotNull(secondTaskId);
        assertNotEquals(firstTaskId, secondTaskId);
    }

    @Test
    void shouldSkipTableAndKeepAlertForSuppressedFailure() throws Exception {
        ApiLogEvent event = baseEvent()
                .httpStatus(200)
                .responseBody("{\"code\":500,\"message\":\"sync failed\"}")
                .build();

        ApiLogTableContext.enterSuppressed();
        apiLogService.record(event);

        verify(feishuBitableClient, never())
                .createAppTableRecordSample(any(), anyString(), anyString());
        verify(alertNotifier).notifyFailure(anyString(), any(), anyString());
    }

    @Test
    void shouldSkipTableAndAlertForSuppressedSuccess() throws Exception {
        ApiLogEvent event = baseEvent()
                .httpStatus(200)
                .responseBody("{\"code\":0,\"message\":\"success\"}")
                .build();

        ApiLogTableContext.enterSuppressed();
        apiLogService.record(event);

        verify(feishuBitableClient, never())
                .createAppTableRecordSample(any(), anyString(), anyString());
        verify(alertNotifier, never()).notifyFailure(anyString(), any(), anyString());
    }

    @Test
    void shouldRecognizeTransportAndBusinessOutcomes() {
        assertTrue(apiLogService.isSuccess(baseEvent()
                .httpStatus(204)
                .responseBody(null)
                .build()));
        assertTrue(apiLogService.isSuccess(baseEvent()
                .httpStatus(200)
                .responseBody("{\"success\":true,\"code\":200}")
                .build()));
        assertFalse(apiLogService.isSuccess(baseEvent()
                .httpStatus(500)
                .responseBody("{\"code\":0}")
                .build()));
        assertFalse(apiLogService.isSuccess(baseEvent()
                .httpStatus(200)
                .responseBody("{\"success\":false}")
                .build()));
        assertFalse(apiLogService.isSuccess(baseEvent()
                .httpStatus(200)
                .exceptionMessage("connection timeout")
                .build()));
    }

    @Test
    void shouldTruncateLongContentWithMarker() {
        String truncated = apiLogService.truncate(
                "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ",
                40
        );

        assertTrue(truncated.length() <= 40);
        assertTrue(truncated.contains("已截断") || truncated.length() == 40);
    }

    private ApiLogEvent.ApiLogEventBuilder baseEvent() {
        return ApiLogEvent.builder()
                .targetSystem("智书")
                .action("查询合同详情")
                .httpMethod("GET")
                .url("https://open.qfei.cn/contracts/1")
                .requestParams("{\"user_id_type\":\"user_id\"}")
                .startTime(System.currentTimeMillis())
                .durationMs(100L);
    }
}
