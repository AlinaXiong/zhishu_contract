package com.hero.middleware.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.hero.middleware.client.zhishu.ZhishuApiClient;
import com.hero.middleware.client.zhishu.ZhishuContractClient;
import com.hero.middleware.client.zhishu.ZhishuPaymentClient;
import com.hero.middleware.client.zhishu.request.SyncReceiptRequest;
import com.hero.middleware.client.zhishu.response.ReceiptResponse;
import com.hero.middleware.config.ZhishuApiConfig;
import com.hero.middleware.dto.PaymentSyncDTO;
import com.hero.middleware.dto.ReceiptSyncDTO;
import com.hero.middleware.service.ContractService;
import com.hero.middleware.service.PaymentService;
import com.hero.middleware.service.ReceiptService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@SpringBootTest
class ReceiptServiceImplTest {

    @Autowired
    private ZhishuPaymentClient zhishuPaymentClient;

    @Autowired
    private ReceiptService receiptService;

    @Test
    void testPaymentSync() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String nowDate = sdf.format(new Date());
        ReceiptSyncDTO dto = new ReceiptSyncDTO();
        dto.setContractId("1138985035538366793");
//        dto.setCreateUserId("1a42d113");
        dto.setBusinessCode("ACP202606010016_608");
        dto.setReceiptAmount("2000.0");
        dto.setCurrencyCode("CNY");
//        dto.setTransactionStatus("PAYMENT_SUCCESS");
        dto.setTransactionTime(nowDate);
        dto.setCollectionPlanId(1138985885702816073L);
//        dto.setCounterPartyCode("V00100001");
//        dto.setBankAccountNumber("62100006015616354400");
//        dto.setInvoiceAmount("220");
//        dto.setInvoiceNumber("2000002");
        dto.setInvoiceEntity("7417792981136868915");
        dto.setClientName("第三新（深圳）文化传播有限公司");
        dto.setCostCenter("300008");
        dto.setOrderNumber("B02-2026-496-001");
//        dto.setOrderName("订单名称001");
        receiptService.syncReceipt(dto);
    }

    private SyncReceiptRequest buildPaymentPlan(ReceiptSyncDTO dto){
        SyncReceiptRequest request = new SyncReceiptRequest();
        // 设置主单据信息
        request.setApplicantUserid(dto.getCreateUserId());
        request.setFinanceNumber(dto.getBusinessCode());
        request.setFinanceAmount(String.valueOf(dto.getReceiptAmount()));
        request.setFinanceCurrency(dto.getCurrencyName());
        // financeurl 如果没有对应字段可留空或根据业务设置
        // request.setFinanceurl(dto.getFinanceUrl());

        // 构建支付行信息
        SyncReceiptRequest.PaymentLine paymentLine = new SyncReceiptRequest.PaymentLine();
        paymentLine.setSourceid(dto.getBusinessCode());
        paymentLine.setTransactionAmount(String.valueOf(dto.getReceiptAmount()));
        paymentLine.setTransactionStatus(dto.getTransactionStatus());
        paymentLine.setTransactionTime(dto.getTransactionTime());
        paymentLine.setCurrency(dto.getCurrencyCode());
//        paymentLine.setTradingPartyCode(dto.getCounterPartyCode());

        // 设置交易方账户信息
//        if (dto.getBankAccountNumber() != null && !dto.getBankAccountNumber().isEmpty()) {
//            SyncReceiptRequest.TradingPartyAccount account = new SyncReceiptRequest.TradingPartyAccount();
//            account.setAccountNumber(dto.getBankAccountNumber());
//            paymentLine.setTradingPartyAccount(account);
//        }

        // 构建关联单据信息
        SyncReceiptRequest.Relation relation = new SyncReceiptRequest.Relation();
        relation.setContractId(dto.getContractId());
        relation.setAmount(String.valueOf(dto.getReceiptAmount()));
        // paymentPlanuuid 如果没有对应字段可留空或根据业务设置
        // relation.setPaymentPlanuuid(dto.getPaymentPlanUuid());

        List<SyncReceiptRequest.Relation> relations = new ArrayList<>();
        relations.add(relation);
        paymentLine.setRelations(relations);

        // 设置支付行集合
        List<SyncReceiptRequest.PaymentLine> paymentLines = new ArrayList<>();
        paymentLines.add(paymentLine);
        request.setPaymentLines(paymentLines);
        return request;
    }
}
