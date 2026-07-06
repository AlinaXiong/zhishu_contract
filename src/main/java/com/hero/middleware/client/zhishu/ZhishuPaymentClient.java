package com.hero.middleware.client.zhishu;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hero.middleware.client.zhishu.request.SyncPaymentRequest;
import com.hero.middleware.client.zhishu.request.SyncReceiptRequest;
import com.hero.middleware.client.zhishu.response.PaymentResponse;
import com.hero.middleware.client.zhishu.response.ReceiptResponse;
import com.hero.middleware.dto.ReceiptSyncDTO;
import com.hero.middleware.enums.ZhishuAndYecaiFiledEnum;
import com.hero.middleware.utils.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class ZhishuPaymentClient {

    @Autowired
    private ZhishuApiClient zhishuApiClient;

    private static final String SYNC_PAYMENT_PATH = "/open-apis/contract/v1/payment/notify?user_id_type=user_id";
    private static final String SYNC_RECEIPT_PATH = "/open-apis/contract/v1/collection/:contract_id/notify?user_id_type=user_id";

    public PaymentResponse syncPayment(SyncPaymentRequest request) {
        log.info("同步智书付款记录请求参数: {}", JSON.toJSONString(request));

        if (request.getContractId() == null || request.getContractId().trim().isEmpty()) {
            throw new RuntimeException("同步智书付款记录失败: 合同ID不能为空");
        }

        JSONObject body = new JSONObject();
        body.put("finance_number", request.getPaymentId());
        body.put("finance_amount", request.getAmount());
        body.put("finance_currency", request.getCurrency());
        body.put("finance_url", "");

        JSONArray paymentLines = new JSONArray();
        JSONObject paymentLine = new JSONObject();
        paymentLine.put("source_id", request.getPaymentId());
        paymentLine.put("transaction_amount", request.getAmount());
        paymentLine.put("transaction_status", request.getPaymentStatus());
        paymentLine.put("transaction_time", request.getPaymentTime());
        paymentLine.put("currency", request.getCurrency());
        paymentLine.put("url", "");

        JSONArray relations = new JSONArray();
        JSONObject relation = new JSONObject();
        relation.put("contract_id", request.getContractId());
        relation.put("amount", request.getAmount());
        relations.add(relation);
        paymentLine.put("relations", relations);
        paymentLines.add(paymentLine);
        body.put("payment_lines", paymentLines);

        log.info("同步智书付款记录请求路径: {}", SYNC_PAYMENT_PATH);
        log.info("同步智书付款记录请求体: {}", body.toJSONString());
        String response = zhishuApiClient.doPost("同步智书付款记录", SYNC_PAYMENT_PATH, body);
        log.info("同步智书付款记录响应: {}", response);
        return parseResponse(response, PaymentResponse.class);
    }

    public ReceiptResponse syncReceipt(ReceiptSyncDTO request) {
        log.info("同步智书收款记录请求参数: {}", JSON.toJSONString(request));

//        SyncReceiptRequest.PaymentLine paymentLine = null;
//        List<SyncReceiptRequest.PaymentLine> paymentLines = request.getPaymentLines();
//        if (paymentLines != null && !paymentLines.isEmpty()) {
//            paymentLine = paymentLines.get(0);
//        }
//
//        SyncReceiptRequest.Relation relation = null;
//        if (paymentLine != null && paymentLine.getRelations() != null && !paymentLine.getRelations().isEmpty()) {
//            relation = paymentLine.getRelations().get(0);
//        }

        String contractId = request != null ? request.getContractId() : null;
        if (contractId == null || contractId.trim().isEmpty()) {
            throw new RuntimeException("同步智书收款记录失败: 合同ID不能为空");
        }

        String recordAmount = request.getReceiptAmount() != null
                ? request.getReceiptAmount()
                : "0";
        String recordAmountCurrency = request.getCurrencyCode() != null
                ? request.getCurrencyCode()
                : "CNY";
        String recordNumber = request.getBusinessCode();
        String recordStatus = request.getTransactionStatus();
        String recordDate = request.getTransactionTime();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        JSONObject body = new JSONObject();
        body.put("collection_plan_id", request.getCollectionPlanId());//明细行id
        body.put("record_amount", recordAmount);//付款金额
        body.put("record_amount_currency", "CNY".equals(recordAmountCurrency)?"1":recordAmountCurrency);//币种
        body.put("record_number", recordNumber);//业务编号
        body.put("record_status", recordStatus);
        try {
            body.put("record_date", sdf.format(sdf.parse(recordDate)));//交易时间
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        Map<String,String> amountMap = null;
        if(request.getInvoiceAmount()!=null){
            amountMap = new HashMap<String,String>();
            amountMap.put("currency","1");
            amountMap.put("amount",request.getInvoiceAmount());
        }

        JSONArray collectionRecord = new JSONArray();
        addCollectionRecord(collectionRecord, ZhishuAndYecaiFiledEnum.RECEIPT_INVOICE_NUMBER.getZhishuFiled(), request.getInvoiceNumber());//开票编号
        addCollectionRecord(collectionRecord, ZhishuAndYecaiFiledEnum.RECEIPT_INVOICE_AMOUNT.getZhishuFiled(), amountMap);//开票金额
        addCollectionRecord(collectionRecord, ZhishuAndYecaiFiledEnum.RECEIPT_INVOICE_ENTITY.getZhishuFiled(), request.getInvoiceEntity());//开票主体
        addCollectionRecord(collectionRecord, ZhishuAndYecaiFiledEnum.RECEIPT_ORDER_NUMBER.getZhishuFiled(), request.getOrderNumber());//订单编号
        addCollectionRecord(collectionRecord, ZhishuAndYecaiFiledEnum.RECEIPT_ORDER_NAME.getZhishuFiled(), request.getOrderName());//订单名称
        addCollectionRecord(collectionRecord, ZhishuAndYecaiFiledEnum.RECEIPT_COST_CENTER.getZhishuFiled(), request.getCostCenter());//成本中心
        addCollectionRecord(collectionRecord, ZhishuAndYecaiFiledEnum.RECEIPT_CLIENT_NAME.getZhishuFiled(), request.getClientName());//客户名称
        body.put("collection_record", collectionRecord);

        String syncReceiptPath = SYNC_RECEIPT_PATH.replace(":contract_id", contractId);
        log.info("同步智书收款记录请求路径: {}", syncReceiptPath);
        log.info("同步智书收款记录请求体: {}", body.toJSONString());
        String response = zhishuApiClient.doPost("同步智书收款记录", syncReceiptPath, body);
        log.info("同步智书收款记录响应: {}", response);

        JSONObject responseObject = JSONObject.parseObject(response);
        ReceiptResponse receiptResponse = new ReceiptResponse();
        receiptResponse.setCode(responseObject.getInteger("code"));
        receiptResponse.setMsg(responseObject.getString("msg"));
        return receiptResponse;
    }

    private <T> T parseResponse(String response, Class<T> clazz) {
        return com.alibaba.fastjson.JSON.parseObject(response, clazz);
    }

    private void addCollectionRecord(JSONArray collectionRecord, String attributeKey, Object attributeValue) {
        if(attributeValue == null){
            return;
        }
        JSONObject record = new JSONObject();
        record.put("attribute_key", attributeKey);
        record.put("attribute_value", attributeValue);
        collectionRecord.add(record);
    }

}
