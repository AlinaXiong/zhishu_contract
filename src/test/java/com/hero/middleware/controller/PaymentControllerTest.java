package com.hero.middleware.controller;

import com.alibaba.fastjson.JSON;
import com.hero.middleware.dto.PaymentSyncDTO;
import com.hero.middleware.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@DisplayName("付款控制器测试")
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    private PaymentSyncDTO paymentSyncDTO;

    @BeforeEach
    void setUp() {
        paymentSyncDTO = new PaymentSyncDTO();
        paymentSyncDTO.setContractId("contract123");
        paymentSyncDTO.setCreateUserId("user001");
        paymentSyncDTO.setBusinessCode("BIZ202603120001");
        paymentSyncDTO.setPaymentAmount(new BigDecimal("10000.00"));
        paymentSyncDTO.setCurrencyCode("CNY");
        paymentSyncDTO.setTransactionStatus("PAYMENT_SUCCESS");
        paymentSyncDTO.setTransactionTime("2026-03-12 10:30:00");
//        paymentSyncDTO.setCounterPartyCode("COUNTER001");
//        paymentSyncDTO.setBankAccountNumber("6222021234567890123");
    }

    @Test
    @DisplayName("付款记录同步 - 成功")
    void testSyncPayment_Success() throws Exception {
        doNothing().when(paymentService).syncPayment(any(PaymentSyncDTO.class));

        mockMvc.perform(post("/api/payment/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(paymentSyncDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("付款记录同步成功"));
    }

    @Test
    @DisplayName("付款记录同步 - 缺少必填参数")
    void testSyncPayment_MissingRequiredField() throws Exception {
        PaymentSyncDTO invalidDTO = new PaymentSyncDTO();

        mockMvc.perform(post("/api/payment/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(invalidDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("付款记录同步 - 合同ID为空")
    void testSyncPayment_EmptyContractId() throws Exception {
        paymentSyncDTO.setContractId("");

        mockMvc.perform(post("/api/payment/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(paymentSyncDTO)))
                .andExpect(status().isBadRequest());
    }
}
