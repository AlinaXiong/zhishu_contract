package com.hero.middleware.client.zhishu.request;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.util.List;

@Data
public class CreateVendorRequest {

    private String id;
    /**
     * 注册地址国家/地区
     * 示例：CN
     */
    @JSONField(name = "ad_country")
    private String adCountry;

    /**
     * 注册地址省份
     * 示例：MDPS00000001
     */
    @JSONField(name = "ad_province")
    private String adProvince;

    /**
     * 注册地址城市
     * 示例：MDCY00001226
     */
    @JSONField(name = "ad_city")
    private String adCity;

    /**
     * 注册地址详情
     * 示例：上海市浦东新区世纪大道1000号
     */
    @JSONField(name = "address")
    private String address;

    /**
     * 注册地址邮编
     * 示例：100100
     */
    @JSONField(name = "ad_postcode")
    private String adPostcode;

    /**
     * 法定代表人
     * 示例：张三
     */
    @JSONField(name = "legal_person")
    private String legalPerson;

    /**
     * 证件类型
     * 示例：0
     */
    @JSONField(name = "certification_type")
    private String certificationType;

    /**
     * 证件ID（如统一社会信用代码）
     * 示例：913100xxxxx555781R
     */
    @JSONField(name = "certification_id")
    private String certificationId;

    /**
     * 联系人
     * 示例：李四
     */
    @JSONField(name = "contact_person")
    private String contactPerson;

    /**
     * 联系电话（固定电话）
     * 示例：021-87853200
     */
    @JSONField(name = "contact_telephone")
    private String contactTelephone;

    /**
     * 联系手机
     * 示例：+8617621685955
     */
    @JSONField(name = "contact_mobile_phone")
    private String contactMobilePhone;

    /**
     * 传真
     * 示例：021-87853200
     */
    @JSONField(name = "fax")
    private String fax;

    /**
     * 邮箱
     * 示例：shunxing@xxx.com
     */
    @JSONField(name = "email")
    private String email;

    /**
     * 状态：1-启用，0-禁用
     * 示例：1
     */
    @JSONField(name = "status")
    private Integer status;

    /**
     * 交易方编码
     * 示例：V00108006
     */
    @JSONField(name = "vendor")
    private String vendor;

    /**
     * 交易方名称
     * 示例：张三样例
     */
    @JSONField(name = "vendor_text")
    private String vendorText;

    /**
     * 简称
     * 示例：王五
     */
    @JSONField(name = "short_text")
    private String shortText;

    /**
     * 交易方类型
     * 示例：""
     */
    @JSONField(name = "vendor_type")
    private String vendorType;

    /**
     * 交易方类别
     * 示例：11
     */
    @JSONField(name = "vendor_category")
    private String vendorCategory;

    /**
     * 交易方性质
     * 示例：0
     */
    @JSONField(name = "vendor_nature")
    private String vendorNature;

    /**
     * 关联员工ID
     * 示例：6959513973725069601
     */
    @JSONField(name = "linked_employee")
    private String linkedEmployee;

    /**
     * 关联客户
     * 示例：客户
     */
    @JSONField(name = "linked_customer")
    private String linkedCustomer;

    /**
     * 是否风险交易方
     * 示例：false
     */
    @JSONField(name = "is_risk")
    private Boolean isRisked;

    /**
     * 是否关联法人实体
     * 示例：true
     */
    @JSONField(name = "associated_with_legal_entity")
    private Boolean associatedWithLegalEntity;

    /**
     * 附件列表
     */
    @JSONField(name = "appendix")
    private List<Appendix> appendix;

    /**
     * 扩展信息列表
     */
    @JSONField(name = "extend_info")
    private List<ExtendInfo> extendInfo;

    /**
     * 银行账户列表
     */
    @JSONField(name = "vendor_accounts")
    private List<VendorAccount> vendorAccounts;

    /**
     * 地址列表
     */
    @JSONField(name = "vendor_addresses")
    private List<VendorAddress> vendorAddresses;

