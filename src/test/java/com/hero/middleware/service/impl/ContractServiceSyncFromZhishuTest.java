package com.hero.middleware.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hero.middleware.client.yuecai.YuecaiContractClient;
import com.hero.middleware.client.yuecai.request.ContSyncRequest;
import com.hero.middleware.client.yuecai.response.YuecaiResponse;
import com.hero.middleware.client.zhishu.ZhishuApiClient;
import com.hero.middleware.client.zhishu.ZhishuContractClient;
import com.hero.middleware.client.zhishu.response.ContractQueryResponse;
import com.hero.middleware.client.zhishu.response.ContractResponse;
import com.hero.middleware.dto.ContractSyncDTO;
import com.hero.middleware.entity.ContractSyncLog;
import com.hero.middleware.exception.BusinessException;
import com.hero.middleware.mapper.ContractSyncLogMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContractServiceSyncFromZhishuTest {

    @Mock
    private ContractSyncLogMapper contractSyncLogMapper;

    @Mock
    private ZhishuContractClient zhishuContractClient;

    @Mock
    private ZhishuApiClient zhishuApiClient;

    @Mock
    private YuecaiContractClient yuecaiContractClient;

    @InjectMocks
    private ContractServiceImpl contractService;

    @Test
    void syncContractFromZhishuRecordsFailWhenYuecaiReturnsBusinessError() {
        ContractSyncDTO dto = new ContractSyncDTO();
        dto.setContractId("100");
        when(zhishuContractClient.getContract(eq("100"), anyMap()))
                .thenReturn(contractResponse(buildSyncableContract()));
        YuecaiResponse yuecaiResponse = new YuecaiResponse();
        yuecaiResponse.setCode(200);
        yuecaiResponse.setSuccess(false);
        yuecaiResponse.setMessage("业务校验失败");
        when(yuecaiContractClient.syncContract(any(ContSyncRequest.class))).thenReturn(yuecaiResponse);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> contractService.syncContractFromZhishu(dto));

        assertTrue(exception.getMessage().contains("业务校验失败"));
        ArgumentCaptor<ContractSyncLog> logCaptor = ArgumentCaptor.forClass(ContractSyncLog.class);
        verify(contractSyncLogMapper).insert(logCaptor.capture());
        ContractSyncLog syncLog = logCaptor.getValue();
        assertEquals("SYNC", syncLog.getSyncType());
        assertEquals("ZHISHU_TO_YUECAI", syncLog.getSyncDirection());
        assertEquals("FAIL", syncLog.getSyncStatus());
        assertTrue(syncLog.getErrorMessage().contains("业务校验失败"));
    }

    @Test
    void syncContractFromZhishuRecordsSuccessOnlyAfterYuecaiSuccess() {
        ContractSyncDTO dto = new ContractSyncDTO();
        dto.setContractId("100");
        when(zhishuContractClient.getContract(eq("100"), anyMap()))
                .thenReturn(contractResponse(buildSyncableContract()));
        YuecaiResponse yuecaiResponse = new YuecaiResponse();
        yuecaiResponse.setCode(200);
        yuecaiResponse.setSuccess(true);
        yuecaiResponse.setMessage("success");
        when(yuecaiContractClient.syncContract(any(ContSyncRequest.class))).thenReturn(yuecaiResponse);

        contractService.syncContractFromZhishu(dto);

        ArgumentCaptor<ContractSyncLog> logCaptor = ArgumentCaptor.forClass(ContractSyncLog.class);
        verify(contractSyncLogMapper).insert(logCaptor.capture());
        assertEquals("SUCCESS", logCaptor.getValue().getSyncStatus());
    }

    private ContractResponse contractResponse(ContractQueryResponse contract) {
        ContractResponse response = new ContractResponse();
        response.setCode(0);
        Map<String, Object> data = new HashMap<>();
        data.put("contract", JSONObject.parseObject(JSON.toJSONString(contract)));
        response.setData(data);
        return response;
    }

    private ContractQueryResponse buildSyncableContract() {
        ContractQueryResponse contract = new ContractQueryResponse();
        contract.setContractId(100L);
        contract.setContractNumber("C-001");
        contract.setContractName("测试合同");
        contract.setContractStatusCode(3);
        contract.setPayTypeCode(4);
        contract.setStartDate("2026-01-01");
        contract.setEndDate("2026-12-31");
        contract.setCurrencyCode("CNY");
        contract.setCreateUserId("user-1");
        contract.setParentContractCategoryAbbreviation("GENERAL");
        contract.setOurPartyList(Collections.emptyList());
        contract.setCounterPartyList(Collections.emptyList());
        ContractQueryResponse.MultiUrl multiUrl = new ContractQueryResponse.MultiUrl();
        multiUrl.setPcUrl("https://contract.example/detail/100");
        contract.setMultiUrl(multiUrl);
        return contract;
    }
}
