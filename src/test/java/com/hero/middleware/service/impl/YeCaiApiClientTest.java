package com.hero.middleware.service.impl;

import cn.hutool.core.util.URLUtil;
import cn.hutool.json.JSONObjectIter;
import com.alibaba.fastjson.JSONObject;
import com.hero.middleware.client.yuecai.YuecaiApiClient;
import com.hero.middleware.client.yuecai.YuecaiContractClient;
import com.hero.middleware.client.yuecai.request.ContSyncLineRequest;
import com.hero.middleware.client.yuecai.request.ContSyncRequest;
import com.hero.middleware.client.yuecai.response.MasterDataRes;
import com.hero.middleware.client.yuecai.response.YuecaiResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@SpringBootTest
public class YeCaiApiClientTest {
    @Autowired
    private YuecaiApiClient yuecaiApiClient;
    @Autowired
    private YuecaiContractClient yuecaiContractClient;

    @Test
    public void testGetToken(){
        String accessToken = yuecaiApiClient.getAccessToken();
        System.out.println(accessToken);
    }

    @Test
    public void testGetMasterData() {
        Map<String,Object> params = new HashMap<>();
        params.put("page", 0);
        params.put("size", 20);
        params.put("dataType", "ORDER");
//        params.put("prjDimOrderValue", "S1-2024-0004-001");
        params.put("startTime", URLUtil.encode("2024-01-01 00:00:00"));
        MasterDataRes masterData = yuecaiContractClient.getMasterData(params);
        log.info(JSONObject.toJSONString(masterData));
    }

    @Test
    public void testSyncContract(){
        ContSyncRequest request = new ContSyncRequest();
        request.setContractId("CONTRACT_2024001");
        request.setContractType("1L");
        request.setContractNumber("HT-2024-001");
        request.setMagOrgCode("1000");
        request.setCompanyCode("HeroEsports");
        request.setUnitCode("UNIT001");
        request.setPositionCode("POS001");
        request.setEmployeeCode("EMP001");
        request.setEntityCodes("0103-0001");
        request.setResponsibilityCenterCode("RC001");
        request.setCurrencyCode("CNY");
        request.setExchangeRate(new BigDecimal("1.0"));
        request.setStatus("有效");
        request.setCreatedBy(1001L);
        request.setCreationDate(new Date());
        request.setLastUpdatedBy(1001L);
        request.setLastUpdateDate(new Date());
        request.setTenantId(10086L);
        request.setAttachmentClass(1);
        List<ContSyncLineRequest> lineRequests = new ArrayList<>();
        ContSyncLineRequest lineRequest = new ContSyncLineRequest();
        lineRequest.setLineNumber(1L);
        lineRequest.setCurrencyCode("CNY");
        lineRequest.setPaymentRatioFlag(false);
        lineRequest.setCreatedBy(1001L);
        lineRequest.setCreationDate(new Date());
        lineRequest.setLastUpdatedBy(1001L);
        lineRequest.setLastUpdateDate(new Date());
        lineRequest.setTenantId(10086L);
        lineRequests.add(lineRequest);
        request.setContSyncLines(lineRequests);
        YuecaiResponse yuecaiResponse = yuecaiContractClient.syncContract(request);
        System.out.println(JSONObject.toJSON(yuecaiResponse));
    }
}
