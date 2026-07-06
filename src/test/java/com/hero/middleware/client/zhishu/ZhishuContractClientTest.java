package com.hero.middleware.client.zhishu;

import com.hero.middleware.client.zhishu.response.ResultResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ZhishuContractClientTest {

    @Mock
    private ZhishuApiClient zhishuApiClient;

    @InjectMocks
    private ZhishuContractClient zhishuContractClient;

    @Test
    void deleteDraftContractCallsDeleteApiAndParsesResponse() {
        when(zhishuApiClient.doDelete(eq("删除智书草稿合同"),
                eq("/open-apis/contract/v1/contracts/100")))
                .thenReturn("{\"code\":0,\"msg\":\"success\"}");

        ResultResponse response = zhishuContractClient.deleteDraftContract("100");

        assertEquals(Integer.valueOf(0), response.getCode());
        assertEquals("success", response.getMsg());
        verify(zhishuApiClient).doDelete(eq("删除智书草稿合同"),
                eq("/open-apis/contract/v1/contracts/100"));
    }
}
