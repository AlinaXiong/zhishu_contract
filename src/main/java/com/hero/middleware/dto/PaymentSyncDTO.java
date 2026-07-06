package com.hero.middleware.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class PaymentSyncDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "合同ID不能为空")
    private String contractId;

//    @NotBlank(message = "创建人ID不能为空")
    private String createUserId;

    @NotBlank(message = "业务编号不能为空")
    private String businessCode;

    @NotNull(message = "付款金额不能为空")
    private BigDecimal paymentAmount;

    @NotBlank(message = "币种不能为空")
    private String currencyCode;

//    @NotBlank(message = "交易状态不能为空")
    private String transactionStatus;

    @NotBlank(message = "交易时间不能为空")
    private String transactionTime;

    @NotBlank(message = "对方主体编码不能为空")
    private String counterPartyCode;

    private String paymentPlanUuid;
    @NotBlank(message = "对方主体银行账号不能为空")
    private String bankAccountNumber;

    public String getCurrencyName() {
        if ("CNY".equalsIgnoreCase(currencyCode)) {
            return "人民币元";
        } else if ("USD".equalsIgnoreCase(currencyCode)) {
            return "美元";
        } else if ("JPY".equalsIgnoreCase(currencyCode)) {
            return "日元";
        } else if ("EUR".equalsIgnoreCase(currencyCode)) {
            return "欧元";
        }
        return currencyCode;
    }

}
