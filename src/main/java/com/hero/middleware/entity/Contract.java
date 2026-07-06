package com.hero.middleware.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_contract")
public class Contract implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String contractId;

    private String contractName;

    private String contractType;

    private String contractStatus;

    private String sourceType;

    private String sourceId;

    private String sourceNo;

    private String partyA;

    private String partyAName;

    private String partyB;

    private String partyBName;

    private BigDecimal contractAmount;

    private String currency;

    private String startDate;

    private String endDate;

    private String signDate;

    private String operatorId;

    private String operatorName;

    private String deptId;

    private String deptName;

    private String zhishuContractId;

    private String yuecaiContractId;

    private String draftUrl;

    private String remark;

    private String formData;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;

}
