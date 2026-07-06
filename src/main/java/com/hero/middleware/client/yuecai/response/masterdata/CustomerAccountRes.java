package com.hero.middleware.client.yuecai.response.masterdata;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
public class CustomerAccountRes extends BaseRes {
    /**
     * 客户银行账户表主键 ID
     */
    private Long accountId;

    /**
     * 客户编码
     */
    private String customerCode;

    /**
     * 账户名
     */
    private String bankAccountName;

    /**
     * 账户开户行
     */
    private String bankCode;

    /**
     * 银行开户支行
     */
    private String bankLocationName;

    /**
     * 联行号/swift
     */
    private String bankLocationCode;

    /**
     * IBAN国际银行账号
     */
    private String iban;

    /**
     * 银行账号
     */
    private String bankAccountNumber;

    /**
     * 账户币种
     */
    private String pkCurrtype;

    /**
     * 是否默认账户
     */
    private Integer primaryFlag;

    /**
     * 账户状态
     */
    private String accountStatus;

    /**
     * 启用标志
     */
    private Boolean enabledFlag;

    /**
     * 账户性质
     */
    private String accountProperty;

    /**
     * 备注
     */
    private String notes;

    /**
     * 最新创建人/修改人
     */
    private Long lastUpdatedBy;

    /**
     * 最新创建/修改日期
     */
    private Date lastUpdatedDate;
}
