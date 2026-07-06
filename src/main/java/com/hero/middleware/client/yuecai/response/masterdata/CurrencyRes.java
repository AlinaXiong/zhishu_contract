package com.hero.middleware.client.yuecai.response.masterdata;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class CurrencyRes extends BaseRes {
    /**
     * 国家CODE
     */
    private String countryCode;

    /**
     * 国家 ID
     */
    private Long countryId;

    /**
     * 国家描述
     */
    private String countryName;

    /**
     * 币种 CODE
     */
    private String currencyCode;

    /**
     * 币种 ID
     */
    private Long currencyId;

    /**
     * 币种描述
     */
    private String currencyName;

    /**
     * 币种符号
     */
    private String currencySymbol;

    /**
     * 默认精度
     */
    private Integer defaultPrecision;

    /**
     * 启用标志
     */
    private Boolean enabledFlag;

    /**
     * 财务精度
     */
    private Integer financialPrecision;
}
