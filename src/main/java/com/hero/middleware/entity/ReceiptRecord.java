package com.hero.middleware.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_receipt_record")
public class ReceiptRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String receiptId;

    private String contractId;

    private String zhishuReceiptId;

    private String yuecaiReceiptId;

    private String receiptType;

    private BigDecimal amount;

    private String currency;

    private String receiptMethod;

    private String receiptStatus;

    private String receiptTime;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;

}
