package com.hero.middleware.dto;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

@Data
public class CreateContractDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JSONField(name = "document_number")
    @NotBlank(message = "单据编号不能为空")
    private String documentNumber;

    @JSONField(name = "document_type")
    @NotNull(message = "单据类型不能为空")
    private Integer documentType;

    @JSONField(name = "create_user_id")
    @NotBlank(message = "创建人ID不能为空")
    private String createUserId;

    @JSONField(name = "template_id")
    private String templateId;

    @JSONField(name = "anchor_line_id")
    private Long anchorLineId;

    /**
     * 业务类型编码 - 必传
     * 0: 合同申请
     * 示例: 0
     */
    @JSONField(name = "business_type_code")
//    @NotBlank(message = "业务类型编码不能为空")
    private Integer businessTypeCode;

    /**
     * 合同金额 - 必传
     * 示例: 100.00
     */
    @JSONField(name = "amount")
//    @NotBlank(message = "合同金额不能为空")
    private Double amount;

    /**
     * 币种代码 - 必传
     * 示例: "CNY" (人民币)
     */
    @JSONField(name = "currency_code")
//    @NotBlank(message = "币种代码不能为空")
    private String currencyCode;

    /**
     * 收支类型编码
     * 示例值：0
     * 可选值有：
     * 1：收入类
     * 2：支出类
     * 4：无金额
     * 3：既收又支
     */
    @JSONField(name = "pay_type_code")
//    @NotBlank(message = "收付款类型不能为空")
    private Integer payTypeCode;

    /**
     * 计价方式编码
     * 0：固定总价
     * 1：不固定总价
     * 2：无金额
     * 3：其他
     * 仅收支类型为收入类或支出类时必填
     */
    @JSONField(name = "property_type_code")
    private Integer propertyTypeCode;

    /**
     * 固定期限编码 - 必需
     * 示例值：0
     * 说明：仅固定期限时必填
     */
    @JSONField(name = "fixed_validity_code")
//    @NotBlank(message = "固定期限编码不能为空")
    private Integer fixedValidityCode;

    @JSONField(name = "start_date")
//    @NotBlank(message = "合同起始日期")
    private String startDate;

    @JSONField(name = "end_date")
//    @NotBlank(message = "合同终止日期")
    private String endDate;

    @JSONField(name = "counter_party_list")
//    @NotNull(message = "对方主体列表不能为空")
    private List<CounterPartyDTO> counterPartyList;

    @JSONField(name = "our_party_list")
//    @NotNull(message = "我方主体列表不能为空")
    private List<OurPartyDTO> ourPartyList;

    @Data
    public static class CounterPartyDTO implements Serializable {
        private static final long serialVersionUID = 1L;

        @JSONField(name = "counter_party_code")
//        @NotBlank(message = "对方主体编码不能为空")
        private String counterPartyCode;
    }

    @Data
    public static class OurPartyDTO implements Serializable {
        private static final long serialVersionUID = 1L;

        @JSONField(name = "our_party_code")
//        @NotBlank(message = "我方主体编码不能为空")
        private String ourPartyCode;
    }

}
