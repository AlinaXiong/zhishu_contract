package com.hero.middleware.service;

import com.hero.middleware.client.yuecai.request.ApprovalCallbackRequest;
import com.hero.middleware.client.yuecai.request.ProcessInstanceRequest;
import com.hero.middleware.client.yuecai.response.ProcessInstanceResponse;
import com.hero.middleware.client.zhishu.request.BaseEventRequest;
import com.hero.middleware.client.zhishu.request.ContractEventRequest;
import com.hero.middleware.dto.BatchApprovalDTO;
import com.hero.middleware.dto.BatchApprovalResultDTO;

public interface ApprovalService {

    BatchApprovalResultDTO batchApproval(BatchApprovalDTO dto);

    void callback(BaseEventRequest request);

    /**
     * 同步审批流程及审批任务
     * @param dto 审批请求对象
     * @return
     */
    void approval(ContractEventRequest request);

    /**
     * 同步审批流程及审批任务到多维表格
     * @param request 审批请求对象
     */
    void fnSyncData(ContractEventRequest request) throws Exception;

    /**
     * 回调合同审批
     * @param request
     * @return
     */
    ProcessInstanceResponse approvalCallBack(ApprovalCallbackRequest request);
}
