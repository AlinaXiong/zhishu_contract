package com.hero.middleware.service.impl;

import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSONObject;
import com.hero.middleware.annotation.ZhishuEventLog;
import com.hero.middleware.client.zhishu.request.BaseEventRequest;
import com.hero.middleware.client.zhishu.request.ContractEventRequest;
import com.hero.middleware.dto.BatchApprovalDTO;
import com.hero.middleware.dto.BatchApprovalResultDTO;
import com.hero.middleware.service.ApprovalService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.reflect.Array;
import java.util.Arrays;

@Slf4j
@SpringBootTest
public class ApprovalServiceImplTest {

    @Autowired
    private ApprovalService approvalService;

    @Test
    public void testBatchApproval() {
        BatchApprovalDTO dto = new BatchApprovalDTO();
        dto.setContractIds(Arrays.asList("1111444485046272329"));
        dto.setApproval("1a42d113");
        dto.setApprovalStatus("1");
        dto.setApprovalOpinion("同意审批！！！");
        BatchApprovalResultDTO batchApprovalResultDTO = approvalService.batchApproval(dto);
        System.out.println(JSONObject.toJSONString(batchApprovalResultDTO));
    }

    @Test
    @ZhishuEventLog
    public void testBatchApproval2() {
        String json = "{\n" +
                "  \"schema\": \"2.0\",\n" +
                "  \"header\": {\n" +
                "    \"token\": \"kbrdrruoWvDgrGx4fbpfoIBLDdPQ5qaUWKDZv\",\n" +
                "    \"tenant_key\": \"1510d056f6ced740\",\n" +
                "    \"create_time\": \"1782291489\",\n" +
                "    \"event_id\": \"1149018383694430316\",\n" +
                "    \"event_type\": \"contract.contract.change_v1\",\n" +
                "    \"app_id\": \"cli_zfZiPI7NXBQKAT8q\"\n" +
                "  },\n" +
                "  \"event\": {\n" +
                "    \"contract_stage_code\": 14,\n" +
                "    \"contract_stage_name\": \"节点拒绝时\",\n" +
                "    \"business_type_code\": 0,\n" +
                "    \"previous_id\": \"0\",\n" +
                "    \"contract_id\": \"1149012790782984521\",\n" +
                "    \"group_id\": \"1149012790787178825\",\n" +
                "    \"contract_number\": \"H-ZB202606240135\",\n" +
                "    \"contract_category_abbreviation\": \"ZBJJ\",\n" +
                "    \"extra_info\": {\n" +
                "      \"node_id\": \"UserTask_approve_1777424933564:1\",\n" +
                "      \"node_name\": \"申请人确认签约性质\"\n" +
                "    },\n" +
                "    \"app_id\": \"\"\n" +
                "  }\n" +
                "}";
        BaseEventRequest dto = JSONUtil.toBean(json, BaseEventRequest.class);
        ContractEventRequest event = JSONObject.parseObject(dto.getEvent().toString(), ContractEventRequest.class);
        approvalService.approval(event);
    }

    @Test
    public void testCallback() {
        String json = "{\n" +
                "  \"schema\": \"2.0\",\n" +
                "  \"header\": {\n" +
                "    \"token\": \"g6cgX1MIZbqGsL1jyoX6SKeU3MGFT1WMV0hP7\",\n" +
                "    \"tenant_key\": \"1aca3560facd5b94\",\n" +
                "    \"create_time\": \"1778471313\",\n" +
                "    \"event_id\": \"1132610853078564972\",\n" +
                "    \"event_type\": \"contract.contract.change_v1\",\n" +
                "    \"app_id\": \"cli_z4uokZHuv6DLkqy6\"\n" +
                "  },\n" +
                "  \"event\": {\n" +
                "    \"contract_stage_code\": 10,\n" +
                "    \"contract_stage_name\": \"节点通过时\",\n" +
                "    \"business_type_code\": 0,\n" +
                "    \"previous_id\": \"0\",\n" +
                "    \"contract_id\": \"1131600872401273161\",\n" +
                "    \"group_id\": \"1132610315389763948\",\n" +
                "    \"contract_number\": \"CT20260511000003\",\n" +
                "    \"contract_category_abbreviation\": \"ZS\",\n" +
                "    \"extra_info\": {\n" +
                "      \"node_id\": \"UserTask_approve_1778471075026:1\",\n" +
                "      \"node_name\": \"审批节点\"\n" +
                "    },\n" +
                "    \"app_id\": \"\"\n" +
                "  }\n" +
                "}";
        BaseEventRequest dto = JSONUtil.toBean(json, BaseEventRequest.class);
        ContractEventRequest event = JSONUtil.toBean(dto.getEvent().toString(), ContractEventRequest.class);
//        approvalService.approval(event);
        try {
            approvalService.fnSyncData(event);
        } catch (Exception e) {
            log.error("审批对应多维表格测试异常",e.getMessage());
        }
//        return JSONUtil.parseObj(json);
    }

}
