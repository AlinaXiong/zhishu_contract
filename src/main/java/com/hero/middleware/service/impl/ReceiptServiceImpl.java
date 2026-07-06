package com.hero.middleware.service.impl;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hero.middleware.client.feishu.FeishuBitableClient;
import com.hero.middleware.client.zhishu.ZhishuContractClient;
import com.hero.middleware.client.zhishu.ZhishuPaymentClient;
import com.hero.middleware.client.zhishu.request.SyncReceiptRequest;
import com.hero.middleware.client.zhishu.response.ReceiptResponse;
import com.hero.middleware.config.FeiShuBitableConfig;
import com.hero.middleware.dto.ReceiptSyncDTO;
import com.hero.middleware.entity.ReceiptRecord;
import com.hero.middleware.exception.BusinessException;
import com.hero.middleware.mapper.ReceiptRecordMapper;
import com.hero.middleware.service.ReceiptService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class ReceiptServiceImpl implements ReceiptService {

    @Autowired
    private ReceiptRecordMapper receiptRecordMapper;

    @Autowired
    private ZhishuPaymentClient zhishuPaymentClient;

    @Autowired
    private FeishuBitableClient feishuBitableClient;

    @Autowired
    private FeiShuBitableConfig feiShuBitableConfig;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncReceipt(ReceiptSyncDTO dto) {
        log.info("========== 收款记录同步开始 ==========");
        log.info("接收业财系统收款记录同步请求, 合同ID: {}, 业务编号: {}, 收款金额: {}",
                dto.getContractId(), dto.getBusinessCode(), dto.getReceiptAmount());
        log.debug("收款记录同步请求详细参数: {}", JSON.toJSONString(dto));

        String receiptId = UUID.randomUUID().toString().replace("-", "");
        log.info("生成中间件收款记录ID: {}", receiptId);
        dto.setTransactionStatus("2");
//        SyncReceiptRequest request = buildPaymentPlan(dto);
//        log.info("组装智书收款记录请求完成");
//        log.debug("智书收款记录请求详细参数: {}", JSONObject.toJSON(request));

        ReceiptResponse response = zhishuPaymentClient.syncReceipt(dto);

        if (response == null || response.getCode() != 0) {
            log.error("收款记录同步至智书失败: {}", response != null ? response.getMsg() : "响应为空");
            throw new BusinessException("收款记录同步失败: " + (response != null ? response.getMsg() : "响应为空"));
        }

        //2.飞书多为表格数据同步
        try {
            addFeishuExcelData(dto);
        } catch (Exception e) {
            log.error("飞书收款记录同步API异常: {}", e.getMessage(), e);
            throw new BusinessException("飞书收款记录同步API异常: " + e.getMessage());
        }

        ReceiptRecord record = new ReceiptRecord();
        record.setReceiptId(receiptId);
        record.setContractId(dto.getContractId());
        record.setYuecaiReceiptId(dto.getBusinessCode());
        record.setAmount(new BigDecimal(dto.getReceiptAmount()));
        record.setCurrency(dto.getCurrencyCode());
        record.setReceiptStatus(dto.getTransactionStatus());
        record.setReceiptTime(dto.getTransactionTime());
        record.setCreateTime(LocalDateTime.now());
        receiptRecordMapper.insert(record);

        log.info("收款记录保存到数据库成功, paymentId: {}", receiptId);

        log.info("========== 收款记录同步完成 ==========");
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

    /**
     * 飞书多维表格同步
     */
    public void addFeishuExcelData(ReceiptSyncDTO dto) throws Exception {
        JSONObject jsonObject = new JSONObject()
                .fluentPut("合同ID", dto.getContractId())
                .fluentPut("业务编号", dto.getBusinessCode())
                .fluentPut("收款金额", Double.valueOf(dto.getReceiptAmount()))
                .fluentPut("币种", dto.getCurrencyCode())
                .fluentPut("交易时间", DateUtil.parse(dto.getTransactionTime()).getTime())
                .fluentPut("创建时间", System.currentTimeMillis())
                .fluentPut("收款计划id", dto.getCollectionPlanId()==null?"":String.valueOf(dto.getCollectionPlanId()))
                ;
        feishuBitableClient.createAppTableRecordSample(jsonObject, feiShuBitableConfig.getReceiptTableId());
    }
}
