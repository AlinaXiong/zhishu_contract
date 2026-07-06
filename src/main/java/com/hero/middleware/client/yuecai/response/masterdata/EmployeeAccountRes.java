package com.hero.middleware.client.yuecai.response.masterdata;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class EmployeeAccountRes extends BaseRes {
    /**
     * 员工银行账号信息表主键 ID
     */
    private Long employeeAccountId;

    /**
     * 员工代码
     */
    private String employeeCode;

    /**
     * 行号
     */
    private Long lineNumber;

    /**
     * 银行代码
     */
    private String bankCode;

    /**
     * 银行名称
     */
    private String bankName;

    /**
     * 分行代码
     */
    private String bankLocationCode;

    /**
     * 分行名称
     */
    private String bankLocation;

    /**
     * 分行所在省
     */
    private String provinceCode;

    /**
     * 省名称
     */
    private String provinceName;

    /**
     * 分行所在城市
     */
    private String cityCode;

    /**
     * 市名称
     */
    private String cityName;

    /**
     * 银行帐号
     */
    private String bankAccountNumber;

    /**
     * 银行户名
     */
    private String bankAccountName;

    /**
     * 账户代码
     */
    private String bankAccountCode;

    /**
     * 备注
     */
    private String notes;

    /**
     * 主帐号标志
     */
    private Boolean primaryFlag;

    /**
     * 启用标志
     */
    private Boolean enabledFlag;
}
