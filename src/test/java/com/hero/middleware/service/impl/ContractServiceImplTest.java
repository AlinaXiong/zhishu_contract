package com.hero.middleware.service.impl;

import cn.hutool.core.util.URLUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hero.middleware.client.feishu.FeiShuApiClient;
import com.hero.middleware.client.feishu.response.FeishuUserInfoResponse;
import com.hero.middleware.client.yuecai.YuecaiContractClient;
import com.hero.middleware.client.yuecai.request.ContSyncRequest;
import com.hero.middleware.client.yuecai.response.AnchorCardResponse;
import com.hero.middleware.client.yuecai.response.MasterDataRes;
import com.hero.middleware.client.yuecai.response.YuecaiResponse;
import com.hero.middleware.client.zhishu.ZhiShuVendorClient;
import com.hero.middleware.client.zhishu.ZhishuApiClient;
import com.hero.middleware.client.zhishu.ZhishuContractClient;
import com.hero.middleware.client.zhishu.request.PrecedingDocRequest;
import com.hero.middleware.client.zhishu.request.ZhishuCreateContractRequest;
import com.hero.middleware.client.zhishu.response.*;
import com.hero.middleware.config.YuecaiApiConfig;
import com.hero.middleware.config.ZhishuApiConfig;
import com.hero.middleware.dto.*;
import com.hero.middleware.entity.Contract;
import com.hero.middleware.entity.ContractSyncLog;
import com.hero.middleware.exception.BusinessException;
import com.hero.middleware.mapper.ContractMapper;
import com.hero.middleware.mapper.ContractSyncLogMapper;
import com.hero.middleware.service.ContractService;
import com.hero.middleware.service.DocumentService;
import com.hero.middleware.service.FanWeiSynService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verifyNoInteractions;

@Slf4j
@SpringBootTest
class ContractServiceImplTest {

    @Autowired
    private ZhishuContractClient zhishuContractClient;

    @Autowired
    private ZhishuApiConfig zhishuApiConfig;
    @Autowired
    private ZhiShuVendorClient zhiShuVendorClient;

    @Autowired
    private ContractService contractService;
    @Autowired
    private DocumentService documentService;
    @Autowired
    private FeiShuApiClient feiShuApiClient;
    @Autowired
    private FanWeiSynService fanWeiSynService;
    @Autowired
    private YuecaiContractClient yuecaiContractClient;

