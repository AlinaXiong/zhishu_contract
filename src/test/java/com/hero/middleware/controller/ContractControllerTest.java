package com.hero.middleware.controller;

import com.alibaba.fastjson.JSON;
import com.hero.middleware.dto.CreateContractDTO;
import com.hero.middleware.dto.CreateContractResultDTO;
import com.hero.middleware.service.ContractService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ContractController.class)
@DisplayName("合同控制器测试")
class ContractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ContractService contractService;

    private CreateContractDTO createContractDTO;
    private CreateContractResultDTO createContractResultDTO;

    @BeforeEach
    void setUp() {
        createContractDTO = new CreateContractDTO();
        createContractDTO.setDocumentNumber("DOC202603120001");
        createContractDTO.setDocumentType(1);
        createContractDTO.setCreateUserId("user001");
        createContractDTO.setCurrencyCode("CNY");
        createContractDTO.setStartDate("2026-03-12");
        createContractDTO.setEndDate("2027-03-12");
        createContractDTO.setFixedValidityCode(1);
        createContractDTO.setPayTypeCode(1);
        createContractDTO.setPropertyTypeCode(1);

        List<CreateContractDTO.OurPartyDTO> ourPartyList = new ArrayList<>();
        CreateContractDTO.OurPartyDTO ourParty = new CreateContractDTO.OurPartyDTO();
        ourParty.setOurPartyCode("OUR001");
        ourPartyList.add(ourParty);
        createContractDTO.setOurPartyList(ourPartyList);

        List<CreateContractDTO.CounterPartyDTO> counterPartyList = new ArrayList<>();
        CreateContractDTO.CounterPartyDTO counterParty = new CreateContractDTO.CounterPartyDTO();
        counterParty.setCounterPartyCode("COUNTER001");
        counterPartyList.add(counterParty);
        createContractDTO.setCounterPartyList(counterPartyList);

        createContractResultDTO = new CreateContractResultDTO();
        createContractResultDTO.setContractId("contract123");
        createContractResultDTO.setZhishuContractId("zhishu456");
        createContractResultDTO.setContractNumber("HT202603120001");
        createContractResultDTO.setContractName("合同-DOC202603120001");
        createContractResultDTO.setContractStatus("DRAFT");
        createContractResultDTO.setDraftUrl("https://zhishu.com/contract/draft?id=zhishu456");
        createContractResultDTO.setPcUrl("https://zhishu.com/contract/pc?id=zhishu456");
        createContractResultDTO.setMobileUrl("https://zhishu.com/contract/mobile?id=zhishu456");
    }

    @Test
    @DisplayName("创建合同 - 成功")
    void testCreateContract_Success() throws Exception {
        when(contractService.createContract(any(CreateContractDTO.class)))
                .thenReturn(createContractResultDTO);

        mockMvc.perform(post("/api/contract/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(createContractDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("合同创建成功"))
                .andExpect(jsonPath("$.data.contractId").value("contract123"))
                .andExpect(jsonPath("$.data.zhishuContractId").value("zhishu456"))
                .andExpect(jsonPath("$.data.contractNumber").value("HT202603120001"))
                .andExpect(jsonPath("$.data.contractStatus").value("DRAFT"));
    }

    @Test
    @DisplayName("创建合同 - 缺少必填参数")
    void testCreateContract_MissingRequiredField() throws Exception {
        CreateContractDTO invalidDTO = new CreateContractDTO();

        mockMvc.perform(post("/api/contract/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(invalidDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("创建合同 - 单据编号为空")
    void testCreateContract_EmptyDocumentNumber() throws Exception {
        createContractDTO.setDocumentNumber("");

        mockMvc.perform(post("/api/contract/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(createContractDTO)))
                .andExpect(status().isBadRequest());
    }
}
