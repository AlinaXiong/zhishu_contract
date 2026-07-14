package com.hero.middleware.service.impl;

import com.hero.middleware.client.zhishu.ZhiShuVendorClient;
import com.hero.middleware.client.zhishu.ZhishuContractClient;
import com.hero.middleware.client.zhishu.request.CreateVendorRequest;
import com.hero.middleware.client.zhishu.request.CreateTemplateInstanceRequest;
import com.hero.middleware.client.zhishu.request.ZhishuCreateContractRequest;
import com.hero.middleware.client.zhishu.response.CreateTemplateInstanceResponse;
import com.hero.middleware.client.zhishu.response.CreateVendorResponse;
import com.hero.middleware.client.zhishu.response.ContractQueryResponse;
import com.hero.middleware.client.zhishu.response.QueryAllVendorResponse;
import com.hero.middleware.client.zhishu.response.ResultResponse;
import com.hero.middleware.client.zhishu.response.VendorInfoResponse;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    @Mock
    private ZhiShuVendorClient zhiShuVendorClient;

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

    @Test
    void updateCounterPartyAntiBriberySignedUpdatesExistingField() {
        QueryAllVendorResponse.Item item = queryVendorItem("vendor-id-001", "V001");
        VendorInfoResponse vendorInfo = vendorInfo("vendor-id-001", "V001",
                Arrays.asList(vendorExtendInfo("VBI00100001", "否"), vendorExtendInfo("VBI00102002", "是")));
        VendorInfoResponse.VendorAccount account = new VendorInfoResponse.VendorAccount();
        account.setAccount("6222000000000000");
        account.setAccountName("account-name");
        vendorInfo.setVendorAccounts(Collections.singletonList(account));
        CreateVendorResponse updateResponse = createVendorResponse("vendor-id-001");
        when(zhiShuVendorClient.getVendorByCode("V001")).thenReturn(queryVendorResponse(item));
        when(zhiShuVendorClient.getVendorV2("vendor-id-001")).thenReturn(vendorInfo);
        when(zhiShuVendorClient.updateVendor(any(CreateVendorRequest.class), eq("vendor-id-001")))
                .thenReturn(updateResponse);

        ResultResponse result = contractService.updateCounterPartyAntiBriberySigned(contractWithCounterParty("V001"));

        assertEquals(Integer.valueOf(0), result.getCode());
        assertEquals("success", result.getMsg());
        assertEquals(updateResponse, result.getData());
        ArgumentCaptor<CreateVendorRequest> requestCaptor = ArgumentCaptor.forClass(CreateVendorRequest.class);
        verify(zhiShuVendorClient).updateVendor(requestCaptor.capture(), eq("vendor-id-001"));
        CreateVendorRequest request = requestCaptor.getValue();
        assertEquals("V001", request.getVendor());
        assertEquals("测试交易方", request.getVendorText());
        assertEquals("6222000000000000", request.getVendorAccounts().get(0).getAccount());
        assertEquals("是", findExtendInfo(request.getExtendInfo(), "VBI00100001").getFieldValue());
        assertEquals("是", findExtendInfo(request.getExtendInfo(), "VBI00102002").getFieldValue());
    }

    @Test
    void updateCounterPartyAntiBriberySignedAddsFieldWhenMissing() {
        QueryAllVendorResponse.Item item = queryVendorItem("vendor-id-001", "V001");
        VendorInfoResponse vendorInfo = vendorInfo("vendor-id-001", "V001",
                Collections.singletonList(vendorExtendInfo("VBI00102002", "是")));
        when(zhiShuVendorClient.getVendorByCode("V001")).thenReturn(queryVendorResponse(item));
        when(zhiShuVendorClient.getVendorV2("vendor-id-001")).thenReturn(vendorInfo);
        when(zhiShuVendorClient.updateVendor(any(CreateVendorRequest.class), eq("vendor-id-001")))
                .thenReturn(createVendorResponse("vendor-id-001"));

        ResultResponse result = contractService.updateCounterPartyAntiBriberySigned(contractWithCounterParty("V001"));

        assertEquals(Integer.valueOf(0), result.getCode());
        ArgumentCaptor<CreateVendorRequest> requestCaptor = ArgumentCaptor.forClass(CreateVendorRequest.class);
        verify(zhiShuVendorClient).updateVendor(requestCaptor.capture(), eq("vendor-id-001"));
        CreateVendorRequest.ExtendInfo signedField = findExtendInfo(requestCaptor.getValue().getExtendInfo(), "VBI00100001");
        assertNotNull(signedField);
        assertEquals(Integer.valueOf(3), signedField.getFieldType());
        assertEquals("是", signedField.getFieldValue());
    }

    @Test
    void updateCounterPartyAntiBriberySignedWithEmptyContractReturnsFail() {
        ResultResponse result = contractService.updateCounterPartyAntiBriberySigned(null);

        assertEquals(Integer.valueOf(400), result.getCode());
        verifyNoInteractions(zhiShuVendorClient);
    }

    @Test
    void updateCounterPartyAntiBriberySignedWithNoExactVendorReturnsFail() {
        when(zhiShuVendorClient.getVendorByCode("V001"))
                .thenReturn(queryVendorResponse(queryVendorItem("vendor-id-002", "V002")));

        ResultResponse result = contractService.updateCounterPartyAntiBriberySigned(contractWithCounterParty("V001"));

        assertEquals(Integer.valueOf(404), result.getCode());
        verify(zhiShuVendorClient, never()).getVendorV2(any());
        verify(zhiShuVendorClient, never()).updateVendor(any(CreateVendorRequest.class), any());
    }

    @Test
    void updateCounterPartyAntiBriberySignedWithUpdateFailReturnsFail() {
        QueryAllVendorResponse.Item item = queryVendorItem("vendor-id-001", "V001");
        VendorInfoResponse vendorInfo = vendorInfo("vendor-id-001", "V001",
                Collections.singletonList(vendorExtendInfo("VBI00100001", "否")));
        when(zhiShuVendorClient.getVendorByCode("V001")).thenReturn(queryVendorResponse(item));
        when(zhiShuVendorClient.getVendorV2("vendor-id-001")).thenReturn(vendorInfo);
        when(zhiShuVendorClient.updateVendor(any(CreateVendorRequest.class), eq("vendor-id-001")))
                .thenReturn(null);

        ResultResponse result = contractService.updateCounterPartyAntiBriberySigned(contractWithCounterParty("V001"));

        assertEquals(Integer.valueOf(500), result.getCode());
        assertEquals("修改交易方失败", result.getMsg());
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

    private ContractQueryResponse contractWithCounterParty(String counterPartyCode) {
        ContractQueryResponse contract = new ContractQueryResponse();
        ContractQueryResponse.CounterParty counterParty = new ContractQueryResponse.CounterParty();
        counterParty.setCounterPartyCode(counterPartyCode);
        contract.setCounterPartyList(Collections.singletonList(counterParty));
        return contract;
    }

    private QueryAllVendorResponse queryVendorResponse(QueryAllVendorResponse.Item... items) {
        QueryAllVendorResponse response = new QueryAllVendorResponse();
        List<QueryAllVendorResponse.Item> itemList = new ArrayList<>();
        if (items != null) {
            itemList.addAll(Arrays.asList(items));
        }
        response.setItems(itemList);
        response.setHasMore(false);
        return response;
    }

    private QueryAllVendorResponse.Item queryVendorItem(String id, String vendorCode) {
        QueryAllVendorResponse.Item item = new QueryAllVendorResponse.Item();
        item.setId(id);
        item.setVendor(vendorCode);
        return item;
    }

    private VendorInfoResponse vendorInfo(String id, String vendorCode, List<VendorInfoResponse.ExtendInfo> extendInfo) {
        VendorInfoResponse response = new VendorInfoResponse();
        response.setId(id);
        response.setVendor(vendorCode);
        response.setVendorText("测试交易方");
        response.setVendorType("2");
        response.setStatus(1);
        response.setExtendInfo(extendInfo);
        return response;
    }

    private VendorInfoResponse.ExtendInfo vendorExtendInfo(String fieldCode, String fieldValue) {
        VendorInfoResponse.ExtendInfo extendInfo = new VendorInfoResponse.ExtendInfo();
        extendInfo.setFieldCode(fieldCode);
        extendInfo.setFieldType(3);
        extendInfo.setFieldValue(fieldValue);
        return extendInfo;
    }

    private CreateVendorRequest.ExtendInfo findExtendInfo(List<CreateVendorRequest.ExtendInfo> extendInfoList,
                                                          String fieldCode) {
        assertNotNull(extendInfoList);
        for (CreateVendorRequest.ExtendInfo extendInfo : extendInfoList) {
            if (fieldCode.equals(extendInfo.getFieldCode())) {
                return extendInfo;
            }
        }
        return null;
    }

    private CreateVendorResponse createVendorResponse(String id) {
        CreateVendorResponse response = new CreateVendorResponse();
        response.setId(id);
        return response;
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
