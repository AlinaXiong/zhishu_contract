package com.hero.middleware.component;

import com.hero.middleware.client.feishu.FeiShuApiClient;
import com.hero.middleware.client.feishu.request.FeiShuMessageSendRequest;
import com.hero.middleware.client.feishu.response.FeiShuMessageSendResponse;
import com.hero.middleware.config.ApiLogProperties;
import com.hero.middleware.dto.ApiLogEvent;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApiFailureAlertNotifierTest {

    @Test
    void shouldSendToChatAndUserAndDeduplicateRepeatedFailure() {
        FeiShuApiClient feiShuApiClient = mock(FeiShuApiClient.class);
        FeiShuMessageSendResponse successResponse = new FeiShuMessageSendResponse();
        successResponse.setCode(0);
        when(feiShuApiClient.sendMessage(any(FeiShuMessageSendRequest.class)))
                .thenReturn(successResponse);

        ApiLogProperties properties = new ApiLogProperties();
        properties.getAlert().setEnabled(true);
        properties.getAlert().setReceiveChatIds(Collections.singletonList("oc_alert_chat"));
        properties.getAlert().setReceiveUserIds(Collections.singletonList("alert_user"));
        properties.getAlert().setDedupWindowSeconds(300);

        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("test");
        ApiFailureAlertNotifier notifier =
                new ApiFailureAlertNotifier(feiShuApiClient, properties, environment);
        ApiLogEvent event = ApiLogEvent.builder()
                .targetSystem("业财")
                .action("同步合同")
                .httpMethod("POST")
                .url("https://yuecai.test/contracts")
                .httpStatus(500)
                .startTime(System.currentTimeMillis())
                .build();

        notifier.notifyFailure("10001", event, "server error");
        notifier.notifyFailure("10002", event, "server error");

        verify(feiShuApiClient, times(2)).sendMessage(any(FeiShuMessageSendRequest.class));
    }
}
