package com.hero.middleware.client.yuecai.request;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ProcessInstanceRequest {
    /**
     * 流程编码（唯一标识）
     * 判断定义是否存在，创建空图流程并直接发布（可以发起实例）
     */
    private String flowCode;

    /**
     * 租户ID
     * 租户ID与租户编码必输其中一个
     */
    private Long tenantId;

    /**
     * 租户编码
     * 租户ID与租户编码必输其中一个
     */
    private String tenantNum;

    /**
     * 实例状态
     * RUN 运行中
     * SUSPEND 挂起
     * END 正常结束
     * INTERRUPT 中断
     * WITHDRAW 已撤回
     * EXCEPTION 异常暂挂
     * BLOCK 阻塞
     */
    private String instanceStatus;

    /**
     * 快捷表单展示数据json，用于监控、发起的流程、参与的流程等实例层级详情查看表单
     */
    private String formJson;

    /**
     * 流程实例ID，实例唯一标识
     */
    private Long instanceId;

    /**
     * 单据编码
     * 判断是创建实例还是更新实例
     */
    private String businessKey;

    /**
     * 实例跳转地址，可以配置多端地址
     * 传递页面打开类型：openType IFRAM/NEW_TAB
     */
    private Map<String, Object> links;

    /**
     * 实例描述
     */
    private String description;

    /**
     * 发起人用户ID
     */
    private String starterUser;

    /**
     * 发起人所属租户ID
     * 租户ID与租户编码必输其中一个
     */
    private Long starterTenantId;

    /**
     * 发起人所属租户编码
     * 租户ID与租户编码必输其中一个
     */
    private String starterTenantNum;

    /**
     * 实例发起时间，yyyy-MM-dd HH:mm:ss
     */
    private String startTime;

    /**
     * 实例结束时间，yyyy-MM-dd HH:mm:ss
     */
    private String endTime;

    /**
     * 实例更新时间，增量更新时，用于过滤更新调用
     * 传入的updateTime大于上次更新时间的调用会被执行
     */
    private String updateTime;

    /**
     * 实例更新方式：ALL 全量 UPDATE 增量，默认
     */
    private String updateMethod;

    /**
     * 审批任务列表
     */
    private List<ProcessTaskRequest> taskList;

    /**
     * 名称多语言
     * Map结构，key为语言代码，value为对应的翻译文本
     */
    private Map<String, String> tls;

    /**
     * 流程审批结果
     * APPROVED 通过
     * REJECTED 驳回
     */
    private String approveResult;

    /**
     * 启动附件
     * 上传文件至文件H0文件服务后获取uuid，可以传递附件
     */
    private String attachmentUuid;
}
