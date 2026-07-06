package com.hero.middleware.client.feishu.response;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class FeiShuMessageSendResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer code;

    private String msg;

    private DataInfo data;

    public boolean isSuccess() {
        return code != null && code == 0;
    }

    @Data
    public static class DataInfo implements Serializable {

        private static final long serialVersionUID = 1L;

        @JSONField(name = "message_id")
        private String messageId;

        @JSONField(name = "root_id")
        private String rootId;

        @JSONField(name = "parent_id")
        private String parentId;

        @JSONField(name = "thread_id")
        private String threadId;

        @JSONField(name = "msg_type")
        private String msgType;

        @JSONField(name = "create_time")
        private String createTime;

        @JSONField(name = "update_time")
        private String updateTime;

        private Boolean deleted;

        private Boolean updated;

        @JSONField(name = "chat_id")
        private String chatId;

        @JSONField(name = "sender")
        private Map<String, Object> sender;

        private String body;
    }
}
