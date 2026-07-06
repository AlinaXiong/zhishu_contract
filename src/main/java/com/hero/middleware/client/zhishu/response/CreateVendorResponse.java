package com.hero.middleware.client.zhishu.response;

import lombok.Data;

import java.util.List;

@Data
public class CreateVendorResponse {
    /**
     * 交易方唯一标识ID（创建后返回）
     * 示例：7023646046559404327
     */
    private String id;

    /**
     * 注册地址国家/地区
     * 示例：CN
     */
    private String adCountry;

    /**
     * 注册地址省份
     * 示例：MDPS00000001
     */
    private String adProvince;

    /**
     * 注册地址城市
     * 示例：MDCY00001226
     */
    private String adCity;

    /**
     * 注册地址详情
     * 示例：上海市浦东新区世纪大道1000号
     */
    private String address;

    /**
     * 注册地址邮编
     * 示例：100100
     */
    private String adPostcode;

    /**
     * 法定代表人
     * 示例：张三
     */
    private String legalPerson;

    /**
     * 证件类型
     * 示例：0
     */
    private String certificationType;

    /**
     * 证件ID（如统一社会信用代码）
     * 示例：913100xxxxx555781R
     */
    private String certificationId;

    /**
     * 联系人
     * 示例：李四
     */
    private String contactPerson;

    /**
     * 联系电话（固定电话）
     * 示例：021-87853200
     */
    private String contactTelephone;

    /**
     * 联系手机
     * 示例：+8617621685955
     */
    private String contactMobilePhone;

    /**
     * 传真
     * 示例：021-87853200
     */
    private String fax;

    /**
     * 邮箱
     * 示例：shunxing@xxx.com
     */
    private String email;

    /**
     * 状态：1-启用，0-禁用
     * 示例：1
     */
    private Integer status;

    /**
     * 交易方编码
     * 示例：V00108006
     */
    private String vendor;

    /**
     * 交易方名称
     * 示例：张三样例
     */
    private String vendorText;

    /**
     * 简称
     * 示例：王五
     */
    private String shortText;

    /**
     * 交易方类型
     * 示例：""
     */
    private String vendorType;

    /**
     * 交易方类别
     * 示例：11
     */
    private String vendorCategory;

    /**
     * 交易方性质
     * 示例：0
     */
    private String vendorNature;

    /**
     * 关联员工ID
     * 示例：6959513973725069601
     */
    private String linkedEmployee;

    /**
     * 关联客户
     * 示例：客户
     */
    private String linkedCustomer;

    /**
     * 是否风险交易方
     * 示例：false
     */
    private Boolean isRisked;

    /**
     * 是否关联法人实体
     * 示例：true
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
     * 示例：22020101
     */
    private String glAccount;

    /**
     * 预付定金付款条款
     * 示例：PT09
     */
    private String downPaymentTerm;

    /**
     * 付款条款
     * 示例：PT08
     */
    private String paymentTerm;

    /**
     * 交易方地点编码
     * 示例：999999
     */
    private String vendorSiteCode;

    // ===== 内部嵌套类（返回对象中的嵌套类比请求对象多了ID字段） =====

    /**
     * 附件信息
     */
    @Data
    public static class Appendix {
        /**
         * 租户ID
         * 示例：6977354570259330000
         */
        private String tenantId;

        /**
         * 文件ID
         * 示例：609a128628ad4eaebd3063c59928a103
         */
        private String fileId;

        /**
         * 文件名
         * 示例：xxxx.xlsx
         */
        private String fileName;

        /**
         * 文件类型
         * 示例：XLSX
         */
        private String fileType;

        /**
         * 文件大小（字节）
         * 示例：13367
         */
        private Integer fileSize;

        /**
         * 下载地址
         * 示例：https://xxxx.qfei.cn/downloadxxxxxxxxxx
         */
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
        private Integer fieldType;

        /**
         * 字段值（文本类型时使用）
         * 示例："文本值"
         */
        private String fieldValue;

        /**
         * 选项列表（选项类型时使用）
         * 示例：[""]
         */
        private List<String> options;

