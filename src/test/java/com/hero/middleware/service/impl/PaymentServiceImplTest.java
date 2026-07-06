package com.hero.middleware.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.hero.middleware.client.zhishu.ZhishuApiClient;
import com.hero.middleware.client.zhishu.ZhishuContractClient;
import com.hero.middleware.client.zhishu.request.ZhishuCreateContractRequest;
import com.hero.middleware.client.zhishu.response.ContractResponse;
import com.hero.middleware.client.zhishu.response.ZhishuCreateContractResponse;
import com.hero.middleware.config.ZhishuApiConfig;
import com.hero.middleware.dto.CreateContractDTO;
import com.hero.middleware.dto.CreateContractResultDTO;
import com.hero.middleware.dto.PaymentSyncDTO;
import com.hero.middleware.service.ContractService;
import com.hero.middleware.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@SpringBootTest
class PaymentServiceImplTest {

    @Autowired
    private PaymentService paymentService;
    @Autowired
    private ZhishuApiClient zhishuApiClient;

    @Test
    void testPaymentSync() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String nowDate = sdf.format(new Date());
        PaymentSyncDTO dto = new PaymentSyncDTO();
        dto.setContractId("1138985035538366793");
//        dto.setCreateUserId("1a42d113");
        dto.setBusinessCode("BX20260001315_1228");
        dto.setPaymentAmount(new BigDecimal("30000.0"));
        dto.setCurrencyCode("CNY");
//        dto.setTransactionStatus("PAYMENT_SUCCESS");
        dto.setTransactionTime(nowDate);
        dto.setPaymentPlanUuid("aef2a5e5-de0c-4b5f-8056-809903df390f");
        dto.setCounterPartyCode("V-C-CN-OT-IPC-6860");
        dto.setBankAccountNumber("62100006015616354400");
        paymentService.syncPayment(dto);

    }

    private JSONObject buildPaymentPlan(PaymentSyncDTO dto) {
        JSONObject plan = new JSONObject();
        plan.put("applicant_user_id", dto.getCreateUserId());
        plan.put("finance_number", dto.getBusinessCode());
        plan.put("finance_amount", dto.getPaymentAmount());
        plan.put("finance_currency", dto.getCurrencyName());

        List<Map<String, Object>> paymentLines = new ArrayList<>();
        Map<String, Object> paymentLine = new HashMap<>();
        paymentLine.put("source_id", dto.getBusinessCode());
        paymentLine.put("transaction_amount", dto.getPaymentAmount());
        paymentLine.put("transaction_status", dto.getTransactionStatus());
        paymentLine.put("transaction_time", dto.getTransactionTime());
        paymentLine.put("currency", dto.getCurrencyCode());
//        paymentLine.put("trading_party_code", dto.getCounterPartyCode());
//
//        if (dto.getBankAccountNumber() != null && !dto.getBankAccountNumber().isEmpty()) {
//            Map<String, Object> tradingPartyAccount = new HashMap<>();
//            tradingPartyAccount.put("account_number", dto.getBankAccountNumber());
//            paymentLine.put("trading_party_account", tradingPartyAccount);
//        }

        List<Map<String, Object>> relations = new ArrayList<>();
        Map<String, Object> relation = new HashMap<>();
        relation.put("contract_id", dto.getContractId());
        relation.put("amount", dto.getPaymentAmount());
        relations.add(relation);
        paymentLine.put("relations", relations);

        paymentLines.add(paymentLine);
        plan.put("payment_lines", paymentLines);

        return plan;
    }
}