    @Test
    void testCreateContractV2() {
        System.out.println("测试zhishuContractClient是否能够正常工作...");
        System.out.println("zhishuContractClient: " + zhishuContractClient);
        System.out.println("zhishuApiConfig.baseUrl: " + zhishuApiConfig.getBaseUrl());
        System.out.println("zhishuApiConfig.appId: " + zhishuApiConfig.getAppId());

        // 创建测试请求
        ZhishuCreateContractRequest request = new ZhishuCreateContractRequest();
        request.setAmount(new BigDecimal(1000));
        request.setContractName("测试合同");
        request.setContractCategoryAbbreviation("DEFAULT");
        request.setCreateUserId("test_user");
        request.setContractStatusCode("0");
        request.setSourceId("TEST_20260313");
        request.setCurrencyCode("CNY");
        request.setStartDate("2026-03-13");
        request.setEndDate("2027-03-13");
        request.setFixedValidityCode(1);
        request.setPayTypeCode(1);
        request.setPropertyTypeCode(1);

        // 添加我方主体
        List<ZhishuCreateContractRequest.OurPartyInfo> ourPartyList = new ArrayList<>();
        ZhishuCreateContractRequest.OurPartyInfo ourParty = new ZhishuCreateContractRequest.OurPartyInfo();
        ourParty.setOurPartyCode("OUR001");
        ZhishuCreateContractRequest.SignInfoResource ourSignInfo = new ZhishuCreateContractRequest.SignInfoResource();
        ourSignInfo.setEnable(false);
        ourParty.setOurPartySignInfoResource(ourSignInfo);
        ourPartyList.add(ourParty);
        request.setOurPartyList(ourPartyList);

        // 添加对方主体
        List<ZhishuCreateContractRequest.CounterPartyInfo> counterPartyList = new ArrayList<>();
        ZhishuCreateContractRequest.CounterPartyInfo counterParty = new ZhishuCreateContractRequest.CounterPartyInfo();
        counterParty.setCounterPartyCode("COUNTER001");
        ZhishuCreateContractRequest.SignInfoResource counterSignInfo = new ZhishuCreateContractRequest.SignInfoResource();
        counterSignInfo.setEnable(false);
        counterParty.setCounterPartySignInfoResource(counterSignInfo);
        counterPartyList.add(counterParty);
        request.setCounterPartyList(counterPartyList);

        System.out.println("请求参数: " + com.alibaba.fastjson.JSON.toJSONString(request));

        try {
            System.out.println("调用createContractV2方法...");
            ZhishuCreateContractResponse response = zhishuContractClient.createContractV2(request);
            System.out.println("响应: " + com.alibaba.fastjson.JSON.toJSONString(response));
        } catch (Exception e) {
            System.out.println("异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    public void testCreateContract() {
        CreateContractDTO dto = new CreateContractDTO();
        dto.setDocumentNumber("57");
        dto.setDocumentType(3);
        dto.setCreateUserId("654bce86");
//        dto.setAmount(120.0);
//        dto.setCurrencyCode("CNY");
//        dto.setStartDate("2026-03-15");
//        dto.setEndDate("2027-03-15");
//        dto.setFixedValidityCode(1);
//        dto.setPayTypeCode(1);
//        dto.setPropertyTypeCode(1);
//        List<CreateContractDTO.OurPartyDTO> ourPartyList = new ArrayList<>();
//        CreateContractDTO.OurPartyDTO ourParty = new CreateContractDTO.OurPartyDTO();
//        ourParty.setOurPartyCode("L00100001");
//        ourPartyList.add(ourParty);
//        dto.setOurPartyList(ourPartyList);
//
//        List<CreateContractDTO.CounterPartyDTO> counterPartyList = new ArrayList<>();
//        CreateContractDTO.CounterPartyDTO counterParty = new CreateContractDTO.CounterPartyDTO();
//        counterParty.setCounterPartyCode("V00100001");
//        counterPartyList.add(counterParty);
//        dto.setCounterPartyList(counterPartyList);

        CreateContractResultDTO contract = contractService.createContract(dto);
        log.info(JSONObject.toJSONString(contract));

    }

    @Test
    public void testGetContract() {
        Map<String, Object> params = new HashMap<>();
        params.put("user_id_type", "user_id");
        ContractResponse contract = zhishuContractClient.getContract("1111444485046272329", params);
        System.out.println(JSONObject.toJSONString(contract));
    }

    @Test
    public void testGetAnchorCardInfo() {
//        PrecedingDocRequest request = new PrecedingDocRequest();
//        ResultResponse anchorCardInfo = documentService.getAnchorCardInfo(request);
//        log.info(JSONObject.toJSONString(anchorCardInfo));
        Map<String,Object> documentParams = new HashMap<>();
        documentParams.put("page", 0);
        documentParams.put("size", 1);
        MasterDataRes masterDataRes = yuecaiContractClient.getAnchorCard(documentParams, "30", "id");
        List<Object> content = masterDataRes.getContent();
        AnchorCardResponse documentData = JSONObject.parseObject(content.get(0).toString(), AnchorCardResponse.class);
        System.out.println(JSONObject.toJSONString(documentData));
    }

    @Test
    public void testGetProcurementInfo() {
        PrecedingDocRequest request = new PrecedingDocRequest();
        ResultResponse anchorCardInfo = documentService.getProcurementInfo(request);
        log.info(JSONObject.toJSONString(anchorCardInfo));
    }

    @Test
    public void testGetOrderDetail() {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("contractId", "1128259238062195052");
        List<Map<String,String>> mapList = new ArrayList<>();
        Map<String, String> map = new HashMap<>();
        mapList.add(map);
        map.put("serialId","BC-2024-0018-001");
        paramMap.put("orderInfoList", mapList);
//        String jsonString = JSONObject.toJSONString(paramMap);
//        System.out.println(jsonString);
//        JSONArray objects = JSONObject.parseArray(JSONObject.parseObject(jsonString).getString("orderInfoList"));
//        System.out.println(objects.getJSONObject(0).getString("serialId"));
        Map<String, Object> orderDetail = documentService.getOrderDetail(paramMap);
        System.out.println(JSONObject.toJSON(orderDetail));
    }

    @Test
    public void testCalculateAmount() {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("contractId", "1128515386208158060");
        Map<String, Object> info = contractService.calculateAmount(paramMap);
        System.out.println(JSONObject.toJSON(info));
    }

    @Test
    public void testCheckAmount() {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("contractId", "1151504095454953836");
        paramMap.put("acceptedFlag", "是");
//        Map map = JSONObject.parseObject(" {\"USER_ID_TYPE\":\"user_id\",\"belongedDepartmentId\":\"1120429029711676489\",\"submitterUserId\":\"149744414\",\"custom_1012_7e2c970e63f648268eaefbd13d6bfc8f\":{\"amount\":\"0\",\"currency\":1,\"enName\":\"CNY\",\"currencyName\":\"CNY-人民币元\"},\"sumDeposit\":\"{\\\"amount\\\":\\\"0\\\",\\\"currency\\\":1,\\\"enName\\\":\\\"CNY\\\",\\\"currencyName\\\":\\\"CNY-人民币元\\\"}\",\"contractId\":\"1128595220523385161\",\"contractNumber\":\"H-OS202604300007\",\"submitterEmployeeCode\":\"V81603\"}", Map.class);

        ContractCheckResultDTO checkResultDTO = contractService.submitCheck(paramMap);
        System.out.println(JSONObject.toJSON(checkResultDTO));
    }

    @Test
    public void syncContractFromZhishu() {
        ContractSyncDTO dto = new ContractSyncDTO();
//        dto.setContractId("1131928701680746825");//收入
//        dto.setContractId("1131935884480872777");//支出
//        dto.setContractId("1131936738382446921");//主播

        dto.setContractId("1144273488194830700");

        contractService.syncContractFromZhishu(dto);
    }

    @Test
    public void getSMByType() {
        try {
            log.error("测试税目税率::{}", contractService.getTaxData(202605060009L,""));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void createAntiBriberyContractWithBlankVendorReturnsFail() {

        VendorInfoResponse vendorV2 = zhiShuVendorClient.getVendorV2("1131565811895370569");
        QueryAllVendorResponse.Item item = JSONObject.parseObject(JSONObject.toJSONString(vendorV2), QueryAllVendorResponse.Item.class);

        CreateAntiBriberyContractResultDTO result = contractService.createAntiBriberyContract(item);

    }

    @Test
    public void testGetUserInfo() throws ParseException {
//        FeishuUserInfoResponse userInfo = feiShuApiClient.getUserInfo("155051733");
//        System.out.println(userInfo.getUser().getStatus().getResigned());
//        String response = "{\"msg\":\"success\",\"code\":0,\"data\":{\"adCountry\":\"CN\",\"vendorType\":\"2\",\"updatedBy\":\"程林枫\",\"certificationType\":\"0\",\"certificationId\":\"91310106MA1FY9LT3N\",\"updatedAppId\":\"web\",\"createdAppId\":\"1868\",\"isRisked\":false,\"vendorContacts\":[],\"extendInfo\":[{\"fieldCode\":\"VBI00100001\",\"fieldType\":3,\"fieldValue\":\"是\"},{\"fieldCode\":\"VBI00102002\",\"fieldType\":3,\"fieldValue\":\"是\"}],\"vendorAccounts\":[{\"country\":\"CN\",\"accountName\":\"上海临冠数据科技有限公司\",\"bankName\":\"中国建设银行\",\"id\":\"2053678177832747010\",\"extendInfo\":[],\"account\":\"31050175360000002034\",\"primaryAccount\":false}],\"ownerDepts\":[],\"createdSource\":\"Open API\",\"vendorCompanyViews\":[],\"createdBy\":\"黄劭文\",\"vendor\":\"V-C-CN-OT-IPC-0593\",\"updatedSource\":\"web\",\"vendorText\":\"上海临冠数据科技有限公司\",\"vendorNature\":\"0\",\"id\":\"1131565811895370569\",\"status\":1,\"vendorAddresses\":[]}}";
//        JSONObject resultRes = JSONObject.parseObject(response);
//        log.info("获取交易方详情信息信息-返回信息：{}", response);
//        VendorInfoResponse vendorInfoResponse = parseResponse(resultRes.getString("data"), VendorInfoResponse.class);
//        System.out.println(JSONObject.toJSON(vendorInfoResponse));
        String str = "2026-05-13 16:52:07";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        System.out.println(sdf.format(sdf.parse(str)));
    }

    private <T> T parseResponse(String response, Class<T> clazz) {
        return com.alibaba.fastjson.JSON.parseObject(response, clazz);
    }
}