        /**
         * 数值（数字类型时使用）
         * 示例：1.11
         */
        private Double num;

        /**
         * 日期（日期类型时使用）
         * 示例："2021-10-14"
         */
        private String date;

        /**
         * 日期范围（日期范围类型时使用）
         * 示例：[""]
         */
        private List<String> rangeDate;

        /**
         * 字段编码
         * 示例：VXX000001
         */
        private String fieldCode;

        /**
         * 附件列表（扩展信息中的附件）
         */
        private List<Appendix> appendix;


    }

    /**
     * 交易方银行账户信息
     */
    @Data
    public static class VendorAccount {
        /**
         * 银行账户记录ID（创建后返回）
         * 示例：1453263653228318721
         */
        private String id;

        /**
         * 银行账号
         * 示例：62448345986564434
         */
        private String account;

        /**
         * IBAN（国际银行账号）
         * 示例：46677
         */
        private String iban;

        /**
         * 账户名称
         * 示例：上海xxx技术有限（上海）分公司
         */
        private String accountName;

        /**
         * 银行ID
         * 示例：MDBK00061195
         */
        private String bankId;

        /**
         * 银行编码
         * 示例：308290003732
         */
        private String bankCode;

        /**
         * SWIFT代码
         * 示例：BOFAUS3NINQ
         */
        private String swiftCode;

        /**
         * 交易方地点编码
         * 示例：99999999
         */
        private String vendorSiteCode;

        /**
         * 银行名称
         * 示例：xx银行股份有限公司苏州支行
         */
        private String bankName;

        /**
         * 银行缩写
         * 示例：ZJTLCB
         */
        private String bankAcronym;

        /**
         * 国家/地区
         * 示例：CN
         */
        private String country;

        /**
         * 银行控制码
         * 示例：99999999
         */
        private String bankControlCode;

        /**
         * 扩展信息列表
         */
        private List<ExtendInfo> extendInfo;


    }

    /**
     * 交易方地址信息
     */
    @Data
    public static class VendorAddress {
        /**
         * 地址记录ID（创建后返回）
         * 示例：1433488030078558209
         */
        private String id;

        /**
         * 国家/地区
         * 示例：CN
         */
        private String country;

        /**
         * 省份
         * 示例：MDPS00000001
         */
        private String province;

        /**
         * 城市
         * 示例：MDCY00000001
         */
        private String city;

        /**
         * 区县
         * 示例：MDCA00002746
         */
        private String county;

        /**
         * 详细地址
         * 示例：北京市海淀区苏州街
         */
        private String address;

        /**
         * 扩展信息列表
         */
        private List<ExtendInfo> extendInfo;


    }

    /**
     * 交易方公司视图（各公司的财务配置）
     */
    @Data
    public static class VendorCompanyView {
        /**
         * 公司视图记录ID（创建后返回）
         * 示例：1453263653228318721
         */
        private String id;

        /**
         * 公司代码
         * 示例：1001
         */
        private String companyCode;

        /**
         * 统驭科目
         * 示例：22020101
         */
        private String glAccount;

        /**
         * 交易方地点编码
         * 示例：999999
         */
        private String vendorSiteCode;

        /**
         * 付款条款
         * 示例：PT09
         */
        private String paymentTerm;

        /**
         * 预付定金付款条款
         * 示例：PT08
         */
        private String downPaymentTerm;

        /**
         * 扩展信息列表
         */
        private List<ExtendInfo> extendInfo;


    }

    /**
     * 交易方联系人信息
     */
    @Data
    public static class VendorContact {
        /**
         * 联系人记录ID（创建后返回）
         * 示例：1433488091906793474
         */
        private String id;

        /**
         * 联系人姓名
         * 示例：张三
         */
        private String name;

        /**
         * 职位
         * 示例：董事长
         */
        private String position;

        /**
         * 邮箱
         * 示例：haha@xxx.com
         */
        private String email;

        /**
         * 电话
         * 示例：13333323333
         */
        private String phone;

        /**
         * 备注
         * 示例：备注
         */
        private String remark;

        /**
         * 扩展信息列表
         */
        private List<ExtendInfo> extendInfo;


    }

}
