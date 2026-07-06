package com.hero.middleware.component;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hero.middleware.client.feishu.FeiShuApiClient;
import com.hero.middleware.client.feishu.request.FeiShuMessageSendRequest;
import com.hero.middleware.client.feishu.response.FeiShuMessageSendResponse;
import com.hero.middleware.config.ApiLogProperties;
import com.hero.middleware.dto.ApiLogEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ApiFailureAlertNotifier {

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("Asia/Shanghai"));

    private final FeiShuApiClient feiShuApiClient;

    private final ApiLogProperties apiLogProperties;

    private final Environment environment;

    private final Map<String, AlertState> alertStates = new ConcurrentHashMap<>();

    public ApiFailureAlertNotifier(FeiShuApiClient feiShuApiClient,
                                   ApiLogProperties apiLogProperties,
                                   Environment environment) {
        this.feiShuApiClient = feiShuApiClient;
        this.apiLogProperties = apiLogProperties;
        this.environment = environment;
    }

    public synchronized void notifyFailure(String taskId, ApiLogEvent event, String errorSummary) {
        ApiLogProperties.Alert alert = apiLogProperties.getAlert();
        if (!alert.isEnabled() || !hasRecipients(alert)) {
            return;
        }

        long now = System.currentTimeMillis();
        long dedupWindowMillis = Math.max(0L, alert.getDedupWindowSeconds()) * 1000L;
        String dedupKey = buildDedupKey(event, errorSummary);
        AlertState previousState = alertStates.get(dedupKey);
        if (previousState != null && now - previousState.lastSentTime < dedupWindowMillis) {
            previousState.suppressedCount++;
            return;
        }

        int suppressedCount = previousState == null ? 0 : previousState.suppressedCount;
        alertStates.put(dedupKey, new AlertState(now));
        cleanupExpiredStates(now, dedupWindowMillis);

        String content = JSON.toJSONString(buildAlertCard(taskId, event, errorSummary, suppressedCount));
        for (String chatId : alert.getReceiveChatIds()) {
            sendMessage("chat_id", chatId, content);
        }
        for (String userId : alert.getReceiveUserIds()) {
            sendMessage("user_id", userId, content);
        }
    }

    private void sendMessage(String receiveIdType, String receiveId, String content) {
        if (!hasText(receiveId)) {
            return;
        }
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                FeiShuMessageSendRequest request = new FeiShuMessageSendRequest();
                request.setReceiveIdType(receiveIdType);
                request.setReceiveId(receiveId.trim());
                request.setMsgType("interactive");
                request.setContent(content);
                FeiShuMessageSendResponse response = feiShuApiClient.sendMessage(request);
                if (response != null && response.isSuccess()) {
                    return;
                }
                throw new IllegalStateException("飞书消息发送失败："
                        + (response == null ? "响应为空" : response.getMsg()));
            } catch (Exception e) {
                if (attempt == 2) {
                    log.error("发送API失败告警失败，接收方类型={}，接收方ID={}",
                            receiveIdType, receiveId, e);
                } else {
                    sleepQuietly(300L);
                }
            }
        }
    }

    private JSONObject buildAlertCard(String taskId, ApiLogEvent event, String errorSummary, int suppressedCount) {
        JSONObject card = new JSONObject();

        JSONObject config = new JSONObject();
        config.put("wide_screen_mode", true);
        card.put("config", config);

        JSONObject title = new JSONObject();
        title.put("tag", "plain_text");
        title.put("content", "接口调用失败");

        JSONObject header = new JSONObject();
        header.put("template", "red");
        header.put("title", title);
        card.put("header", header);

        StringBuilder content = new StringBuilder();
        content.append("运行环境：").append(getEnvironmentName()).append('\n');
        content.append("任务号：").append(defaultText(taskId)).append('\n');
        content.append("目标系统：").append(defaultText(event.getTargetSystem())).append('\n');
        content.append("执行动作：").append(defaultText(event.getAction())).append('\n');
        content.append("请求接口：").append(defaultText(event.getHttpMethod()))
                .append(' ').append(defaultText(event.getUrl())).append('\n');
        content.append("HTTP 状态：").append(event.getHttpStatus() == null ? "-" : event.getHttpStatus()).append('\n');
        content.append("发生时间：").append(formatTime(event.getStartTime())).append('\n');
        content.append("错误摘要：").append(defaultText(errorSummary));
        if (suppressedCount > 0) {
            content.append('\n').append("上个窗口已抑制：").append(suppressedCount).append(" 次重复告警");
        }

        JSONObject text = new JSONObject();
        text.put("tag", "plain_text");
        text.put("content", content.toString());

        JSONObject element = new JSONObject();
        element.put("tag", "div");
        element.put("text", text);

        JSONArray elements = new JSONArray();
        elements.add(element);
        card.put("elements", elements);
        return card;
    }

    private boolean hasRecipients(ApiLogProperties.Alert alert) {
        return alert.getReceiveChatIds().stream().anyMatch(this::hasText)
                || alert.getReceiveUserIds().stream().anyMatch(this::hasText);
    }

    private String buildDedupKey(ApiLogEvent event, String errorSummary) {
        return defaultText(event.getTargetSystem()) + '|'
                + defaultText(event.getAction()) + '|'
                + defaultText(event.getUrl()) + '|'
                + defaultText(errorSummary);
    }

    private void cleanupExpiredStates(long now, long dedupWindowMillis) {
        if (alertStates.size() < 1000) {
            return;
        }
        long retentionMillis = Math.max(60000L, dedupWindowMillis * 2);
        alertStates.entrySet().removeIf(entry -> now - entry.getValue().lastSentTime > retentionMillis);
    }

    private String getEnvironmentName() {
        String[] profiles = environment.getActiveProfiles();
        return profiles.length == 0 ? "默认环境" : String.join(",", Arrays.asList(profiles));
    }

    private String formatTime(Long epochMillis) {
        long value = epochMillis == null ? System.currentTimeMillis() : epochMillis;
        return TIME_FORMATTER.format(Instant.ofEpochMilli(value));
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String defaultText(String value) {
        return hasText(value) ? value : "-";
    }

    private static class AlertState {

        private final long lastSentTime;

        private int suppressedCount;

        private AlertState(long lastSentTime) {
            this.lastSentTime = lastSentTime;
        }
    }
}
