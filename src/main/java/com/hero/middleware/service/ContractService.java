package com.hero.middleware.service;

import com.alibaba.fastjson.JSONObject;
import com.hero.middleware.client.zhishu.request.PrecedingDocRequest;
import com.hero.middleware.client.zhishu.response.ContractQueryResponse;
import com.hero.middleware.client.zhishu.response.QueryAllVendorResponse;
import com.hero.middleware.client.zhishu.response.ResultResponse;
import com.hero.middleware.common.Result;
import com.hero.middleware.dto.ContractCheckResultDTO;
import com.hero.middleware.dto.CreateContractDTO;
import com.hero.middleware.dto.CreateContractResultDTO;
import com.hero.middleware.dto.ContractSyncDTO;
import com.hero.middleware.dto.CreateAntiBriberyContractResultDTO;
import com.hero.middleware.dto.bill.PrecedingReceiptsDTO;

import java.util.Map;
import java.util.Set;

public interface ContractService {

    CreateContractResultDTO createContract(CreateContractDTO dto);

    CreateAntiBriberyContractResultDTO createAntiBriberyContract(QueryAllVendorResponse.Item item);

    ResultResponse updateCounterPartyAntiBriberySigned(ContractQueryResponse contractQueryInfo);

    void syncContractFromZhishu(ContractSyncDTO dto);

    /**
     * 同步智书合同，并返回面向调用方的处理说明。
     */
    String syncContractFromZhishuWithRemark(ContractSyncDTO dto);

    void updateContractFromZhishu(ContractSyncDTO dto);

    void updateContractFromYuecai(ContractSyncDTO dto);

    Result<PrecedingReceiptsDTO> receiptsList(ContractSyncDTO dto);

    /**
     * 获取合同表单数据信息
     * @param contractQueryInfo
     * @return
     */
    Map<String,Object> getContractFormData(ContractQueryResponse contractQueryInfo);

    /**
     * 根据履约计划中类型分别计算金额总值并返回
     * @param paramMap
     * @return
     */
    Map<String,Object> calculateAmount(Map<String,Object> paramMap);

    /**
     * 根据专项分类获确认是否需要验收
     * @param paramMap
     * @return
     */
    Map<String,Object> specCategoryMapping(Map<String,Object> paramMap);
    Map<String,Object> specCategoryMapping(String specialCategory);

    /**
     * 提交合同金额校验
     * @param paramMap
     * @return
     */
    ContractCheckResultDTO submitCheck(Map<String,Object> paramMap);
    ContractCheckResultDTO submitAnchorCardCheck(Map<String,Object> paramMap);

    ContractCheckResultDTO checkOppositeSide(Map<String, Object> paramMap);

    Map<String,Object> getTaxData(Map<String,Object> paramMap)throws Exception;
    Map<String, Set<Object>> getTaxData(Long type, String itemName)throws Exception;

    /**
     * 获取合同信息
     * @param contractId 合同主键
     * @return
     */
    ContractQueryResponse getContractInfo(String contractId);

    /**
     * 保存日志信息
     * @param contractId
     * @param syncType
     * @param syncDirection
     * @param syncStatus
     * @param requestParam
     * @param responseData
     * @param errorMessage
     */
    void saveSyncLog(String contractId, String syncType, String syncDirection,
                            String syncStatus, String requestParam, String responseData, String errorMessage);
}
