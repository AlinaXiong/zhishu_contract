package com.hero.middleware.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hero.middleware.client.yuecai.response.OrderInfoResponse;
import com.hero.middleware.enums.FormAttributeTypeEnum;
import com.hero.middleware.enums.ZhishuAndYecaiFiledEnum;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZhiShuSynServiceOrderInfoFieldsTest {

    @Test
    void orderInfoCostCenterAndOrderTypeAreWrittenWithExpectedTypes() {
        ZhiShuSynServiceImpl service = new ZhiShuSynServiceImpl("unused.xlsx", 200);
        OrderInfoResponse orderInfo = new OrderInfoResponse();
        orderInfo.setCostCenter("CC001");
        orderInfo.setOrderType("ORDER_TYPE_A");

        Object values = ReflectionTestUtils.invokeMethod(service, "buildOrderMemberFieldValues",
                orderInfo, Collections.emptyMap(), "C-001", "O-001");
        JSONArray formAttributes = new JSONArray();

        Boolean changed = ReflectionTestUtils.invokeMethod(service, "putOrderMemberFields",
                formAttributes, values);

        assertTrue(Boolean.TRUE.equals(changed));
        assertEquals(2, formAttributes.size());
        JSONObject costCenterAttribute = findAttribute(formAttributes, ZhishuAndYecaiFiledEnum.ORDERHT_COST_CENTER);
        assertEquals(FormAttributeTypeEnum.DROPDOWN_OPTION.getCode(), costCenterAttribute.getString("attribute_type"));
        assertEquals(Collections.singletonList("CC001"),
                costCenterAttribute.getJSONObject("attribute_value").getJSONArray("key").toJavaList(String.class));

        JSONObject orderTypeAttribute = findAttribute(formAttributes, ZhishuAndYecaiFiledEnum.ORDERHT_ORDER_TYPE);
        assertEquals(FormAttributeTypeEnum.DROPDOWN_RADIO.getCode(), orderTypeAttribute.getString("attribute_type"));
        assertEquals("ORDER_TYPE_A", orderTypeAttribute.getJSONObject("attribute_value").getString("key"));
        assertEquals("ORDER_TYPE_A", orderTypeAttribute.getJSONObject("attribute_value").getString("name"));
    }

    private JSONObject findAttribute(JSONArray formAttributes, ZhishuAndYecaiFiledEnum fieldEnum) {
        for (int index = 0; index < formAttributes.size(); index++) {
            JSONObject attribute = formAttributes.getJSONObject(index);
            if (fieldEnum.getZhishuFiled().equals(attribute.getString("attribute_code"))) {
                return attribute;
            }
        }
        throw new AssertionError("attribute not found: " + fieldEnum.getZhishuFiled());
    }
}
