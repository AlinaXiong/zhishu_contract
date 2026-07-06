package com.hero.middleware.client.zhishu.response;

import lombok.Data;

import java.util.List;

@Data
public class VendorInfoResponse {

    /**
     * 交易方唯一标识ID
     */
    private String id;

    /**
     * 注册地址国家/地区
     */
    private String adCountry;

    /**
     * 注册地址省份
     */
    private String adProvince;

    /**
     * 注册地址城市
     */
    private String adCity;

    /**
     * 注册地址详情
     */
    private String address;

    /**
     * 注册地址邮编
     */
    private String adPostcode;

    /**
     * 法定代表人
     */
    private String legalPerson;

    /**
     * 证件类型
     */
    private String certificationType;

    /**
     * 证件ID（如统一社会信用代码）
     */
    private String certificationId;

    /**
     * 联系人
     */
    private String contactPerson;

    /**
     * 联系电话（固定电话）
     */
    private String contactTelephone;

    /**
     * 联系手机
     */
    private String contactMobilePhone;

    /**
     * 传真
     */
    private String fax;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 状态：1-启用，0-禁用
     */
    private Integer status;

    /**
     * 交易方编码
     */
    private String vendor;

    /**
     * 交易方名称
     */
    private String vendorText;

    /**
     * 简称
     */
    private String shortText;

    /**
     * 交易方类型
     */
    private String vendorType;

    /**
     * 交易方类别
     */
    private String vendorCategory;

    /**
     * 交易方性质
     */
    private String vendorNature;

    /**
     * 关联员工ID
     */
    private String linkedEmployee;

    /**
     * 关联客户
     */
    private String linkedCustomer;

    /**
     * 是否风险交易方
     */
    private Boolean isRisked;

    /**
     * 是否关联法人实体
     */
    private Boolean associatedWithLegalEntity;

    /**
     * 附件列表
     */
    private List<Appendix> appendix;

    /**
     * 扩展信息列表
     */
    private List<ExtendInfo> extendInfo;

    /**
     * 银行账户列表
     */
    private List<VendorAccount> vendorAccounts;

    /**
     * 地址列表
     */
    private List<VendorAddress> vendorAddresses;

    /**
     * 公司视图列表（各公司的财务信息）
     */
    private List<VendorCompanyView> vendorCompanyViews;

    /**
     * 联系人列表
     */
    private List<VendorContact> vendorContacts;

    /**
     * 统驭科目
     */
    private String glAccount;

    /**
     * 预付定金付款条款
     */
    private String downPaymentTerm;

    /**
     * 付款条款
     */
    private String paymentTerm;

    /**
     * 交易方地点编码
     */
    private String vendorSiteCode;

    /**
     * 交易方所属部门
     */
    private String ownerDepts;

    @Data
    public static class Appendix {

        /**
         * 租户ID
         */
        private String tenantId;

        /**
         * 文件ID
         */
        private String fileId;

        /**
         * 文件名
         */
        private String fileName;

        /**
         * 文件类型
         */
        private String fileType;

        /**
         * 文件大小（字节）
         */
        private Integer fileSize;

        /**
         * 下载地址
         */
        private String downloadUrl;
    }

    @Data
    public static class ExtendInfo {

        /**
         * 字段类型：0-文本，1-选项，2-数字，3-日期，4-日期范围
         */
        private Integer fieldType;

        /**
         * 字段值（文本类型时使用）
         */
        private String fieldValue;

        /**
         * 选项列表（选项类型时使用）
         */
        private List<String> options;

        /**
         * 数值（数字类型时使用）
         */
        private Double num;

        /**
         * 日期（日期类型时使用）
         */
        private String date;

        /**
         * 日期范围（日期范围类型时使用）
         */
        private List<String> rangeDate;

        /**
         * 字段编码
         */
        private String fieldCode;

        /**
         * 附件列表（扩展信息中的附件）
         */
        private List<Appendix> appendix;
    }

    @Data
    public static class VendorAccount {

        /**
         * 银行账户记录ID
         */
        private String id;

        /**
         * 银行账号
         */
        private String account;

        /**
         * IBAN（国际银行账号）
         */
        private String iban;

        /**
         * 账户名称
         */
        private String accountName;

        /**
         * 银行ID
         */
        private String bankId;

        /**
         * 银行编码
         */
        private String bankCode;

        /**
         * SWIFT代码
         */
        private String swiftCode;

        /**
         * 交易方地点编码
         */
        private String vendorSiteCode;

        /**
         * 银行名称
         */
        private String bankName;

        /**
         * 银行缩写
         */
        private String bankAcronym;

        /**
         * 国家/地区
         */
        private String country;

        /**
         * 银行控制码
         */
        private String bankControlCode;

        /**
         * 扩展信息列表
         */
        private List<ExtendInfo> extendInfo;
    }

    @Data
    public static class VendorAddress {

        /**
         * 地址记录ID
         */
        private String id;

        /**
         * 国家/地区
         */
        private String country;

        /**
         * 省份
         */
        private String province;

        /**
         * 城市
         */
        private String city;

        /**
         * 区县
         */
        private String county;

        /**
         * 详细地址
         */
        private String address;

        /**
         * 扩展信息列表
         */
        private List<ExtendInfo> extendInfo;
    }

    @Data
    public static class VendorCompanyView {

        /**
         * 公司视图记录ID
         */
        private String id;

        /**
         * 公司代码
         */
        private String companyCode;

        /**
         * 统驭科目
         */
        private String glAccount;

        /**
         * 交易方地点编码
         */
        private String vendorSiteCode;

        /**
         * 付款条款
         */
        private String paymentTerm;

        /**
         * 预付定金付款条款
         */
        private String downPaymentTerm;

        /**
         * 扩展信息列表
         */
        private List<ExtendInfo> extendInfo;
    }

    @Data
    public static class VendorContact {

        /**
         * 联系人记录ID
         */
        private String id;

        /**
         * 联系人姓名
         */
        private String name;

        /**
         * 职位
         */
        private String position;

        /**
         * 邮箱
         */
        private String email;

        /**
         * 电话
         */
        private String phone;

        /**
         * 备注
         */
        private String remark;

        /**
         * 扩展信息列表
         */
        private List<ExtendInfo> extendInfo;
    }
}
