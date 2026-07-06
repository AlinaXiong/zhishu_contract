package com.hero.middleware.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hero.middleware.client.zhishu.ZhishuContractClient;
import com.hero.middleware.client.zhishu.request.ContractFormCreatResponse;
import com.hero.middleware.client.zhishu.request.CreateTemplateInstanceRequest;
import com.hero.middleware.client.zhishu.request.ZhishuCreateContractRequest;
import com.hero.middleware.client.zhishu.response.CreateTemplateInstanceResponse;
import com.hero.middleware.client.zhishu.response.ZhishuCreateContractResponse;
import com.hero.middleware.config.YeCaiDataConfig;
import com.hero.middleware.dto.fanwei.FanWeicontractBoardDto;
import com.hero.middleware.enums.FormAttributeTypeEnum;
import com.hero.middleware.enums.ZhishuAndYecaiFiledEnum;
import com.hero.middleware.service.FanWeiSynService;
import com.hero.middleware.utils.ExcelUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
public class FanWeiSynServiceImpl implements FanWeiSynService {

    private static final String DEFAULT_CONTRACT_EXCEL_PATH = "E:/lidongliang/\u9700\u6c42\u6587\u6863/\u82f1\u96c4\u7535\u7ade/\u6cdb\u5fae\u5408\u540c\u4fe1\u606f_\u6309\u667a\u4e66\u88683\u5b57\u6bb5\u5bfc\u51fa_20260402_154821.xlsx";
    private static final String DEFAULT_PERSONNEL_EXCEL_PATH = "E:/lidongliang/\u9700\u6c42\u6587\u6863/\u82f1\u96c4\u7535\u7ade/\u4eba\u5458\u4fe1\u606f-\u98de\u4e66id.xls";
    private static final int FAN_WEI_CONTRACT_SHEET_INDEX = 1;
    private static final int HEADER_ROW_INDEX = 0;
    private static final int DATA_START_ROW_INDEX = 1;
    private static final String PARAM_OLD_MAIN_ID = "old_main_id";
    private static final String PARAM_TEMPLATE_NUMBER = "templateNumber";
    private static final String PARAM_CONTRACT_EXCEL_PATH = "contractExcelPath";
    private static final String PARAM_PERSONNEL_EXCEL_PATH = "personnelExcelPath";
    private static final String PARAM_MAX_ROWS = "maxRows";

    private static final List<CustomFieldMapping> CUSTOM_FIELD_MAPPINGS = Arrays.asList(
            new CustomFieldMapping("executor_workcode", ZhishuAndYecaiFiledEnum.CONTRACT_EXECUTOR, FormAttributeTypeEnum.EMPLOYEE),
            new CustomFieldMapping("major_contract_flag", ZhishuAndYecaiFiledEnum.MAJOR_CONTRACT_FLAG, FormAttributeTypeEnum.RADIO),
            new CustomFieldMapping("bank_charge_bearer", ZhishuAndYecaiFiledEnum.BANK_CHARGE_PAYER, FormAttributeTypeEnum.DROPDOWN_RADIO),
            new CustomFieldMapping("anchor_name", ZhishuAndYecaiFiledEnum.ANCHOR_NAME, FormAttributeTypeEnum.SINGLELINE_TEXT),
            new CustomFieldMapping("anchor_nick_name", ZhishuAndYecaiFiledEnum.ANCHOR_NICKNAME, FormAttributeTypeEnum.SINGLELINE_TEXT),
            new CustomFieldMapping("anchor_room_or_id", ZhishuAndYecaiFiledEnum.ANCHOR_ROOM_ID, FormAttributeTypeEnum.SINGLELINE_TEXT),
            new CustomFieldMapping("anchor_id_no", ZhishuAndYecaiFiledEnum.ANCHOR_ID_CARD, FormAttributeTypeEnum.SINGLELINE_TEXT),
            new CustomFieldMapping("team_name", ZhishuAndYecaiFiledEnum.ANCHOR_TEAM_NAME, FormAttributeTypeEnum.SINGLELINE_TEXT),
            new CustomFieldMapping("gift_basic_share_ratio", ZhishuAndYecaiFiledEnum.GIFT_BASIC_SHARE_RATIO, FormAttributeTypeEnum.NUMBER),
            new CustomFieldMapping("official_signing_fee", ZhishuAndYecaiFiledEnum.OFFICIAL_SIGNING_FEE, FormAttributeTypeEnum.AMOUNT),
            new CustomFieldMapping("official_signing_fee_share_ratio", ZhishuAndYecaiFiledEnum.OFFICIAL_SIGNING_FEE_SHARE_RATIO, FormAttributeTypeEnum.NUMBER),
            new CustomFieldMapping("fixed_base_salary_per_month", ZhishuAndYecaiFiledEnum.FIXED_BASE_SALARY_PER_MONTH, FormAttributeTypeEnum.AMOUNT),
            new CustomFieldMapping("company_signing_fee", ZhishuAndYecaiFiledEnum.COMPANY_SIGNING_FEE, FormAttributeTypeEnum.AMOUNT),
            new CustomFieldMapping("signing_term_months", ZhishuAndYecaiFiledEnum.SIGNING_TERM_MONTHS, FormAttributeTypeEnum.NUMBER),
            new CustomFieldMapping("platform_name", ZhishuAndYecaiFiledEnum.PLATFORM, FormAttributeTypeEnum.DROPDOWN_RADIO),
            new CustomFieldMapping("estimated_payback_cycle_months", ZhishuAndYecaiFiledEnum.ESTIMATED_PAYBACK_CYCLE_MONTHS, FormAttributeTypeEnum.NUMBER)
    );

