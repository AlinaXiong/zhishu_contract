package com.hero.middleware.client.zhishu.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class ApprovalQueryResponse {
    /**
     * 返回码，0表示成功
     */
    private Integer code;

    /**
     * 返回消息
     */
    private String msg;

    /**
     * 实际返回的数据
     */
    private DataBean data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DataBean {

        /**
         * 审批实例详情
         */
        @JsonProperty("process_instance")
        private ProcessInstance processInstance;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProcessInstance {

        /**
         * 审批实例ID
         */
        @JsonProperty("process_instance_id")
        private String processInstanceId;

        /**
         * 流程定义ID
         */
        @JsonProperty("process_definition_id")
        private String processDefinitionId;

        /**
         * 流程定义键
         */
        @JsonProperty("process_definition_key")
        private String processDefinitionKey;

        /**
         * 实例状态（如：RUNNING）
         */
        @JsonProperty("instance_status")
        private String instanceStatus;

        /**
         * 发起人ID
         */
        @JsonProperty("initiator_id")
        private String initiatorId;

        /**
         * 开始时间（时间戳）
         */
        @JsonProperty("start_time")
        private String startTime;

        /**
         * 结束时间（时间戳）
         */
        @JsonProperty("end_time")
        private String endTime;

        /**
         * 流程主题（多语言）
         */
        @JsonProperty("process_subject")
        private MultiLanguage processSubject;

        /**
         * 流程名称（多语言）
         */
        @JsonProperty("process_name")
        private MultiLanguage processName;

        /**
         * 任务实例列表
         */
        @JsonProperty("task_instance_list")
        private List<TaskInstance> taskInstanceList;

        /**
         * 通知列表
         */
        @JsonProperty("notice_list")
        private List<Notice> noticeList;

        /**
         * 业务键
         */
        @JsonProperty("biz_key")
        private String bizKey;

        /**
         * 最新流程事件序列ID
         */
        @JsonProperty("latest_process_event_sequence_id")
        private Integer latestProcessEventSequenceId;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MultiLanguage {

        /**
         * 中文
         */
        private String zh;

        /**
         * 英文
         */
        private String en;

        /**
         * 日文
         */
        private String ja;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TaskInstance {

        /**
         * 任务实例ID
         */
        @JsonProperty("task_instance_id")
        private String taskInstanceId;

        /**
         * 节点ID
         */
        @JsonProperty("node_id")
        private String nodeId;

        /**
         * 节点名称（多语言）
         */
        @JsonProperty("node_name")
        private MultiLanguage nodeName;

        /**
         * 创建时间（时间戳）
         */
        @JsonProperty("create_time")
        private String createTime;

        /**
         * 结束时间（时间戳）
         */
        @JsonProperty("end_time")
        private String endTime;

        /**
         * 任务主题（多语言）
         */
        @JsonProperty("task_subject")
        private MultiLanguage taskSubject;

        /**
         * 处理人ID列表
         */
        @JsonProperty("assignee_ids")
        private List<String> assigneeIds;

        /**
         * 命令类型
         */
        @JsonProperty("command_type")
        private String commandType;

        /**
         * 命令类型名称（多语言）
         */
        @JsonProperty("command_type_name")
        private MultiLanguage commandTypeName;

        /**
         * 任务评论
         */
        @JsonProperty("task_comment")
        private String taskComment;

        /**
         * 表单URL（Web端）
         */
        @JsonProperty("form_url")
        private String formUrl;

        /**
         * 应用URL（移动端）
         */
        @JsonProperty("app_url")
        private String appUrl;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Notice {

        /**
         * 通知ID
         */
        @JsonProperty("notice_id")
        private String noticeId;

        /**
         * 任务实例ID
         */
        @JsonProperty("task_instance_id")
        private String taskInstanceId;

        /**
         * 通知主题（多语言）
         */
        @JsonProperty("notice_subject")
        private MultiLanguage noticeSubject;

        /**
         * 创建时间（时间戳）
         */
        @JsonProperty("create_time")
        private String createTime;

        /**
         * 更新时间（时间戳）
         */
        @JsonProperty("update_time")
        private String updateTime;

        /**
         * 发送人ID
         */
        @JsonProperty("send_user_id")
        private String sendUserId;

        /**
         * 被通知人ID
         */
        @JsonProperty("noticed_user_id")
        private String noticedUserId;

        /**
         * 表单URL（Web端）
         */
        @JsonProperty("form_url")
        private String formUrl;

        /**
         * 应用URL（移动端）
         */
        @JsonProperty("app_url")
        private String appUrl;
    }
}
