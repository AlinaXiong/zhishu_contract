package com.hero.middleware.client.yuecai.response.masterdata;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
public class EmployeeRes extends BaseRes {
    /**
     * 员工基础定义表主键 ID
     */
    private Long employeeId;

    /**
     * 员工代码
     */
    private String employeeCode;

    /**
     * 姓名
     */
    private String name;

    /**
     * e-mail
     */
    private String email;

    /**
     * 移动电话
     */
    private String mobil;

    /**
     * 固定电话
     */
    private String phone;

    /**
     * 员工类型代码
     */
    private String employeeTypeCode;

    /**
     * 证件类型
     */
    private String idType;

    /**
     * 证件编码
     */
    private String idCode;

    /**
     * 备注
     */
    private String notes;

    /**
     * 证件编号
     */
    private String nationalIdentifier;

    /**
     * hmap同步标志
     */
    private Boolean hmapSyncFlag;

    /**
     * hmap同步日期
     */
    private Date hmapSyncDate;

    /**
     * 启用标志
     */
    private Boolean enabledFlag;

    /**
     * 是否电局登录
     */
    private Boolean etaxFlag;

    /**
     * 电局登录方式
     */
    private Long loginMethod;

    /**
     * 电局登录类型
     */
    private Long loginType;

    /**
     * 电局登录身份
     */
    private Long loginIdentity;

    /**
     * 电局登录身份密码
     */
    private String loginIdentityPassword;

    /**
     * 办税人员身份证号
     */
    private String idNumber;

    /**
     * 电局登录账号
     */
    private String loginAccount;

    /**
     * 电局登录密码
     */
    private String loginPassword;

    /**
     * 中间号
     */
    private String middleNumber;

    /**
     * 地区编码
     */
    private String regionCode;

    /**
     * 办税人手机号
     */
    private String taxpayersPhone;

    /**
     * 核算主体代码（全电用）
     */
    private String accEntityCode;

    /**
     * 销项通道（全电用）
     */
    private String outChannelCode;

    /**
     * 信用得分
     */
    private BigDecimal creditScore;

    /**
     * 信用等级
     */
    private String creditLevel;

    /**
     * 飞书userId
     */
    private String feishuEmployeeId;
}
