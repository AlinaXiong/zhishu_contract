package com.hero.middleware.client.zhishu.response;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class ZhishuCreateContractResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer code;

    private String msg;

    private ContractData data;

    public boolean isSuccess() {
        return code != null && code == 0;
    }

    @Data
    public static class ContractData implements Serializable {
        private static final long serialVersionUID = 1L;

        private ContractInfo contract;
    }

    @Data
    public static class ContractInfo implements Serializable {
        private static final long serialVersionUID = 1L;

        private String contractId;

        private String contractNumber;

        private String contractName;

        private Integer contractStatusCode;

        private String contractStatusName;

        private String createUserId;

        private String createUserName;

        private String departmentId;

        private String departmentName;

        private BigDecimal amount;

        private String currencyCode;

        private String startDate;

        private String endDate;

        private String remark;

        private MultiUrl multiUrl;

        private String sourceId;

        private String createTime;

        private String updateTime;
    }

    @Data
    public static class MultiUrl implements Serializable {
        private static final long serialVersionUID = 1L;

        private String pcUrl;

        private String mobileUrl;
    }

}
