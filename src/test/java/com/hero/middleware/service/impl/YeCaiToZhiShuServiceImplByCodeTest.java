package com.hero.middleware.service.impl;

import com.hero.middleware.client.yuecai.YuecaiContractClient;
import com.hero.middleware.client.yuecai.response.MasterDataRes;
import com.hero.middleware.client.yuecai.response.masterdata.BankBranchRes;
import com.hero.middleware.client.yuecai.response.masterdata.BankRes;
import com.hero.middleware.client.yuecai.response.masterdata.CustomerAccountRes;
import com.hero.middleware.client.yuecai.response.masterdata.CustomerRes;
import com.hero.middleware.client.yuecai.response.masterdata.VenderAccountRes;
import com.hero.middleware.client.yuecai.response.masterdata.VenderRes;
import com.hero.middleware.client.zhishu.ZhiShuVendorClient;
import com.hero.middleware.client.zhishu.request.CreateVendorRequest;
import com.hero.middleware.client.zhishu.response.CreateVendorResponse;
import com.hero.middleware.client.zhishu.response.QueryAllVendorResponse;
import com.hero.middleware.client.zhishu.response.VendorInfoResponse;
import com.hero.middleware.config.YeCaiDataConfig;
import com.hero.middleware.enums.MasterDataTypeEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class YeCaiToZhiShuServiceImplByCodeTest {

    @Mock
    private ZhiShuVendorClient zhiShuVendorClient;

    @Mock
    private YuecaiContractClient yuecaiContractClient;

    @Mock
    private YeCaiDataConfig yeCaiDataConfig;

    private YeCaiToZhiShuServiceImpl service;

    @BeforeEach
    void setUp() {
        service = spy(new YeCaiToZhiShuServiceImpl());
        ReflectionTestUtils.setField(service, "zhiShuVendorClient", zhiShuVendorClient);
        doReturn(Collections.emptyList()).when(service).getAllMasterData(MasterDataTypeEnum.BANK.getCode());
    }

    @Test
    void syncVendorByCodeCreatesWhenZhishuVendorNotFound() {
        VenderRes venderRes = buildVendor("V001", null);
        doReturn(Collections.singletonList(venderRes)).when(service).getAllMasterData(
                eq(MasterDataTypeEnum.VENDER.getCode()), any(), any(), any(), any());
        doReturn(Collections.emptyList()).when(service).getAllAccountData(
                eq(MasterDataTypeEnum.VENDER_ACCOUNT.getCode()), eq("V001"));
        when(zhiShuVendorClient.getVendorByCode("V001")).thenReturn(queryResponse());
        when(zhiShuVendorClient.createVendor(any(CreateVendorRequest.class))).thenReturn(createVendorResponse("ID1"));

        service.synMasterDataByVendorCode(MasterDataTypeEnum.VENDER.getCode(), null, null, null, null, null);

        ArgumentCaptor<CreateVendorRequest> requestCaptor = ArgumentCaptor.forClass(CreateVendorRequest.class);
        verify(zhiShuVendorClient).createVendor(requestCaptor.capture());
        assertEquals("V001", requestCaptor.getValue().getVendor());
        assertEquals("2", requestCaptor.getValue().getVendorType());
        verify(zhiShuVendorClient, never()).getVendorAll(anyString(), anyString());
    }

    @Test
    void syncVendorByCodeUpdatesWhenZhishuVendorExists() {
        VenderRes venderRes = buildVendor("V001", null);
        QueryAllVendorResponse.Item item = queryItem("ID1", "V001");
        doReturn(Collections.singletonList(venderRes)).when(service).getAllMasterData(
                eq(MasterDataTypeEnum.VENDER.getCode()), any(), any(), any(), any());
        doReturn(Collections.emptyList()).when(service).getAllAccountData(
                eq(MasterDataTypeEnum.VENDER_ACCOUNT.getCode()), eq("V001"));
        when(zhiShuVendorClient.getVendorByCode("V001")).thenReturn(queryResponse(item));
        when(zhiShuVendorClient.getVendorV2("ID1")).thenReturn(vendorInfo("ID1", "V001"));
        when(zhiShuVendorClient.updateVendor(any(CreateVendorRequest.class), eq("ID1")))
                .thenReturn(createVendorResponse("ID1"));

        service.synMasterDataByVendorCode(MasterDataTypeEnum.VENDER.getCode(), null, null, null, null, null);

        verify(zhiShuVendorClient).getVendorV2("ID1");
        ArgumentCaptor<CreateVendorRequest> requestCaptor = ArgumentCaptor.forClass(CreateVendorRequest.class);
        verify(zhiShuVendorClient).updateVendor(requestCaptor.capture(), eq("ID1"));
        assertEquals("V001", requestCaptor.getValue().getVendor());
        assertEquals("2", requestCaptor.getValue().getVendorType());
        verify(zhiShuVendorClient, never()).getVendorAll(anyString(), anyString());
    }

    @Test
    void syncCustomerByCodeUsesJoinedVendorCodeAndUpdates() {
        CustomerRes customerRes = buildCustomer("C001", "V001");
        QueryAllVendorResponse.Item item = queryItem("ID1", "V001;C001");
        doReturn(Collections.singletonList(customerRes)).when(service).getAllMasterData(
                eq(MasterDataTypeEnum.CUSTOMER.getCode()), any(), any(), any(), any());
        doReturn(Collections.emptyList()).when(service).getAllAccountData(
                eq(MasterDataTypeEnum.CUSTOMER_ACCOUNT.getCode()), eq("V001;C001"));
        when(zhiShuVendorClient.getVendorByCode("V001;C001")).thenReturn(queryResponse(item));
        when(zhiShuVendorClient.getVendorV2("ID1")).thenReturn(vendorInfo("ID1", "V001;C001"));
        when(zhiShuVendorClient.updateVendor(any(CreateVendorRequest.class), eq("ID1")))
                .thenReturn(createVendorResponse("ID1"));

        service.synMasterDataByVendorCode(MasterDataTypeEnum.CUSTOMER.getCode(), null, null, null, null, null);

        verify(zhiShuVendorClient).getVendorByCode("V001;C001");
        ArgumentCaptor<CreateVendorRequest> requestCaptor = ArgumentCaptor.forClass(CreateVendorRequest.class);
        verify(zhiShuVendorClient).updateVendor(requestCaptor.capture(), eq("ID1"));
        assertEquals("V001;C001", requestCaptor.getValue().getVendor());
        assertEquals("1,2", requestCaptor.getValue().getVendorType());
        verify(zhiShuVendorClient, never()).getVendorAll(anyString(), anyString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAllMasterDataParsesCnapsItems() {
        ReflectionTestUtils.setField(service, "yuecaiContractClient", yuecaiContractClient);
        ReflectionTestUtils.setField(service, "yeCaiDataConfig", yeCaiDataConfig);
        when(yeCaiDataConfig.getPageSize()).thenReturn(100);
        when(yeCaiDataConfig.getStartTime()).thenReturn("2024-01-01");
        MasterDataRes masterDataRes = new MasterDataRes();
        masterDataRes.setTotalPages(1);
        masterDataRes.setContent(Collections.singletonList(
                "{\"bankLocationCode\":\"LOC001\",\"bankLocationName\":\"Branch One\"}"));
        when(yuecaiContractClient.getMasterData(anyMap())).thenReturn(masterDataRes);

        List<BankBranchRes> result = (List<BankBranchRes>) service.getAllMasterData(MasterDataTypeEnum.CNAPS.getCode());

        assertEquals(1, result.size());
        assertEquals("LOC001", result.get(0).getBankLocationCode());
        assertEquals("Branch One", result.get(0).getBankLocationName());
    }

    @Test
    void syncVendorByCodeUsesBankNameWithoutLoadingCnaps() {
        VenderRes venderRes = buildVendor("V001", null);
        doReturn(Collections.singletonList(venderRes)).when(service).getAllMasterData(
                eq(MasterDataTypeEnum.VENDER.getCode()), any(), any(), any(), any());
        doReturn(Collections.singletonList(bank("BANK001", "Bank One", "CN")))
                .when(service).getAllMasterData(MasterDataTypeEnum.BANK.getCode());
        doReturn(Collections.singletonList(vendorAccount("BANK001", "LOC001", "1001")))
                .when(service).getAllAccountData(eq(MasterDataTypeEnum.VENDER_ACCOUNT.getCode()), eq("V001"));
        when(zhiShuVendorClient.getVendorByCode("V001")).thenReturn(queryResponse());
        when(zhiShuVendorClient.createVendor(any(CreateVendorRequest.class))).thenReturn(createVendorResponse("ID1"));

        service.synMasterDataByVendorCode(MasterDataTypeEnum.VENDER.getCode(), null, null, null, null, null);

        ArgumentCaptor<CreateVendorRequest> requestCaptor = ArgumentCaptor.forClass(CreateVendorRequest.class);
        verify(zhiShuVendorClient).createVendor(requestCaptor.capture());
        CreateVendorRequest.VendorAccount account = requestCaptor.getValue().getVendorAccounts().get(0);
        assertEquals("Bank One", account.getBankName());
        assertEquals("CN", account.getCountry());
        verify(service, never()).getAllMasterData(MasterDataTypeEnum.CNAPS.getCode());
    }

    @Test
    void syncVendorByCodeUsesMainBankExcelCountryCode() {
        VenderRes venderRes = buildVendor("V001", null);
        doReturn(Collections.singletonList(venderRes)).when(service).getAllMasterData(
                eq(MasterDataTypeEnum.VENDER.getCode()), any(), any(), any(), any());
        doReturn(Collections.singletonList(bank("314", "Bank From Master", "OLD")))
                .when(service).getAllMasterData(MasterDataTypeEnum.BANK.getCode());
        doReturn(Collections.singletonList(vendorAccount("314", "LOC001", "1001")))
                .when(service).getAllAccountData(eq(MasterDataTypeEnum.VENDER_ACCOUNT.getCode()), eq("V001"));
        when(zhiShuVendorClient.getVendorByCode("V001")).thenReturn(queryResponse());
        when(zhiShuVendorClient.createVendor(any(CreateVendorRequest.class))).thenReturn(createVendorResponse("ID1"));

        service.synMasterDataByVendorCode(MasterDataTypeEnum.VENDER.getCode(), null, null, null, null, null);

        ArgumentCaptor<CreateVendorRequest> requestCaptor = ArgumentCaptor.forClass(CreateVendorRequest.class);
        verify(zhiShuVendorClient).createVendor(requestCaptor.capture());
        CreateVendorRequest.VendorAccount account = requestCaptor.getValue().getVendorAccounts().get(0);
        assertEquals("CN", account.getCountry());
    }

    @Test
    void syncVendorByCodeFallsBackToCnapsNameAndLoadsCnapsOnce() {
        VenderRes venderRes = buildVendor("V001", null);
        doReturn(Collections.singletonList(venderRes)).when(service).getAllMasterData(
                eq(MasterDataTypeEnum.VENDER.getCode()), any(), any(), any(), any());
        doReturn(Collections.emptyList()).when(service).getAllMasterData(MasterDataTypeEnum.BANK.getCode());
        doReturn(Arrays.asList(
                vendorAccount("BANK001", "LOC001", "1001"),
                vendorAccount("BANK002", "LOC002", "1002")))
                .when(service).getAllAccountData(eq(MasterDataTypeEnum.VENDER_ACCOUNT.getCode()), eq("V001"));
        doReturn(Arrays.asList(
                bankBranch("LOC001", "Branch One"),
                bankBranch("LOC002", "Branch Two")))
                .when(service).getAllMasterData(MasterDataTypeEnum.CNAPS.getCode());
        when(zhiShuVendorClient.getVendorByCode("V001")).thenReturn(queryResponse());
        when(zhiShuVendorClient.createVendor(any(CreateVendorRequest.class))).thenReturn(createVendorResponse("ID1"));

        service.synMasterDataByVendorCode(MasterDataTypeEnum.VENDER.getCode(), null, null, null, null, null);

        ArgumentCaptor<CreateVendorRequest> requestCaptor = ArgumentCaptor.forClass(CreateVendorRequest.class);
        verify(zhiShuVendorClient).createVendor(requestCaptor.capture());
        List<CreateVendorRequest.VendorAccount> accounts = requestCaptor.getValue().getVendorAccounts();
        assertEquals("Branch One", accounts.get(0).getBankName());
        assertEquals("Branch Two", accounts.get(1).getBankName());
        verify(service, times(1)).getAllMasterData(MasterDataTypeEnum.CNAPS.getCode());
    }

    @Test
    void syncCustomerByCodeFallsBackToCnapsName() {
        CustomerRes customerRes = buildCustomer("C001", null);
        doReturn(Collections.singletonList(customerRes)).when(service).getAllMasterData(
                eq(MasterDataTypeEnum.CUSTOMER.getCode()), any(), any(), any(), any());
        doReturn(Collections.emptyList()).when(service).getAllMasterData(MasterDataTypeEnum.BANK.getCode());
        doReturn(Collections.singletonList(customerAccount("BANK001", "LOC001", "2001")))
                .when(service).getAllAccountData(eq(MasterDataTypeEnum.CUSTOMER_ACCOUNT.getCode()), eq("C001"));
        doReturn(Collections.singletonList(bankBranch("LOC001", "Customer Branch")))
                .when(service).getAllMasterData(MasterDataTypeEnum.CNAPS.getCode());
        when(zhiShuVendorClient.getVendorByCode("C001")).thenReturn(queryResponse());
        when(zhiShuVendorClient.createVendor(any(CreateVendorRequest.class))).thenReturn(createVendorResponse("ID1"));

        service.synMasterDataByVendorCode(MasterDataTypeEnum.CUSTOMER.getCode(), null, null, null, null, null);

        ArgumentCaptor<CreateVendorRequest> requestCaptor = ArgumentCaptor.forClass(CreateVendorRequest.class);
        verify(zhiShuVendorClient).createVendor(requestCaptor.capture());
        assertEquals("Customer Branch", requestCaptor.getValue().getVendorAccounts().get(0).getBankName());
        verify(service, times(1)).getAllMasterData(MasterDataTypeEnum.CNAPS.getCode());
    }

    @Test
    void syncByBusinessCodesFiltersVendorsAndUsesJoinedVendorCode() {
        VenderRes matchedVendor = buildVendor("V001", "C001");
        VenderRes skippedVendor = buildVendor("V002", null);
        doReturn(Arrays.asList(matchedVendor, skippedVendor)).when(service)
                .getAllMasterData(MasterDataTypeEnum.VENDER.getCode());
        doReturn(Collections.emptyList()).when(service).getAllAccountData(
                eq(MasterDataTypeEnum.VENDER_ACCOUNT.getCode()), eq("V001;C001"));
        when(zhiShuVendorClient.getVendorByCode("V001;C001")).thenReturn(queryResponse());
        when(zhiShuVendorClient.createVendor(any(CreateVendorRequest.class))).thenReturn(createVendorResponse("ID1"));

        service.synMasterDataByBusinessCodes(
                MasterDataTypeEnum.VENDER.getCode(), Arrays.asList(" V001 ", "V001", "UNKNOWN", " "));

        verify(zhiShuVendorClient).getVendorByCode("V001;C001");
        verify(zhiShuVendorClient, never()).getVendorByCode("V002");
        verify(zhiShuVendorClient, never()).getVendorAll(anyString(), anyString());
        ArgumentCaptor<CreateVendorRequest> requestCaptor = ArgumentCaptor.forClass(CreateVendorRequest.class);
        verify(zhiShuVendorClient).createVendor(requestCaptor.capture());
        assertEquals("V001;C001", requestCaptor.getValue().getVendor());
        assertEquals("1,2", requestCaptor.getValue().getVendorType());
    }

    @Test
    void syncByBusinessCodesFiltersCustomersAndUsesJoinedVendorCode() {
        CustomerRes matchedCustomer = buildCustomer("C001", "V001");
        CustomerRes skippedCustomer = buildCustomer("C002", null);
        doReturn(Arrays.asList(matchedCustomer, skippedCustomer)).when(service)
                .getAllMasterData(MasterDataTypeEnum.CUSTOMER.getCode());
        doReturn(Collections.emptyList()).when(service).getAllAccountData(
                eq(MasterDataTypeEnum.CUSTOMER_ACCOUNT.getCode()), eq("V001;C001"));
        when(zhiShuVendorClient.getVendorByCode("V001;C001")).thenReturn(queryResponse());

        service.synMasterDataByBusinessCodes(
                MasterDataTypeEnum.CUSTOMER.getCode(), Arrays.asList(" C001 ", "C001", "UNKNOWN"));

        verify(zhiShuVendorClient).getVendorByCode("V001;C001");
        verify(zhiShuVendorClient, never()).getVendorByCode("C002");
        verify(zhiShuVendorClient, never()).getVendorAll(anyString(), anyString());
    }

    @Test
    void syncByBusinessCodesSkipsEmptyCodes() {
        service.synMasterDataByBusinessCodes(MasterDataTypeEnum.VENDER.getCode(), Arrays.asList(" ", null, ""));

        verify(zhiShuVendorClient, never()).getVendorByCode(anyString());
        verify(zhiShuVendorClient, never()).getVendorAll(anyString(), anyString());
    }

    @Test
    void syncByBusinessCodesSkipsUnsupportedType() {
        service.synMasterDataByBusinessCodes(MasterDataTypeEnum.BANK.getCode(), Collections.singletonList("BANK001"));

        verify(zhiShuVendorClient, never()).getVendorByCode(anyString());
        verify(zhiShuVendorClient, never()).getVendorAll(anyString(), anyString());
    }

    private VenderRes buildVendor(String venderCode, String customerCode) {
        VenderRes venderRes = new VenderRes();
        venderRes.setVenderCode(venderCode);
        venderRes.setCustomerCode(customerCode);
        venderRes.setTaxpayerNumber("TAX-" + venderCode);
        venderRes.setPkCountry("CN");
        venderRes.setDescription("Vendor " + venderCode);
        venderRes.setCompanyBusinessType("C");
        venderRes.setPkCustclass("1");
        venderRes.setBlacklist("0");
        return venderRes;
    }

    private CustomerRes buildCustomer(String customerCode, String venderCode) {
        CustomerRes customerRes = new CustomerRes();
        customerRes.setCustomerCode(customerCode);
        customerRes.setVenderCode(venderCode);
        customerRes.setTaxpayerNumber("TAX-" + customerCode);
        customerRes.setPkCountry("CN");
        customerRes.setDescription("Customer " + customerCode);
        customerRes.setCompanyBusinessType("C");
        customerRes.setPkCustclass("1");
        return customerRes;
    }

    private BankRes bank(String bankCode, String bankName, String countryCode) {
        BankRes bankRes = new BankRes();
        bankRes.setBankCode(bankCode);
        bankRes.setBankName(bankName);
        bankRes.setCountryCode(countryCode);
        return bankRes;
    }

    private BankBranchRes bankBranch(String bankLocationCode, String bankLocationName) {
        BankBranchRes bankBranchRes = new BankBranchRes();
        bankBranchRes.setBankLocationCode(bankLocationCode);
        bankBranchRes.setBankLocationName(bankLocationName);
        return bankBranchRes;
    }

    private VenderAccountRes vendorAccount(String bankCode, String bankLocationCode, String accountNumber) {
        VenderAccountRes accountRes = new VenderAccountRes();
        accountRes.setBankCode(bankCode);
        accountRes.setBankLocationCode(bankLocationCode);
        accountRes.setBankAccountNumber(accountNumber);
        accountRes.setBankAccountName("Account " + accountNumber);
        return accountRes;
    }

    private CustomerAccountRes customerAccount(String bankCode, String bankLocationCode, String accountNumber) {
        CustomerAccountRes accountRes = new CustomerAccountRes();
        accountRes.setBankCode(bankCode);
        accountRes.setBankLocationCode(bankLocationCode);
        accountRes.setBankAccountNumber(accountNumber);
        accountRes.setBankAccountName("Account " + accountNumber);
        return accountRes;
    }

    private QueryAllVendorResponse queryResponse(QueryAllVendorResponse.Item... items) {
        QueryAllVendorResponse response = new QueryAllVendorResponse();
        ArrayList<QueryAllVendorResponse.Item> itemList = new ArrayList<>();
        Collections.addAll(itemList, items);
        response.setItems(itemList);
        response.setHasMore(false);
        return response;
    }

    private QueryAllVendorResponse.Item queryItem(String id, String vendorCode) {
        QueryAllVendorResponse.Item item = new QueryAllVendorResponse.Item();
        item.setId(id);
        item.setVendor(vendorCode);
        item.setAdCountry("CN");
        item.setVendorText("Old " + vendorCode);
        item.setCertificationId("OLD-TAX");
        item.setVendorType("2");
        item.setStatus(1L);
        return item;
    }

    private VendorInfoResponse vendorInfo(String id, String vendorCode) {
        VendorInfoResponse response = new VendorInfoResponse();
        response.setId(id);
        response.setVendor(vendorCode);
        response.setAdCountry("CN");
        response.setVendorText("Old " + vendorCode);
        response.setCertificationId("OLD-TAX");
        response.setCertificationType("0");
        response.setVendorType("2");
        response.setVendorNature("0");
        response.setStatus(1);
        response.setExtendInfo(new ArrayList<>());
        response.setVendorAccounts(new ArrayList<>());
        response.setVendorAddresses(new ArrayList<>());
        response.setVendorCompanyViews(new ArrayList<>());
        response.setVendorContacts(new ArrayList<>());
        return response;
    }

    private CreateVendorResponse createVendorResponse(String id) {
        CreateVendorResponse response = new CreateVendorResponse();
        response.setId(id);
        return response;
    }
}
