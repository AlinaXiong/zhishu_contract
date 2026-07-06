package com.hero.middleware.service.impl;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hero.middleware.client.feishu.FeishuBitableClient;
import com.hero.middleware.client.zhishu.ZhishuApiClient;
import com.hero.middleware.client.zhishu.ZhishuContractClient;
import com.hero.middleware.client.zhishu.response.ContractQueryResponse;
import com.hero.middleware.client.zhishu.response.ContractResponse;
import com.hero.middleware.config.FeiShuBitableConfig;
import com.hero.middleware.dto.BatchApprovalResultDTO;
import com.hero.middleware.dto.PaymentSyncDTO;
import com.hero.middleware.entity.PaymentRecord;
import com.hero.middleware.exception.BusinessException;
import com.hero.middleware.mapper.PaymentRecordMapper;
import com.hero.middleware.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentRecordMapper paymentRecordMapper;

    @Autowired
    private ZhishuApiClient zhishuApiClient;

    @Autowired
    private ZhishuContractClient zhishuContractClient;

    @Autowired
    private FeishuBitableClient feishuBitableClient;

    @Autowired
    private FeiShuBitableConfig feiShuBitableConfig;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncPayment(PaymentSyncDTO dto) {
        log.info("========== 付款记录同步开始 ==========");
        log.info("接收业财系统付款记录同步请求, 合同ID: {}, 业务编号: {}, 付款金额: {}",
                dto.getContractId(), dto.getBusinessCode(), dto.getPaymentAmount());
        log.debug("付款记录同步请求详细参数: {}", JSON.toJSONString(dto));

        String paymentId = UUID.randomUUID().toString().replace("-", "");
        log.info("生成中间件付款记录ID: {}", paymentId);

        log.info("开始获取合同信息，合同ID：{}", dto.getContractId());
        Map<String, Object> params = new HashMap<>();
        params.put("user_id_type", "user_id");
        ContractResponse contractInfo = zhishuContractClient.getContract(dto.getContractId(),params);
        if(contractInfo==null){
            log.info("未获取到合同信息！！！");
            return;
        }
        log.info("合同信息，合同ID：{}", JSONObject.toJSON(contractInfo));

        Map<String, Object> data = contractInfo.getData();
        ContractQueryResponse contractQueryInfo = JSONObject.parseObject(String.valueOf(data.get("contract")), ContractQueryResponse.class);

        dto.setTransactionStatus("PAYMENT_SUCCESS");
        dto.setCreateUserId(contractQueryInfo.getCreateUserId());
        JSONObject plan = buildPaymentPlan(dto);
        log.info("组装智书付款记录请求完成");
        log.debug("智书付款记录请求详细参数: {}", plan.toJSONString());

        String response;
        try {
            log.info("调用智书付款记录同步API...");
            response = zhishuApiClient.paymentSave(new HashMap<>(), plan);
            log.info("智书API响应: {}", response);
        } catch (Exception e) {
            log.error("调用智书付款记录同步API异常: {}", e.getMessage(), e);
            throw new BusinessException("智书付款记录同步失败: " + e.getMessage());
        }

        JSONObject responseJson = JSON.parseObject(response);
        Integer code = responseJson.getInteger("code");
        if (code == null || code != 0) {
            String errorMsg = responseJson.getString("msg");
            log.error("智书付款记录同步失败: {}", errorMsg);
            throw new BusinessException("智书付款记录同步失败: " + errorMsg);
        }

        //2.飞书多为表格数据同步
        try {
            addFeishuExcelData(dto);
        } catch (Exception e) {
            log.error("飞书付款记录同步API异常: {}", e.getMessage(), e);
            throw new BusinessException("飞书付款记录同步API异常: " + e.getMessage());
        }

        PaymentRecord record = new PaymentRecord();
        record.setPaymentId(paymentId);
        record.setContractId(dto.getContractId());
        record.setYuecaiPaymentId(dto.getBusinessCode());
        record.setAmount(dto.getPaymentAmount());
        record.setCurrency(dto.getCurrencyCode());
        record.setPaymentStatus(dto.getTransactionStatus());
        record.setPaymentTime(dto.getTransactionTime());
        record.setCreateTime(LocalDateTime.now());
        paymentRecordMapper.insert(record);
        log.info("付款记录保存到数据库成功, paymentId: {}", paymentId);

        log.info("========== 付款记录同步完成 ==========");
    }

    private JSONObject buildPaymentPlan(PaymentSyncDTO dto) {
        JSONObject plan = new JSONObject();
        plan.put("applicant_user_id", dto.getCreateUserId());
        plan.put("finance_number", dto.getBusinessCode());
        plan.put("finance_amount", dto.getPaymentAmount());
        plan.put("finance_currency", dto.getCurrencyName());
        Map<String, Object> params = new HashMap<>();
        params.put("user_id_type", "user_id");
        //获取合同信息
        ContractResponse contractInfo = zhishuContractClient.getContract(dto.getContractId(), params);
        String counterPartyCode = dto.getCounterPartyCode();
        if(contractInfo==null){
            log.info("未查询到合同信息：ContractId = {}", dto.getContractId());
            return plan;
        }else{
            Map<String, Object> data = contractInfo.getData();
            ContractQueryResponse contract = JSONObject.parseObject(String.valueOf(data.get("contract")), ContractQueryResponse.class);
            List<ContractQueryResponse.CounterParty> counterPartyList = contract.getCounterPartyList();
            for (ContractQueryResponse.CounterParty counterParty : counterPartyList) {
                String counterPartyCodeQuery = counterParty.getCounterPartyCode();
                if(counterPartyCodeQuery.contains(dto.getCounterPartyCode())){
                    counterPartyCode = counterPartyCodeQuery;
                    break;
                }
            }
        }

        List<Map<String, Object>> paymentLines = new ArrayList<>();
        Map<String, Object> paymentLine = new HashMap<>();
        paymentLine.put("source_id", dto.getBusinessCode());
        paymentLine.put("transaction_amount", dto.getPaymentAmount());
        paymentLine.put("transaction_status", dto.getTransactionStatus());
        paymentLine.put("transaction_time", dto.getTransactionTime());
        paymentLine.put("currency", dto.getCurrencyCode());
        paymentLine.put("trading_party_code", counterPartyCode);

        if (dto.getBankAccountNumber() != null && !dto.getBankAccountNumber().isEmpty()) {
            Map<String, Object> tradingPartyAccount = new HashMap<>();
            tradingPartyAccount.put("account_number", dto.getBankAccountNumber());
            paymentLine.put("trading_party_account", tradingPartyAccount);
        }

        List<Map<String, Object>> relations = new ArrayList<>();
        Map<String, Object> relation = new HashMap<>();
        relation.put("contract_id", dto.getContractId());
        relation.put("amount", dto.getPaymentAmount());
        relation.put("payment_plan_uuid", dto.getPaymentPlanUuid());
        relations.add(relation);
        paymentLine.put("relations", relations);

        paymentLines.add(paymentLine);
        plan.put("payment_lines", paymentLines);

        return plan;
    }

    /**
     * 飞书多维表格同步
     */
    public void addFeishuExcelData(PaymentSyncDTO dto) throws Exception {
        JSONObject jsonObject = new JSONObject()
                .fluentPut("合同ID", dto.getContractId())
                .fluentPut("业务编号", dto.getBusinessCode())
                .fluentPut("付款金额", dto.getPaymentAmount())
                .fluentPut("币种", dto.getCurrencyCode())
                .fluentPut("交易时间", DateUtil.parse(dto.getTransactionTime()).getTime())
                .fluentPut("创建时间", System.currentTimeMillis())
                ;
        feishuBitableClient.createAppTableRecordSample(jsonObject, feiShuBitableConfig.getPaymentTableId());
    }
}
