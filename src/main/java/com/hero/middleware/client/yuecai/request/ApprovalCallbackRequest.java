package com.hero.middleware.client.yuecai.request;

import lombok.Data;

import java.util.Map;

@Data
public class ApprovalCallbackRequest {
    /**
     * 任务ID（唯一标识）
     */
    private String taskId;

    /**
     * 动作类型
     * APPROVE 同意
     * REJECT 驳回
     */
    private String actionType;

    /**
     * 任务审批人（用户ID）
     */
    private String operatorUser;

    /**
     * 流程编码（唯一标识）
     */
    private String flowCode;

    /**
     * 流程实例ID，实例唯一标识
     */
    private Long instanceId;

    /**
     * 审批意见
     */
    private String comment;

    /**
     * 回调token
     */
    private String token;

    /**
     * 回调额外参数
     * 同步任务时传递过来的额外参数，原样写入参数进行回调
     */
    private Map<String, Object> actionBackParam;
}
