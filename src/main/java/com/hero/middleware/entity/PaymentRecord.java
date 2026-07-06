package com.hero.middleware.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_payment_record")
public class PaymentRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String paymentId;

    private String contractId;

    private String zhishuPaymentId;

    private String yuecaiPaymentId;

    private String paymentType;

    private BigDecimal amount;

    private String currency;

    private String paymentMethod;

    private String paymentStatus;

    private String paymentTime;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;

}
