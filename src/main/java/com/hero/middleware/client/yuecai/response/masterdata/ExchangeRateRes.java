package com.hero.middleware.client.yuecai.response.masterdata;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
public class ExchangeRateRes extends BaseRes {
    /**
     * 汇率唯一KEY
     */
    private String exchangeKey;

    /**
     * 汇率ID
     */
    private Long exchangeRateId;

    /**
     * 汇率类型CODE
     */
    private String rateTypeCode;

    /**
     * 被转换币种CODE
     */
    private String fromCurrencyCode;

    /**
     * 转换币种CODE
     */
    private String toCurrencyCode;

    /**
     * 中转币种CODE
     */
    private String transitCurrencyCode;

    /**
     * 汇率日期
     */
    private Date rateDate;

    /**
     * 汇率
     */
    private BigDecimal rate;

    /**
     * 启用标志
     */
    private Boolean enabledFlag;

    /**
     * 起始日期
     */
    private Date startDate;

    /**
     * 截止日期
     */
    private Date endDate;

    /**
     * 交叉汇率基准币种code
     */
    private String currencyCode;

    /**
     * 被转换汇率，用于计算最终交叉汇率，计算方式为 fromRate*toRate
     */
    private BigDecimal fromRate;

    /**
     * 转换汇率，用于计算最终交叉汇率，计算方式为 fromRate*toRate
     */
    private BigDecimal toRate;

}
