package com.hero.middleware.client.yuecai.response.masterdata;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
public class CompanyRes extends BaseRes {
    /**
     * 管理公司表主键 ID
     */
    private Long companyId;

    /**
     * 公司代码
     */
    private String companyCode;

    /**
     * 公司简称
     */
    private String companyShortName;

    /**
     * 公司全称
     */
    private String companyFullName;

    /**
     * 管理组织代码
     */
    private String magOrgCode;

    /**
     * 公司地址
     */
    private String address;

    /**
     * 公司级别代码
     */
    private String companyLevelCode;

    /**
     * 父公司代码
     */
    private String parentCompanyCode;

    /**
     * 系统时区主键
     */
    private Long systemTimezoneId;

    /**
     * 默认语言
     */
    private String language;

    /**
     * 管理币种
     */
    private String managingCurrency;

    /**
     * 公司主岗位代码
     */
    private String chiefPositionCode;

    /**
     * 启用日期
     */
    private Date startDateActive;

    /**
     * 失效日期
     */
    private Date endDateActive;

    /**
     * 公司信息网址
     */
    private String companyInfoUrl;
}
