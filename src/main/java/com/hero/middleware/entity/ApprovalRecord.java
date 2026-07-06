package com.hero.middleware.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("t_approval_record")
public class ApprovalRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String approvalId;

    private String contractId;

    private String approverId;

    private String approverName;

    private String approvalStatus;

    private String approvalOpinion;

//    private String currentNode;

    private String nextApprover;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;

}
