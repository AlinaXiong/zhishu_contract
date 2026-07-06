package com.hero.middleware.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hero.middleware.client.yuecai.YuecaiContractClient;
import com.hero.middleware.client.yuecai.response.masterdata.CustomerAccountRes;
import com.hero.middleware.client.yuecai.response.masterdata.ExchangeRateRes;
import com.hero.middleware.client.zhishu.ZhiShuVendorClient;
import com.hero.middleware.config.YeCaiDataConfig;
import com.hero.middleware.enums.MasterDataTypeEnum;
import com.hero.middleware.service.YeCaiToZhiShuService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@SpringBootTest
public class YeCaiToZhiShuServiceImplTest {
    @Autowired
    private YeCaiToZhiShuService yeCaiToZhiShuService;

    @Test
    public void testSynMasterData(){
//        yeCaiToZhiShuService.synMasterData("CUSTOMER",null,null,null,0,100);//客户
//        yeCaiToZhiShuService.synMasterData("CUSTOMER");//客户
//        yeCaiToZhiShuService.synMasterData("VENDER");//供应商
//        yeCaiToZhiShuService.synMasterData("CUSTOMER","6148105875");//客户
//        yeCaiToZhiShuService.synMasterData("VENDER","130103199712251559");//供应商
//        yeCaiToZhiShuService.synMasterDataByVendorCode("CUSTOMER","408-82-13442",null,null,null,null);
//        List<String> list = new ArrayList<>();
//        list.add("C-C-OS-0052");
//        list.add("C-I-OS-0043");
//        list.add("V-C-CN-SP-APP-0061");
//        list.add("V-I-CN-EC-LRO-0906");
//        list.add("V-C-CN-OT-IPC-0288");
//        yeCaiToZhiShuService.synMasterDataByBusinessCodes("CUSTOMER",list);

        String startTime = "2026-07-03 00:00:00";
        String endTime = "2026-07-05 00:00:00";
        log.info("供应商一小时增量同步开始，查询起始时间：{}，查询终止时间：{}", startTime, endTime);
        yeCaiToZhiShuService.synMasterDataByVendorCode(
                MasterDataTypeEnum.CUSTOMER.getCode(), null, startTime, endTime, null, null);
        log.info("供应商一小时增量同步结束，查询起始时间：{}，查询终止时间：{}", startTime, endTime);
    }

    @Test
    public void testSynVENDERMasterData(){
//        yeCaiToZhiShuService.synMasterData("CUSTOMER",null,null,null,0,100);//客户
//        yeCaiToZhiShuService.synMasterData("CUSTOMER");//客户
//        yeCaiToZhiShuService.synMasterData("VENDER");//供应商
//        yeCaiToZhiShuService.synMasterData("CUSTOMER","6148105875");//客户
//        yeCaiToZhiShuService.synMasterData("VENDER","130103199712251559");//供应商
//        yeCaiToZhiShuService.synMasterDataByVendorCode("VENDER",null,null,null,null,null);
//        List<String> list = new ArrayList<>();
//        for (String code : getList()) {
//            if(code.contains(";")){
//                code = code.split(";")[0];
//            }
//            list.add(code);
//        }
        List<String> list = new ArrayList<>();
        list.add("VC-CN-SP-PPC-0004");
//        list.add("V-C-CN-SP-TPS-0012");
//        list.add("V-C-OS-SP-TPS-0156");
//        list.add("V-C-CN-MC-ADV-0014");
//        list.add("V-C-CN-OT-IPC-0288");
//        System.out.println(JSONObject.toJSON(list));
        yeCaiToZhiShuService.synMasterDataByBusinessCodes("VENDER",list);
//        String startTime = "2026-07-02 00:00:00";
//        String endTime = "2026-07-03 00:00:00";
//        log.info("供应商一小时增量同步开始，查询起始时间：{}，查询终止时间：{}", startTime, endTime);
//        yeCaiToZhiShuService.synMasterDataByVendorCode(
//                MasterDataTypeEnum.VENDER.getCode(), null, startTime, endTime, null, null);
//        log.info("供应商一小时增量同步结束，查询起始时间：{}，查询终止时间：{}", startTime, endTime);
    }

    @Test
    public void testGetAllMasterData(){
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
//        List<ExchangeRateRes> allExchangeRateList = (List<ExchangeRateRes>)yeCaiToZhiShuService.getAllMasterData("EXCHANGE_RATE");
////        for (ExchangeRateRes exchangeRateRes : allExchangeRateList) {
////            System.out.println(sdf.format(exchangeRateRes.getRateDate()));
////        }
//        System.out.println(allExchangeRateList.size());
        String dimension2Code = "H-P202600001";
        String substr = dimension2Code.substring(0, 5);
        System.out.println(substr);
    }

    @Test
    public void testGetAllAccountData(){
//        List<VenderAccountRes> list = (List<VenderAccountRes>)yeCaiToZhiShuService.getAllAccountData("VENDER_ACCOUNT","V-C-CN-OT-IPC-0058");
//        for (VenderAccountRes item : list) {
//            System.out.println(item);
//        }
        List<CustomerAccountRes> list1 = (List<CustomerAccountRes>)yeCaiToZhiShuService.getAllAccountData("CUSTOMER_ACCOUNT","XN001");
        for (CustomerAccountRes item : list1) {
            System.out.println(JSON.toJSONString(item));
        }
    }

    private List<String> getList(){
        List<String> list = new ArrayList<>();
        list.add("V-I-CN-OT-IPC-0001");
        list.add("V-I-CN-OT-OTH-0050");
        list.add("V-I-CN-OT-OTH-0062");
        list.add("V-I-CN-OT-OTH-0400");
        list.add("V-I-CN-OT-OTH-0830");
        list.add("V-I-CN-OT-OTH-0878");
        list.add("V-I-CN-OT-OTH-1471");
        list.add("V-I-CN-OT-OTH-1490");
        list.add("V-I-CN-OT-OTH-1673");
        list.add("V-I-CN-OT-OTH-1686");
        list.add("V-I-CN-OT-OTH-1689");
        list.add("V-I-CN-OT-OTH-1693");
        list.add("V-I-CN-OT-OTH-1934");
        list.add("V-I-CN-OT-OTH-1971");
        list.add("V-I-OS-OT-OTH-0120");
        list.add("V-I-CN-OT-OTH-2122");
        list.add("V-I-CN-OT-OTH-2600");
        list.add("V-I-CN-OT-OTH-2828");
        list.add("V-I-OS-OT-OTH-0192");
        list.add("V-I-CN-OT-OTH-2888");
        list.add("V-I-CN-OT-OTH-2933");
        list.add("V-I-CN-SP-TPS-0054");
        list.add("V-I-CN-OT-OTH-3047");
        list.add("V-C-OS-SP-TPS-0238");
        list.add("V-I-OS-OT-OTH-0395");
        list.add("V-I-CN-EC-LRO-0363");
        list.add("V-I-CN-EC-LRO-0843");
        list.add("V-I-OS-SP-TPS-0435");
        list.add("V-C-OS-SP-TAL-0091");
        list.add("V-I-CN-OT-OTH-2194");
        list.add("V-C-OS-OT-OTH-0770");
        list.add("V-I-OS-SP-TAL-0104");
        list.add("V-I-CN-OT-OTH-4254");
        return list;
    }
}
