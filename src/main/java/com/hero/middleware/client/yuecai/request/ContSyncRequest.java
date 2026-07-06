package com.hero.middleware.client.yuecai.request;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
public class ContSyncRequest {

    //合同ID（业务唯一标识）
    private String contractId;

    //合同类型ID
    private String contractType;

    //单据编号
    private String contractNumber;

    //管理组织
    private String magOrgCode;

    //公司
    private String companyCode;

    //部门
    private String unitCode;

    //岗位
    private String positionCode;

    //员工
    private String employeeCode;

    //申请日期
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private Date requisitionDate;

    //对象类型
    private String partnerCategory;

    //合同方
    private String partnerCode;

    //合同号
    private String documentNumber;

    //合同名称
    private String documentDesc;

    //开始时间
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private Date startDate;

    //结束时间
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private Date endDate;

    //核算实体
    private String entityCodes;

    //责任中心
    private String responsibilityCenterCode;

    //责任人
    private String respEmployeeCode;

    //责任部门
    private String respUnitCode;

    //使用部门
    private String applyUnitCode;

    //币种
    private String currencyCode;

    //汇率
    private BigDecimal exchangeRate;

    //金额
    private BigDecimal amount;

    //本币金额
    private BigDecimal functionAmount;

    //状态
    private String status;

    //维度1
    private String dimension1Code;

    //维度2
    private String dimension2Code;

    //创建人
    private Long createdBy;

    //创建日期
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private Date creationDate;

    //最后更新人
    private Long lastUpdatedBy;

    //最后更新日期
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private Date lastUpdateDate;

    //租户id
    private Long tenantId;

    //附件分类
    private Integer attachmentClass;

    // ========== 新增字段（第一部分） ==========

    //主播协议分成比例
    private BigDecimal anchorProtocolRatio;

    //礼物基础分成比例
    private BigDecimal giftBaseRatio;

    //官签时长分成比例
    private BigDecimal officialSignDurationRatio;

    //商务分成比例
    private BigDecimal businessRatio;

    //官签签约金
    private BigDecimal officialSignBonus;

    //官签签约金分成比例
    private BigDecimal officialSignBonusRatio;

    //固定底薪
    private BigDecimal fixedBaseSalary;

    //公司签约金
    private BigDecimal companyBonus;

    // ========== 新增字段（第二部分） ==========

    //采购申请
    private String purchaseRequest;

    //专项分类
    private String specialCategory;

    //有效时间
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private Date effectiveTime;

    //借款开始日期
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private Date loanStartDate;

    //借款结束日期
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private Date loanEndDate;

    //是否需验收（Y/N）
    private String acceptanceRequired;

    //收支类型
    private String incomeExpenseType;

    //计价方式
    private String valuationMethod;

    //预估金额
    private BigDecimal estimatedAmount;

    //合同期限类型
    private String contractTermType;

    //是否为重大合同（Y/N）
    private String majorContractFlag;

    //银行手续费承担方
    private String bankChargePayer;

    //发票种类
    private String invoiceType;

    //人民币总金额
    private BigDecimal totalAmountCny;

    //税额
    private BigDecimal taxAmount;

    //不含税金额
    private BigDecimal amountExcludingTax;

    //押金
    private BigDecimal deposit;

    //质保金
    private BigDecimal warrantyAmount;

    //主播昵称
    private String anchorNickname;

    //房间号/主播ID
    private String roomId;

    //主播身份证号码
    private String anchorIdCard;

    //平台
    private String platform;

    //智书合同地址
    private String contUrl;

    //合同计划行dto
    private List<ContSyncLineRequest> contSyncLines;
}
