package com.hero.middleware.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("t_contract_sync_log")
public class ContractSyncLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String contractId;

    private String syncType;

    private String syncDirection;

    private String syncStatus;

    private String requestParam;

    private String responseData;

    private String errorMessage;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

}
