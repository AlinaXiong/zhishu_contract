package com.hero.middleware.client.yuecai.response.masterdata;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class BankRes extends BaseRes {
    /**
     * 银行定义表主键 ID
     */
    private Long bankId;

    /**
     * 银行代码
     */
    private String bankCode;

    /**
     * 银行名称
     */
    private String bankName;

    /**
     * 银行简称
     */
    private String bankNameAlt;

    /**
     * 国家
     */
    private String countryCode;

    /**
     * 银行类型
     */
    private String bankType;

    /**
     * 图标
     */
    private String icon;

    /**
     * 启用标志
     */
    private Boolean enabledFlag;
}
