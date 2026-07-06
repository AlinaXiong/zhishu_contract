package com.hero.middleware.client.zhishu.request;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class SyncReceiptRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 支付单申请人id，参照user_id_type
     */
    private String applicantUserid;
    /**
     * 支付金额
     */
    private String financeAmount;
    /**
     * 币种类型国际编码（"CNY", "USD"等）
     * CNY(1, "人民币元", "CNY", "¥"),
     * USD(2, "美元", "USD", "$"),
     * JPY(3, "日元", "JPY", "J￥"),
     * EUR(4, "欧元", "EUR", "€"),
     * GBP(5, "英镑", "GBP", "￡"),
     * SGD(6, "新加坡元", "SGD", "S$"),
     * THB(7, "泰铢", "THB", "฿");
     */
    private String financeCurrency;
    /**
     * 支付申请单号
     */
    private String financeNumber;
    /**
     * 支付申请单跳转链接
     */
    private String financeurl;
    /**
     * 支付行信息集合
     */
    private List<PaymentLine> paymentLines;

    @Data
    public static class PaymentLine {
        /**
         * 币种类型国际编码（"CNY", "USD"等）
         * CNY(1, "人民币元", "CNY", "¥"),
         * USD(2, "美元", "USD", "$"),
         * JPY(3, "日元", "JPY", "J￥"),
         * EUR(4, "欧元", "EUR", "€"),
         * GBP(5, "英镑", "GBP", "￡"),
         * SGD(6, "新加坡元", "SGD", "S$"),
         * THB(7, "泰铢", "THB", "฿");
         */
        private String currency;
        /**
         * 交易关联单据信息
         */
        private List<Relation> relations;
        /**
         * 支付行号
         */
        private String sourceid;
        /**
         * 交易方对方账户
         */
        private TradingPartyAccount tradingPartyAccount;
        /**
         * 交易方对方编码
         */
        private String tradingPartyCode;
        /**
         * 交易金额
         */
        private String transactionAmount;
        /**
         * 交易状态
         * IN_APPROVAL(0, "IN_APPROVAL", "审批中"),
         * APPROVAL_COMPLETE(1,"APPROVAL_COMPLETE", "审批完成"),
         * APPROVAL_REVOKE(2, "APPROVAL_REVOKE", "审批驳回"),
         * APPROVAL_WITHDRAW(3,"APPROVAL_WITHDRAW", "审批撤回"),
         * PAYMENT_PROCESSING(4,"PAYMENT_PROCESSING", "支付中"),
         * PAYMENT_SUCCESS(5, "PAYMENT_SUCCESS", "支付成功"),
         * PAYMENT_FAIL(6, "PAYMENT_FAIL", "支付失败"),
         * DISCARD(7, "DISCARD", "作废"),
         * IN_DRAFT(8, "IN_DRAFT", "草稿"),
         * DELETE(9, "DELETE", "删除"),
         */
        private String transactionStatus;
        /**
         * 交易时间
         * "yyyy-MM-dd HH:mm:ss"
         */
        private String transactionTime;
        /**
         * 支付行跳转链接, 不填传空字符串
         */
        private String url;
    }

    @Data
    public static class Relation {
        /**
         * 金额
         */
        private String amount;
        /**
         * 合同id
         */
        private String contractId;
        /**
         * 付款计划唯一标识
         */
        private String paymentPlanUuid;

    }
    /**
     * 交易方对方账户
     */
    @Data
    public static class TradingPartyAccount {
        /**
         * 交易方对方银行账号
         */
        private String accountNumber;

    }
}
