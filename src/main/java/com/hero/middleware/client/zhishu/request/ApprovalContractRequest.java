package com.hero.middleware.client.zhishu.request;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ApprovalContractRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 任务实例id，流程作废操作时非必填，其他操作必填，若该字段非空，则以这一任务实例id为准
     */
    @JSONField(name = "task_instance_id")
    private String taskInstanceId;

    /**
     * 审批操作code
     * 示例值："general"
     * 可选值有：
     * general：审批同意
     * terminationProcess：流程作废
     * rollBackTaskByExpression：审批拒绝
     * recover：撤回到提交节点
     */
    @JSONField(name = "command_type")
    private String commandType;

    /**
     * 审批人id
     */
    @JSONField(name = "assignee_id")
    private String assigneeId;

    /**
     * 审批意见，审批操作为「拒绝」时，该字段必填
     */
    @JSONField(name = "task_comment")
    private String taskComment;

    /**
     * 审批id
     */
    private String processInstanceId;
}
