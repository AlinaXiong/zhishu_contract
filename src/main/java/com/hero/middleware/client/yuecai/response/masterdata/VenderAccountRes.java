package com.hero.middleware.client.yuecai.response.masterdata;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
public class VenderAccountRes extends BaseRes {
    /**
     * 供应商银行账户表主键 ID
     */
    private Long accountId;

    /**
     * 供应商编码
     */
    private String supplierCode;

    /**
     * 账户名
     */
    private String bankAccountName;

    /**
     * 账户备注名
     */
    private String noteData;

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
     * 收款方国家
     */
    private String bankCountry;

    /**
     * ABA ROUTING NO.（银行路由号）
     */
    private String abaRoutingNo;

    /**
     * WIRE
     */
    private String wire;

    /**
     * 供应商联系地址
     */
    private String address;

    /**
     * 供应商联系电话
     */
    private String telephone;

    /**
     * 是否默认账户
     */
    private Integer primaryFlag;

    /**
     * 账户状态
     */
    private String enabledFlag;

    /**
     * 账户性质
     */
    private String accountProperty;

    /**
     * 身份证号
     */
    private String taxIdentifier;

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
