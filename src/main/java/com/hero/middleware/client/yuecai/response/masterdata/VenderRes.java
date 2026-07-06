package com.hero.middleware.client.yuecai.response.masterdata;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class VenderRes extends BaseRes {
    /**
     * 系统级供应商主数据主键 ID
     */
    private Long venderId;

    /**
     * 申请人
     */
    private Long createdBy;

    /**
     * 创建日期
     */
    private Date creationDate;

    /**
     * 更新时间
     */
    private Date lastUpdateDate;

    /**
     * 企业类型
     */
    private String companyBusinessType;

    /**
     * 是否用于云账户付款
     */
    private String cloudAccount;

    /**
     * 供应商所属国家
     */
    private String pkCountry;

    /**
     * 供应商基本分类
     */
    private String pkCustclass;

    /**
     * 供应商名称
     */
    private String description;

    /**
     * 纳税识别号/身份证号/护照号
     */
    private String taxpayerNumber;

    /**
     * 供应商编码
     */
    private String venderCode;
    private String supplierCode;

    /**
     * 客户编码
     */
    private String customerCode;

    /**
     * 注册资金币种
     */
    private String pkCurrtype;

    /**
     * 供应商所属省份
     */
    private String province;

    /**
     * 供应商所属城市
     */
    private String city;

    /**
     * 供应商明细地址
     */
    private String detailedAddress;

    /**
     * 客商简介
     */
    private String introduction;

    /**
     * 供应商分类
     */
    private String supplierCategory;

    /**
     * 客商所属集团公司
     */
    private String companyBelong;

    /**
     * 供应商来源
     */
    private String supplierSource;

    /**
     * 内部推荐人
     */
    private String internalReferrer;

    /**
     * 客商关系
     */
    private String businessRelationship;

    /**
     * 主营范围
     */
    private String mainBusiness;

    /**
     * 法人及控股股东
     */
    private String artificialPerson;

    /**
     * 供应商分级
     */
    private String supplierRating;

    /**
     * 供应商年度绩效评分
     */
    private String performanceScore;

    /**
     * 加入黑名单
     */
    private String blacklist;

    /**
     * 启用标志
     */
    private Boolean enabledFlag;

    private List<VendorLine> contactList;

    @Data
    public static class VendorLine {
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