    @Autowired
    private ZhishuContractClient zhishuContractClient;

    @Autowired
    private YeCaiDataConfig yeCaiDataConfig;

    @Override
    public void fanWeiToZhiShuContract(Map<String, Object> paramMap) {
        Map<String, Object> params = paramMap == null ? Collections.emptyMap() : paramMap;
        String templateNumber = trimToNull(toStringValue(params.get(PARAM_TEMPLATE_NUMBER)));
        if (templateNumber == null) {
            log.error("泛微合同同步智书失败，templateNumber为空，不调用智书创建接口");
            return;
        }

        String oldMainId = trimToNull(toStringValue(params.get(PARAM_OLD_MAIN_ID)));
        String contractExcelPath = firstNotBlank(toStringValue(params.get(PARAM_CONTRACT_EXCEL_PATH)), DEFAULT_CONTRACT_EXCEL_PATH);
        String personnelExcelPath = firstNotBlank(toStringValue(params.get(PARAM_PERSONNEL_EXCEL_PATH)), DEFAULT_PERSONNEL_EXCEL_PATH);
        Integer maxRows = toInteger(params.get(PARAM_MAX_ROWS));

        log.info("泛微合同同步智书开始，old_main_id={}，templateNumber={}，合同Excel路径={}，人员Excel路径={}，最大处理行数={}",
                oldMainId, templateNumber, contractExcelPath, personnelExcelPath, maxRows);

        Map<String, PersonnelInfo> personnelMap = loadPersonnelMap(personnelExcelPath);
        log.info("泛微合同同步智书人员映射加载完成，数量={}", personnelMap.size());

        Set<String> missingCustomFields = collectMissingCustomFields();
        if (!missingCustomFields.isEmpty()) {
            log.warn("泛微合同同步智书存在未配置到ZhishuAndYecaiFiledEnum的自定义字段：{}", missingCustomFields);
        }

        final int[] scannedCount = {0};
        final int[] matchedCount = {0};
        final int[] successCount = {0};
        final int[] failCount = {0};

        ExcelUtils.readExcelByRow(contractExcelPath, FAN_WEI_CONTRACT_SHEET_INDEX, HEADER_ROW_INDEX,
                DATA_START_ROW_INDEX, maxRows, rowData -> {
                    scannedCount[0]++;
                    FanWeicontractBoardDto contractData = ExcelUtils.mapToBean(rowData, FanWeicontractBoardDto.class);
                    String rowOldMainId = toStringValue(getValue(contractData, "old_main_id"));
                    if (oldMainId != null && !oldMainId.equals(rowOldMainId)) {
                        return true;
                    }

                    matchedCount[0]++;
                    boolean success = createOneContract(contractData, templateNumber, personnelMap);
                    if (success) {
                        successCount[0]++;
                    } else {
                        failCount[0]++;
                    }
                    log.info("成功数量：{}", successCount[0]);
                    log.info("失败数量：{}", failCount[0]);
                    return oldMainId == null;
                });

        if (oldMainId != null && matchedCount[0] == 0) {
            log.info("泛微合同同步智书结束，未匹配到old_main_id={}的数据", oldMainId);
            return;
        }
        log.info("泛微合同同步智书结束，扫描行数={}，匹配行数={}，成功数量={}，失败数量={}",
                scannedCount[0], matchedCount[0], successCount[0], failCount[0]);
    }

