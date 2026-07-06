package com.hero.middleware.controller;

import com.hero.middleware.dto.DeleteDraftContractsResultDTO;
import com.hero.middleware.service.ZhiShuSynService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ContractSynController.class)
class ContractSynControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ZhiShuSynService zhiShuSynService;

    @Test
    void deleteDraftContractsCallsService() throws Exception {
        DeleteDraftContractsResultDTO result = new DeleteDraftContractsResultDTO();
        result.setTotalCount(1);
        when(zhiShuSynService.deleteAllDraftContracts()).thenReturn(result);

        mockMvc.perform(post("/api/contract/syn/draft-contracts/delete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalCount").value(1));

        verify(zhiShuSynService).deleteAllDraftContracts();
    }
}
