package com.hero.middleware.service.impl;

import com.hero.middleware.client.zhishu.ZhishuContractClient;
import com.hero.middleware.client.zhishu.request.ApprovalContractRequest;
import com.hero.middleware.client.zhishu.response.ApprovalQueryResponse;
import com.hero.middleware.client.zhishu.response.ApprovalResponse;
import com.hero.middleware.client.zhishu.response.ContractQueryResponse;
import com.hero.middleware.client.zhishu.response.ContractsSearchResponse;
import com.hero.middleware.client.zhishu.response.SubmitContractResponse;
import com.hero.middleware.dto.ApproveContractToNodeResultDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ZhiShuSynServiceApproveToNodeTest {

    private static final String TARGET_NODE = "申请人确认签约性质";

    @Mock
    private ZhishuContractClient zhishuContractClient;

    @Test
    void approveContractsToNodeSubmitsDraftAndStopsAtTargetNode() {
        ZhiShuSynServiceImpl service = buildService();
        when(zhishuContractClient.searchContracts(any()))
                .thenReturn(searchResponse(contract("C-001", 100L, 0, "editing", null)));
        when(zhishuContractClient.submitContract("100")).thenReturn(submitResponse("100", "process-1"));
        when(zhishuContractClient.getApprovalContract(eq("process-1"), any()))
                .thenReturn(approvalQuery("task-1", "初审", "", "user-1"))
                .thenReturn(approvalQuery("task-2", TARGET_NODE, "", "user-2"));
        when(zhishuContractClient.approvalContract(any(ApprovalContractRequest.class), eq("process-1")))
                .thenReturn(approvalResponse(0, "success"));

        ApproveContractToNodeResultDTO result =
                service.approveContractsToNode(Collections.singleton("C-001"), TARGET_NODE);

        assertEquals(Integer.valueOf(1), result.getSuccessCount());
        assertEquals(Integer.valueOf(0), result.getFailCount());
        assertEquals("C-001", result.getSuccesses().get(0).getContractNumber());
        assertEquals(TARGET_NODE, result.getSuccesses().get(0).getCurrentNodeName());

        ArgumentCaptor<ApprovalContractRequest> requestCaptor =
                ArgumentCaptor.forClass(ApprovalContractRequest.class);
        verify(zhishuContractClient).approvalContract(requestCaptor.capture(), eq("process-1"));
        ApprovalContractRequest approvalRequest = requestCaptor.getValue();
        assertEquals("task-1", approvalRequest.getTaskInstanceId());
        assertEquals("user-1", approvalRequest.getAssigneeId());
        assertEquals("general", approvalRequest.getCommandType());
        assertEquals("自动审核到指定节点", approvalRequest.getTaskComment());
    }

    @Test
    void approveContractsToNodeDoesNotSubmitWhenProcessAlreadyAtTargetNode() {
        ZhiShuSynServiceImpl service = buildService();
        when(zhishuContractClient.searchContracts(any()))
                .thenReturn(searchResponse(contract("C-001", 100L, 1, "approving", "process-1")));
        when(zhishuContractClient.getApprovalContract(eq("process-1"), any()))
                .thenReturn(approvalQuery("task-1", TARGET_NODE, "", "user-1"));

        ApproveContractToNodeResultDTO result =
                service.approveContractsToNode(Collections.singleton("C-001"), TARGET_NODE);

        assertEquals(Integer.valueOf(1), result.getSuccessCount());
        assertEquals(Integer.valueOf(0), result.getFailCount());
        verify(zhishuContractClient, never()).submitContract(anyString());
        verify(zhishuContractClient, never()).approvalContract(any(ApprovalContractRequest.class), anyString());
    }

    @Test
    void approveContractsToNodeShowsSubmitValidationDetails() {
        ZhiShuSynServiceImpl service = buildService();
        when(zhishuContractClient.searchContracts(any()))
                .thenReturn(searchResponse(contract("C-001", 100L, 0, "editing", null)));
        when(zhishuContractClient.submitContract("100")).thenReturn(submitValidationFailureResponse());

        ApproveContractToNodeResultDTO result =
                service.approveContractsToNode(Collections.singleton("C-001"), TARGET_NODE);

        assertEquals(Integer.valueOf(0), result.getSuccessCount());
        assertEquals(Integer.valueOf(1), result.getFailCount());
        assertEquals("张三(owner-1)", result.getFailures().get(0).getContractOwner());
        assertEquals("主播专项/主播经纪(009/009001)", result.getFailures().get(0).getZhishuContractType());
        String reason = result.getFailures().get(0).getReason();
        assertTrue(reason.contains("合同提交校验失败"));
        assertTrue(reason.contains("关联合同/关联合同为空"));
        assertTrue(reason.contains("履约计划/付款计划/付款性质为空"));
        assertTrue(reason.contains("attribute_key=associated_contract"));
        assertTrue(reason.contains("attribute_key=custom_15_071a641657e94f2faf65bf973850166e"));
    }

    @Test
    void approveContractsToNodeFailsWhenContractIsMissing() {
        ZhiShuSynServiceImpl service = buildService();
        when(zhishuContractClient.searchContracts(any()))
                .thenReturn(searchResponse());

        ApproveContractToNodeResultDTO result =
                service.approveContractsToNode(Collections.singleton("C-001"), TARGET_NODE);

        assertEquals(Integer.valueOf(0), result.getSuccessCount());
        assertEquals(Integer.valueOf(1), result.getFailCount());
        assertTrue(result.getFailures().get(0).getReason().contains("未查询到合同编号"));
    }

    @Test
    void approveContractsToNodeFailsWhenContractNumberMatchesMultipleContracts() {
        ZhiShuSynServiceImpl service = buildService();
        when(zhishuContractClient.searchContracts(any()))
                .thenReturn(searchResponse(
                        contract("C-001", 100L, 0, "editing", null),
                        contract("C-001", 101L, 0, "editing", null)));

        ApproveContractToNodeResultDTO result =
                service.approveContractsToNode(Collections.singleton("C-001"), TARGET_NODE);

        assertEquals(Integer.valueOf(0), result.getSuccessCount());
        assertEquals(Integer.valueOf(1), result.getFailCount());
        assertTrue(result.getFailures().get(0).getReason().contains("多条"));
    }

    @Test
    void approveContractsToNodeFailsWhenApprovalApiFails() {
        ZhiShuSynServiceImpl service = buildService();
        when(zhishuContractClient.searchContracts(any()))
                .thenReturn(searchResponse(contract("C-001", 100L, 1, "approving", "process-1")));
        when(zhishuContractClient.getApprovalContract(eq("process-1"), any()))
                .thenReturn(approvalQuery("task-1", "初审", "", "user-1"));
        when(zhishuContractClient.approvalContract(any(ApprovalContractRequest.class), eq("process-1")))
                .thenReturn(approvalResponse(1, "failed"));

        ApproveContractToNodeResultDTO result =
                service.approveContractsToNode(Collections.singleton("C-001"), TARGET_NODE);

        assertEquals(Integer.valueOf(0), result.getSuccessCount());
        assertEquals(Integer.valueOf(1), result.getFailCount());
        assertTrue(result.getFailures().get(0).getReason().contains("审批当前节点失败"));
    }

    @Test
    void approveContractsToNodeFailsWhenProcessEndedBeforeTargetNode() {
        ZhiShuSynServiceImpl service = buildService();
        when(zhishuContractClient.searchContracts(any()))
                .thenReturn(searchResponse(contract("C-001", 100L, 1, "approving", "process-1")));
        when(zhishuContractClient.getApprovalContract(eq("process-1"), any()))
                .thenReturn(approvalQuery("task-1", "初审", "1700000000000", "user-1"));

        ApproveContractToNodeResultDTO result =
                service.approveContractsToNode(Collections.singleton("C-001"), TARGET_NODE);

        assertEquals(Integer.valueOf(0), result.getSuccessCount());
        assertEquals(Integer.valueOf(1), result.getFailCount());
        assertTrue(result.getFailures().get(0).getReason().contains("流程已结束"));
    }

    @Test
    void approveContractsToNodeFailsAfterMaxAutoApprovedSteps() {
        ZhiShuSynServiceImpl service = buildService();
        when(zhishuContractClient.searchContracts(any()))
                .thenReturn(searchResponse(contract("C-001", 100L, 1, "approving", "process-1")));
        when(zhishuContractClient.getApprovalContract(eq("process-1"), any()))
                .thenReturn(approvalQuery("task-1", "初审", "", "user-1"));
        when(zhishuContractClient.approvalContract(any(ApprovalContractRequest.class), eq("process-1")))
                .thenReturn(approvalResponse(0, "success"));

        ApproveContractToNodeResultDTO result =
                service.approveContractsToNode(Collections.singleton("C-001"), TARGET_NODE);

        assertEquals(Integer.valueOf(0), result.getSuccessCount());
        assertEquals(Integer.valueOf(1), result.getFailCount());
        assertTrue(result.getFailures().get(0).getReason().contains("超过最大自动审批节点数"));
        verify(zhishuContractClient, times(20))
                .approvalContract(any(ApprovalContractRequest.class), eq("process-1"));
    }

    @Test
    void exportNotFoundContractsWritesExcelWhenContractIsMissing() throws Exception {
        ZhiShuSynServiceImpl service = buildService();
        when(zhishuContractClient.searchContracts(any()))
                .thenReturn(searchResponse(contract("C-001", 100L, 1, "approving", "process-1")))
                .thenReturn(searchResponse());

        String exportPath = service.exportNotFoundContracts(Arrays.asList("C-001", "C-404"));
        Path path = Paths.get(exportPath);

        assertTrue(Files.exists(path));
        assertTrue(Files.size(path) > 0);
        Files.deleteIfExists(path);
        verify(zhishuContractClient, times(2)).searchContracts(any());
    }

    private ZhiShuSynServiceImpl buildService() {
        ZhiShuSynServiceImpl service = new ZhiShuSynServiceImpl("unused.xlsx", 200);
        ReflectionTestUtils.setField(service, "zhishuContractClient", zhishuContractClient);
        return service;
    }

    private ContractsSearchResponse searchResponse(ContractQueryResponse... contracts) {
        ContractsSearchResponse response = new ContractsSearchResponse();
        response.setCode(0);
        response.setMsg("success");
        ContractsSearchResponse.DataInfo dataInfo = new ContractsSearchResponse.DataInfo();
        dataInfo.setItems(Arrays.asList(contracts));
        response.setData(dataInfo);
        return response;
    }

    private ContractQueryResponse contract(String contractNumber,
                                           Long contractId,
                                           Integer statusCode,
                                           String statusName,
                                           String processInstanceId) {
        ContractQueryResponse contract = new ContractQueryResponse();
        contract.setContractNumber(contractNumber);
        contract.setContractId(contractId);
        contract.setContractStatusCode(statusCode);
        contract.setContractStatusName(statusName);
        contract.setProcessInstanceId(processInstanceId);
        contract.setOwnerUserName("张三");
        contract.setOwnerUserId("owner-1");
        contract.setParentContractCategoryName("主播专项");
        contract.setParentContractCategoryNumber("009");
        contract.setContractCategoryName("主播经纪");
        contract.setContractCategoryNumber("009001");
        return contract;
    }

    private SubmitContractResponse submitResponse(String contractId, String processInstanceId) {
        SubmitContractResponse response = new SubmitContractResponse();
        response.setCode(0);
        response.setMsg("success");
        SubmitContractResponse.DataInfo dataInfo = new SubmitContractResponse.DataInfo();
        dataInfo.setContractId(contractId);
        dataInfo.setProcessInstanceId(processInstanceId);
        response.setData(dataInfo);
        return response;
    }

    private SubmitContractResponse submitValidationFailureResponse() {
        SubmitContractResponse response = new SubmitContractResponse();
        response.setCode(111761);
        response.setMsg("合同提交校验失败");

        SubmitContractResponse.InvalidAttribute relation = invalidAttribute(
                "关联合同", "", "关联合同", "EMPTY", "associated_contract");
        SubmitContractResponse.InvalidAttribute paymentType = invalidAttribute(
                "履约计划", "付款计划", "付款性质", "EMPTY",
                "custom_15_071a641657e94f2faf65bf973850166e");

        SubmitContractResponse.DataInfo dataInfo = new SubmitContractResponse.DataInfo();
        dataInfo.setInvalidAttributeList(Arrays.asList(relation, paymentType));
        response.setData(dataInfo);
        return response;
    }

    private SubmitContractResponse.InvalidAttribute invalidAttribute(String moduleName,
                                                                     String groupName,
                                                                     String attributeName,
                                                                     String reason,
                                                                     String attributeKey) {
        SubmitContractResponse.InvalidAttribute invalidAttribute = new SubmitContractResponse.InvalidAttribute();
        invalidAttribute.setModuleName(moduleName);
        invalidAttribute.setGroupName(groupName);
        invalidAttribute.setAttributeName(attributeName);
        invalidAttribute.setReason(reason);
        invalidAttribute.setAttributeKey(attributeKey);
        return invalidAttribute;
    }

    private ApprovalQueryResponse approvalQuery(String taskId,
                                                 String nodeName,
                                                 String endTime,
                                                 String assigneeId) {
        ApprovalQueryResponse response = new ApprovalQueryResponse();
        response.setCode(0);
        response.setMsg("success");

        ApprovalQueryResponse.MultiLanguage multiLanguage = new ApprovalQueryResponse.MultiLanguage();
        multiLanguage.setZh(nodeName);

        ApprovalQueryResponse.TaskInstance taskInstance = new ApprovalQueryResponse.TaskInstance();
        taskInstance.setTaskInstanceId(taskId);
        taskInstance.setNodeName(multiLanguage);
        taskInstance.setEndTime(endTime);
        taskInstance.setAssigneeIds(Collections.singletonList(assigneeId));

        ApprovalQueryResponse.ProcessInstance processInstance = new ApprovalQueryResponse.ProcessInstance();
        processInstance.setTaskInstanceList(Collections.singletonList(taskInstance));

        ApprovalQueryResponse.DataBean dataBean = new ApprovalQueryResponse.DataBean();
        dataBean.setProcessInstance(processInstance);
        response.setData(dataBean);
        return response;
    }

    private ApprovalResponse approvalResponse(Integer code, String msg) {
        ApprovalResponse response = new ApprovalResponse();
        response.setCode(code);
        response.setMsg(msg);
        return response;
    }
}