    /**
     * 公司视图列表（各公司的财务信息）
     */
    @JSONField(name = "vendor_company_views")
    private List<VendorCompanyView> vendorCompanyViews;

    /**
     * 联系人列表
     */
    @JSONField(name = "vendor_contacts")
    private List<VendorContact> vendorContacts;

    /**
     * 统驭科目
     * 示例：22020101
     */
    @JSONField(name = "gl_account")
    private String glAccount;

    /**
     * 预付定金付款条款
     * 示例：PT09
     */
    @JSONField(name = "down_payment_term")
    private String downPaymentTerm;

    /**
     * 付款条款
     * 示例：PT08
     */
    @JSONField(name = "payment_term")
    private String paymentTerm;

    /**
     * 交易方地点编码
     * 示例：999999
     */
    @JSONField(name = "vendor_site_code")
    private String vendorSiteCode;

    /**
     * 交易方所属部门
     */
    @JSONField(name = "owner_depts")
    private String ownerDepts;

    /**
     * 附件信息
     */
    @Data
    public static class Appendix {
        /**
         * 租户ID
         * 示例：6977354570259330000
         */
        @JSONField(name = "tenant_id")
        private String tenantId;

        /**
         * 文件ID
         * 示例：609a128628ad4eaebd3063c59928a103
         */
        @JSONField(name = "file_id")
        private String fileId;

        /**
         * 文件名
         * 示例：xxxx.xlsx
         */
        @JSONField(name = "file_name")
        private String fileName;

        /**
         * 文件类型
         * 示例：XLSX
         */
        @JSONField(name = "file_type")
        private String fileType;

        /**
         * 文件大小（字节）
         * 示例：13367
         */
        @JSONField(name = "file_size")
        private Integer fileSize;

        /**
         * 下载地址
         * 示例：https://xxxx.qfei.cn/downloadxxxxxxxxxx
         */
        @JSONField(name = "download_url")
        private String downloadUrl;
    }

    /**
     * 扩展信息
     */
    @Data
    public static class ExtendInfo {
        /**
         * 字段类型：0-文本，1-选项，2-数字，3-日期，4-日期范围
         * 示例：0
         */
        @JSONField(name = "field_type")
        private Integer fieldType;

        /**
         * 字段值（文本类型时使用）
         * 示例："文本值"
         * 字段类型为 单行文本框(0)、多行文本框(1)、单选框(3)、下拉单选框(5) 时的值
         */
        @JSONField(name = "field_value")
        private String fieldValue;

        /**
         * 选项列表（选项类型时使用）
         * 示例：[""]
         * 字段类型为 多选框(4) 下拉多选(6) 时的值
         */
        @JSONField(name = "options")
        private List<String> options;

        /**
         * 数值（数字类型时使用）
         * 示例：1.11
         * 字段类型为 数字(2) 时的值
         */
        @JSONField(name = "num")
        private Double num;

        /**
         * 日期（日期类型时使用）
         * 示例："2021-10-14"
         * 字段类型是 日期(7)时候的值  2021-10-14
         */
        @JSONField(name = "date")
        private String date;

        /**
         * 日期范围（日期范围类型时使用）
         * 示例：[""]
         */
        @JSONField(name = "range_date")
        private List<String> rangeDate;

        /**
         * 字段编码
         * 示例：VXX000001
         */
        @JSONField(name = "field_code")
        private String fieldCode;

        /**
         * 附件列表（扩展信息中的附件）
         * 附件列表 字段类型是 附件(12) 时候的值
         */
        @JSONField(name = "appendix")
        private List<Appendix> appendix;

        /**
         * 用户user_id(14)
         */
        @JSONField(name = "employee")
        private String[] employee;
    }

    /**
     * 交易方银行账户信息
     */
    @Data
    public static class VendorAccount {
        /**
         * 银行账号
         * 示例：62448345986564434
         */
        @JSONField(name = "account")
        private String account;

        /**
         * IBAN（国际银行账号）
         * 示例：46677
         */
        @JSONField(name = "iban")
        private String iban;