    private boolean createOneContract(FanWeicontractBoardDto contractData, String templateNumber,
                                      Map<String, PersonnelInfo> personnelMap) {
        String oldMainId = toStringValue(getValue(contractData, "old_main_id"));
        String contractName = firstNotBlank(toStringValue(getValue(contractData, "contract_name")), "FanWei contract-" + oldMainId);
        String createUserId = resolveUserId(getValue(contractData, "applicant_workcode"), personnelMap);
        if (createUserId == null) {
            createUserId = trimToNull(yeCaiDataConfig.getUserId());
        }
        if (createUserId == null) {
            log.error("泛微合同创建失败，创建人userId为空，old_main_id={}，合同名称={}",
                    oldMainId, contractName);
            return false;
        }

        try {
            CreateTemplateInstanceRequest templateRequest = buildTemplateInstanceRequest(contractData, templateNumber,
                    createUserId, personnelMap);
            log.debug("泛微合同创建模板实例请求，old_main_id={}，请求参数={}",
                    oldMainId, JSON.toJSONString(templateRequest));
//            CreateTemplateInstanceResponse templateResponse = zhishuContractClient.createTemplateInstance(templateRequest);
            CreateTemplateInstanceResponse templateResponse = templateInstanceResponse();
            log.debug("泛微合同创建模板实例返回，old_main_id={}，返回参数={}",
                    oldMainId, JSON.toJSONString(templateResponse));
            if (templateResponse == null || trimToNull(templateResponse.getTemplateInstanceid()) == null) {
                log.error("泛微合同创建模板实例失败，old_main_id={}，合同名称={}",
                        oldMainId, contractName);
                return false;
            }

            ZhishuCreateContractRequest createRequest = buildCreateContractRequest(contractData, createUserId,
                    templateResponse.getTemplateInstanceid(), personnelMap);
            log.debug("泛微合同创建智书合同请求，old_main_id={}，请求参数={}",
                    oldMainId, JSON.toJSONString(createRequest));
//            ZhishuCreateContractResponse createResponse = zhishuContractClient.createContractV2(createRequest);
            ZhishuCreateContractResponse createResponse = templateCreateContractResponse();
            log.debug("泛微合同创建智书合同返回，old_main_id={}，返回参数={}",
                    oldMainId, JSON.toJSONString(createResponse));
            if (createResponse == null || !createResponse.isSuccess()) {
                log.error("泛微合同创建智书合同失败，old_main_id={}，合同名称={}，错误信息={}",
                        oldMainId, contractName, createResponse == null ? "智书响应为空" : createResponse.getMsg());
                return false;
            }
            if (createResponse.getData() == null || createResponse.getData().getContract() == null) {
                log.error("泛微合同创建智书合同失败，old_main_id={}，合同名称={}，错误信息=智书合同信息为空",
                        oldMainId, contractName);
                return false;
            }
            ZhishuCreateContractResponse.ContractInfo contractInfo = createResponse.getData().getContract();
            log.info("泛微合同创建智书合同成功，old_main_id={}，合同名称={}，智书合同ID={}，智书合同编号={}",
                    oldMainId, contractName, contractInfo.getContractId(), contractInfo.getContractNumber());
            return true;
        } catch (Exception e) {
            log.error("泛微合同创建智书合同异常，old_main_id={}，合同名称={}，错误信息={}",
                    oldMainId, contractName, e.getMessage(), e);
            return false;
        }
    }

    private CreateTemplateInstanceResponse templateInstanceResponse(){
        String str = "{\"code\":0,\"msg\":\"success\",\"data\":{\"template_instance\":{\"source_id\":null,\"template_number\":\"202604220006\",\"template_id\":\"1133383242137731401\",\"template_instance_id\":\"1133423183454536044\",\"template_instance_file_id\":null}},\"success\":true}";
        JSONObject resultRes = JSONObject.parseObject(str);
        return JSONObject.parseObject(resultRes.getJSONObject("data").getString("template_instance"), CreateTemplateInstanceResponse.class);
    }

