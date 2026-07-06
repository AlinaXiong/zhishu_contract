package com.hero.middleware.client.zhishu.response;


import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ContractQueryResponse {
    @JSONField(name = "create_user_id")
    private String createUserId;

    @JSONField(name = "contract_id")
    private Long contractId;

    @JSONField(name = "contract_number")
    private String contractNumber;

    @JSONField(name = "contract_version")
    private Integer contractVersion;

    @JSONField(name = "group_id")
    private Long groupId;

    @JSONField(name = "template_id")
    private Long templateId;

    @JSONField(name = "business_type_code")
    private Integer businessTypeCode;

    @JSONField(name = "business_type_name")
    private String businessTypeName;

    @JSONField(name = "contract_name")
    private String contractName;

    @JSONField(name = "department_id")
    private String departmentId;

    private String remark;
    private BigDecimal amount;

    @JSONField(name = "estimated_amount")
    private BigDecimal estimatedAmount;

    @JSONField(name = "currency_code")
    private String currencyCode;

    @JSONField(name = "currency_name")
    private String currencyName;

    @JSONField(name = "contract_status_code")
    private Integer contractStatusCode;

    @JSONField(name = "contract_status_name")
    private String contractStatusName;

    @JSONField(name = "source_type_code")
    private Integer sourceTypeCode;

    @JSONField(name = "source_type_name")
    private String sourceTypeName;

    @JSONField(name = "contract_category_number")
    private String contractCategoryNumber;

    @JSONField(name = "contract_category_name")
    private String contractCategoryName;

    @JSONField(name = "parent_contract_category_number")
    private String parentContractCategoryNumber;

    @JSONField(name = "parent_contract_category_name")
    private String parentContractCategoryName;

    @JSONField(name = "pay_type_code")
    private Integer payTypeCode;

    @JSONField(name = "pay_type_name")
    private String payTypeName;

    @JSONField(name = "submitted_time")
    private String submittedTime;

    @JSONField(name = "approved_time")
    private String approvedTime;

    @JSONField(name = "signed_time")
    private String signedTime;

    @JSONField(name = "archived_time")
    private String archivedTime;

    @JSONField(name = "create_time")
    private String createTime;

    @JSONField(name = "update_time")
    private String updateTime;

    /**
     * 表单自定义字段，JSON字符串格式
     */
    private String form;

    @JSONField(name = "create_user_name")
    private String createUserName;

    @JSONField(name = "start_date")
    private String startDate;

    @JSONField(name = "end_date")
    private String endDate;

    @JSONField(name = "department_name")
    private String departmentName;

    @JSONField(name = "our_party_list")
    private List<OurParty> ourPartyList;

    @JSONField(name = "counter_party_list")
    private List<CounterParty> counterPartyList;

    @JSONField(name = "payment_plan_list")
    private List<PaymentPlan> paymentPlanList;

    @JSONField(name = "attachment_file_id_list")
    private List<String> attachmentFileIdList;

    @JSONField(name = "contract_budget_list")
    private List<ContractBudget> contractBudgetList;

    @JSONField(name = "contract_category_abbreviation")
    private String contractCategoryAbbreviation;

    @JSONField(name = "parent_contract_category_abbreviation")
    private String parentContractCategoryAbbreviation;

    @JSONField(name = "contract_procurement")
    private ContractProcurement contractProcurement;

    @JSONField(name = "form_id")
    private String formId;

    @JSONField(name = "property_type_code")
    private Integer propertyTypeCode;

    @JSONField(name = "property_type_name")
    private String propertyTypeName;

    @JSONField(name = "fixed_validity_code")
    private Integer fixedValidityCode;

    @JSONField(name = "fixed_validity_name")
    private String fixedValidityName;

    @JSONField(name = "source_id")
    private String sourceId;

    @JSONField(name = "multi_url")
    private MultiUrl multiUrl;

    @JSONField(name = "validity_date_desc")
    private String validityDateDesc;

    @JSONField(name = "archive_number")
    private String archiveNumber;

    private Relation relation;

    @JSONField(name = "contract_files")
    private ContractFiles contractFiles;

    @JSONField(name = "submitter_user_id")
    private String submitterUserId;

    @JSONField(name = "submitter_user_name")
    private String submitterUserName;

    @JSONField(name = "sign_type_code")
    private Integer signTypeCode;

    @JSONField(name = "owner_user_id")
    private String ownerUserId;

    @JSONField(name = "owner_user_name")
    private String ownerUserName;

    @JSONField(name = "process_instance_id")
    private String processInstanceId;

    @JSONField(name = "attribute_permission_list")
    private List<AttributePermission> attributePermissionList;

    @JSONField(name = "previous_id")
    private String previousId;

    @JSONField(name = "previous_contract_number")
    private String previousContractNumber;

    @JSONField(name = "collection_plan_list")
    private List<CollectionPlan> collectionPlanList;

    @JSONField(name = "contract_procurement_list")
    private List<ContractProcurement> contractProcurementList;

    @JSONField(name = "process_definition_key")
    private String processDefinitionKey;

    @JSONField(name = "seal_number")
    private Integer sealNumber;

    @JSONField(name = "change_remark")
    private String changeRemark;

    @JSONField(name = "termination_remark")
    private String terminationRemark;

    @JSONField(name = "termination_date_type_code")
    private Integer terminationDateTypeCode;

    @JSONField(name = "termination_date")
    private String terminationDate;

    @JSONField(name = "anti_dated_info")
    private AntiDatedInfo antiDatedInfo;

    @JSONField(name = "termination_date_type_name")
    private String terminationDateTypeName;

    @JSONField(name = "demand_user_ids")
    private List<String> demandUserIds;

    @JSONField(name = "in_amount")
    private BigDecimal inAmount;

    @JSONField(name = "out_amount")
    private BigDecimal outAmount;

    @JSONField(name = "in_currency_code")
    private String inCurrencyCode;

    @JSONField(name = "out_currency_code")
    private String outCurrencyCode;

    @JSONField(name = "info_change_version")
    private Integer infoChangeVersion;

    @JSONField(name = "contract_category_id")
    private String contractCategoryId;

    @JSONField(name = "counter_party_sign_short_url_list")
    private List<CounterPartySignShortUrl> counterPartySignShortUrlList;

    @JSONField(name = "anti_dated_type_code")
    private Integer antiDatedTypeCode;

    @JSONField(name = "converted_amounts")
    private List<ConvertedAmount> convertedAmounts;

    @JSONField(name = "demand_department_ids")
    private List<String> demandDepartmentIds;

    @JSONField(name = "contract_effective_status_code")
    private String contractEffectiveStatusCode;

    @JSONField(name = "info_change_reason")
    private String infoChangeReason;

    @JSONField(name = "terminated_time")
    private String terminatedTime;

    @JSONField(name = "association_party")
    private Boolean associationParty;

    @JSONField(name = "template_number")
    private String templateNumber;

    @JSONField(name = "template_name")
    private String templateName;

    @JSONField(name = "template_custom_field_values")
    private String templateCustomFieldValues;

    @JSONField(name = "standard_contract_type_code")
    private Integer standardContractTypeCode;

    @JSONField(name = "framework_contract_code")
    private Integer frameworkContractCode;

    @JSONField(name = "standard_framework_contract_code")
    private Integer standardFrameworkContractCode;

    @JSONField(name = "create_employee_code")
    private String createEmployeeCode;

    @JSONField(name = "submitter_employee_code")
    private String submitterEmployeeCode;

    @JSONField(name = "fixed_validity_detail_code")
    private Integer fixedValidityDetailCode;

    // 我方签约方
    @Data
    public static class OurParty {
        @JSONField(name = "our_party_id")
        private String ourPartyId;

        @JSONField(name = "our_party_name")
        private String ourPartyName;

        @JSONField(name = "our_party_code")
        private String ourPartyCode;

        @JSONField(name = "our_party_type_code")
        private Integer ourPartyTypeCode;

        @JSONField(name = "our_party_sign_info_resource")
        private SignInfoResource ourPartySignInfoResource;

        @JSONField(name = "sign_status")
        private String signStatus;

    }

    // 对方签约方
    @Data
    public static class CounterParty {
        @JSONField(name = "counter_party_id")
        private String counterPartyId;

        @JSONField(name = "counter_party_name")
        private String counterPartyName;

        @JSONField(name = "counter_party_code")
        private String counterPartyCode;

        @JSONField(name = "counter_party_sign_info_resource")
        private CounterPartySignInfoResource counterPartySignInfoResource;

        @JSONField(name = "bank_accounts")
        private List<BankAccount> bankAccounts;

        @JSONField(name = "register_address")
        private Address registerAddress;

        @JSONField(name = "contact_persons")
        private List<ContactPerson> contactPersons;

        @JSONField(name = "sign_status")
        private String signStatus;

    }

    // 签约信息资源（我方）
    @Data
    public static class SignInfoResource {
        private Boolean enable;
        @JSONField(name = "seal_type_codes")
        private String sealTypeCodes;
        @JSONField(name = "seal_position_type_code")
        private Integer sealPositionTypeCode;
        private String keyword;
        @JSONField(name = "date_seal_enabled")
        private Boolean dateSealEnabled;
        @JSONField(name = "date_seal_keyword")
        private String dateSealKeyword;
        @JSONField(name = "personal_seal_enabled")
        private Boolean personalSealEnabled;
        @JSONField(name = "personal_seal_keyword")
        private String personalSealKeyword;
        @JSONField(name = "cross_page_seal_enabled")
        private Boolean crossPageSealEnabled;

    }

    // 对方签约信息资源（继承自我方，并扩展字段）
    @Data
    public static class CounterPartySignInfoResource extends SignInfoResource {
        @JSONField(name = "signer_name")
        private String signerName;
        @JSONField(name = "signer_mobile")
        private String signerMobile;
        @JSONField(name = "signer_email")
        private String signerEmail;

    }

    // 银行账户
    @Data
    public static class BankAccount {
        private String account;
        @JSONField(name = "account_name")
        private String accountName;
        @JSONField(name = "bank_code")
        private String bankCode;
        @JSONField(name = "bank_name")
        private String bankName;
        private String country;
        private Boolean selected;

    }

    // 地址
    @Data
    public static class Address {
        private String country;
        private String province;
        private String city;
        @JSONField(name = "detail_address")
        private String detailAddress;

    }

    // 联系人
    @Data
    public static class ContactPerson {
        private String name;
        private String position;
        private String email;
        private String phone;
        private String telephone;
        private Boolean selected;

    }

    // 付款计划
    @Data
    public static class PaymentPlan {
        @JSONField(name = "payment_amount")
        private BigDecimal paymentAmount;
        @JSONField(name = "currency_code")
        private String currencyCode;
        @JSONField(name = "payment_date")
        private String paymentDate;
        @JSONField(name = "payment_desc")
        private String paymentDesc;
        @JSONField(name = "payment_plan_id")
        private String paymentPlanId;
        @JSONField(name = "payment_interval_days")
        private String paymentIntervalDays;
        @JSONField(name = "source_id")
        private String sourceId;
        @JSONField(name = "uuid")
        private String uuid;
        @JSONField(name = "payment_custom_attributes")
        private String paymentCustomAttributes;
        @JSONField(name = "payment_counter_party")
        private PaymentCounterParty paymentCounterParty;

    }

    // 付款计划的对方信息
    @Data
    public static class PaymentCounterParty {
        @JSONField(name = "counter_party_id")
        private String counterPartyId;

        @JSONField(name = "counter_party_code")
        private String counterPartyCode;

    }

    // 合同预算
    @Data
    public static class ContractBudget {
        @JSONField(name = "budget_year")
        private String budgetYear;
        @JSONField(name = "budget_code")
        private String budgetCode;
        @JSONField(name = "budget_balance")
        private BigDecimal budgetBalance;
        @JSONField(name = "budget_balance_currency")
        private String budgetBalanceCurrency;
        @JSONField(name = "budget_occupied_amount")
        private BigDecimal budgetOccupiedAmount;
        @JSONField(name = "budget_occupied_amount_currency")
        private String budgetOccupiedAmountCurrency;
        @JSONField(name = "tax_rate")
        private BigDecimal taxRate;
        @JSONField(name = "tax_amount")
        private BigDecimal taxAmount;
        @JSONField(name = "tax_amount_currency")
        private String taxAmountCurrency;
        @JSONField(name = "source_id")
        private String sourceId;
        private String uuid;
        @JSONField(name = "contract_budget_id")
        private String contractBudgetId;
        @JSONField(name = "budget_department_id")
        private String budgetDepartmentId;
        @JSONField(name = "budget_department_name")
        private String budgetDepartmentName;
        @JSONField(name = "budget_department_info")
        private String budgetDepartmentInfo;
        @JSONField(name = "budget_subject_code")
        private String budgetSubjectCode;
        @JSONField(name = "budget_subject_name")
        private String budgetSubjectName;
        @JSONField(name = "budget_subject_info")
        private String budgetSubjectInfo;
        @JSONField(name = "cost_center_code")
        private String costCenterCode;
        @JSONField(name = "cost_center_name")
        private String costCenterName;
        @JSONField(name = "cost_center_info")
        private String costCenterInfo;
        @JSONField(name = "budget_code_name")
        private String budgetCodeName;
        private String remark;
        @JSONField(name = "extra_info")
        private String extraInfo;
        @JSONField(name = "contract_center_group_code")
        private String contractCenterGroupCode;
        @JSONField(name = "cost_center_group_name")
        private String costCenterGroupName;
        @JSONField(name = "cost_center_group_info")
        private String costCenterGroupInfo;
        @JSONField(name = "budget_taxed_amount")
        private BigDecimal budgetTaxedAmount;
        @JSONField(name = "budget_taxed_amount_currency")
        private String budgetTaxedAmountCurrency;

    }

    // 合同采购（单个）
    @Data
    public static class ContractProcurement {
        @JSONField(name = "contract_procurement_id")
        private String contractProcurementId;
        @JSONField(name = "source_id")
        private String sourceId;
        @JSONField(name = "procurement_order_source_id")
        private String procurementOrderSourceId;
        @JSONField(name = "procurement_order_number")
        private String procurementOrderNumber;
        @JSONField(name = "procurement_order_type_code")
        private Integer procurementOrderTypeCode;
        @JSONField(name = "procurement_order_url")
        private String procurementOrderUrl;
        @JSONField(name = "procurement_order_info")
        private String procurementOrderInfo;
        @JSONField(name = "procurement_amount")
        private BigDecimal procurementAmount;
        @JSONField(name = "procurement_currency")
        private String procurementCurrency;
        @JSONField(name = "operator_user_id")
        private String operatorUserId;
        @JSONField(name = "operator_name")
        private String operatorName;
        @JSONField(name = "operator_department_id")
        private String operatorDepartmentId;
        @JSONField(name = "operator_department_name")
        private String operatorDepartmentName;
        @JSONField(name = "supplier_id")
        private String supplierId;
        @JSONField(name = "supplier_source_id")
        private String supplierSourceId;
        @JSONField(name = "supplier_name")
        private String supplierName;
        @JSONField(name = "supplier_info")
        private String supplierInfo;
        @JSONField(name = "extra_info")
        private String extraInfo;

    }

    // 多端URL
    @Data
    public static class MultiUrl {
        @JSONField(name = "pc_url")
        private String pcUrl;
        @JSONField(name = "mobile_url")
        private String mobileUrl;

    }

    // 关联关系
    @Data
    public static class Relation {
        @JSONField(name = "relation_contracts")
        private List<RelationContracts> relationContracts;
        @JSONField(name = "related_contract_ids")
        private List<String> relatedContractIds;

    }

    // 关联合同
    @Data
    public static class RelationContracts {
        @JSONField(name = "relation_key")
        private String relationKey;
        @JSONField(name = "relation_name")
        private String relationName;
        @JSONField(name = "contract_ids")
        private List<String> contractIds;

    }

    // 合同文件集合
    @Data
    public static class ContractFiles {
        @JSONField(name = "contract_text")
        private ContractFile contractText;
        @JSONField(name = "contract_causes")
        private List<ContractFile> contractCauses;
        @JSONField(name = "contract_attachments")
        private List<ContractFile> contractAttachments;
        @JSONField(name = "contract_scans")
        private List<ContractScan> contractScans;
        @JSONField(name = "contract_archive_attachments")
        private List<ContractFile> contractArchiveAttachments;
        @JSONField(name = "contract_form_attachments")
        private List<ContractFile> contractFormAttachments;
        @JSONField(name = "contract_template_custom_attachments")
        private List<ContractFile> contractTemplateCustomAttachments;
        @JSONField(name = "contract_ocr_comparisons")
        private List<ContractOcrComparison> contractOcrComparisons;

    }

    // 合同文件基础信息
    @Data
    public static class ContractFile {
        @JSONField(name = "file_id")
        private String fileId;
        @JSONField(name = "file_name")
        private String fileName;
        @JSONField(name = "file_size")
        private String fileSize;
        private String mime;

    }

    // 合同扫描件（扩展）
    @Data
    public static class ContractScan extends ContractFile {
        @JSONField(name = "scan_type_code")
        private Integer scanTypeCode;
        @JSONField(name = "scan_type_name")
        private String scanTypeName;

    }

    // OCR比对文件（扩展）
    @Data
    public static class ContractOcrComparison extends ContractFile {
        @JSONField(name = "node_id")
        private String nodeId;
        @JSONField(name = "node_name")
        private String nodeName;

    }

    // 属性权限
    @Data
    public static class AttributePermission {
        @JSONField(name = "attribute_key")
        private String attributeKey;
        @JSONField(name = "permission_type_code")
        private Integer permissionTypeCode;

    }

    // 收款计划
    @Data
    public static class CollectionPlan {
        @JSONField(name = "collection_plan_id")
        private String collectionPlanId;
        @JSONField(name = "source_id")
        private String sourceId;
        @JSONField(name = "collection_amount")
        private BigDecimal collectionAmount;
        @JSONField(name = "collection_date")
        private String collectionDate;
        @JSONField(name = "collection_desc")
        private String collectionDesc;
        @JSONField(name = "currency_code")
        private String currencyCode;
        @JSONField(name = "collection_counter_party")
        private CollectionCounterParty collectionCounterParty;
        @JSONField(name = "collection_custom_attributes")
        private String collectionCustomAttributes;

    }

    // 收款计划的对方信息
    @Data
    public static class CollectionCounterParty {
        @JSONField(name = "counter_party_id")
        private String counterPartyId;

        @JSONField(name = "counter_party_code")
        private String counterPartyCode;

    }

    // 倒签信息
    @Data
    public static class AntiDatedInfo {
        private String description;
        private List<AntiDatedReason> reasons;

    }

    // 倒签原因
    @Data
    public static class AntiDatedReason {
        private String id;
        private String reason;
        private Integer order;

    }

    // 对方签约短链接
    @Data
    public static class CounterPartySignShortUrl {
        @JSONField(name = "identityId")
        private String identityId;
        private String name;
        @JSONField(name = "shortUrl")
        private String shortUrl;

    }

    // 转换后金额
    @Data
    public static class ConvertedAmount {
        private BigDecimal amount;
        @JSONField(name = "currency_code")
        private String currencyCode;
        @JSONField(name = "exchange_rate")
        private BigDecimal exchangeRate;

    }

}
