package com.hero.middleware.client.zhishu.request;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class ZhishuCreateContractRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @JSONField(name = "contract_name")
    private String contractName;

    @JSONField(name = "contract_category_abbreviation")
    private String contractCategoryAbbreviation;

    @JSONField(name = "create_user_id")
    private String createUserId;

    @JSONField(name = "amount")
    private BigDecimal amount;

    @JSONField(name = "estimated_amount")
    private BigDecimal estimatedAmount;

    @JSONField(name = "currency_code")
    private String currencyCode;

    @JSONField(name = "in_currency_code")
    private String inCurrencyCode;

    @JSONField(name = "in_amount")
    private BigDecimal inAmount;

    @JSONField(name = "out_currency_code")
    private String outCurrencyCode;

    @JSONField(name = "out_amount")
    private BigDecimal outAmount;

    @JSONField(name = "pay_type_code")
    private Integer payTypeCode;

    @JSONField(name = "property_type_code")
    private Integer propertyTypeCode;

    @JSONField(name = "fixed_validity_code")
    private Integer fixedValidityCode;

    @JSONField(name = "start_date")
    private String startDate;

    @JSONField(name = "end_date")
    private String endDate;

    @JSONField(name = "validity_date_desc")
    private String validityDateDesc;

    @JSONField(name = "our_party_list")
    private List<OurPartyInfo> ourPartyList;

    @JSONField(name = "counter_party_list")
    private List<CounterPartyInfo> counterPartyList;

    @JSONField(name = "contract_status_code")
    private String contractStatusCode;

    @JSONField(name = "submitted_time")
    private String submittedTime;

    @JSONField(name = "source_id")
    private String sourceId;

    @JSONField(name = "form")
    private String form;

    @JSONField(name = "remark")
    private String remark;

    @JSONField(name = "contract_number")
    private String contractNumber;

    @JSONField(name = "business_type_code")
    private Integer businessTypeCode;

    @JSONField(name = "template_instance_id")
    private String templateInstanceId;

    @JSONField(name = "text_file_id")
    private String textFileId;

    @JSONField(name = "scan_file_id")
    private String scanFileId;

    @JSONField(name = "attachment_file_id_list")
    private List<String> attachmentFileIdList;

    @JSONField(name = "contract_cause_file_id_list")
    private List<String> contractCauseFileIdList;

    @JSONField(name = "sign_type_code")
    private Integer signTypeCode;

    @JSONField(name = "seal_number")
    private Integer sealNumber;

    @JSONField(name = "relation")
    private RelationInfo relation;

    @JSONField(name = "relation_list")
    private List<RelationList> relationList;

    @JSONField(name = "payment_plan_list")
    private List<PaymentPlanInfo> paymentPlanList;

    @JSONField(name = "collection_plan_list")
    private List<CollectionPlanInfo> collectionPlanList;

    @JSONField(name = "contract_files")
    private ContractFiles contractFiles;

    @Data
    public static class OurPartyInfo implements Serializable {
        private static final long serialVersionUID = 1L;

        @JSONField(name = "our_party_code")
        private String ourPartyCode;

        @JSONField(name = "sign_party_no")
        private Integer signPartyNo;

        @JSONField(name = "our_party_sign_info_resource")
        private SignInfoResource ourPartySignInfoResource;

        @JSONField(name = "docusign_signer_email")
        private String docusignSignerEmail;
    }

    @Data
    public static class CounterPartyInfo implements Serializable {
        private static final long serialVersionUID = 1L;

        @JSONField(name = "counter_party_code")
        private String counterPartyCode;

        @JSONField(name = "sign_party_no")
        private Integer signPartyNo;

        @JSONField(name = "counter_party_sign_info_resource")
        private SignInfoResource counterPartySignInfoResource;

        @JSONField(name = "business_address_info")
        private AddressInfo businessAddressInfo;

        @JSONField(name = "bank_account_info")
        private BankAccountInfo bankAccountInfo;

        @JSONField(name = "contact_info")
        private ContactInfo contactInfo;
    }

    @Data
    public static class SignInfoResource implements Serializable {
        private static final long serialVersionUID = 1L;

        @JSONField(name = "enable")
        private Boolean enable;

        @JSONField(name = "seal_type_codes")
        private String sealTypeCodes;

        @JSONField(name = "seal_position_type_code")
        private Integer sealPositionTypeCode;

        @JSONField(name = "keyword")
        private String keyword;

        @JSONField(name = "date_seal_enabled")
        private Boolean dateSealEnabled;

        @JSONField(name = "personal_seal_enabled")
        private Boolean personalSealEnabled;

        @JSONField(name = "personal_seal_keyword")
        private String personalSealKeyword;

        @JSONField(name = "cross_page_seal_enabled")
        private Boolean crossPageSealEnabled;

        @JSONField(name = "signer_name")
        private String signerName;

        @JSONField(name = "signer_mobile")
        private String signerMobile;

        @JSONField(name = "signer_email")
        private String signerEmail;
    }

    @Data
    public static class AddressInfo implements Serializable {
        private static final long serialVersionUID = 1L;

        @JSONField(name = "id")
        private String id;

        @JSONField(name = "value")
        private String value;
    }

    @Data
    public static class BankAccountInfo implements Serializable {
        private static final long serialVersionUID = 1L;

        @JSONField(name = "id")
        private String id;

        @JSONField(name = "value")
        private String value;
    }

    @Data
    public static class ContactInfo implements Serializable {
        private static final long serialVersionUID = 1L;

        @JSONField(name = "id")
        private String id;

        @JSONField(name = "value")
        private String value;
    }

    @Data
    public static class RelationInfo implements Serializable {
        private static final long serialVersionUID = 1L;

        @JSONField(name = "relation_contracts")
        private List<String> relationContracts;

        @JSONField(name = "framework_contract_number")
        private String frameworkContractNumber;
    }

    @Data
    public static class RelationList implements Serializable {
        private static final long serialVersionUID = 1L;

        @JSONField(name = "contract_numbers")
        private List<String> contractNumbers;

        @JSONField(name = "relation_name")
        private String relationName;
    }

    @Data
    public static class PaymentPlanInfo implements Serializable {
        private static final long serialVersionUID = 1L;

        @JSONField(name = "payment_date")
        private String paymentDate;

        @JSONField(name = "prepaid")
        private Boolean prepaid;

        @JSONField(name = "payment_amount")
        private BigDecimal paymentAmount;

        @JSONField(name = "payment_desc")
        private String paymentDesc;

        @JSONField(name = "currency_code")
        private String currencyCode;

        @JSONField(name = "payment_custom_attributes")
        private String paymentCustomAttributes;

        @JSONField(name = "payment_counter_party")
        private CounterPartyRef paymentCounterParty;
    }

    @Data
    public static class CollectionPlanInfo implements Serializable {
        private static final long serialVersionUID = 1L;

        @JSONField(name = "collection_date")
        private String collectionDate;

        @JSONField(name = "collection_amount")
        private BigDecimal collectionAmount;

        @JSONField(name = "collection_desc")
        private String collectionDesc;

        @JSONField(name = "currency_code")
        private String currencyCode;

        @JSONField(name = "collection_counter_party")
        private CounterPartyRef collectionCounterParty;
    }

    @Data
    public static class CounterPartyRef implements Serializable {
        private static final long serialVersionUID = 1L;

        @JSONField(name = "counter_party_code")
        private String counterPartyCode;
    }

    @Data
    public static class ContractFiles implements Serializable {
        private static final long serialVersionUID = 1L;

        @JSONField(name = "contract_text")
        private String contractText;

        @JSONField(name = "contract_causes")
        private List<String> contractCauses;

        @JSONField(name = "contract_attachments")
        private List<String> contractAttachments;
    }

}
