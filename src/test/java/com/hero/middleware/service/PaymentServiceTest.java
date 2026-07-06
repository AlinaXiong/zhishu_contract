package com.hero.middleware.service;

import com.alibaba.fastjson.JSONObject;
import com.hero.middleware.client.zhishu.ZhishuApiClient;
import com.hero.middleware.dto.PaymentSyncDTO;
import com.hero.middleware.entity.PaymentRecord;
import com.hero.middleware.exception.BusinessException;
import com.hero.middleware.mapper.PaymentRecordMapper;
import com.hero.middleware.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("付款服务测试")
class PaymentServiceTest {

    @Mock
    private PaymentRecordMapper paymentRecordMapper;

    @Mock
    private ZhishuApiClient zhishuApiClient;

    @InjectMocks
    private PaymentServiceImpl paymentService;

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
    void testSyncPayment_Success() {
        JSONObject successResponse = new JSONObject();
        successResponse.put("code", 200);
        successResponse.put("msg", "success");

        when(zhishuApiClient.paymentSave(any(), any(JSONObject.class)))
                .thenReturn(successResponse.toJSONString());
        when(paymentRecordMapper.insert(any(PaymentRecord.class))).thenReturn(1);

        assertDoesNotThrow(() -> {
            paymentService.syncPayment(paymentSyncDTO);
        });

        verify(paymentRecordMapper, times(1)).insert(any(PaymentRecord.class));
        verify(zhishuApiClient, times(1)).paymentSave(any(), any(JSONObject.class));
    }

    @Test
    @DisplayName("付款记录同步 - 智书API调用失败")
    void testSyncPayment_ZhishuApiFailed() {
        JSONObject failResponse = new JSONObject();
        failResponse.put("code", 400);
        failResponse.put("msg", "参数错误");

        when(zhishuApiClient.paymentSave(any(), any(JSONObject.class)))
                .thenReturn(failResponse.toJSONString());

        assertThrows(BusinessException.class, () -> {
            paymentService.syncPayment(paymentSyncDTO);
        });

        verify(paymentRecordMapper, never()).insert(any(PaymentRecord.class));
    }

    @Test
    @DisplayName("付款记录同步 - 智书API响应为空")
    void testSyncPayment_ZhishuApiNullResponse() {
        when(zhishuApiClient.paymentSave(any(), any(JSONObject.class)))
                .thenReturn(null);

        assertThrows(BusinessException.class, () -> {
            paymentService.syncPayment(paymentSyncDTO);
        });

        verify(paymentRecordMapper, never()).insert(any(PaymentRecord.class));
    }

    @Test
    @DisplayName("付款记录同步 - 智书API调用异常")
    void testSyncPayment_ZhishuApiException() {
        when(zhishuApiClient.paymentSave(any(), any(JSONObject.class)))
                .thenThrow(new RuntimeException("网络异常"));

        assertThrows(BusinessException.class, () -> {
            paymentService.syncPayment(paymentSyncDTO);
        });

        verify(paymentRecordMapper, never()).insert(any(PaymentRecord.class));
    }
}
