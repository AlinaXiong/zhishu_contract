package com.hero.middleware.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.lang.Snowflake;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hero.middleware.client.feishu.FeishuBitableClient;
import com.hero.middleware.component.ApiFailureAlertNotifier;
import com.hero.middleware.config.ApiLogProperties;
import com.hero.middleware.context.ApiLogTaskContext;
import com.hero.middleware.context.ApiLogTableContext;
import com.hero.middleware.dto.ApiLogEvent;
import com.hero.middleware.service.ApiLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

@Slf4j
@Service
public class ApiLogServiceImpl implements ApiLogService {

    private static final Snowflake TASK_ID_GENERATOR = IdUtil.getSnowflake();

    private final ApiLogProperties apiLogProperties;

    private final FeishuBitableClient feishuBitableClient;

    private final ApiFailureAlertNotifier apiFailureAlertNotifier;

    private final Executor apiLogExecutor;

    private final Executor apiAlertExecutor;

    public ApiLogServiceImpl(ApiLogProperties apiLogProperties,
                             FeishuBitableClient feishuBitableClient,
                             ApiFailureAlertNotifier apiFailureAlertNotifier,
                             @Qualifier("apiLogExecutor") Executor apiLogExecutor,
                             @Qualifier("apiAlertExecutor") Executor apiAlertExecutor) {
        this.apiLogProperties = apiLogProperties;
        this.feishuBitableClient = feishuBitableClient;
        this.apiFailureAlertNotifier = apiFailureAlertNotifier;
        this.apiLogExecutor = apiLogExecutor;
        this.apiAlertExecutor = apiAlertExecutor;
    }

    @PostConstruct
    public void validateConfiguration() {
        if (apiLogProperties.isEnabled()
                && (!hasText(apiLogProperties.getApiAppToken())
                || !hasText(apiLogProperties.getApiTableId()))) {
            log.warn("API日志已启用，但未完整配置 api-log.api-app-token 和 api-log.api-table-id，将跳过多维表格记录");
        }
    }

    @Override
    public void record(ApiLogEvent event) {
        if (!apiLogProperties.isEnabled() || event == null) {
            return;
        }

        String contextTaskId = ApiLogTaskContext.currentTaskId();
        String taskId = hasText(contextTaskId) ? contextTaskId : TASK_ID_GENERATOR.nextIdStr();
        boolean success = isSuccess(event);
        ApiLogEvent snapshot = snapshot(event);
        String errorSummary = success ? null : buildErrorSummary(snapshot);

        if (hasTableConfiguration() && !ApiLogTableContext.isSuppressed()) {
            submit(apiLogExecutor,
                    () -> writeTableRecord(taskId, snapshot, success, errorSummary),
                    "API日志");
        }
        if (!success && apiLogProperties.getAlert().isEnabled()) {
            submit(apiAlertExecutor,
                    () -> apiFailureAlertNotifier.notifyFailure(taskId, snapshot, errorSummary),
                    "API告警");
        }
    }

    boolean isSuccess(ApiLogEvent event) {
        if (hasText(event.getExceptionMessage())) {
            return false;
        }
        Integer httpStatus = event.getHttpStatus();
        if (httpStatus == null || httpStatus < 200 || httpStatus >= 300) {
            return false;
        }
        if (!hasText(event.getResponseBody())) {
            return true;
        }

        try {
            Object parsed = JSON.parse(event.getResponseBody());
            if (!(parsed instanceof JSONObject)) {
                return true;
            }
            JSONObject response = (JSONObject) parsed;
            Boolean success = response.getBoolean("success");
            if (Boolean.FALSE.equals(success)) {
                return false;
            }
            if (response.containsKey("code")) {
                String code = response.getString("code");
                if (hasText(code)) {
                    return "0".equals(code) || "200".equals(code) || "success".equals(code);
                }
            }
            return true;
        } catch (Exception ignored) {
            return true;
        }
    }