        /**
         * 账户名称
         * 示例：上海xxx技术有限（上海）分公司
         */
        @JSONField(name = "account_name")
        private String accountName;

        /**
         * 银行ID
         * 示例：MDBK00061195
         */
        @JSONField(name = "bank_id")
        private String bankId;

        /**
         * 银行编码
         * 示例：308290003732
         */
        @JSONField(name = "bank_code")
        private String bankCode;

        /**
         * SWIFT代码
         * 示例：BOFAUS3NINQ
         */
        @JSONField(name = "swift_code")
        private String swiftCode;

        /**
         * 交易方地点编码
         * 示例：99999999
         */
        @JSONField(name = "vendor_site_code")
        private String vendorSiteCode;

        /**
         * 银行名称
         * 示例：xx银行股份有限公司苏州支行
         */
        @JSONField(name = "bank_name")
        private String bankName;

        /**
         * 银行缩写
         * 示例：ZJTLCB
         */
        @JSONField(name = "bank_acronym")
        private String bankAcronym;

        /**
         * 国家/地区
         * 示例：CN
         */
        @JSONField(name = "country")
        private String country;

        /**
         * 银行控制码
         * 示例：99999999
         */
        @JSONField(name = "bank_control_code")
        private String bankControlCode;

        /**
         * 扩展信息列表
         */
        @JSONField(name = "extend_info")
        private List<ExtendInfo> extendInfo;
    }

    /**
     * 交易方地址信息
     */
    @Data
    public static class VendorAddress {
        /**
         * 国家/地区
         * 示例：CN
         */
        @JSONField(name = "country")
        private String country;

        /**
         * 省份
         * 示例：MDPS00000001
         */
        @JSONField(name = "province")
        private String province;

        /**
         * 城市
         * 示例：MDCY00000001
         */
        @JSONField(name = "city")
        private String city;

        /**
         * 区县
         * 示例：MDCA00002746
         */
        @JSONField(name = "county")
        private String county;

        /**
         * 详细地址
         * 示例：北京市海淀区苏州街
         */
        @JSONField(name = "address")
        private String address;

        /**
         * 扩展信息列表
         */
        @JSONField(name = "extend_info")
        private List<ExtendInfo> extendInfo;
    }

    /**
     * 交易方公司视图（各公司的财务配置）
     */
    @Data
    public static class VendorCompanyView {
        /**
         * 公司代码
         * 示例：1001
         */
        @JSONField(name = "company_code")
        private String companyCode;

        /**
         * 统驭科目
         * 示例：22020101
         */
        @JSONField(name = "gl_account")
        private String glAccount;

        /**
         * 交易方地点编码
         * 示例：999999
         */
        @JSONField(name = "vendor_site_code")
        private String vendorSiteCode;

        /**
         * 付款条款
         * 示例：PT09
         */
        @JSONField(name = "payment_term")
        private String paymentTerm;

        /**
         * 预付定金付款条款
         * 示例：PT08
         */
        @JSONField(name = "down_payment_term")
        private String downPaymentTerm;

        /**
         * 扩展信息列表
         */
        @JSONField(name = "extend_info")
        private List<ExtendInfo> extendInfo;
    }

    /**
     * 交易方联系人信息
     */
    @Data
    public static class VendorContact {
        /**
         * 联系人姓名
         * 示例：张三
         */
        @JSONField(name = "name")
        private String name;

        /**
         * 职位
         * 示例：董事长
         */
        @JSONField(name = "position")
        private String position;

        /**
         * 邮箱
         * 示例：haha@xxx.com
         */
        @JSONField(name = "email")
        private String email;

        /**
         * 电话
         * 示例：13333323333
         */
        @JSONField(name = "phone")
        private String phone;

        /**
         * 备注
         * 示例：备注
         */
        @JSONField(name = "remark")
        private String remark;

        /**
         * 扩展信息列表
         */
        @JSONField(name = "extend_info")
        private List<ExtendInfo> extendInfo;
    }
}
