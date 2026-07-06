package com.hero.middleware.client.zhishu.response;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ApprovalResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer code;

    private String msg;

    private ProcessInstance data;

    @Data
    public static class ProcessInstance implements Serializable {

        private static final long serialVersionUID = 1L;

        private String processInstanceId;

        private String processDefinitionId;

        private String processDefinitionKey;

        private String instanceStatus;

        private String initiatorId;

        private String startTime;

        private String endTime;

        private MultiLanguage processSubject;

        private MultiLanguage processName;

        private List<TaskInstance> taskInstanceList;

        private List<Notice> noticeList;

        private String bizKey;

        private Long latestProcessEventSequenceId;

    }

    @Data
    public static class MultiLanguage implements Serializable {

        private static final long serialVersionUID = 1L;

        private String zh;

        private String en;

        private String ja;

    }

    @Data
    public static class TaskInstance implements Serializable {

        private static final long serialVersionUID = 1L;

        private String taskInstanceId;

        private String nodeId;

        private MultiLanguage nodeName;

        private String createTime;

        private String endTime;

        private MultiLanguage taskSubject;

        private List<String> assigneeIds;

        private String commandType;

        private MultiLanguage commandTypeName;

        private String taskComment;

        private String formUrl;

        private String appUrl;

    }

    @Data
    public static class Notice implements Serializable {

        private static final long serialVersionUID = 1L;

        private String noticeId;

        private String taskInstanceId;

        private MultiLanguage noticeSubject;

        private String createTime;

        private String updateTime;

        private String sendUserId;

        private String noticedUserId;

        private String formUrl;

        private String appUrl;

    }

}
