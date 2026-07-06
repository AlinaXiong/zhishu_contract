package com.hero.middleware.service.impl;

import com.alibaba.fastjson.JSON;
import com.hero.middleware.client.yuecai.YuecaiContractClient;
import com.hero.middleware.client.yuecai.request.SyncContractStatusRequest;
import com.hero.middleware.client.yuecai.response.YuecaiResponse;
import com.hero.middleware.dto.ContractStatusDTO;
import com.hero.middleware.entity.ContractSyncLog;
import com.hero.middleware.exception.BusinessException;
import com.hero.middleware.mapper.ContractSyncLogMapper;
import com.hero.middleware.service.ContractSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
public class ContractSyncServiceImpl implements ContractSyncService {

    @Autowired
    private YuecaiContractClient yuecaiContractClient;

    @Autowired
    private ContractSyncLogMapper contractSyncLogMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncContractStatus(ContractStatusDTO dto) {
        log.info("同步合同状态至业财系统: {}", JSON.toJSONString(dto));
        
        SyncContractStatusRequest request = new SyncContractStatusRequest();
        request.setContractId(dto.getContractId());
        request.setContractStatus(dto.getContractStatus());
        request.setStatusDesc(dto.getStatusDesc());
        
        YuecaiResponse response = yuecaiContractClient.syncContractStatus(request);
        
        if (response == null || !Boolean.TRUE.equals(response.getSuccess())) {
            saveSyncLog(dto.getContractId(), "STATUS_SYNC", "ZHISHU_TO_YUECAI", "FAIL",
                    JSON.toJSONString(dto), null, response != null ? response.getMessage() : "业财API响应为空");
            throw new BusinessException("合同状态同步失败: " + (response != null ? response.getMessage() : "响应为空"));
        }
        
        saveSyncLog(dto.getContractId(), "STATUS_SYNC", "ZHISHU_TO_YUECAI", "SUCCESS",
                JSON.toJSONString(dto), JSON.toJSONString(response), null);
        
        log.info("合同状态同步成功");
    }

    private void saveSyncLog(String contractId, String syncType, String syncDirection,
                            String syncStatus, String requestParam, String responseData, String errorMessage) {
        ContractSyncLog syncLog = new ContractSyncLog();
        syncLog.setContractId(contractId);
        syncLog.setSyncType(syncType);
        syncLog.setSyncDirection(syncDirection);
        syncLog.setSyncStatus(syncStatus);
        syncLog.setRequestParam(requestParam);
        syncLog.setResponseData(responseData);
        syncLog.setErrorMessage(errorMessage);
        syncLog.setCreateTime(LocalDateTime.now());
        contractSyncLogMapper.insert(syncLog);
    }

}
