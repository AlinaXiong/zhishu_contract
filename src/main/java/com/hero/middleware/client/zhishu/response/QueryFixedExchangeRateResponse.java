package com.hero.middleware.client.zhishu.response;

import lombok.Data;

@Data
public class QueryFixedExchangeRateResponse {
    /**
     * 换算日期，换算日期
     */
    private String effectiveDate;
    /**
     * 汇率值，保留10位小数，汇率值，保留10位小数
     */
    private String exchangeRate;
    /**
     * 原始币种代码，原始币种代码
     */
    private String sourceCurrency;
    /**
     * 状态。枚举值：1_生效，0_失效，状态，枚举值：1_生效，0_失效
     */
    private Long status;
    /**
     * 目标币种代码，目标币种代码
     */
    private String targetCurrency;
}
