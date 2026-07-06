package com.hero.middleware.client.zhishu.request;

import lombok.Data;

@Data
public class BaseEventRequest<T> {
    /**
     * 事件模式
     */
    private String schema;
    /**
     * 事件头
     */
    private EventHeaderRequest header;

    private T event;
}