    private ZhishuCreateContractResponse templateCreateContractResponse(){
        String str = "{\"msg\":\"success\",\"code\":0,\"data\":{\"contract\":{\"signed_time\":\"\",\"owner_user_name\":\"程林枫\",\"create_user_name\":\"程林枫\",\"parent_contract_category_number\":\"009\",\"association_party\":false,\"contract_id\":1133423183852994924,\"parent_contract_category_abbreviation\":\"ZB\",\"business_type_name\":\"合同申请\",\"out_currency_code\":\"CNY\",\"pay_type_code\":1,\"complete_time\":\"\",\"contract_category_number\":\"009001\",\"sign_type_code\":1,\"app_id\":\"1868\",\"create_user_id\":\"654bce86\",\"archive_number\":\"\",\"create_time\":\"1778660449438\",\"business_type_code\":0,\"fixed_validity_detail_name\":\"固定生效日期,固定终止日期\",\"standard_contract_type_code\":0,\"contract_status_name\":\"editing\",\"create_employee_code\":\"\",\"standard_framework_contract_code\":0,\"default_currency_name\":\"人民币元\",\"our_party_list\":[],\"property_type_code\":0,\"owner_employee_id\":1120716665475039305,\"in_currency_code\":\"CNY\",\"sign_type_name\":\"纸质签约-不限制我方/对方先签约\",\"parent_contract_category_name\":\"主播专项\",\"end_date\":\"2026-05-13\",\"contract_version\":1,\"contract_duration_unit\":0,\"counter_party_list\":[{\"certification_id\":\"91110302061258662H\",\"register_address\":{\"country\":\"中国大陆\"},\"certification_name\":\"统一社会信用代码\",\"counter_party_code\":\"V00100001\",\"counter_party_name\":\"北京领英信息技术有限公司\",\"sign_party_no\":0,\"counter_party_sign_info_resource\":{\"seal_type_codes\":\"2\",\"personal_seal_enabled\":false,\"signer_name\":\"\",\"personal_seal_keyword\":\"\",\"date_seal_keyword\":\"\",\"date_seal_enabled\":false,\"enable\":false,\"signer_mobile\":\"\",\"signer_email\":\"\"},\"certification_type\":\"0\",\"trading_party_property_code\":0,\"counter_party_id\":\"1127872233440543596\",\"certifications\":[{\"certification_id\":\"91110302061258662H\",\"certification_name\":\"统一社会信用代码\",\"certification_type\":\"0\",\"status\":1}]}],\"anti_dated_type_code\":0,\"archived_time\":\"\",\"fixed_validity_code\":1,\"validity_date_desc\":\"\",\"source_type_name\":\"模板\",\"contract_status_code\":0,\"currency_code\":\"CNY\",\"terminated_time\":\"\",\"create_employee_id\":\"654bce86\",\"contract_category_name\":\"主播经纪\",\"update_time\":\"1778660449438\",\"process_instance_ids\":[\"\"],\"contract_number\":\"H-ZB202605130153\",\"source_type_code\":1,\"fixed_validity_detail_code\":1,\"direct_fixed_exchange_rate\":\"1\",\"approved_time\":\"\",\"in_currency_name\":\"人民币元\",\"converted_amounts\":[],\"contract_category_abbreviation\":\"ZBJJ\",\"indirect_fixed_exchange_rate\":\"1\",\"property_type_name\":\"固定总价\",\"start_date\":\"2026-05-13\",\"submitter_employee_id\":0,\"contract_duration\":0,\"process_definition_key\":\"\",\"template_custom_field_values\":\"[]\",\"amount\":0.00,\"default_currency_code\":\"CNY\",\"template_Name\":\"1.独家经纪合同（通用版-两方）-20260509\",\"currency_name\":\"人民币元\",\"owner_user_id\":\"654bce86\",\"department_id\":\"od-eb6cbbe42bebae200f024c654a7ad2fb\",\"department_name\":\"线下业务部-V\",\"fixed_validity_name\":\"固定期限\",\"form_id\":1132955407971516745,\"contract_name\":\"合同-57\",\"info_change_version\":1,\"contract_effective_status_code\":0,\"submitted_time\":\"\",\"out_currency_name\":\"人民币元\",\"template_number\":\"202604220006\",\"form\":\"[{\\\"attribute_key\\\":\\\"custom_1_ab6f99ee02e549469ec5b2d4a5a98452\\\",\\\"attribute_name\\\":\\\"主播姓名\\\",\\\"attribute_type\\\":\\\"singleline_text\\\",\\\"module_name\\\":\\\"主播卡片\\\",\\\"attribute_value\\\":\\\"林辰\\\",\\\"attribute_code\\\":\\\"custom_1_ab6f99ee02e549469ec5b2d4a5a98452\\\"},{\\\"attribute_key\\\":\\\"custom_1_4fa3c71e706c489e94977935b512b0f6\\\",\\\"attribute_name\\\":\\\"主播昵称\\\",\\\"attribute_type\\\":\\\"singleline_text\\\",\\\"module_name\\\":\\\"主播卡片\\\",\\\"attribute_value\\\":\\\"夜枫\\\",\\\"attribute_code\\\":\\\"custom_1_4fa3c71e706c489e94977935b512b0f6\\\"},{\\\"attribute_key\\\":\\\"custom_1_543d4d9106f34c31bf3f9397ded6ef28\\\",\\\"attribute_name\\\":\\\"房间号/主播ID\\\",\\\"attribute_type\\\":\\\"singleline_text\\\",\\\"module_name\\\":\\\"主播卡片\\\",\\\"attribute_value\\\":\\\"002\\\",\\\"attribute_code\\\":\\\"custom_1_543d4d9106f34c31bf3f9397ded6ef28\\\"},{\\\"attribute_key\\\":\\\"custom_1_c97a63f71e1048aea384680a64aa3573\\\",\\\"attribute_name\\\":\\\"主播身份证号码\\\",\\\"attribute_type\\\":\\\"singleline_text\\\",\\\"module_name\\\":\\\"主播卡片\\\",\\\"attribute_value\\\":\\\"330106199908152277\\\",\\\"attribute_code\\\":\\\"custom_1_c97a63f71e1048aea384680a64aa3573\\\"}]\",\"group_id\":1133423183853027692,\"contract_files\":{\"contract_archive_attachments\":[],\"contract_attachments\":[],\"contract_ocr_comparisons\":[],\"contract_scans\":[],\"contract_text\":{\"edit_mode_enabled\":false,\"file_name\":\"1.独家经纪合同（通用版-两方）-20260509.docx\",\"file_id\":\"1133423185870455148\",\"mime\":\"application/vnd.openxmlformats-officedocument.wordprocessingml.document\",\"download_url\":\"/clm/api/file/download?businessId=1133423183852994924&businessTypeCode=0&fileId=1133423185870455148&fileTypeCode=2\",\"file_size\":\"83184\"},\"contract_causes\":[]},\"pay_type_name\":\"收入类\",\"process_instance_id\":\"\",\"template_id\":1133383242137731401,\"source_id\":\"57\",\"multi_url\":{\"mobile_url\":\"https://applink.feishu.cn/client/mini_program/open?appId=cli_a63cbce0d9b9500d&mode=window&path=pages%2Ftemplate-use%2Findex%3FcontractId%3D1133423183852994924%26templateId%3D1133383242137731401%26businessTypeCode%3D0%26normalBack%3Dtrue\",\"pc_url\":\"https://contract.qfei.cn/apply/template?contractId=1133423183852994924&templateId=1133383242137731401&businessTypeCode=0&showFeelGood=true\"},\"saas_department_id\":1120429029359354953}},\"success\":true}";
        return JSONObject.parseObject(str, ZhishuCreateContractResponse.class);
    }

