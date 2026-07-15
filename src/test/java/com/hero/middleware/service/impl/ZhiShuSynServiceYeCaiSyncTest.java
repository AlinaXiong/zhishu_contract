package com.hero.middleware.service.impl;

import com.hero.middleware.client.zhishu.ZhishuContractClient;
import com.hero.middleware.client.zhishu.response.ContractQueryResponse;
import com.hero.middleware.client.zhishu.response.ContractsSearchResponse;
import com.hero.middleware.dto.YeCaiContractSyncDTO;
import com.hero.middleware.dto.YeCaiContractSyncResultDTO;
import com.hero.middleware.service.ContractService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ZhiShuSynServiceYeCaiSyncTest {

    @Mock
    private ZhishuContractClient zhishuContractClient;

    @Mock
    private ContractService contractService;

    @InjectMocks
    private ZhiShuSynServiceImpl service;

    @Test
    void keepsExistingSuccessFieldsAndAddsSkipReasonToRemark() {
        YeCaiContractSyncDTO request = new YeCaiContractSyncDTO();
        request.setContractNumbers(Collections.singletonList("XJ-F2-202606007"));
        request.setThreadCount(1);
        when(zhishuContractClient.searchContracts(any())).thenReturn(searchResponse(8));
        when(contractService.syncContractFromZhishuWithRemark(any()))
                .thenReturn("合同状态为归档中，仅状态已撤回、审批中、已拒绝、已归档、已变更允许同步，未调用业财同步接口");

        YeCaiContractSyncResultDTO result = service.syncYeCaiContracts(request);

        assertEquals(1, result.getTotalCount());
        assertEquals(1, result.getSuccessCount());
        assertEquals(0, result.getFailCount());
        assertEquals("成功", result.getItems().get(0).getResult());
        assertTrue(result.getItems().get(0).getRemark().contains("已跳过"));
        assertTrue(result.getItems().get(0).getRemark().contains("归档中"));
    }

    private ContractsSearchResponse searchResponse(int contractStatusCode) {
        ContractQueryResponse contract = new ContractQueryResponse();
        contract.setContractId(1155144910454653257L);
        contract.setContractNumber("XJ-F2-202606007");
        contract.setContractStatusCode(contractStatusCode);

        ContractsSearchResponse.DataInfo data = new ContractsSearchResponse.DataInfo();
        data.setItems(Collections.singletonList(contract));
        ContractsSearchResponse response = new ContractsSearchResponse();
        response.setCode(0);
        response.setData(data);
        return response;
    }
}
