package com.hero.middleware.client.zhishu;

import com.hero.middleware.client.zhishu.response.QueryAllVendorResponse;
import com.hero.middleware.config.YeCaiDataConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ZhiShuVendorClientTest {

    @Mock
    private ZhishuApiClient zhishuApiClient;

    @Mock
    private YeCaiDataConfig yeCaiDataConfig;

    @InjectMocks
    private ZhiShuVendorClient zhiShuVendorClient;

    @Test
    void getVendorByCodeParsesItemsAndSendsExpectedParams() {
        when(zhishuApiClient.doGet(eq("按交易方编码查询智书交易方"),
                eq("/open-apis/mdm/v1/vendors"), anyMap()))
                .thenReturn("{\"code\":0,\"data\":{\"items\":[{\"id\":\"ID1\",\"vendor\":\"V001\",\"vendorText\":\"Vendor 001\"}],\"hasMore\":false}}");

        QueryAllVendorResponse response = zhiShuVendorClient.getVendorByCode("V001");

        assertEquals(1, response.getItems().size());
        assertEquals("ID1", response.getItems().get(0).getId());
        assertEquals("V001", response.getItems().get(0).getVendor());

        ArgumentCaptor<Map> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(zhishuApiClient).doGet(eq("按交易方编码查询智书交易方"),
                eq("/open-apis/mdm/v1/vendors"), paramsCaptor.capture());
        Map params = paramsCaptor.getValue();
        assertEquals("V001", params.get("vendor"));
        assertEquals(10, params.get("page_size"));
        assertEquals("user_id", params.get("user_id_type"));
    }

    @Test
    void getVendorByCodeReturnsEmptyItemsWhenNotFound() {
        when(zhishuApiClient.doGet(eq("按交易方编码查询智书交易方"),
                eq("/open-apis/mdm/v1/vendors"), anyMap()))
                .thenReturn("{\"code\":0,\"data\":{\"items\":[],\"hasMore\":false}}");

        QueryAllVendorResponse response = zhiShuVendorClient.getVendorByCode("V404");

        assertTrue(response.getItems().isEmpty());
    }

    @Test
    void getVendorByCodeReturnsEmptyItemsWhenApiReportsVendorNotFound() {
        when(zhishuApiClient.doGet(eq("按交易方编码查询智书交易方"),
                eq("/open-apis/mdm/v1/vendors"), anyMap()))
                .thenReturn("{\"code\":1640032,\"msg\":\"[vendor : @SENSITIVE@C-C-OS-0294 ]数据不存在\"}");

        QueryAllVendorResponse response = zhiShuVendorClient.getVendorByCode("C-C-OS-0294");

        assertTrue(response.getItems().isEmpty());
    }

    @Test
    void getVendorByCodeThrowsWhenApiFails() {
        when(zhishuApiClient.doGet(eq("按交易方编码查询智书交易方"),
                eq("/open-apis/mdm/v1/vendors"), anyMap()))
                .thenReturn("{\"code\":500,\"msg\":\"failed\"}");

        assertThrows(RuntimeException.class, () -> zhiShuVendorClient.getVendorByCode("V001"));
    }
}
