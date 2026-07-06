package com.hero.middleware.client.feishu.request;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.io.Serializable;

/**
 * 发送消息请求
 */
@Data
public class FeiShuMessageSendRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 接收者ID类型，作为URL参数使用，不放入请求体
     */
    @JSONField(serialize = false)
    private String receiveIdType = "user_id";

    /**
     * 接收者ID
     */
    @JSONField(name = "receive_id")
    private String receiveId;

    /**
     * 消息类型
     */
    @JSONField(name = "msg_type")
    private String msgType = "text";

    /**
     * 消息内容，JSON字符串
     */
    private String content;
}
