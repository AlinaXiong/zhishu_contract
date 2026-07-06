package com.hero.middleware.client.zhishu.response;

import lombok.Data;

import java.io.Serializable;

@Data
public class ReceiptResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer code;
    private String msg;
    private PaymentResponse.PaymentRecordResourceVo data;

    @Data
    public static class PaymentRecordResourceVo {
        private PaymentResponse.PaymentRecord paymentRecord;

    }
    @Data
    public static class PaymentRecord {
        private long businessTypeCode;
        private String businessTypeName;
        private String currencyCode;
        private String currencyName;
        private String departmentid;
        private String extraInfo;
        private String failAmount;
        private String groupid;
        private String operatorUserid;
        private String paymentid;
        private String paymentRecordid;
        private String relatedid;
        private String sourceid;
        private String succeedAmount;
        private String transactionAmount;
        private String transactionTime;

    }

}
