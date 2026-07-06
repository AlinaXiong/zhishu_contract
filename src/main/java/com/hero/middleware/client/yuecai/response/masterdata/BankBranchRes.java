package com.hero.middleware.client.yuecai.response.masterdata;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class BankBranchRes extends BaseRes {
    /**
     * 银行分行定义表主键 ID
     */
    private Long cnapsId;

    /**
     * 银行代码
     */
    private String bankCode;

    /**
     * 银行分行代码
     */
    private String bankLocationCode;

    /**
     * 关联联行号
     */
    private String refBankLocationCode;

    /**
     * 银行分行名称
     */
    private String bankLocationName;

    /**
     * 支行省份代码
     */
    private String provinceCode;

    /**
     * 支行省份名称
     */
    private String provinceName;

    /**
     * 支行城市代码
     */
    private String cityCode;

    /**
     * 支行城市名称
     */
    private String cityName;

    /**
     * 启用标志
     */
    private Boolean enabledFlag;
}
