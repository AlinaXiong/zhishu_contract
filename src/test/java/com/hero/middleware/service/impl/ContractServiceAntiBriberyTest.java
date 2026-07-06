package com.hero.middleware.service.impl;

import com.hero.middleware.client.zhishu.ZhishuContractClient;
import com.hero.middleware.client.zhishu.request.CreateTemplateInstanceRequest;
import com.hero.middleware.client.zhishu.request.ZhishuCreateContractRequest;
import com.hero.middleware.client.zhishu.response.CreateTemplateInstanceResponse;
import com.hero.middleware.client.zhishu.response.QueryAllVendorResponse;
import com.hero.middleware.client.zhishu.response.ZhishuCreateContractResponse;
import com.hero.middleware.config.YeCaiDataConfig;
import com.hero.middleware.dto.CreateAntiBriberyContractResultDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContractServiceAntiBriberyTest {

    @Mock
    private ZhishuContractClient zhishuContractClient;

    @Mock
    private YeCaiDataConfig yeCaiDataConfig;

    @InjectMocks
    private ContractServiceImpl contractService;

    @Test
    void createAntiBriberyContractSuccess() {
        when(yeCaiDataConfig.getTemplateFHLXY()).thenReturn("202604280014");
        when(yeCaiDataConfig.getUserId()).thenReturn("e8d58ag6");
        CreateTemplateInstanceResponse templateInstanceResponse = new CreateTemplateInstanceResponse();
        templateInstanceResponse.setTemplateInstanceid("template-instance-001");
        when(zhishuContractClient.createTemplateInstance(any(CreateTemplateInstanceRequest.class)))
                .thenReturn(templateInstanceResponse);
        ZhishuCreateContractResponse contractResponse = buildSuccessContractResponse();
        when(zhishuContractClient.createContractV2(any(ZhishuCreateContractRequest.class)))
                .thenReturn(contractResponse);

        CreateAntiBriberyContractResultDTO result = contractService.createAntiBriberyContract(buildVendorItem());

        assertTrue(result.getSuccess());
        assertEquals("contract-001", result.getZhishuContractId());
        assertEquals("CT001", result.getContractNumber());
        assertEquals("反贿赂协议-测试供应商", result.getContractName());
        ArgumentCaptor<CreateTemplateInstanceRequest> templateCaptor =
                ArgumentCaptor.forClass(CreateTemplateInstanceRequest.class);
        verify(zhishuContractClient).createTemplateInstance(templateCaptor.capture());
        assertEquals("202604280014", templateCaptor.getValue().getTemplateNumber());
        assertEquals("e8d58ag6", templateCaptor.getValue().getCreateUserid());
        ArgumentCaptor<ZhishuCreateContractRequest> contractCaptor =
                ArgumentCaptor.forClass(ZhishuCreateContractRequest.class);
        verify(zhishuContractClient).createContractV2(contractCaptor.capture());
        ZhishuCreateContractRequest request = contractCaptor.getValue();
        assertEquals("template-instance-001", request.getTemplateInstanceId());
        assertEquals("V001", request.getCounterPartyList().get(0).getCounterPartyCode());
        assertEquals("L00100001", request.getOurPartyList().get(0).getOurPartyCode());
        assertEquals(Integer.valueOf(4), request.getPayTypeCode());
        assertEquals(Integer.valueOf(2), request.getPropertyTypeCode());
        assertEquals(BigDecimal.ZERO, request.getAmount());
        assertEquals("addr-001", request.getCounterPartyList().get(0).getBusinessAddressInfo().getId());
        assertEquals("bank-001", request.getCounterPartyList().get(0).getBankAccountInfo().getId());
        assertEquals("contact-001", request.getCounterPartyList().get(0).getContactInfo().getId());
    }

    @Test
    void createAntiBriberyContractWithNullItemReturnsFail() {
        CreateAntiBriberyContractResultDTO result = contractService.createAntiBriberyContract(null);

        assertFalse(result.getSuccess());
        assertNotNull(result.getErrMessage());
        verifyNoInteractions(zhishuContractClient);
    }

    @Test
    void createAntiBriberyContractWithBlankVendorReturnsFail() {
        QueryAllVendorResponse.Item item = new QueryAllVendorResponse.Item();
        item.setVendor(" ");

        CreateAntiBriberyContractResultDTO result = contractService.createAntiBriberyContract(item);

        assertFalse(result.getSuccess());
        assertNotNull(result.getErrMessage());
        verifyNoInteractions(zhishuContractClient);
    }

    @Test
    void createAntiBriberyContractWithZhishuFailReturnsFail() {
        when(yeCaiDataConfig.getTemplateFHLXY()).thenReturn("202604280014");
        when(yeCaiDataConfig.getUserId()).thenReturn("e8d58ag6");
        CreateTemplateInstanceResponse templateInstanceResponse = new CreateTemplateInstanceResponse();
        templateInstanceResponse.setTemplateInstanceid("template-instance-001");
        when(zhishuContractClient.createTemplateInstance(any(CreateTemplateInstanceRequest.class)))
                .thenReturn(templateInstanceResponse);
        ZhishuCreateContractResponse contractResponse = new ZhishuCreateContractResponse();
        contractResponse.setCode(400);
        contractResponse.setMsg("参数错误");
        when(zhishuContractClient.createContractV2(any(ZhishuCreateContractRequest.class)))
                .thenReturn(contractResponse);

        CreateAntiBriberyContractResultDTO result = contractService.createAntiBriberyContract(buildVendorItem());

        assertFalse(result.getSuccess());
        assertEquals("创建反贿赂协议合同失败: 参数错误", result.getErrMessage());
        verify(zhishuContractClient).createContractV2(any(ZhishuCreateContractRequest.class));
    }

    @Test
    void createAntiBriberyContractWithTemplateFailDoesNotCreateContract() {
        when(yeCaiDataConfig.getTemplateFHLXY()).thenReturn("202604280014");
        when(yeCaiDataConfig.getUserId()).thenReturn("e8d58ag6");
        when(zhishuContractClient.createTemplateInstance(any(CreateTemplateInstanceRequest.class)))
                .thenReturn(null);

        CreateAntiBriberyContractResultDTO result = contractService.createAntiBriberyContract(buildVendorItem());

        assertFalse(result.getSuccess());
        verify(zhishuContractClient, never()).createContractV2(any(ZhishuCreateContractRequest.class));
    }

    private QueryAllVendorResponse.Item buildVendorItem() {
        QueryAllVendorResponse.Item item = new QueryAllVendorResponse.Item();
        item.setVendor("V001");
        item.setVendorText("测试供应商");

        QueryAllVendorResponse.VendorAddress address = new QueryAllVendorResponse.VendorAddress();
        address.setId("addr-001");
        address.setAddress("上海市浦东新区");
        item.setVendorAddresses(Collections.singletonList(address));

        QueryAllVendorResponse.VendorAccount account = new QueryAllVendorResponse.VendorAccount();
        account.setId("bank-001");
        account.setAccount("6222000000000000");
        item.setVendorAccounts(Collections.singletonList(account));

        QueryAllVendorResponse.VendorContact contact = new QueryAllVendorResponse.VendorContact();
        contact.setId("contact-001");
        contact.setName("张三");
        item.setVendorContacts(Collections.singletonList(contact));
        return item;
    }

    private ZhishuCreateContractResponse buildSuccessContractResponse() {
        ZhishuCreateContractResponse response = new ZhishuCreateContractResponse();
        response.setCode(0);
        response.setMsg("success");
        ZhishuCreateContractResponse.ContractData data = new ZhishuCreateContractResponse.ContractData();
        ZhishuCreateContractResponse.ContractInfo contract = new ZhishuCreateContractResponse.ContractInfo();
        contract.setContractId("contract-001");
        contract.setContractNumber("CT001");
        contract.setContractName("反贿赂协议-测试供应商");
        data.setContract(contract);
        response.setData(data);
        return response;
    }
}
