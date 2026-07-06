package com.hero.middleware.service.impl;

import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hero.middleware.client.feishu.FeishuBitableClient;
import com.hero.middleware.client.yuecai.YuecaiContractClient;
import com.hero.middleware.client.yuecai.request.*;
import com.hero.middleware.client.yuecai.response.ProcessInstanceResponse;
import com.hero.middleware.client.zhishu.ZhishuContractClient;
import com.hero.middleware.client.zhishu.request.ApprovalContractRequest;
import com.hero.middleware.client.zhishu.request.BaseEventRequest;
import com.hero.middleware.client.zhishu.request.ContractEventRequest;
import com.hero.middleware.client.zhishu.request.EventHeaderRequest;
import com.hero.middleware.client.zhishu.response.ApprovalQueryResponse;
import com.hero.middleware.client.zhishu.response.ApprovalResponse;
import com.hero.middleware.client.zhishu.response.ContractQueryResponse;
import com.hero.middleware.client.zhishu.response.ContractResponse;
import com.hero.middleware.config.FeiShuApprovalConfig;
import com.hero.middleware.config.FeiShuBitableConfig;
import com.hero.middleware.config.ZhishuBaseConfig;
import com.hero.middleware.dto.BatchApprovalDTO;
import com.hero.middleware.dto.BatchApprovalResultDTO;
import com.hero.middleware.entity.ApprovalRecord;
import com.hero.middleware.entity.SyncFlow;
import com.hero.middleware.enums.ApprovalEnum;
import com.hero.middleware.enums.ContractCallbackTypeEnum;
import com.hero.middleware.enums.ContractStageEnum;
import com.hero.middleware.mapper.ApprovalRecordMapper;
import com.hero.middleware.mapper.SyncFlowMapper;
import com.hero.middleware.service.ApprovalService;
import com.hero.middleware.service.ContractService;
import com.hero.middleware.utils.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ApprovalServiceImpl implements ApprovalService {

    @Autowired
    private ZhishuContractClient zhishuContractClient;

    @Autowired
    private ApprovalRecordMapper approvalRecordMapper;

    @Autowired
    private YuecaiContractClient yuecaiContractClient;

    @Autowired
    private SyncFlowMapper syncFlowMapper;

    @Resource
    private ZhishuBaseConfig zhishuBaseConfig;

    @Resource(name = "approvalTaskExecutor")
    private ThreadPoolTaskExecutor executor;

    private final static Integer[] approvalStatus = {5, 6, 7, 9, 10, 11, 14, 15};
    private final static String approvalCallBackUrl = "/api/approval/approvalCallBack";
    @Autowired
    private ContractService contractService;

    @Autowired
    private FeishuBitableClient feishuBitableClient;

    @Autowired
    private FeiShuBitableConfig feiShuBitableConfig;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchApprovalResultDTO batchApproval(BatchApprovalDTO dto) {
        log.info("批量审批开始: {}", JSON.toJSONString(dto));
        BatchApprovalResultDTO result = new BatchApprovalResultDTO();
        BatchApprovalResultDTO.ApprovalItem resultItem = null;
        List<BatchApprovalResultDTO.ApprovalItem> successItems = new ArrayList<>();
        List<BatchApprovalResultDTO.ApprovalItem> failItems = new ArrayList<>();
        List<String> contractIds = dto.getContractIds();
        for (String contractId : contractIds) {
            String processInstanceId = null;
            ApprovalContractRequest request = new ApprovalContractRequest();
            ApprovalRecord approvalRecord = approvalRecordMapper.selectOne(new QueryWrapper<ApprovalRecord>().eq("contract_id", contractId));
            if(approvalRecord==null){
                log.info("t_approval_record表中未查询到审批信息：{}",contractId);
                Map<String, Object> params = new HashMap<>();
                params.put("user_id_type", "user_id");
                ContractResponse contractInfo = zhishuContractClient.getContract(contractId,params);
                if(contractInfo==null){
                    resultItem = new BatchApprovalResultDTO.ApprovalItem();
                    resultItem.setContractId(contractId);
                    resultItem.setContractName("");
                    resultItem.setSuccess(false);
                    resultItem.setMessage("未查询到审批信息："+contractId);
                    resultItem.setNextApprover("");
                    failItems.add(resultItem);
                    continue;
                }else{
                    Map<String, Object> data = contractInfo.getData();
                    ContractQueryResponse contract = JSONObject.parseObject(String.valueOf(data.get("contract")), ContractQueryResponse.class);
                    processInstanceId = contract.getProcessInstanceId();
                }
            }else{
                processInstanceId = approvalRecord.getApprovalId();//审批id
            }

            String approval = dto.getApproval();//审批人
            Map<String, Object> params = new HashMap<>();
            params.put("user_id_type", "user_id");
            ApprovalQueryResponse approvalContractQuery = zhishuContractClient.getApprovalContract(processInstanceId, params);
            String taskInstanceId = null;//审批节点id
            List<String> assigneeIds = new ArrayList<>();//节点审批人员id
            if(approvalContractQuery.getCode()==0){
                ApprovalQueryResponse.DataBean approvalContract = approvalContractQuery.getData();
                List<ApprovalQueryResponse.TaskInstance> taskInstanceList = approvalContract.getProcessInstance().getTaskInstanceList();
                for (ApprovalQueryResponse.TaskInstance taskInstance : taskInstanceList) {
                    if(taskInstance.getEndTime().isEmpty()){
                        taskInstanceId = taskInstance.getTaskInstanceId();
                        assigneeIds = taskInstance.getAssigneeIds();
                    }
                }
                if(!assigneeIds.contains(approval)){
                    log.info("审批人与当前审批单中人员不一致:assigneeIds = {} approval = {}", assigneeIds, approval);
                    resultItem = new BatchApprovalResultDTO.ApprovalItem();
                    resultItem.setContractId(contractId);
                    resultItem.setContractName("");
                    resultItem.setSuccess(false);
                    resultItem.setMessage("审批失败：审批人与当前审批单中人员不一致:assigneeIds = "+assigneeIds+" approval = "+approval);
                    resultItem.setNextApprover("");
                    failItems.add(resultItem);
                    continue;
                }
            }else{
                log.info("未查询到合同审批信息:processInstanceId = {}", processInstanceId);
                resultItem = new BatchApprovalResultDTO.ApprovalItem();
                resultItem.setContractId(contractId);
                resultItem.setContractName("");
                resultItem.setSuccess(false);
                resultItem.setMessage("审批失败：未查询到合同审批信息:processInstanceId = "+processInstanceId);
                resultItem.setNextApprover("");
                failItems.add(resultItem);
                continue;
            }

            request.setTaskInstanceId(taskInstanceId);
            request.setAssigneeId(approval);
            request.setCommandType(ApprovalEnum.getCommandTypeByCode(dto.getApprovalStatus()));
            request.setTaskComment(dto.getApprovalOpinion()==null?ApprovalEnum.getDescriptionByCode(dto.getApprovalStatus()):dto.getApprovalOpinion());
            ApprovalResponse response = zhishuContractClient.approvalContract(request,processInstanceId);

            if (response != null && response.getCode() == 0) {
                ApprovalResponse.ProcessInstance processInstance = response.getData();

                resultItem = new BatchApprovalResultDTO.ApprovalItem();
                resultItem.setContractId(contractId);
                resultItem.setContractName(processInstance.getProcessName() != null ? processInstance.getProcessName().getZh() : "");
                resultItem.setSuccess(true);
                resultItem.setMessage("审批成功");

                String nextApprover = "";
                if (processInstance.getTaskInstanceList() != null && !processInstance.getTaskInstanceList().isEmpty()) {
                    //TODO 审批后返回对象中任务列表是否是最新的列表，待确认，如果是最新的应该会有新的对象在集合中，且endTime字段为空，否则就重新查询合同审批详情信息
                    List<ApprovalResponse.TaskInstance> taskInstanceList = processInstance.getTaskInstanceList();
                    for (ApprovalResponse.TaskInstance taskInstance : taskInstanceList) {
                        if("".equals(taskInstance.getEndTime())){//结束日期为空的时候为当前审批节点
                            nextApprover = taskInstance.getAssigneeIds().get(taskInstance.getAssigneeIds().size() - 1);
                        }
                    }
                }
                resultItem.setNextApprover(nextApprover);

                successItems.add(resultItem);
                saveApprovalRecord(taskInstanceId, contractId, dto.getApproval(),
                        dto.getApprovalStatus(), dto.getApprovalOpinion(),
                        nextApprover);
            } else {
                resultItem = new BatchApprovalResultDTO.ApprovalItem();
                resultItem.setContractId(contractId);
                resultItem.setContractName("");
                resultItem.setSuccess(false);
                resultItem.setMessage(response != null ? response.getMsg() : "审批失败");
                resultItem.setNextApprover("");
                failItems.add(resultItem);
            }
        }

        result.setSuccessItems(successItems);
        result.setFailItems(failItems);
        result.setSuccessCount(successItems.size());
        result.setFailCount(failItems.size());
        result.setMessage(buildResultMessage(successItems, failItems, dto.getApprovalStatus()));

        log.info("批量审批完成: {}", result.getMessage());
        return result;
    }

    @Override
    public void callback(BaseEventRequest request){
        EventHeaderRequest header = request.getHeader();
        if (header == null) {
            throw new RuntimeException("回调header参数为空");
        }

        ContractCallbackTypeEnum eventTypeEnum = ContractCallbackTypeEnum.getEnum(header.getEventType());
        if (eventTypeEnum == null) {
            log.info("未知回调类型：{}", header.getEventType());
            return;
        }

        switch (eventTypeEnum) {
            //合同状态变更事件
            case STATUS_CHANGE:
                ContractEventRequest event = JSONUtil.toBean(request.getEvent().toString(), ContractEventRequest.class);
                //异步调用contractChange
                executor.execute(() -> {
                    try {
                        this.approval(event);
                        fnSyncData(event);
                    } catch (Exception e) {
                        log.error("合同状态变更回调异步处理异常，event={}", JSON.toJSONString(event), e);
                    }
                });
                break;
            //合同信息变更
            case INFO_CHANGE:
                //TODO 合同信息变更处理
            default:
                log.info("其他回调类型，不处理");
                break;
        }
    }

    /**
     * 同步审批信息到多维表格
     * @param event 审批信息
     */
    public void fnSyncData(ContractEventRequest event) throws Exception {
        // 审批不通过，不同步归档信息，日志留痕
        if (!FeiShuApprovalConfig.SUCCESS_STATE.getTableName().equals(event.getContractStageCode().toString())) {
            log.error("同步审批信息到多维表格,审批失败信息记录,合同编号：{}", event.getContractId());
            return;
        }

        // 通过FeiShuApprovalConfig配置节点对应表单字段，维护归档时间
        if(event.getExtraInfo()==null){
            log.error("event中extraInfo为空：{}", JSONObject.toJSON(event));
            return;
        }

        if (FeiShuApprovalConfig.START_NODE.getNodeId().equals(event.getExtraInfo().getNodeId())) {
            log.info("创建合同归档信息到多维表格");
            ContractQueryResponse res = contractService.getContractInfo(event.getContractId());
            JSONObject jsonObject = new JSONObject()
                    .fluentPut("合同id", res.getContractId().toString())
                    .fluentPut("合同申请人", res.getCreateUserName())
                    .fluentPut("合同名称", res.getContractName())
                    .fluentPut("合同编号", res.getContractNumber())
                    .fluentPut(FeiShuApprovalConfig.START_NODE.getTableName(), System.currentTimeMillis());
            feishuBitableClient.createAppTableRecordSample(jsonObject, feiShuBitableConfig.getArchiveTableId());
        } else if (FeiShuApprovalConfig.NODE_1.getNodeId().contains(event.getExtraInfo().getNodeId())
                || FeiShuApprovalConfig.NODE_2.getNodeId().contains(event.getExtraInfo().getNodeId())
        ) {
            log.info("更新合同归档信息到多维表格");
            //1.查询归档信息条目
            JSONObject jsonObject = new JSONObject()
                    .fluentPut("conjunction", "and")
                    .fluentPut("conditions", new JSONArray().fluentAdd(new JSONObject()
                            .fluentPut("fieldName", "合同id")
                            .fluentPut("operator", "is")
                            .fluentPut("value", new JSONArray().fluentAdd(event.getContractId()))
                    ));
            String recordId = feishuBitableClient.searchRecordId(jsonObject, feiShuBitableConfig.getArchiveTableId());
            //2.更新多维信息条目
            JSONObject updateObject = new JSONObject()
                    .fluentPut(FeiShuApprovalConfig.getTableName(event.getExtraInfo().getNodeId()), System.currentTimeMillis());
            feishuBitableClient.updateAppTableRecordSample(updateObject, feiShuBitableConfig.getArchiveTableId(), recordId);
        }
    }

    @Override
    public void approval(ContractEventRequest request) {
        Integer businessTypeCode = request.getBusinessTypeCode();//合同业务类型编码
        Integer contractStageCode = request.getContractStageCode();//合同阶段编码
        String contractId = request.getContractId();//合同id
        if (0!=businessTypeCode) {
            log.info("不是合同申请业务，不处理。业务类型：{}", businessTypeCode);
            return;
        }
        ContractStageEnum contractStageEnum = ContractStageEnum.getEnum(contractStageCode);
        if (contractStageEnum == null) {
            log.info("未知合同阶段编码。合同阶段编码：{}", contractStageCode);
            return;
        }

        if(Arrays.stream(approvalStatus).anyMatch(stageCode -> stageCode != null && stageCode.equals(contractStageCode))){//审批发起时
            Map<String, Object> paramsContract = new HashMap<>();
            paramsContract.put("user_id_type", "user_id");
            ContractResponse contractInfo = zhishuContractClient.getContract(contractId, paramsContract);
            String processInstanceId = null;
            ContractQueryResponse contract = null;
            if(contractInfo!=null){
                Map<String, Object> data = contractInfo.getData();
                contract = JSONObject.parseObject(String.valueOf(data.get("contract")), ContractQueryResponse.class);
                processInstanceId = contract.getProcessInstanceId();
            }else{
                log.info("id = {}未查询到合同信息", contractId);
                return;
            }
            Map<String, Object> paramsApproval = new HashMap<>();
            paramsApproval.put("user_id_type", "user_id");
            ApprovalQueryResponse approvalContractQuery = zhishuContractClient.getApprovalContract(processInstanceId, paramsApproval);
            ApprovalQueryResponse.ProcessInstance processInstance = null;
            if(approvalContractQuery.getCode()==0){
                ApprovalQueryResponse.DataBean approvalContract = approvalContractQuery.getData();
                processInstance = approvalContract.getProcessInstance();
            }else{
                log.info("未查询到合同审批信息:processInstanceId = {}", processInstanceId);
                return;
            }
            if(processInstance != null){
                SyncFlow syncFlowOne = syncFlowMapper.selectOne(new QueryWrapper<SyncFlow>()
                        .eq("flow_code", processInstanceId));
                //组装任务信息
                ProcessInstanceRequest processInstanceRequest = buildProcessInstanceResponse(contract, processInstance,request);
                if(syncFlowOne==null){//如果不存在则调用同步接口新增
                    //组装流程信息
                    ProcessDefinitionRequest processDefinitionRequest = buildProcessDefinitionRequest(contract, processInstance);

                    String resultFlow = yuecaiContractClient.syncFlow(processDefinitionRequest);
                    JSONObject resultFlowObj = JSONObject.parseObject(resultFlow);
                    String code = resultFlowObj.getString("code");
                    if("success".equals(code)){
                        JSONObject dataObj = resultFlowObj.getJSONObject("data");
                        log.info("同步流程成功：{}",JSONObject.toJSONString(dataObj));
                    }else{
                        log.info("同步流程失败：{}",resultFlow);
                        return;
                    }

                    //保存审批任务
                    SyncFlow syncFlow = new SyncFlow();
                    syncFlow.setFlowCode(processInstanceRequest.getFlowCode());
                    syncFlow.setTenantId(processInstanceRequest.getTenantId());
                    syncFlow.setContractId(request.getContractId());
                    syncFlow.setTenantNum(processInstanceRequest.getTenantNum());
                    syncFlow.setInstanceStatus(processInstanceRequest.getInstanceStatus());
                    syncFlow.setInstanceId(processInstanceRequest.getInstanceId());
                    syncFlow.setBusinessKey(processInstanceRequest.getBusinessKey());
                    syncFlow.setCreateCode(processInstanceRequest.getStarterUser());
                    syncFlow.setCreateTime(new Date());
                    syncFlowMapper.insert(syncFlow);
                }

                String resultInstance = yuecaiContractClient.syncInstance(processInstanceRequest);
                JSONObject resultFlowObj = JSONObject.parseObject(resultInstance);
                String code = resultFlowObj.getString("code");
                if("success".equals(code)){
                    JSONObject dataObj = resultFlowObj.getJSONObject("data");
                    log.info("同步任务成功：{}",JSONObject.toJSONString(dataObj));
                }else{
                    log.info("同步任务失败：{}",resultInstance);
                }
            }
        }
    }

    @Override
    public ProcessInstanceResponse approvalCallBack(ApprovalCallbackRequest request) {
        ProcessInstanceResponse response = new ProcessInstanceResponse();
        String flowCode = request.getFlowCode();
        SyncFlow syncFlowOne = syncFlowMapper.selectOne(new QueryWrapper<SyncFlow>()
                .eq("flow_code", flowCode));
        if(syncFlowOne==null){
            log.info("审批任务不存在，请确认flow_code = {} 在t_sync_flow表中是否存在", flowCode);
            return null;
        }

        ApprovalContractRequest approvalRequest = new ApprovalContractRequest();

        String contractId = syncFlowOne.getContractId();//合同id
        Map<String, Object> paramsContract = new HashMap<>();
        paramsContract.put("user_id_type", "user_id");
        ContractResponse contractInfo = zhishuContractClient.getContract(contractId, paramsContract);
        String processInstanceId = null;
        ContractQueryResponse contract = null;
        if(contractInfo!=null){
            Map<String, Object> data = contractInfo.getData();
            contract = JSONObject.parseObject(String.valueOf(data.get("contract")), ContractQueryResponse.class);
            processInstanceId = contract.getProcessInstanceId();
        }else{
            log.info("回调接口：id = {}未查询到合同信息", contractId);
            return null;
        }

        approvalRequest.setTaskInstanceId(request.getTaskId());
        approvalRequest.setAssigneeId(request.getOperatorUser());
        approvalRequest.setCommandType(ApprovalEnum.getCommandTypeByCode(request.getActionType()));
        approvalRequest.setTaskComment(request.getComment());
        ApprovalResponse approvalResponse = zhishuContractClient.approvalContract(approvalRequest,processInstanceId);
        if (approvalResponse != null && approvalResponse.getCode() == 0) {
            ApprovalResponse.ProcessInstance processInstance = approvalResponse.getData();
            ProcessInstanceRequest processInstanceRequest = buildApprovalProcessInstanceResponse(contract, processInstance);
            String resultInstance = yuecaiContractClient.syncInstance(processInstanceRequest);
            JSONObject resultFlowObj = JSONObject.parseObject(resultInstance);
            String code = resultFlowObj.getString("code");
            if("success".equals(code)){
                JSONObject dataObj = resultFlowObj.getJSONObject("data");
                log.info("回调接口：同步任务成功：{}",JSONObject.toJSONString(dataObj));
            }else{
                log.info("回调接口：同步任务失败：{}",resultInstance);
            }
        }

        return response;
    }

    /**
     * 组装同步流程信息
     * @param contract
     * @param processInstance
     * @return
     */
    private ProcessDefinitionRequest buildProcessDefinitionRequest(ContractQueryResponse contract,ApprovalQueryResponse.ProcessInstance processInstance) {
        ProcessDefinitionRequest processDefinition = new ProcessDefinitionRequest();
        processDefinition.setFlowCode(processInstance.getProcessInstanceId());//流程编码（唯一标识）
        processDefinition.setFlowName(contract.getContractName());//流程名称
        processDefinition.setTenantId(0L);//租户id
        processDefinition.setSourceSystem(zhishuBaseConfig.getSourceSystem());//来源系统
        return processDefinition;
    }

    /**
     * 组装同步任务信息
     * @param contract
     * @param processInstance
     * @return
     */
    private ProcessInstanceRequest buildProcessInstanceResponse(ContractQueryResponse contract, ApprovalQueryResponse.ProcessInstance processInstance, ContractEventRequest request) {
        ProcessInstanceRequest processInstanceRequest = new ProcessInstanceRequest();

        String processInstanceId = processInstance.getProcessInstanceId();
        String instanceStartTime = DateUtils.convertMillisToDate(Long.parseLong(processInstance.getStartTime()),null);//审批发起时间
        String endTime = processInstance.getEndTime();
        String instanceEndTime = null;
        if(endTime!=null&& !endTime.isEmpty()){
            instanceEndTime = DateUtils.convertMillisToDate(Long.parseLong(processInstance.getEndTime()),null);//审批发起时间
        }else{
            instanceEndTime = instanceStartTime;
        }
        List<ApprovalQueryResponse.TaskInstance> taskInstanceList = processInstance.getTaskInstanceList();
        String taskInstanceId = null;//审批节点id
        List<String> assigneeIds = new ArrayList<>();//节点审批人员id
        String startTime = null;//当前节点开始时间
        String nodeId = null;//节点id
        String nodeName = null;//节点名称
        String taskStatus = null;
        for (ApprovalQueryResponse.TaskInstance taskInstance : taskInstanceList) {
            if(15==request.getContractStageCode()
                    ||11==request.getContractStageCode()
                    ||14==request.getContractStageCode()){//如果是节点撤回时/节点通过时，需要获取上一个节点的相关信息
                taskStatus = "APPROVED";
                nodeId = request.getExtraInfo().getNodeId();
                nodeName = request.getExtraInfo().getNodeName();
                if(nodeId.equals(taskInstance.getNodeId())){
                    taskInstanceId = taskInstance.getTaskInstanceId();
                    assigneeIds = taskInstance.getAssigneeIds();
                    startTime = DateUtils.convertMillisToDate(Long.parseLong(taskInstance.getCreateTime()),null);
                }
            }else{
                taskStatus = "PENDING";
                if (taskInstance.getEndTime().isEmpty()) {//结束日期为空则为当前节点
                    taskInstanceId = taskInstance.getTaskInstanceId();
                    assigneeIds = taskInstance.getAssigneeIds();
                    startTime = DateUtils.convertMillisToDate(Long.parseLong(taskInstance.getCreateTime()),null);
                    nodeId = taskInstance.getNodeId();
                    nodeName = taskInstance.getNodeName().getZh();
                }
            }
        }

        processInstanceRequest.setFlowCode(processInstanceId);//流程编码（唯一标识）
        //实例状态RUN-运行中,SUSPEND-挂起,END-正常结束,INTERRUPT-中断,WITHDRAW-已撤回,EXCEPTION-异常暂挂,BLOCK-阻塞
        processInstanceRequest.setInstanceStatus("RUN");
        if(2==contract.getContractStatusCode()){//如果是已撤回传WITHDRAW
            processInstanceRequest.setInstanceStatus("END");
        }
        processInstanceRequest.setBusinessKey(contract.getContractNumber());//单据编码
        processInstanceRequest.setDescription(contract.getContractName());//实例描述
        processInstanceRequest.setStarterUser(contract.getSubmitterUserId());//发起人用户ID
//        processInstanceRequest.setStarterUser("e8d58ag6");//发起人用户ID
        processInstanceRequest.setStartTime(instanceStartTime);//实例发起时间，yyyy-MM-dd hh:mm:ss
        processInstanceRequest.setEndTime(instanceEndTime);//实例结束时间，yyyy-MM-dd hh:mm:ss(暂存开始时间)
        processInstanceRequest.setUpdateTime(DateUtils.convertDateToString(new Date(),null));//实例更新时间，增量更新时，用于过滤更新调用，传入的updateTime大于上次更新时间的调用会被执行
        processInstanceRequest.setTenantId(0L);
        processInstanceRequest.setStarterTenantId(0L);

        List<ProcessTaskRequest> processTaskRequestList = new ArrayList<>();
        ProcessTaskRequest processTaskRequest = new ProcessTaskRequest();
        processTaskRequest.setTaskCode(taskInstanceId);//任务编码（实例唯一）
        processTaskRequest.setTaskAssignee(assigneeIds.get(0));
//        processTaskRequest.setTaskAssignee("e8d58ag6");
        processTaskRequest.setNodeCode(nodeId);
        processTaskRequest.setNodeName(nodeName);
        processTaskRequest.setStartTime(startTime);
        processTaskRequest.setEndTime(startTime);
        processTaskRequest.setUpdateTime(DateUtils.convertDateToString(new Date(),null));
        processTaskRequest.setTaskStatus(taskStatus);
        processTaskRequest.setTaskAssigneeTenantId(0L);
        //审批跳转地址
        String detailUrl = contract.getMultiUrl().getPcUrl();
        Map<String,Object> linksMap = new HashMap<>();
        linksMap.put("url",detailUrl);
        linksMap.put("openType","NEW_TAB");
        linksMap.put("androidUrl","");
        linksMap.put("iosUrl","");
        processTaskRequest.setLinks(linksMap);

        processTaskRequestList.add(processTaskRequest);

        processInstanceRequest.setTaskList(processTaskRequestList);

        List<ProcessActionRequest> processActionRequestList = new ArrayList<>();
        ProcessActionRequest processActionRequest = new ProcessActionRequest();
        processActionRequest.setActionType("APPROVE");//动作类型APPROVE/REJECT
        processActionRequest.setCommentHideFlag(0);//是否显示意见输入框，默认不隐藏
        processActionRequest.setCommentRequiredFlag(0);//审批意见是否必输，默认必输
        processActionRequest.setActionBackUrl(zhishuBaseConfig.getCallbackUrl()+approvalCallBackUrl);//动作回调地址
        processActionRequestList.add(processActionRequest);
        processActionRequest = new ProcessActionRequest();
        processActionRequest.setActionType("REJECT");//动作类型APPROVE/REJECT
        processActionRequest.setCommentHideFlag(0);//是否显示意见输入框，默认不隐藏
        processActionRequest.setCommentRequiredFlag(0);//审批意见是否必输，默认必输
        processActionRequest.setActionBackUrl(zhishuBaseConfig.getCallbackUrl()+approvalCallBackUrl);//动作回调地址
        processActionRequestList.add(processActionRequest);

        processTaskRequest.setActionList(processActionRequestList);

        return processInstanceRequest;
    }

    private ProcessInstanceRequest buildApprovalProcessInstanceResponse(ContractQueryResponse contract, ApprovalResponse.ProcessInstance processInstance) {
        ProcessInstanceRequest processInstanceRequest = new ProcessInstanceRequest();

        String processInstanceId = processInstance.getProcessInstanceId();
        String instanceStartTime = DateUtils.convertMillisToDate(Long.parseLong(processInstance.getStartTime()),null);//审批发起时间
        String instanceEndTime = DateUtils.convertMillisToDate(Long.parseLong(processInstance.getEndTime()),null);//审批结束时间
        List<ApprovalResponse.TaskInstance> taskInstanceList = processInstance.getTaskInstanceList();
        String taskInstanceId = null;//审批节点id
        List<String> assigneeIds = new ArrayList<>();//节点审批人员id
        String startTime = null;//当前节点开始时间
        String endTime = null;//当前节点结束时间
        String nodeId = null;//节点id
        String nodeName = null;//节点名称
        for (ApprovalResponse.TaskInstance taskInstance : taskInstanceList) {
            if (taskInstance.getEndTime().isEmpty()) {//结束日期为空则为当前节点
                taskInstanceId = taskInstance.getTaskInstanceId();
                assigneeIds = taskInstance.getAssigneeIds();
                startTime = DateUtils.convertMillisToDate(Long.parseLong(taskInstance.getCreateTime()),null);
                endTime = DateUtils.convertMillisToDate(Long.parseLong(taskInstance.getEndTime()),null);
                nodeId = taskInstance.getNodeId();
                nodeName = taskInstance.getNodeName().getZh();
            }
        }

        processInstanceRequest.setFlowCode(processInstanceId);//流程编码（唯一标识）
        //实例状态RUN-运行中,SUSPEND-挂起,END-正常结束,INTERRUPT-中断,WITHDRAW-已撤回,EXCEPTION-异常暂挂,BLOCK-阻塞
        processInstanceRequest.setInstanceStatus("RUN");
        if(2==contract.getContractStatusCode()){//如果是已撤回传WITHDRAW
            processInstanceRequest.setInstanceStatus("WITHDRAW");
        }
        processInstanceRequest.setBusinessKey(contract.getContractNumber());//单据编码
        processInstanceRequest.setDescription(contract.getContractName());//实例描述
        processInstanceRequest.setStarterUser(contract.getSubmitterUserId());//发起人用户ID
//        processInstanceRequest.setStarterUser("e8d58ag6");//发起人用户ID
        processInstanceRequest.setStartTime(instanceStartTime);//实例发起时间，yyyy-MM-dd hh:mm:ss
        processInstanceRequest.setEndTime(instanceEndTime);//实例结束时间，yyyy-MM-dd hh:mm:ss(暂存开始时间)
        processInstanceRequest.setUpdateTime(DateUtils.convertDateToString(new Date(),null));//实例更新时间，增量更新时，用于过滤更新调用，传入的updateTime大于上次更新时间的调用会被执行
        processInstanceRequest.setTenantId(0L);
        processInstanceRequest.setStarterTenantId(0L);

        List<ProcessTaskRequest> processTaskRequestList = new ArrayList<>();
        ProcessTaskRequest processTaskRequest = new ProcessTaskRequest();
        processTaskRequest.setTaskCode(taskInstanceId);//任务编码（实例唯一）
        processTaskRequest.setTaskAssignee(assigneeIds.get(0));
        processTaskRequest.setNodeCode(nodeId);
        processTaskRequest.setNodeName(nodeName);
        processTaskRequest.setStartTime(startTime);
        processTaskRequest.setEndTime(endTime);
        processTaskRequest.setUpdateTime(DateUtils.convertDateToString(new Date(),null));
        processTaskRequest.setTaskStatus("APPROVED");
        processTaskRequest.setTaskAssigneeTenantId(0L);

        //审批跳转地址
        String detailUrl = contract.getMultiUrl().getPcUrl();
        Map<String,Object> linksMap = new HashMap<>();
        linksMap.put("url",detailUrl);
        linksMap.put("openType","NEW_TAB");
        linksMap.put("androidUrl","");
        linksMap.put("iosUrl","");
        processTaskRequest.setLinks(linksMap);

        processTaskRequestList.add(processTaskRequest);

        processInstanceRequest.setTaskList(processTaskRequestList);

//        List<ProcessActionRequest> processActionRequestList = new ArrayList<>();
//        ProcessActionRequest processActionRequest = new ProcessActionRequest();
//        processActionRequest.setActionType("APPROVE");//动作类型APPROVE/REJECT
//        processActionRequest.setCommentHideFlag(0);//是否显示意见输入框，默认不隐藏
//        processActionRequest.setCommentRequiredFlag(0);//审批意见是否必输，默认必输
//        processActionRequest.setActionBackUrl(zhishuBaseConfig.getCallbackUrl());//动作回调地址
//        processActionRequestList.add(processActionRequest);
//        processActionRequest = new ProcessActionRequest();
//        processActionRequest.setActionType("REJECT");//动作类型APPROVE/REJECT
//        processActionRequest.setCommentHideFlag(0);//是否显示意见输入框，默认不隐藏
//        processActionRequest.setCommentRequiredFlag(0);//审批意见是否必输，默认必输
//        processActionRequest.setActionBackUrl(zhishuBaseConfig.getCallbackUrl());//动作回调地址
//        processActionRequestList.add(processActionRequest);

//        processTaskRequest.setActionList(processActionRequestList);

        return processInstanceRequest;
    }

    private void saveApprovalRecord(String taskInstanceId,String contractId, String approver, String approvalStatus,
                                   String approvalOpinion, String nextApprover) {
        ApprovalRecord record = new ApprovalRecord();
        record.setContractId(contractId);
        record.setApprovalId(taskInstanceId);
        record.setApproverId(approver);
        record.setApprovalStatus(approvalStatus);
        record.setApprovalOpinion(approvalOpinion);
        record.setNextApprover(nextApprover);
        record.setCreateTime(LocalDateTime.now());
        record.setDeleted(0);
        approvalRecordMapper.insert(record);
    }

    private String buildResultMessage(List<BatchApprovalResultDTO.ApprovalItem> successItems,
                                     List<BatchApprovalResultDTO.ApprovalItem> failItems,
                                     String approvalStatus) {
        StringBuilder message = new StringBuilder();

        if (!successItems.isEmpty()) {
            message.append("共").append(successItems.size()).append("份合同:");
            message.append(successItems.stream()
                    .map(BatchApprovalResultDTO.ApprovalItem::getContractName)
                    .collect(Collectors.joining("、")));
            message.append("执行合同").append(approvalStatus).append("成功!");
        }

        if (!failItems.isEmpty()) {
            message.append("共").append(failItems.size()).append("份合同:");
            message.append(failItems.stream()
                    .map(BatchApprovalResultDTO.ApprovalItem::getContractName)
                    .collect(Collectors.joining("、")));
            message.append("执行合同").append(approvalStatus).append("失败!");
            message.append("失败原因:");
            for (BatchApprovalResultDTO.ApprovalItem item : failItems) {
                message.append(item.getContractName()).append(":").append(item.getMessage()).append("。");
            }
        }
        return message.toString();
    }

}