    private CreateTemplateInstanceRequest buildTemplateInstanceRequest(FanWeicontractBoardDto contractData,
                                                                       String templateNumber,
                                                                       String createUserId,
                                                                       Map<String, PersonnelInfo> personnelMap) {
        CreateTemplateInstanceRequest request = new CreateTemplateInstanceRequest();
        request.setCreateUserid(createUserId);
        request.setSourceid(toStringValue(getValue(contractData, "old_main_id")));
        request.setTemplateNumber(templateNumber);
        request.setTemplateFieldList(buildTemplateFieldList(contractData, personnelMap));
        return request;
    }

    private ZhishuCreateContractRequest buildCreateContractRequest(FanWeicontractBoardDto contractData,
                                                                   String createUserId,
                                                                   String templateInstanceId,
                                                                   Map<String, PersonnelInfo> personnelMap) {
        ZhishuCreateContractRequest request = new ZhishuCreateContractRequest();
        request.setCreateUserId(createUserId);
        request.setTemplateInstanceId(templateInstanceId);
        request.setContractStatusCode("0");
        request.setBusinessTypeCode(0);
        request.setSourceId(toStringValue(getValue(contractData, "old_main_id")));
        request.setContractName(firstNotBlank(toStringValue(getValue(contractData, "contract_name")),
                "FanWei contract-" + request.getSourceId()));
        request.setContractNumber(toStringValue(getValue(contractData, "contract_code")));
        request.setContractCategoryAbbreviation("DEFAULT");
        request.setAmount(firstNotNull(toBigDecimal(getValue(contractData, "contract_total_amount")),
                firstNotNull(toBigDecimal(getValue(contractData, "income_amount")),
                        firstNotNull(toBigDecimal(getValue(contractData, "expense_amount")), BigDecimal.ZERO))));
        request.setEstimatedAmount(toBigDecimal(getValue(contractData, "estimated_amount")));
        request.setCurrencyCode(firstNotBlank(toStringValue(getValue(contractData, "income_currency")),
                firstNotBlank(toStringValue(getValue(contractData, "expense_currency")), "CNY")));
        request.setInCurrencyCode(toStringValue(getValue(contractData, "income_currency")));
        request.setInAmount(toBigDecimal(getValue(contractData, "income_amount")));
        request.setOutCurrencyCode(toStringValue(getValue(contractData, "expense_currency")));
        request.setOutAmount(toBigDecimal(getValue(contractData, "expense_amount")));
        request.setPayTypeCode(parsePayTypeCode(contractData));
        request.setPropertyTypeCode(firstNotNull(toInteger(getValue(contractData, "pricing_method")), 0));
        request.setFixedValidityCode(firstNotNull(toInteger(getValue(contractData, "term_type")), 1));
        String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        request.setStartDate(firstNotBlank(toDateString(getValue(contractData, "contract_period_start")),
                toDateString(getValue(contractData, "valid_start_time")), today));
        request.setEndDate(firstNotBlank(toDateString(getValue(contractData, "contract_period_end")),
                toDateString(getValue(contractData, "valid_end_time")), today));
        request.setValidityDateDesc(toStringValue(getValue(contractData, "term_description")));
        request.setRemark(firstNotBlank(toStringValue(getValue(contractData, "contract_description")),
                toStringValue(getValue(contractData, "other_description"))));
        request.setOurPartyList(buildOurPartyList(getValue(contractData, "our_party")));
        request.setCounterPartyList(buildCounterPartyList(getValue(contractData, "counterparty")));

        ContractFormCreatResponse formCreatResponse = new ContractFormCreatResponse();
        for (CustomFieldMapping mapping : CUSTOM_FIELD_MAPPINGS) {
            addContractFormAttribute(formCreatResponse, mapping, getCustomFieldValue(contractData, mapping, personnelMap));
        }
        if (formCreatResponse.getForm() != null && !formCreatResponse.getForm().isEmpty()) {
            request.setForm(JSON.toJSONString(formCreatResponse.getForm()));
        }
        return request;
    }

