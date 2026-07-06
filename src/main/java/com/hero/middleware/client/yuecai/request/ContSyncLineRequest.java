package com.hero.middleware.client.yuecai.request;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class ContSyncLineRequest {

    //合同计划行ID
    private Long paymentScheduleLineId;

    //方向，新增字段（1：收入；2：支出）
    private String revenueExpenditure;

    //智书行id
    private String zsLineId;

    //人民币金额
    private Double rmbAmount;

    //税率
    private Double taxRate;

    //不含税金额
    private Double taxExclusiveAmount;

    //税额
    private Double taxAmount;

    //是否有票
    private String invoiceFlag;

    //银行账号
    private String bankAccountNumber;

    //里程碑币种到头币种汇率类型
    private String pay2reqExchangeType;

    //里程碑币种到头币种汇率
    private BigDecimal pay2reqExchangeRate;

    //合同头ID
    private Long contractHeaderId;

    //行号
    private Long lineNumber;

    //报销类型ID
    private Long expenseTypeId;

    //申请项目ID
    private Long reqItemId;

    //金额
    private BigDecimal amount;

    //支付金额
    private BigDecimal paymentAmount;

    //付款方法
    private String paymentMethod;

    //付款条款ID
    private Long paymentTermId;

    //对象类型
    private String partnerCategory;

    //对象CODE
    private String partnerCode;

    //开始日期
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private Date startDate;

    //计划付款日期(预计完成时间)
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private Date dueDate;

    //实际付款日期
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private Date actualDate;

    //备注
    private String memo;

    //币种
    private String currencyCode;

    //里程碑行付款币种
    private String paymentCurrencyCode;

    //头币种到里程碑币种汇率类型
    private String req2payExchangeType;

    //头币种到里程碑币种汇率
    private BigDecimal req2payExchangeRate;

    //合同里程碑阶段
    private String landmarkPhase;

    //是否需要验收
    private String acceptedFlag;

    //是否付款节点
    private String paymentNodeFlag;

    //付款条件
    private String paymentTerm;

    //付款节点属性
    private String paymentNodeType;

    //付款比例
    private String paymentRatio;

    //验收状态
    private String status;

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

    //是否按比例计算
    private Boolean paymentRatioFlag;

    //验收时间约定
    private String acceptanceTimeAgreement;
}
