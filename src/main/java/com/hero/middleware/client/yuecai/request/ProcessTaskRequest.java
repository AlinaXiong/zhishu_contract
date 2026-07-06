package com.hero.middleware.client.yuecai.request;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ProcessTaskRequest {
    /**
     * 任务ID（唯一标识）
     */
    private String taskId;

    /**
     * 任务编码（实例唯一）
     */
    private String taskCode;

    /**
     * 任务审批人（用户ID）
     */
    private String taskAssignee;

    /**
     * 审批人所属租户ID
     * 审批人所属租户ID和编码必输其一
     */
    private Long taskAssigneeTenantId;

    /**
     * 审批人所属租户编码
     * 审批人所属租户ID和编码必输其一
     */
    private String taskAssigneeTenantNum;

    /**
     * 节点名称
     */
    private String nodeName;

    /**
     * 节点编码
     */
    private String nodeCode;

    /**
     * 任务发起时间，时间戳毫秒值
     */
    private String startTime;

    /**
     * 任务结束时间，时间戳毫秒值
     */
    private String endTime;

    /**
     * 任务更新时间
     */
    private String updateTime;

    /**
     * 审批动作配置
     */
    private List<ProcessActionRequest> actionList;

    /**
     * 任务跳转地址
     * 传递页面打开类型：openType IFRAM/NEW_TAB
     */
    private Map<String, Object> links;

    /**
     * 是否进行站内信通知，默认通知（使用租户配置的模板）
     * 1-通知，0-不通知，默认1
     */
    private Integer noticeFlag = 1;

    /**
     * 任务状态
     * PENDING 待审批
     * APPROVED 已通过
     * REJECTED 已驳回
     */
    private String taskStatus;

    /**
     * 名称多语言
     * Map结构，key为语言代码，value为对应的翻译文本
     */
    private Map<String, String> tls;
}
