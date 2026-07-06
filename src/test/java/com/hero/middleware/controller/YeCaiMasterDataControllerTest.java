package com.hero.middleware.controller;

import com.alibaba.fastjson.JSON;
import com.hero.middleware.dto.CustomerMasterDataSyncDTO;
import com.hero.middleware.dto.MasterDataSyncByTypeDTO;
import com.hero.middleware.dto.VendorMasterDataSyncDTO;
import com.hero.middleware.enums.MasterDataTypeEnum;
import com.hero.middleware.service.YeCaiToZhiShuService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(YeCaiMasterDataController.class)
@DisplayName("YeCai master data controller")
class YeCaiMasterDataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private YeCaiToZhiShuService yeCaiToZhiShuService;

    @Test
    void syncVendorByCodeCallsServiceWhenRequestIsValid() throws Exception {
        VendorMasterDataSyncDTO dto = new VendorMasterDataSyncDTO();
        dto.setStartTime("2026-01-01 00:00:00");
        dto.setEndTime("2026-01-02 00:00:00");
        dto.setPage(0);
        dto.setSize(20);

        mockMvc.perform(post("/api/yecai/master-data/vendor/sync-by-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(yeCaiToZhiShuService).synMasterDataByVendorCode(
                eq(MasterDataTypeEnum.VENDER.getCode()), isNull(),
                eq("2026-01-01 00:00:00"), eq("2026-01-02 00:00:00"), eq(0), eq(20));
    }

    @Test
    void syncCustomerByCodeCallsServiceWhenRequestIsValid() throws Exception {
        CustomerMasterDataSyncDTO dto = new CustomerMasterDataSyncDTO();
        dto.setCertificationId("TAX001");

        mockMvc.perform(post("/api/yecai/master-data/customer/sync-by-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(yeCaiToZhiShuService).synMasterDataByVendorCode(
                eq(MasterDataTypeEnum.CUSTOMER.getCode()), eq("TAX001"),
                isNull(), isNull(), isNull(), isNull());
    }

    @Test
    void syncByCodeReusesDateRangeValidation() throws Exception {
        VendorMasterDataSyncDTO dto = new VendorMasterDataSyncDTO();
        dto.setStartTime("2026-01-01 00:00:00");

        mockMvc.perform(post("/api/yecai/master-data/vendor/sync-by-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        verifyNoInteractions(yeCaiToZhiShuService);
    }

    @Test
    void syncByCodeCallsServiceByTypeAndTimeRange() throws Exception {
        MasterDataSyncByTypeDTO dto = new MasterDataSyncByTypeDTO();
        dto.setType("CUSTOMER");
        dto.setStartTime("2026-07-01 09:00:00");
        dto.setEndTime("2026-07-01 12:00:00");

        mockMvc.perform(post("/api/yecai/master-data/sync-by-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(yeCaiToZhiShuService).synMasterDataByVendorCode(
                eq(MasterDataTypeEnum.CUSTOMER.getCode()), isNull(),
                eq("2026-07-01 09:00:00"), eq("2026-07-01 12:00:00"), isNull(), isNull());
    }

    @Test
    void syncByCodeAcceptsVendorAlias() throws Exception {
        MasterDataSyncByTypeDTO dto = new MasterDataSyncByTypeDTO();
        dto.setBusinessType("VENDOR");

        mockMvc.perform(post("/api/yecai/master-data/sync-by-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(yeCaiToZhiShuService).synMasterDataByVendorCode(
                eq(MasterDataTypeEnum.VENDER.getCode()), isNull(), isNull(), isNull(), isNull(), isNull());
    }

    @Test
    void syncByCodeAcceptsQueryParams() throws Exception {
        mockMvc.perform(post("/api/yecai/master-data/sync-by-code")
                        .param("type", "VENDER")
                        .param("startTime", "2026-07-01 09:00:00")
                        .param("endTime", "2026-07-01 12:00:00")
                        .param("page", "0")
                        .param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(yeCaiToZhiShuService).synMasterDataByVendorCode(
                eq(MasterDataTypeEnum.VENDER.getCode()), isNull(),
                eq("2026-07-01 09:00:00"), eq("2026-07-01 12:00:00"), eq(0), eq(50));
    }

    @Test
    void syncByCodeRejectsUnsupportedType() throws Exception {
        MasterDataSyncByTypeDTO dto = new MasterDataSyncByTypeDTO();
        dto.setType("BANK");

        mockMvc.perform(post("/api/yecai/master-data/sync-by-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        verifyNoInteractions(yeCaiToZhiShuService);
    }
}
