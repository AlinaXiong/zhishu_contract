package com.hero.middleware.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hero.middleware.client.zhishu.request.ZhishuCreateContractRequest;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZhiShuSynServiceSignPartyTest {

    @Test
    void appliesUnrestrictedSignPartyNumberToOurAndCounterParties() {
        ZhishuCreateContractRequest request = new ZhishuCreateContractRequest();
        request.setOurPartyList(Collections.singletonList(new ZhishuCreateContractRequest.OurPartyInfo()));
        request.setCounterPartyList(Collections.singletonList(new ZhishuCreateContractRequest.CounterPartyInfo()));

        ReflectionTestUtils.invokeMethod(new ZhiShuSynServiceImpl(), "applyUnrestrictedSignPartyNo", request);

        assertEquals(Integer.valueOf(0), request.getOurPartyList().get(0).getSignPartyNo());
        assertEquals(Integer.valueOf(0), request.getCounterPartyList().get(0).getSignPartyNo());
        JSONObject serialized = JSON.parseObject(JSON.toJSONString(request));
        assertEquals(Integer.valueOf(0), serialized.getJSONArray("our_party_list")
                .getJSONObject(0).getInteger("sign_party_no"));
        assertEquals(Integer.valueOf(0), serialized.getJSONArray("counter_party_list")
                .getJSONObject(0).getInteger("sign_party_no"));
    }

    @Test
    void enablesCounterPartySigningOnly() {
        ZhiShuSynServiceImpl service = new ZhiShuSynServiceImpl();

        ZhishuCreateContractRequest.SignInfoResource counterPartySignInfo =
                ReflectionTestUtils.invokeMethod(service, "buildEnabledCounterPartySignInfoResource");
        ZhishuCreateContractRequest.SignInfoResource ourPartySignInfo =
                ReflectionTestUtils.invokeMethod(service, "buildDisabledSignInfoResource");

        assertTrue(counterPartySignInfo.getEnable());
        assertFalse(ourPartySignInfo.getEnable());
    }
}
