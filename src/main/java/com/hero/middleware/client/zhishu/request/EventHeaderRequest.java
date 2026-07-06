package com.hero.middleware.client.zhishu.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class EventHeaderRequest {
    /**
     * 事件 ID
     */
    @JsonProperty(value = "event_id")
    private String eventId;
    /**
     * 事件类型
     */
    @JsonProperty(value = "event_type")
    private String eventType;
    /**
     * 事件创建时间戳（单位：毫秒）
     */
    @JsonProperty(value = "create_time")
    private String createTime;
    /**
     * 事件 Token
     */
    private String token;
    /**
     * 应用 ID
     */
    @JsonProperty(value = "app_id")
    private String appId;
    /**
     * 租户 Key
     */
    @JsonProperty(value = "tenant_key")
    private String tenantKey;
}
