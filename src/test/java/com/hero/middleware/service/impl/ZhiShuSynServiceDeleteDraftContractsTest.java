package com.hero.middleware.service.impl;

import com.hero.middleware.client.zhishu.ZhishuContractClient;
import com.hero.middleware.client.zhishu.request.ContractsSearchRequest;
import com.hero.middleware.client.zhishu.response.ContractQueryResponse;
import com.hero.middleware.client.zhishu.response.ContractsSearchResponse;
import com.hero.middleware.client.zhishu.response.ResultResponse;
import com.hero.middleware.dto.DeleteDraftContractsResultDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ZhiShuSynServiceDeleteDraftContractsTest {

    @Mock
    private ZhishuContractClient zhishuContractClient;

    @Test
    void deleteAllDraftContractsDeletesOnlyDraftContracts() {
        ZhiShuSynServiceImpl service = buildService();
        when(zhishuContractClient.searchContracts(any()))
                .thenReturn(searchResponse(false, null,
                        contract("C-DRAFT", 100L, 0, "editing"),
                        contract("C-APPROVING", 200L, 1, "approving")));
        when(zhishuContractClient.deleteDraftContract("100")).thenReturn(resultResponse(0, "success"));

        DeleteDraftContractsResultDTO result = service.deleteAllDraftContracts();

        assertTrue(result.getCompleted());
        assertEquals(Integer.valueOf(2), result.getTotalCount());
        assertEquals(Integer.valueOf(1), result.getDraftCount());
        assertEquals(Integer.valueOf(1), result.getSuccessCount());
        assertEquals(Integer.valueOf(0), result.getFailCount());
        assertEquals("100", result.getItems().get(0).getContractId());
        assertEquals("C-DRAFT", result.getItems().get(0).getContractNumber());
        assertTrue(result.getItems().get(0).getSuccess());
        verify(zhishuContractClient).deleteDraftContract("100");
        verify(zhishuContractClient, never()).deleteDraftContract("200");
    }

    @Test
    void deleteAllDraftContractsRecordsDeleteFailureReason() {
        ZhiShuSynServiceImpl service = buildService();
        when(zhishuContractClient.searchContracts(any()))
                .thenReturn(searchResponse(false, null, contract("C-DRAFT", 101L, 0, "editing")));
        when(zhishuContractClient.deleteDraftContract("101")).thenReturn(resultResponse(1, "cannot delete"));

        DeleteDraftContractsResultDTO result = service.deleteAllDraftContracts();

        assertTrue(result.getCompleted());
        assertEquals(Integer.valueOf(1), result.getDraftCount());
        assertEquals(Integer.valueOf(0), result.getSuccessCount());
        assertEquals(Integer.valueOf(1), result.getFailCount());
        assertFalse(result.getItems().get(0).getSuccess());
        assertTrue(result.getItems().get(0).getReason().contains("cannot delete"));
    }

    @Test
    void deleteAllDraftContractsUsesPageTokenUntilNoMorePages() {
        ZhiShuSynServiceImpl service = buildService();
        when(zhishuContractClient.searchContracts(any()))
                .thenReturn(searchResponse(true, "next-1", contract("C-001", 100L, 0, "editing")))
                .thenReturn(searchResponse(false, null, contract("C-002", 101L, 0, "editing")));
        when(zhishuContractClient.deleteDraftContract(anyString())).thenReturn(resultResponse(0, "success"));

        DeleteDraftContractsResultDTO result = service.deleteAllDraftContracts();

        assertTrue(result.getCompleted());
        assertEquals(Integer.valueOf(2), result.getTotalCount());
        assertEquals(Integer.valueOf(2), result.getSuccessCount());

        ArgumentCaptor<ContractsSearchRequest> requestCaptor = ArgumentCaptor.forClass(ContractsSearchRequest.class);
        verify(zhishuContractClient, times(2)).searchContracts(requestCaptor.capture());
        List<ContractsSearchRequest> requests = requestCaptor.getAllValues();
        assertNull(requests.get(0).getPageToken());
        assertEquals("next-1", requests.get(1).getPageToken());
        assertEquals(Integer.valueOf(100), requests.get(0).getPageSize());
        assertEquals(Integer.valueOf(100), requests.get(1).getPageSize());
    }

    private ZhiShuSynServiceImpl buildService() {
        ZhiShuSynServiceImpl service = new ZhiShuSynServiceImpl("unused.xlsx", 200);
        ReflectionTestUtils.setField(service, "zhishuContractClient", zhishuContractClient);
        return service;
    }

    private ContractsSearchResponse searchResponse(Boolean hasMore,
                                                   String nextPageToken,
                                                   ContractQueryResponse... contracts) {
        ContractsSearchResponse response = new ContractsSearchResponse();
        response.setCode(0);
        response.setMsg("success");
        ContractsSearchResponse.DataInfo dataInfo = new ContractsSearchResponse.DataInfo();
        dataInfo.setHasMore(hasMore);
        dataInfo.setNextPageToken(nextPageToken);
        dataInfo.setItems(contracts == null ? Collections.emptyList() : Arrays.asList(contracts));
        response.setData(dataInfo);
        return response;
    }

    private ContractQueryResponse contract(String contractNumber,
                                           Long contractId,
                                           Integer statusCode,
                                           String statusName) {
        ContractQueryResponse contract = new ContractQueryResponse();
        contract.setContractNumber(contractNumber);
        contract.setContractId(contractId);
        contract.setContractStatusCode(statusCode);
        contract.setContractStatusName(statusName);
        return contract;
    }

    private ResultResponse resultResponse(Integer code, String msg) {
        ResultResponse response = new ResultResponse();
        response.setCode(code);
        response.setMsg(msg);
        return response;
    }
}
