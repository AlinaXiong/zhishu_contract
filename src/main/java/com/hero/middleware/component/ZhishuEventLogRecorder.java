package com.hero.middleware.component;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hero.middleware.client.feishu.FeishuBitableClient;
import com.hero.middleware.client.zhishu.request.BaseEventRequest;
import com.hero.middleware.config.ApiLogProperties;
import com.hero.middleware.config.ZhishuEventLogProperties;
import com.hero.middleware.enums.ContractBusinessTypeEnum;
import com.hero.middleware.enums.ContractStageEnum;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

@Slf4j
@Component
public class ZhishuEventLogRecorder {

    private static final Snowflake TASK_ID_GENERATOR = IdUtil.getSnowflake();

    private final ObjectMapper objectMapper;

    private final ZhishuEventLogProperties properties;

    private final ApiLogProperties apiLogProperties;

    private final FeishuBitableClient feishuBitableClient;

    private final Executor executor;

    public ZhishuEventLogRecorder(ObjectMapper objectMapper,
                                  ZhishuEventLogProperties properties,
                                  ApiLogProperties apiLogProperties,
                                  FeishuBitableClient feishuBitableClient,
                                  @Qualifier("zhishuEventLogExecutor") Executor executor) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.apiLogProperties = apiLogProperties;
        this.feishuBitableClient = feishuBitableClient;
        this.executor = executor;
    }

    @PostConstruct
    public void validateConfiguration() {
        if (properties.isEnabled() && !hasTableConfiguration()) {
            log.warn("智书事件日志已启用，但未完整配置 api-log.api-app-token 和 "
                    + "zhishu-event-log.table-id，将跳过多维表格记录");
        }
    }

    public PreparedEvent capture(Object[] arguments) {
        if (!properties.isEnabled()) {
            return null;
        }
        String taskId = TASK_ID_GENERATOR.nextIdStr();
        if (arguments == null) {
            log.warn("智书事件日志未找到请求参数，将仅生成调用链任务号");
            return emptyEvent(taskId);
        }

        for (Object argument : arguments) {
            if (argument instanceof BaseEventRequest) {
                return captureRequest(taskId, (BaseEventRequest<?>) argument);
            }
        }
        for (Object argument : arguments) {
            if (argument instanceof String) {
                return captureJson(taskId, (String) argument);
            }
        }

        log.warn("智书事件日志未找到 BaseEventRequest 或 JSON 字符串参数，将仅生成调用链任务号");
        return emptyEvent(taskId);
    }

    public void record(PreparedEvent event, boolean success) {
        if (event == null || !properties.isEnabled() || !hasTableConfiguration()) {
            return;
        }
        try {
            executor.execute(() -> writeTableRecord(event, success));
        } catch (RejectedExecutionException e) {
            log.error("智书事件日志队列已满，本次记录已丢弃，任务号={}", event.getTaskId());
        } catch (Exception e) {
            log.error("提交智书事件日志任务失败，任务号={}", event.getTaskId(), e);
        }
    }

    private PreparedEvent captureRequest(String taskId, BaseEventRequest<?> request) {
        try {
            JsonNode root = objectMapper.valueToTree(request);
            return createPreparedEvent(taskId, root, objectMapper.writeValueAsString(request));
        } catch (Exception e) {
            log.error("序列化智书事件请求失败，将仅生成调用链任务号", e);
            return emptyEvent(taskId);
        }
    }

    private PreparedEvent captureJson(String taskId, String json) {
        try {
            BaseEventRequest<?> request = objectMapper.readValue(json, BaseEventRequest.class);
            JsonNode root = objectMapper.valueToTree(request);
            return createPreparedEvent(taskId, root, objectMapper.writeValueAsString(request));
        } catch (Exception e) {
            log.warn("解析智书事件请求失败，将仅记录原始请求内容：{}", e.getMessage());
            return new PreparedEvent(
                    taskId,
                    truncate(json),
                    null,
                    null,
                    null,
                    null
            );
        }
    }

    private PreparedEvent createPreparedEvent(String taskId, JsonNode root, String normalizedJson) {
        JsonNode header = root.path("header");
        JsonNode event = root.path("event");
        return new PreparedEvent(
                taskId,
                truncate(normalizedJson),
                firstText(header, "event_type", "eventType"),
                firstText(event, "contract_id", "contractId"),
                firstText(event, "business_type_code", "businessTypeCode"),
                firstText(event, "contract_stage_code", "contractStageCode")
        );
    }

    private PreparedEvent emptyEvent(String taskId) {
        return new PreparedEvent(taskId, null, null, null, null, null);
    }

    private void writeTableRecord(PreparedEvent event, boolean success) {
        JSONObject fields = new JSONObject();
        fields.put("任务号", event.getTaskId());
        fields.put("事件原文", defaultText(event.getOriginalEvent()));
        fields.put("event_type", defaultText(event.getEventType()));
        fields.put("contract_id", defaultText(event.getContractId()));
        fields.put("businessTypeCode", formatBusinessTypeCode(event.getBusinessTypeCode()));
        fields.put("contractStageCode", formatContractStageCode(event.getContractStageCode()));
        fields.put("执行结果", success ? "成功" : "失败");

        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                feishuBitableClient.createAppTableRecordSample(
                        fields,
                        apiLogProperties.getApiAppToken(),
                        properties.getTableId()
                );
                return;
            } catch (Exception e) {
                if (attempt == 3) {
                    log.error("写入智书事件多维表格失败，任务号={}", event.getTaskId(), e);
                } else {
                    sleepQuietly(500L * attempt);
                }
            }
        }
    }

    private String firstText(JsonNode node, String... fieldNames) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        for (String fieldName : fieldNames) {
            JsonNode value = node.get(fieldName);
            if (value != null && !value.isNull() && !value.isContainerNode()) {
                return value.asText();
            }
        }
        return null;
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        int maxLength = Math.max(1, properties.getMaxContentLength());
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String defaultText(String value) {
        return value == null ? "" : value;
    }

    private String formatBusinessTypeCode(String code) {
        if (StrUtil.isBlank(code)) {
            return "";
        }
        Integer numericCode = parseInteger(code);
        ContractBusinessTypeEnum businessType = ContractBusinessTypeEnum.getEnum(numericCode);
        return businessType == null ? code : code + "-" + businessType.getName();
    }

    private String formatContractStageCode(String code) {
        if (StrUtil.isBlank(code)) {
            return "";
        }
        Integer numericCode = parseInteger(code);
        ContractStageEnum contractStage = ContractStageEnum.getEnum(numericCode);
        return contractStage == null ? code : code + "-" + contractStage.getName();
    }

    private Integer parseInteger(String value) {
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean hasTableConfiguration() {
        return StrUtil.isNotBlank(apiLogProperties.getApiAppToken())
                && StrUtil.isNotBlank(properties.getTableId());
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Getter
    public static class PreparedEvent {

        private final String taskId;

        private final String originalEvent;

        private final String eventType;

        private final String contractId;

        private final String businessTypeCode;

        private final String contractStageCode;

        private PreparedEvent(String taskId,
                              String originalEvent,
                              String eventType,
                              String contractId,
                              String businessTypeCode,
                              String contractStageCode) {
            this.taskId = taskId;
            this.originalEvent = originalEvent;
            this.eventType = eventType;
            this.contractId = contractId;
            this.businessTypeCode = businessTypeCode;
            this.contractStageCode = contractStageCode;
        }
    }
}