    private ApiLogEvent snapshot(ApiLogEvent event) {
        int maxLength = Math.max(1, apiLogProperties.getMaxContentLength());
        return ApiLogEvent.builder()
                .targetSystem(event.getTargetSystem())
                .action(event.getAction())
                .httpMethod(event.getHttpMethod())
                .url(event.getUrl())
                .requestParams(truncate(event.getRequestParams(), maxLength))
                .responseBody(truncate(event.getResponseBody(), maxLength))
                .httpStatus(event.getHttpStatus())
                .exceptionMessage(truncate(event.getExceptionMessage(), maxLength))
                .startTime(event.getStartTime())
                .durationMs(event.getDurationMs())
                .build();
    }

    private void writeTableRecord(String taskId, ApiLogEvent event, boolean success, String errorSummary) {
        JSONObject fields = new JSONObject();
        fields.put("任务号", taskId);
        fields.put("执行动作", defaultText(event.getAction(), event.getTargetSystem()));
        fields.put("执行结果", success ? "成功" : "失败");
        fields.put("请求接口",
                (defaultText(event.getHttpMethod(), "") + " " + defaultText(event.getUrl(), "")).trim());
        fields.put("请求入参", defaultText(event.getRequestParams(), ""));
        fields.put("返回结果", buildTableResponse(event, errorSummary));
        fields.put("执行时间", event.getStartTime() == null ? System.currentTimeMillis() : event.getStartTime());

        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                feishuBitableClient.createAppTableRecordSample(
                        fields,
                        apiLogProperties.getApiAppToken(),
                        apiLogProperties.getApiTableId()
                );
                return;
            } catch (Exception e) {
                if (attempt == 3) {
                    log.error("写入飞书多维表格API日志失败，任务号={}", taskId, e);
                } else {
                    sleepQuietly(500L * attempt);
                }
            }
        }
    }

    private String buildTableResponse(ApiLogEvent event, String errorSummary) {
        if (hasText(event.getResponseBody())) {
            return event.getResponseBody();
        }
        return defaultText(errorSummary, "");
    }

    private String buildErrorSummary(ApiLogEvent event) {
        String summary;
        if (hasText(event.getExceptionMessage())) {
            summary = event.getExceptionMessage();
        } else {
            summary = extractResponseMessage(event.getResponseBody());
        }
        if (!hasText(summary)) {
            summary = event.getHttpStatus() == null
                    ? "请求未返回 HTTP 状态"
                    : "HTTP 状态码 " + event.getHttpStatus();
        }
        int maxLength = Math.max(1, apiLogProperties.getAlert().getMaxErrorSummaryLength());
        return truncate(summary, maxLength);
    }

    private String extractResponseMessage(String responseBody) {
        if (!hasText(responseBody)) {
            return null;
        }
        try {
            JSONObject response = JSON.parseObject(responseBody);
            String message = firstText(
                    response.getString("message"),
                    response.getString("msg"),
                    response.getString("error_description"),
                    response.getString("error")
            );
            return hasText(message) ? message : responseBody;
        } catch (Exception e) {
            return responseBody;
        }
    }

    String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        String marker = "\n...[已截断，原始长度=" + value.length() + "]...\n";
        if (marker.length() >= maxLength) {
            return value.substring(0, maxLength);
        }
        int remaining = maxLength - marker.length();
        int prefixLength = remaining / 2;
        int suffixLength = remaining - prefixLength;
        return value.substring(0, prefixLength)
                + marker
                + value.substring(value.length() - suffixLength);
    }

    private void submit(Executor executor, Runnable task, String taskType) {
        try {
            executor.execute(task);
        } catch (RejectedExecutionException e) {
            log.error("{}队列已满，本次任务已丢弃", taskType);
        } catch (Exception e) {
            log.error("提交{}任务失败", taskType, e);
        }
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private String defaultText(String value, String defaultValue) {
        return hasText(value) ? value : defaultValue;
    }

    private boolean hasText(String value) {
        return StrUtil.isNotBlank(value);
    }

    private boolean hasTableConfiguration() {
        return hasText(apiLogProperties.getApiAppToken())
                && hasText(apiLogProperties.getApiTableId());
    }
}