    private List<CreateTemplateInstanceRequest.TemplateFieldList> buildTemplateFieldList(FanWeicontractBoardDto contractData,
                                                                                         Map<String, PersonnelInfo> personnelMap) {
        List<CreateTemplateInstanceRequest.TemplateFieldList> fieldList = new ArrayList<>();
        for (CustomFieldMapping mapping : CUSTOM_FIELD_MAPPINGS) {
            Object value = getCustomFieldValue(contractData, mapping, personnelMap);
            if (isBlankValue(value)) {
                continue;
            }
            CreateTemplateInstanceRequest.TemplateFieldList templateField = new CreateTemplateInstanceRequest.TemplateFieldList();
            templateField.setEditDisabled(false);
            templateField.setFieldKey(mapping.getFieldEnum().getZhishuFiled());
            Map<String, Object> content = new LinkedHashMap<>();
            content.put("content", buildTemplateContentValue(mapping.getAttributeType(), value));
            templateField.setFieldValue(JSON.toJSONString(content));
            fieldList.add(templateField);
        }
        return fieldList;
    }

    private Object getCustomFieldValue(FanWeicontractBoardDto contractData, CustomFieldMapping mapping,
                                       Map<String, PersonnelInfo> personnelMap) {
        Object value = getValue(contractData, mapping.getFanWeiField());
        if (mapping.getAttributeType() == FormAttributeTypeEnum.EMPLOYEE) {
            return resolveUserId(value, personnelMap);
        }
        return value;
    }

    private void addContractFormAttribute(ContractFormCreatResponse contractFormCreatResponse,
                                          CustomFieldMapping mapping,
                                          Object attributeValue) {
        if (contractFormCreatResponse == null || mapping == null || isBlankValue(attributeValue)) {
            return;
        }
        if (contractFormCreatResponse.getForm() == null) {
            contractFormCreatResponse.setForm(new ArrayList<>());
        }
        ContractFormCreatResponse.FormAttribute formAttribute = new ContractFormCreatResponse.FormAttribute();
        formAttribute.setAttributeCode(mapping.getFieldEnum().getZhishuFiled());
        formAttribute.setAttributeKey(mapping.getFieldEnum().getZhishuFiled());
        formAttribute.setAttributeType(mapping.getAttributeType().getCode());
        formAttribute.setAttributeValue(buildFormAttributeValue(mapping.getAttributeType(), attributeValue));
        contractFormCreatResponse.getForm().add(formAttribute);
    }

