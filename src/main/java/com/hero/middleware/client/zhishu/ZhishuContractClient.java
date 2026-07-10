package com.hero.middleware.client.zhishu;

import com.alibaba.fastjson.JSONObject;
import com.hero.middleware.client.zhishu.request.*;
import com.hero.middleware.client.zhishu.response.*;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@Slf4j
public class ZhishuContractClient {

    @Autowired
    private ZhishuApiClient zhishuApiClient;

    private static final String DELETE_DRAFT_CONTRACT_PATH = "/open-apis/contract/v1/contracts/:contract_id";

    private static final String CREATE_CONTRACT_PATH = "/open-apis/contract/v1/contracts?user_id_type=user_id";//合同创建
    private static final String SUBMIT_CONTRACT_PATH = "/open-apis/contract/v1/contracts/:contract_id/submit";//提交合同
    private static final String TASK_APPROVAL_PATH = "/open-apis/contract/v1/process_instances/:process_instance_id/task_approval?user_id_type=user_id";//合同审批
    private static final String GET_TASK_APPROVAL_PATH = "/open-apis/contract/v1/process_instances/:process_instance_id";//合同审批
    private static final String GET_CONTRACT_PATH = "/open-apis/contract/v1/contracts/:contract_id";//合同审批
    private static final String CONTRACTS_SEARCH_PATH = "/open-apis/contract/v1/contracts/search?user_id_type=user_id";//搜索合同
    private static final String CREATE_TEMPLATE_INSTANCES_PATH = "/open-apis/contract/v1/template_instances?user_id_type=user_id";//创建模版实例
    private static final String GET_TEMPLATE_LIST_PATH = "/open-apis/contract/v1/templates?user_id_type=user_id";//查看模版列表
    private static final String GET_TEMPLATES_PATH = "/open-apis/contract/v1/templates/:template_id";//查看模版详情
    private static final String GET_CONTRACT_CATEGORYS_PATH = "/open-apis/contract/v1/contract_categorys";//查询合同类型目录
    private static final String UPLOAD_CONTRACT_FILE_PATH = "/open-apis/contract/v1/files/upload";//上传合同相关文件


    public ZhishuCreateContractResponse createContractV2(ZhishuCreateContractRequest request) {
        String response = zhishuApiClient.doPost("创建智书合同", CREATE_CONTRACT_PATH, request);
        return parseResponse(response, ZhishuCreateContractResponse.class);
    }

    public SubmitContractResponse submitContract(String contractId) {
        String submitContractPath = SUBMIT_CONTRACT_PATH.replace(":contract_id", contractId);
        String response = zhishuApiClient.doPostWithoutBody("提交智书合同", submitContractPath);
        return parseResponse(response, SubmitContractResponse.class);
    }

    public ContractResponse createContract(CreateContractRequest request) {
        String response = zhishuApiClient.doPost("创建智书合同", "/api/contract/create", request);
        return parseResponse(response, ContractResponse.class);
    }

    public ContractResponse updateContract(String contractId, UpdateContractRequest request) {
        String response = zhishuApiClient.doPut("更新智书合同", "/api/contract/" + contractId, request);
        return parseResponse(response, ContractResponse.class);
    }

    public ContractResponse getContract(String contractId, Map<String, Object> params) {
        log.debug("合同详情查询，合同id为：{}", contractId);
        String getContractPath = GET_CONTRACT_PATH.replace(":contract_id", contractId);
        String response = zhishuApiClient.doGet("查询智书合同详情", getContractPath, params);
        log.debug("查询合同详情完成，contractId={}，responseSize={}", contractId, response == null ? 0 : response.length());
        return parseResponse(response, ContractResponse.class);
    }

    public ContractsSearchResponse searchContracts(ContractsSearchRequest request) {
        if (log.isDebugEnabled()) {
            log.debug("搜索合同请求参数：{}", JSONObject.toJSONString(request));
        }
        String response = zhishuApiClient.doPost("搜索智书合同", CONTRACTS_SEARCH_PATH, request);
        log.debug("搜索合同完成，responseSize={}", response == null ? 0 : response.length());
        return parseResponse(response, ContractsSearchResponse.class);
    }

    public ResultResponse deleteDraftContract(String contractId) {
        String deletePath = DELETE_DRAFT_CONTRACT_PATH.replace(":contract_id", contractId);
        String response = zhishuApiClient.doDelete("删除智书草稿合同", deletePath);
        log.debug("删除智书草稿合同完成，contractId={}，responseSize={}",
                contractId, response == null ? 0 : response.length());
        return parseResponse(response, ResultResponse.class);
    }

