package com.hero.middleware.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hero.middleware.client.yuecai.request.UpdateAnchorCardRequest;
import com.hero.middleware.client.zhishu.response.ContractQueryResponse;
import com.hero.middleware.enums.FormAttributeTypeEnum;
import com.hero.middleware.enums.ZhishuAndYecaiFiledEnum;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ContractServiceImplAnchorCardTest {

    @Test
    void makeAnchorCardInfoMapsConfiguredFieldsFromFormData() throws Exception {
        ContractServiceImpl service = new ContractServiceImpl();
        ContractQueryResponse contract = buildContract(buildAnchorForm());

        Map<String, Object> formData = service.getContractFormData(contract);
        UpdateAnchorCardRequest request = invokeMakeAnchorCardInfo(service, contract, formData);

        assertEquals(161L, request.getId());
        assertEquals("67498924", request.getAnchorId());
        assertEquals("2", request.getPlatform());
        assertEquals("Team-A", request.getTeamName());
        assertEquals("2026-05-12", request.getContractStartDate());
        assertEquals("2027-05-11", request.getContractEndDate());
        assertEquals(1000D, request.getOfficialSigningBonusIncome(), 0.000001);
        assertEquals(0.25D, request.getOfficialSigningBonusRatio(), 0.000001);
        assertEquals(7000D, request.getCompanySigningBonus(), 0.000001);
        assertEquals(1500D, request.getFixedBaseSalary(), 0.000001);
        assertEquals(0.2D, request.getSalaryRatio(), 0.000001);
        assertEquals(0.1D, request.getGiftRatio(), 0.000001);
        assertEquals(0.3D, request.getBusinessRatio(), 0.000001);
        assertEquals(0.4D, request.getSelfMediaRatio(), 0.000001);
        assertEquals("88.88", request.getOtherInfo());
        assertNull(request.getAnchorNickname());
    }

    @Test
    void makeAnchorCardInfoLeavesUnmappedFieldsEmpty() throws Exception {
        ContractServiceImpl service = new ContractServiceImpl();
        ContractQueryResponse contract = buildContract(new JSONArray());

        Map<String, Object> formData = service.getContractFormData(contract);
        UpdateAnchorCardRequest request = invokeMakeAnchorCardInfo(service, contract, formData);

        assertNull(request.getId());
        assertNull(request.getAnchorId());
        assertNull(request.getPlatform());
        assertNull(request.getTeamName());
        assertNull(request.getCompanySigningBonus());
        assertNull(request.getSalaryRatio());
        assertNull(request.getGiftRatio());
        assertNull(request.getBusinessRatio());
        assertNull(request.getSelfMediaRatio());
        assertNull(request.getOtherInfo());
        assertEquals("2026-05-12", request.getContractStartDate());
        assertEquals("2027-05-11", request.getContractEndDate());
    }

    private ContractQueryResponse buildContract(JSONArray form) {
        ContractQueryResponse contract = new ContractQueryResponse();
        contract.setStartDate("2026-05-12");
        contract.setEndDate("2027-05-11");
        contract.setForm(form.toJSONString());
        return contract;
    }

    private JSONArray buildAnchorForm() {
        JSONArray form = new JSONArray();
        form.add(attribute(ZhishuAndYecaiFiledEnum.ANCHOR_DOCUMENT_NUMBER, FormAttributeTypeEnum.FEISHU_APPROVAL.getCode(), approvalValue("161")));
        form.add(attribute(ZhishuAndYecaiFiledEnum.ANCHOR_ROOM_ID, FormAttributeTypeEnum.SINGLELINE_TEXT.getCode(), "67498924"));
        form.add(attribute(ZhishuAndYecaiFiledEnum.PLATFORM, FormAttributeTypeEnum.DROPDOWN_RADIO.getCode(), keyNameValue("2", "Huya")));
        form.add(attribute(ZhishuAndYecaiFiledEnum.ANCHOR_TEAM_NAME, FormAttributeTypeEnum.SINGLELINE_TEXT.getCode(), "Team-A"));
        form.add(attribute(ZhishuAndYecaiFiledEnum.OFFICIAL_SIGNING_FEE, FormAttributeTypeEnum.AMOUNT.getCode(), amountValue("1000")));
        form.add(attribute(ZhishuAndYecaiFiledEnum.OFFICIAL_SIGNING_FEE_SHARE_RATIO, FormAttributeTypeEnum.NUMBER.getCode(), 0.25D));
        form.add(attribute(ZhishuAndYecaiFiledEnum.COMPANY_SIGNING_FEE, FormAttributeTypeEnum.AMOUNT.getCode(), amountValue("7000")));
        form.add(attribute(ZhishuAndYecaiFiledEnum.FIXED_BASE_SALARY_PER_MONTH, FormAttributeTypeEnum.AMOUNT.getCode(), amountValue("1500")));
        form.add(attribute(ZhishuAndYecaiFiledEnum.LIVE_PLATFORM_BASIC_COOPERATION_FEE, FormAttributeTypeEnum.NUMBER.getCode(), 0.2D));
        form.add(attribute(ZhishuAndYecaiFiledEnum.GIFT_BASIC_SHARE_RATIO, FormAttributeTypeEnum.NUMBER.getCode(), 0.1D));
        form.add(attribute(ZhishuAndYecaiFiledEnum.SELF_MEDIA_BUSINESS_INCOME, FormAttributeTypeEnum.NUMBER.getCode(), 0.3D));
        form.add(attribute(ZhishuAndYecaiFiledEnum.SELF_MEDIA_ACCOUNT_INCOME, FormAttributeTypeEnum.NUMBER.getCode(), 0.4D));
        form.add(attribute(ZhishuAndYecaiFiledEnum.OTHER_FEE, FormAttributeTypeEnum.AMOUNT.getCode(), amountValue("88.88")));
        return form;
    }

    private JSONObject attribute(ZhishuAndYecaiFiledEnum fieldEnum, String type, Object value) {
        JSONObject attribute = new JSONObject();
        attribute.put("attribute_code", fieldEnum.getZhishuFiled());
        attribute.put("attribute_key", fieldEnum.getZhishuFiled());
        attribute.put("attribute_name", fieldEnum.getName());
        attribute.put("attribute_type", type);
        attribute.put("attribute_value", value);
        return attribute;
    }

    private JSONArray approvalValue(String content) {
        JSONObject value = new JSONObject();
        value.put("content", content);
        JSONArray values = new JSONArray();
        values.add(value);
        return values;
    }

    private JSONObject keyNameValue(String key, String name) {
        JSONObject value = new JSONObject();
        value.put("key", key);
        value.put("name", name);
        return value;
    }

    private JSONObject amountValue(String amount) {
        JSONObject value = new JSONObject();
        value.put("amount", amount);
        value.put("currency", "CNY");
        value.put("currency_name", "CNY-RMB");
        return value;
    }

    private UpdateAnchorCardRequest invokeMakeAnchorCardInfo(ContractServiceImpl service,
                                                             ContractQueryResponse contract,
                                                             Map<String, Object> formData) throws Exception {
        Method method = ContractServiceImpl.class.getDeclaredMethod("makeAnchorCardInfo", ContractQueryResponse.class, Map.class);
        method.setAccessible(true);
        return (UpdateAnchorCardRequest) method.invoke(service, contract, formData);
    }
}
