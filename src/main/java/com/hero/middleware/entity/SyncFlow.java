package com.hero.middleware.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("t_sync_flow")
public class SyncFlow {
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 流程编码（唯一标识）判断定义是否存在，创建空图流程并直接发布（可以发起实例）
     */
    private String flowCode;

    /**
     * 合同id
     */
    private String contractId;

    /**
     * 合同编码
     */
    private String contractNum;

    /**
     * 租户ID，租户ID与租户编码必输其中一个
     */
    private Long tenantId;

    /**
     * 租户编码，租户ID与租户编码必输其中一个
     */
    private String tenantNum;

    /**
     * 实例状态RUN 运行中SUSPEND 挂起END 正常结束INTERRUPT 中断WITHDRAW   已撤回EXCEPTION 异常暂挂BLOCK 阻塞
     */
    private String instanceStatus;

    /**
     * 流程实例ID，实例唯一标识
     */
    private Long instanceId;

    /**
     * 单据编码，判断是创建实例还是更新实例
     */
    private String businessKey;

    /**
     * 流程审批结果APPROVED、REJECTED
     */
    private String approveResult;

    /**
     * 实例描述
     */
    private String description;

    /**
     * 创建日期
     */
    private Date createTime;

    /**
     * 创建人
     */
    private String createCode;

    /**
     * 修改日期
     */
    private Date updateTime;

    /**
     * 修改人
     */
    private String updateCode;

    /**
     * 标志
     */
    private String flag;

    /**
     * 备注
     */
    private String remark;
}
