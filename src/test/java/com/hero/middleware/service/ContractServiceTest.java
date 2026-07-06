package com.hero.middleware.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hero.middleware.client.zhishu.ZhishuApiClient;
import com.hero.middleware.client.zhishu.ZhishuContractClient;
import com.hero.middleware.client.zhishu.request.ZhishuCreateContractRequest;
import com.hero.middleware.client.zhishu.response.ZhishuCreateContractResponse;
import com.hero.middleware.client.yuecai.YuecaiContractClient;
import com.hero.middleware.dto.CreateContractDTO;
import com.hero.middleware.dto.CreateContractResultDTO;
import com.hero.middleware.entity.Contract;
import com.hero.middleware.entity.ContractSyncLog;
import com.hero.middleware.exception.BusinessException;
import com.hero.middleware.mapper.ContractMapper;
import com.hero.middleware.mapper.ContractSyncLogMapper;
import com.hero.middleware.service.impl.ContractServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("合同服务测试")
@Slf4j
class ContractServiceTest {

    @Mock
    private ContractMapper contractMapper;

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

    private CreateContractDTO createContractDTO;
    private ZhishuCreateContractResponse successResponse;

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

        successResponse = new ZhishuCreateContractResponse();
        successResponse.setCode(200);
        successResponse.setMsg("success");

        ZhishuCreateContractResponse.ContractData data = new ZhishuCreateContractResponse.ContractData();
        ZhishuCreateContractResponse.ContractInfo contractInfo = new ZhishuCreateContractResponse.ContractInfo();
        contractInfo.setContractId("zhishu456");
        contractInfo.setContractNumber("HT202603120001");

        ZhishuCreateContractResponse.MultiUrl multiUrl = new ZhishuCreateContractResponse.MultiUrl();
        multiUrl.setPcUrl("https://zhishu.com/contract/pc?id=zhishu456");
        multiUrl.setMobileUrl("https://zhishu.com/contract/mobile?id=zhishu456");
        contractInfo.setMultiUrl(multiUrl);

