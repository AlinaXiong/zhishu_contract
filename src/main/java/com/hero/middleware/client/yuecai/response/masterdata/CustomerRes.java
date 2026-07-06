package com.hero.middleware.client.yuecai.response.masterdata;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class CustomerRes extends BaseRes{
    /**
     * 系统级客户主数据表主键 ID
     */
    private Long customerId;

    /**
     * 启用标志
     */
    private Boolean enabledFlag;

    /**
     * 申请人
     */
    private Long createdBy;

    /**
     * 创建日期
     */
    private Date creationDate;

    /**
     * 最近更新人
     */
    private Long lastUpdatedBy;

    /**
     * 最近更新日期
     */
    private Date lastUpdateDate;

    /**
     * 企业类型
     */
    private String companyBusinessType;

    /**
     * 客户所属国家
     */
    private String pkCountry;

    /**
     * 客户基本分类
     */
    private String pkCustclass;

    /**
     * 客户名称
     */
    private String description;

    /**
     * 纳税识别号/身份证号/护照号
     */
    private String taxpayerNumber;
    private String taxIdentifier;

    /**
     * 客户编码
     */
    private String customerCode;

    /**
     * 供应商编码
     */
    private String venderCode;
    private String supplierCode;

    /**
     * 注册资金币种
     */
    private String pkCurrtype;

    /**
     * 客户所属省份
     */
    private String province;

    /**
     * 客户所属城市
     */
    private String city;

    /**
     * 客户明细地址
     */
    private String paperReceiverAddress;

    /**
     * 客商所属集团公司
     */
    private String companyBelong;

    /**
     * 客商简介
     */
    private String introduction;

    private List<CustomerLine> contactList;

    @Data
    public static class CustomerLine {
        //联系人岗位
        private String contactPosition;
        //联系人姓名
        private String contactName;
        //电话
        private String telephone;
        //移动电话
        private String mobilePhone;
        //邮箱
        private String email;
    }
}