    private Object buildFormAttributeValue(FormAttributeTypeEnum attributeType, Object value) {
        if (attributeType == FormAttributeTypeEnum.AMOUNT) {
            ContractFormCreatResponse.AmountValue amountValue = new ContractFormCreatResponse.AmountValue();
            amountValue.setAmount(toBigDecimal(value));
            amountValue.setCurrency("CNY");
            return amountValue;
        }
        if (attributeType == FormAttributeTypeEnum.EMPLOYEE) {
            ContractFormCreatResponse.EmployeeValue employeeValue = new ContractFormCreatResponse.EmployeeValue();
            employeeValue.setUserId(toStringValue(value));
            employeeValue.setUserIdType("lark_user_id");
            return Collections.singletonList(employeeValue);
        }
        if (attributeType == FormAttributeTypeEnum.RADIO || attributeType == FormAttributeTypeEnum.DROPDOWN_RADIO) {
            Map<String, Object> optionValue = new LinkedHashMap<>();
            optionValue.put("key", toStringValue(value));
            optionValue.put("name", toStringValue(value));
            return optionValue;
        }
        if (attributeType == FormAttributeTypeEnum.NUMBER) {
            BigDecimal decimal = toBigDecimal(value);
            return decimal == null ? toStringValue(value) : decimal.stripTrailingZeros().toPlainString();
        }
        return toStringValue(value);
    }

    private Object buildTemplateContentValue(FormAttributeTypeEnum attributeType, Object value) {
        if (attributeType == FormAttributeTypeEnum.AMOUNT || attributeType == FormAttributeTypeEnum.NUMBER) {
            BigDecimal decimal = toBigDecimal(value);
            return decimal == null ? toStringValue(value) : decimal;
        }
        return toStringValue(value);
    }

    private Map<String, PersonnelInfo> loadPersonnelMap(String personnelExcelPath) {
        Map<String, PersonnelInfo> personnelMap = new LinkedHashMap<>();
        List<List<Object>> rows = ExcelUtils.readExcelRows(personnelExcelPath, 0, 0, null);
        for (List<Object> row : rows) {
            if (row.size() < 3) {
                continue;
            }
            String workCode = trimToNull(toStringValue(row.get(0)));
            String name = trimToNull(toStringValue(row.get(1)));
            String userId = trimToNull(toStringValue(row.get(2)));
            if (workCode == null || userId == null || isPersonnelHeader(workCode, userId)) {
                continue;
            }
            PersonnelInfo personnelInfo = new PersonnelInfo();
            personnelInfo.setWorkCode(workCode);
            personnelInfo.setName(name);
            personnelInfo.setUserId(userId);
            personnelMap.put(workCode, personnelInfo);
        }
        return personnelMap;
    }

    private boolean isPersonnelHeader(String workCode, String userId) {
        String workCodeLower = workCode.toLowerCase();
        String userIdLower = userId.toLowerCase();
        return workCode.contains("\u5de5\u53f7") || workCodeLower.contains("work")
                || userId.contains("\u7528\u6237") || userIdLower.contains("userid") || userIdLower.contains("user_id");
    }

    private String resolveUserId(Object workCodeValue, Map<String, PersonnelInfo> personnelMap) {
        String workCode = trimToNull(toStringValue(workCodeValue));
        if (workCode == null) {
            return null;
        }
        PersonnelInfo personnelInfo = personnelMap.get(workCode);
        if (personnelInfo == null) {
            log.warn("泛微人员工号未匹配到userId，工号={}", workCode);
            return null;
        }
        return personnelInfo.getUserId();
    }

    private List<ZhishuCreateContractRequest.OurPartyInfo> buildOurPartyList(Object value) {
        List<ZhishuCreateContractRequest.OurPartyInfo> ourPartyList = new ArrayList<>();
        for (String partyCode : splitMultiValue(value)) {
            ZhishuCreateContractRequest.OurPartyInfo ourPartyInfo = new ZhishuCreateContractRequest.OurPartyInfo();
            ourPartyInfo.setOurPartyCode(partyCode);
            ZhishuCreateContractRequest.SignInfoResource signInfoResource = new ZhishuCreateContractRequest.SignInfoResource();
            signInfoResource.setEnable(false);
            ourPartyInfo.setOurPartySignInfoResource(signInfoResource);
            ourPartyList.add(ourPartyInfo);
        }
        return ourPartyList;
    }

    private List<ZhishuCreateContractRequest.CounterPartyInfo> buildCounterPartyList(Object value) {
        List<ZhishuCreateContractRequest.CounterPartyInfo> counterPartyList = new ArrayList<>();
        for (String counterPartyCode : splitMultiValue(value)) {
            ZhishuCreateContractRequest.CounterPartyInfo counterPartyInfo = new ZhishuCreateContractRequest.CounterPartyInfo();
            counterPartyInfo.setCounterPartyCode(counterPartyCode);
            ZhishuCreateContractRequest.SignInfoResource signInfoResource = new ZhishuCreateContractRequest.SignInfoResource();
            signInfoResource.setEnable(false);
            counterPartyInfo.setCounterPartySignInfoResource(signInfoResource);
            counterPartyList.add(counterPartyInfo);
        }
        return counterPartyList;
    }