        data.setContract(contractInfo);
        successResponse.setData(data);
    }

    @Test
    @DisplayName("创建合同 - 成功")
    void testCreateContract_Success() {
        when(zhishuContractClient.createContractV2(any(ZhishuCreateContractRequest.class)))
                .thenReturn(successResponse);
        when(zhishuApiClient.buildDraftPageUrl(anyString()))
                .thenReturn("https://zhishu.com/contract/draft?id=zhishu456");
        when(contractMapper.insert(any(Contract.class))).thenReturn(1);
        when(contractSyncLogMapper.insert(any(ContractSyncLog.class))).thenReturn(1);

        CreateContractResultDTO result = contractService.createContract(createContractDTO);

        assertNotNull(result);
        assertNotNull(result.getContractId());
        assertEquals("zhishu456", result.getZhishuContractId());
        assertEquals("HT202603120001", result.getContractNumber());
        assertEquals("DRAFT", result.getContractStatus());
        assertNotNull(result.getDraftUrl());

        verify(contractMapper, times(1)).insert(any(Contract.class));
        verify(contractSyncLogMapper, times(1)).insert(any(ContractSyncLog.class));
    }

    @Test
    @DisplayName("创建合同 - 智书API调用失败")
    void testCreateContract_ZhishuApiFailed() {
        ZhishuCreateContractResponse failResponse = new ZhishuCreateContractResponse();
        failResponse.setCode(400);
        failResponse.setMsg("参数错误");

        when(zhishuContractClient.createContractV2(any(ZhishuCreateContractRequest.class)))
                .thenReturn(failResponse);
        when(contractSyncLogMapper.insert(any(ContractSyncLog.class))).thenReturn(1);

        assertThrows(BusinessException.class, () -> {
            contractService.createContract(createContractDTO);
        });

        verify(contractMapper, never()).insert(any(Contract.class));
        verify(contractSyncLogMapper, times(1)).insert(any(ContractSyncLog.class));
    }

    @Test
    @DisplayName("创建合同 - 智书API响应为空")
    void testCreateContract_ZhishuApiNullResponse() {
        when(zhishuContractClient.createContractV2(any(ZhishuCreateContractRequest.class)))
                .thenReturn(null);
        when(contractSyncLogMapper.insert(any(ContractSyncLog.class))).thenReturn(1);

        assertThrows(BusinessException.class, () -> {
            contractService.createContract(createContractDTO);
        });

        verify(contractMapper, never()).insert(any(Contract.class));
        verify(contractSyncLogMapper, times(1)).insert(any(ContractSyncLog.class));
    }

    @Test
    @DisplayName("创建合同 - 智书API调用异常")
    void testCreateContract_ZhishuApiException() {
        when(zhishuContractClient.createContractV2(any(ZhishuCreateContractRequest.class)))
                .thenThrow(new RuntimeException("网络异常"));
        when(contractSyncLogMapper.insert(any(ContractSyncLog.class))).thenReturn(1);

        assertThrows(BusinessException.class, () -> {
            contractService.createContract(createContractDTO);
        });

        verify(contractMapper, never()).insert(any(Contract.class));
        verify(contractSyncLogMapper, times(1)).insert(any(ContractSyncLog.class));
    }

    @Test
    public void testMain(){
        List<String> assigneeIds = new ArrayList<>();//节点审批人员id
        assigneeIds.add("zhishu123");
        assigneeIds.add("zhishu456");
        assigneeIds.add("zhishu789");
        String approval = "zhishu666";
        if(!assigneeIds.contains(approval)){
            log.info("审批人与当前审批单中人员不一致:assigneeIds = {} approval = {}", assigneeIds, approval);
        }else{
            log.info("审批人与当前审批单中人员一致");
        }

    }


    private ZhishuCreateContractRequest buildZhishuRequest(CreateContractDTO dto) {
        ZhishuCreateContractRequest request = new ZhishuCreateContractRequest();

        request.setAmount(new BigDecimal(0));
        request.setContractName("合同-" + dto.getDocumentNumber());
        request.setContractCategoryAbbreviation("DEFAULT");
        request.setCreateUserId(dto.getCreateUserId());
        request.setContractStatusCode("0");
        request.setSourceId(dto.getDocumentNumber());

        // 确保必填参数不为空
        if (dto.getCurrencyCode() == null) {
            request.setCurrencyCode("CNY");
        } else {
            request.setCurrencyCode(dto.getCurrencyCode());
        }

        request.setStartDate(dto.getStartDate());
        request.setEndDate(dto.getEndDate());

        if (dto.getFixedValidityCode() == null) {
            request.setFixedValidityCode(1);
        } else {
            request.setFixedValidityCode(dto.getFixedValidityCode());
        }

        if (dto.getPayTypeCode() == null) {
            request.setPayTypeCode(1);
        } else {
            request.setPayTypeCode(dto.getPayTypeCode());
        }

        //仅收支类型为收入类或支出类时必填
        if((1==dto.getPayTypeCode()||2==dto.getPayTypeCode()) && dto.getPropertyTypeCode() != null){
            request.setPropertyTypeCode(dto.getPropertyTypeCode());
        }

        List<ZhishuCreateContractRequest.OurPartyInfo> ourPartyList = new ArrayList<>();
        if (dto.getOurPartyList() != null) {
            for (CreateContractDTO.OurPartyDTO ourPartyDTO : dto.getOurPartyList()) {
                ZhishuCreateContractRequest.OurPartyInfo ourParty = new ZhishuCreateContractRequest.OurPartyInfo();
                ourParty.setOurPartyCode(ourPartyDTO.getOurPartyCode());
                ZhishuCreateContractRequest.SignInfoResource ourSignInfo = new ZhishuCreateContractRequest.SignInfoResource();
                ourSignInfo.setEnable(false);
                ourParty.setOurPartySignInfoResource(ourSignInfo);
                ourPartyList.add(ourParty);
            }
        }
        request.setOurPartyList(ourPartyList);

        List<ZhishuCreateContractRequest.CounterPartyInfo> counterPartyList = new ArrayList<>();
        if (dto.getCounterPartyList() != null) {
            for (CreateContractDTO.CounterPartyDTO counterPartyDTO : dto.getCounterPartyList()) {
                ZhishuCreateContractRequest.CounterPartyInfo counterParty = new ZhishuCreateContractRequest.CounterPartyInfo();
                counterParty.setCounterPartyCode(counterPartyDTO.getCounterPartyCode());
                ZhishuCreateContractRequest.SignInfoResource counterSignInfo = new ZhishuCreateContractRequest.SignInfoResource();
                counterSignInfo.setEnable(false);
                counterParty.setCounterPartySignInfoResource(counterSignInfo);
                counterPartyList.add(counterParty);
            }
        }
        request.setCounterPartyList(counterPartyList);

        log.info("构建智书合同创建请求: {}", JSON.toJSONString(request));
        return request;
    }
}
