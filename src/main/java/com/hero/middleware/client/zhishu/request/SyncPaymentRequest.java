package com.hero.middleware.client.zhishu.request;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class SyncPaymentRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String contractId;

    private String paymentId;

    private String paymentType;

    private BigDecimal amount;

    private String currency;

    private String paymentMethod;

    private String paymentStatus;

    private String paymentTime;

    private String remark;

}