    private List<String> splitMultiValue(Object value) {
        String text = trimToNull(toStringValue(value));
        if (text == null) {
            return new ArrayList<>();
        }
        List<String> result = new ArrayList<>();
        String[] values = text.split("[,\\uFF0C;\\uFF1B\\u3001\\r\\n]+");
        for (String item : values) {
            String trimItem = trimToNull(item);
            if (trimItem != null) {
                result.add(trimItem);
            }
        }
        return result;
    }

    private Integer parsePayTypeCode(FanWeicontractBoardDto contractData) {
        Integer directCode = firstNotNull(toInteger(getValue(contractData, "income_expense_both_flag")),
                toInteger(getValue(contractData, "income_expense_type")));
        if (directCode != null) {
            return directCode;
        }
        BigDecimal incomeAmount = toBigDecimal(getValue(contractData, "income_amount"));
        BigDecimal expenseAmount = toBigDecimal(getValue(contractData, "expense_amount"));
        if (isPositive(incomeAmount) && isPositive(expenseAmount)) {
            return 3;
        }
        if (isPositive(incomeAmount)) {
            return 1;
        }
        if (isPositive(expenseAmount)) {
            return 2;
        }
        String text = firstNotBlank(toStringValue(getValue(contractData, "income_expense_both_flag")),
                toStringValue(getValue(contractData, "income_expense_type")));
        if (text != null) {
            if ((text.contains("\u6536\u5165") && text.contains("\u652f\u51fa")) || text.contains("\u6536\u652f")) {
                return 3;
            }
            if (text.contains("\u6536\u5165")) {
                return 1;
            }
            if (text.contains("\u652f\u51fa")) {
                return 2;
            }
        }
        return 4;
    }

    private boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private Set<String> collectMissingCustomFields() {
        Set<String> missingFields = new LinkedHashSet<>();
        for (CustomFieldMapping mapping : CUSTOM_FIELD_MAPPINGS) {
            if (mapping.getFieldEnum() == null
                    || ZhishuAndYecaiFiledEnum.getByZhishuFiled(mapping.getFieldEnum().getZhishuFiled()) == null) {
                missingFields.add(mapping.getFanWeiField());
            }
        }
        return missingFields;
    }

    private Object getValue(FanWeicontractBoardDto contractData, String fieldName) {
        if (contractData == null || fieldName == null) {
            return null;
        }
        try {
            Field field = FanWeicontractBoardDto.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(contractData);
        } catch (Exception e) {
            log.warn("读取泛微合同字段失败，字段名={}，错误信息={}", fieldName, e.getMessage());
            return null;
        }
    }

    private String toDateString(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Date) {
            return new SimpleDateFormat("yyyy-MM-dd").format((Date) value);
        }
        return trimToNull(toStringValue(value));
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return new BigDecimal(String.valueOf(value));
        }
        String text = trimToNull(toStringValue(value));
        if (text == null) {
            return null;
        }
        text = text.replace(",", "").replace("\uFF0C", "").replace("%", "");
        if (text.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer toInteger(Object value) {
        BigDecimal decimal = toBigDecimal(value);
        if (decimal == null) {
            return null;
        }
        return decimal.intValue();
    }

    private String toStringValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Date) {
            return new SimpleDateFormat("yyyy-MM-dd").format((Date) value);
        }
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).stripTrailingZeros().toPlainString();
        }
        return String.valueOf(value);
    }

    private boolean isBlankValue(Object value) {
        return trimToNull(toStringValue(value)) == null;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimValue = value.trim();
        return trimValue.isEmpty() ? null : trimValue;
    }

    @SafeVarargs
    private final <T> T firstNotNull(T... values) {
        if (values == null) {
            return null;
        }
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String firstNotBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String trimValue = trimToNull(value);
            if (trimValue != null) {
                return trimValue;
            }
        }
        return null;
    }

    @Data
    private static class CustomFieldMapping {
        private final String fanWeiField;
        private final ZhishuAndYecaiFiledEnum fieldEnum;
        private final FormAttributeTypeEnum attributeType;
    }

    @Data
    private static class PersonnelInfo {
        private String workCode;
        private String name;
        private String userId;
    }
}
