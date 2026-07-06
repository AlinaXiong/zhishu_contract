package com.hero.middleware.component;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hero.middleware.client.feishu.FeishuBitableClient;
import com.hero.middleware.client.zhishu.request.BaseEventRequest;
import com.hero.middleware.client.zhishu.request.ContractEventRequest;
import com.hero.middleware.client.zhishu.request.EventHeaderRequest;
import com.hero.middleware.config.ApiLogProperties;
import com.hero.middleware.config.ZhishuEventLogProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ZhishuEventLogRecorderTest {

    private FeishuBitableClient feishuBitableClient;

    private ZhishuEventLogProperties properties;

    private ApiLogProperties apiLogProperties;

    private ZhishuEventLogRecorder recorder;

    @BeforeEach
    void setUp() {
        feishuBitableClient = mock(FeishuBitableClient.class);
        properties = new ZhishuEventLogProperties();
        properties.setEnabled(true);
        properties.setTableId("tblEventLog");
        properties.setMaxContentLength(20000);
        apiLogProperties = new ApiLogProperties();
        apiLogProperties.setApiAppToken("appApiLog");
        recorder = createRecorder(Runnable::run);
    }

    @Test
    void shouldParseStringRequestAndWriteExpectedFields() throws Exception {
        String json = "{\n"
                + "  \"schema\": \"2.0\",\n"
                + "  \"header\": {\"event_type\": \"contract.contract.info_change_v1\"},\n"
                + "  \"event\": {\n"
                + "    \"business_type_code\": 0,\n"
                + "    \"contract_id\": \"7109086882906079243\",\n"
                + "    \"contract_number\": \"PLATFORMV1000382\",\n"
                + "    \"contract_stage_code\": 6\n"
                + "  }\n"
                + "}";

        ZhishuEventLogRecorder.PreparedEvent event = recorder.capture(new Object[]{json});
        recorder.record(event, true);

        ArgumentCaptor<JSONObject> fieldsCaptor = ArgumentCaptor.forClass(JSONObject.class);
        verify(feishuBitableClient).createAppTableRecordSample(
                fieldsCaptor.capture(), eq("appApiLog"), eq("tblEventLog"));
        JSONObject fields = fieldsCaptor.getValue();
        assertNotNull(fields.getString("任务号"));
        assertEquals("contract.contract.info_change_v1", fields.getString("event_type"));
        assertEquals("7109086882906079243", fields.getString("contract_id"));
        assertEquals("0-合同申请", fields.getString("businessTypeCode"));
        assertEquals("6-审批完成时", fields.getString("contractStageCode"));
        assertEquals("成功", fields.getString("执行结果"));
        assertTrue(fields.getString("事件原文")
                .contains("\"contract_number\":\"PLATFORMV1000382\""));
        assertFalse(fields.containsKey("合同编号"));
        assertFalse(fields.containsKey("创建时间"));
        assertFalse(fields.containsKey("按钮"));
    }

    @Test
    void shouldSerializeTypedBaseEventRequestWithSnakeCaseFields() throws Exception {
        EventHeaderRequest header = new EventHeaderRequest();
        header.setEventType("contract.contract.created_v1");
        ContractEventRequest contractEvent = new ContractEventRequest();
        contractEvent.setContractId("contract-1");
        contractEvent.setContractNumber("CONTRACT-001");
        contractEvent.setBusinessTypeCode(2);
        contractEvent.setContractStageCode(5);
        BaseEventRequest<ContractEventRequest> request = new BaseEventRequest<>();
        request.setSchema("2.0");
        request.setHeader(header);
        request.setEvent(contractEvent);

        ZhishuEventLogRecorder.PreparedEvent event = recorder.capture(new Object[]{request});
        recorder.record(event, false);

        ArgumentCaptor<JSONObject> fieldsCaptor = ArgumentCaptor.forClass(JSONObject.class);
        verify(feishuBitableClient).createAppTableRecordSample(
                fieldsCaptor.capture(), eq("appApiLog"), eq("tblEventLog"));
        JSONObject fields = fieldsCaptor.getValue();
        assertEquals("失败", fields.getString("执行结果"));
        assertEquals("contract-1", fields.getString("contract_id"));
        assertEquals("2-合同变更（补充协议）", fields.getString("businessTypeCode"));
        assertEquals("5-审批发起时", fields.getString("contractStageCode"));
        assertTrue(fields.getString("事件原文").contains("\"event_type\""));
        assertTrue(fields.getString("事件原文").contains("\"contract_id\""));
        assertTrue(fields.getString("事件原文").contains("\"contract_number\":\"CONTRACT-001\""));
        assertTrue(fields.getString("事件原文").contains("\"business_type_code\":2"));
        assertTrue(fields.getString("事件原文").contains("\"contract_stage_code\":5"));
    }

    @Test
    void shouldTruncateMalformedOriginalJsonAndStillRecordFailure() throws Exception {
        properties.setMaxContentLength(10);

        ZhishuEventLogRecorder.PreparedEvent event =
                recorder.capture(new Object[]{"not-json-content"});
        recorder.record(event, false);

        ArgumentCaptor<JSONObject> fieldsCaptor = ArgumentCaptor.forClass(JSONObject.class);
        verify(feishuBitableClient).createAppTableRecordSample(
                fieldsCaptor.capture(), eq("appApiLog"), eq("tblEventLog"));
        assertEquals("not-json-c", fieldsCaptor.getValue().getString("事件原文"));
        assertEquals("失败", fieldsCaptor.getValue().getString("执行结果"));
    }

    @Test
    void shouldKeepUnknownCodesUnchanged() throws Exception {
        String unknownCodeJson = "{\"event\":{\"business_type_code\":99,\"contract_stage_code\":\"future\"}}";

        recorder.record(recorder.capture(new Object[]{unknownCodeJson}), true);

        ArgumentCaptor<JSONObject> fieldsCaptor = ArgumentCaptor.forClass(JSONObject.class);
        verify(feishuBitableClient).createAppTableRecordSample(
                fieldsCaptor.capture(), eq("appApiLog"), eq("tblEventLog"));
        JSONObject unknownCodeFields = fieldsCaptor.getValue();
        assertEquals("99", unknownCodeFields.getString("businessTypeCode"));
        assertEquals("future", unknownCodeFields.getString("contractStageCode"));
    }

    @Test
    void shouldWriteEmptyTextWhenCodesAreMissing() throws Exception {
        recorder.record(recorder.capture(new Object[]{"{\"event\":{}}"}), true);

        ArgumentCaptor<JSONObject> missingCodeFieldsCaptor = ArgumentCaptor.forClass(JSONObject.class);
        verify(feishuBitableClient).createAppTableRecordSample(
                missingCodeFieldsCaptor.capture(), eq("appApiLog"), eq("tblEventLog"));
        JSONObject missingCodeFields = missingCodeFieldsCaptor.getValue();
        assertEquals("", missingCodeFields.getString("businessTypeCode"));
        assertEquals("", missingCodeFields.getString("contractStageCode"));
    }

    @Test
    void shouldSkipCaptureAndWriteWhenDisabled() throws Exception {
        properties.setEnabled(false);

        ZhishuEventLogRecorder.PreparedEvent event =
                recorder.capture(new Object[]{"{\"schema\":\"2.0\"}"});
        recorder.record(event, true);

        assertEquals(null, event);
        verify(feishuBitableClient, never())
                .createAppTableRecordSample(any(), any(), any());
    }

    @Test
    void shouldCreateTaskIdWhenTableConfigurationIsMissing() throws Exception {
        properties.setTableId("");

        ZhishuEventLogRecorder.PreparedEvent event =
                recorder.capture(new Object[]{"{\"schema\":\"2.0\",\"event\":{}}"});
        recorder.record(event, true);

        assertNotNull(event);
        assertNotNull(event.getTaskId());
        verify(feishuBitableClient, never())
                .createAppTableRecordSample(any(), any(), any());
    }

    @Test
    void shouldNotAffectCallbackWhenExecutorRejectsTask() {
        Executor rejectedExecutor = task -> {
            throw new RejectedExecutionException("queue full");
        };
        recorder = createRecorder(rejectedExecutor);
        ZhishuEventLogRecorder.PreparedEvent event =
                recorder.capture(new Object[]{"{\"schema\":\"2.0\",\"event\":{}}"});

        assertDoesNotThrow(() -> recorder.record(event, true));
    }

    private ZhishuEventLogRecorder createRecorder(Executor executor) {
        return new ZhishuEventLogRecorder(
                new ObjectMapper(),
                properties,
                apiLogProperties,
                feishuBitableClient,
                executor
        );
    }
}