    public ContractListResponse getContractList(Map<String, Object> params) {
        String response = zhishuApiClient.doGet("查询智书合同列表", "/api/contract/list", params);
        return parseResponse(response, ContractListResponse.class);
    }

    public ApprovalResponse approvalContract(ApprovalContractRequest request,String processInstanceId) {
        String taskApprovalPath = TASK_APPROVAL_PATH.replace(":process_instance_id", processInstanceId);
        String response = zhishuApiClient.doPost("审批智书合同", taskApprovalPath, request);
        return parseResponse(response, ApprovalResponse.class);
    }

    public ApprovalQueryResponse getApprovalContract(String processInstanceId, Map<String, Object> params) {
        String getTaskApprovalPath = GET_TASK_APPROVAL_PATH.replace(":process_instance_id", processInstanceId);
        String response = zhishuApiClient.doGet("查询智书合同审批", getTaskApprovalPath, params);
        return parseResponse(response, ApprovalQueryResponse.class);
    }

    public QueryTemplateResponse getTemplate(String templateId, Map<String, Object> params) {
        log.debug("获取模板详情，templateId={}", templateId);
        String getTemplatesPath = GET_TEMPLATES_PATH.replace(":template_id", templateId);
        String response = zhishuApiClient.doGet("查询智书合同模板", getTemplatesPath, params);
        log.debug("获取模板详情完成，templateId={}，responseSize={}", templateId,
                response == null ? 0 : response.length());
        JSONObject resultRes = JSONObject.parseObject(response);
        String code = resultRes.getString("code");
        if("0".equals(code)){
            return parseResponse(resultRes.getString("data"), QueryTemplateResponse.class);
        }else{
            throw new RuntimeException("调用创建模板实例失败："+response);
        }
    }

    public QueryTemplateListResponse getTemplateList(Map<String, Object> params) {
        log.debug("获取模板列表");
        String response = zhishuApiClient.doGet(GET_TEMPLATE_LIST_PATH, params);
        log.debug("获取模板列表完成，responseSize={}", response == null ? 0 : response.length());
        return parseResponse(response, QueryTemplateListResponse.class);
    }

    public QueryContractCategoryResponse queryContractCategorys(Map<String, Object> params) {
        log.debug("查询合同类型目录");
        String response = zhishuApiClient.doGet(GET_CONTRACT_CATEGORYS_PATH, params);
        log.debug("查询合同类型目录完成，responseSize={}", response == null ? 0 : response.length());
        return parseResponse(response, QueryContractCategoryResponse.class);
    }

    public UploadContractFileResponse uploadContractFile(File file, String fileType, boolean needConvertToPdf) {
        Map<String, Object> formData = new LinkedHashMap<>();
        formData.put("file_type", fileType);
        formData.put("file_name", file.getName());
        formData.put("need_convert_to_pdf", String.valueOf(needConvertToPdf));
        formData.put("file", file);
        log.debug("上传合同相关文件，fileName={}，fileType={}，needConvertToPdf={}，fileSize={}",
                file.getName(), fileType, needConvertToPdf, file.length());
        String response = zhishuApiClient.doPostMultipart(UPLOAD_CONTRACT_FILE_PATH, formData);
        log.debug("上传合同相关文件完成，fileName={}，responseSize={}",
                file.getName(), response == null ? 0 : response.length());
        return parseResponse(response, UploadContractFileResponse.class);
    }

    public CreateTemplateInstanceResponse createTemplateInstance(CreateTemplateInstanceRequest request) {
        log.debug("创建模板实例");
        String response = zhishuApiClient.doPost("创建智书模板实例", CREATE_TEMPLATE_INSTANCES_PATH, request);
        log.debug("创建模板实例完成，responseSize={}", response == null ? 0 : response.length());
        JSONObject resultRes = JSONObject.parseObject(response);
        String code = resultRes.getString("code");
        if("0".equals(code)){
            return parseResponse(resultRes.getJSONObject("data").getString("template_instance"), CreateTemplateInstanceResponse.class);
        }else{
            throw new RuntimeException("调用创建模板实例失败："+response);
        }

    }

    private <T> T parseResponse(String response, Class<T> clazz) {
        return com.alibaba.fastjson.JSON.parseObject(response, clazz);
    }

}
