package com.hero.middleware.service.impl;

import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hero.middleware.client.feishu.FeiShuApiClient;
import com.hero.middleware.client.feishu.FeiShuTokenManager;
import com.hero.middleware.client.feishu.FeishuBitableClient;
import com.hero.middleware.client.feishu.response.FeishuUserInfoResponse;
import com.hero.middleware.client.yuecai.request.ContSyncLineRequest;
import com.hero.middleware.client.yuecai.request.ContSyncRequest;
import com.hero.middleware.client.yuecai.request.UpdateAnchorCardRequest;
import com.hero.middleware.client.yuecai.response.*;
import com.hero.middleware.client.zhishu.ZhiShuVendorClient;
import com.hero.middleware.client.zhishu.ZhishuApiClient;
import com.hero.middleware.client.zhishu.ZhishuContractClient;
import com.hero.middleware.client.zhishu.request.*;
import com.hero.middleware.client.zhishu.response.*;
import com.hero.middleware.client.yuecai.YuecaiContractClient;
import com.hero.middleware.client.yuecai.request.YuecaiUpdateContractRequest;
import com.hero.middleware.common.Result;
import com.hero.middleware.config.FeiShuBitableConfig;
import com.hero.middleware.config.YeCaiDataConfig;
import com.hero.middleware.dto.*;
import com.hero.middleware.entity.Contract;
import com.hero.middleware.entity.ContractSyncLog;
import com.hero.middleware.enums.*;
import com.hero.middleware.exception.BusinessException;
import com.hero.middleware.mapper.ContractMapper;
import com.hero.middleware.mapper.ContractSyncLogMapper;
import com.hero.middleware.service.ContractService;
import com.hero.middleware.utils.DateUtils;
import com.lark.oapi.service.bitable.v1.model.AppTableRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class ContractServiceImpl implements ContractService {

    @Autowired
    private ContractMapper contractMapper;

    @Autowired
    private ContractSyncLogMapper contractSyncLogMapper;

    @Autowired
    private ZhishuContractClient zhishuContractClient;

    @Autowired
    private ZhishuApiClient zhishuApiClient;

    @Autowired
    private YuecaiContractClient yuecaiContractClient;

    @Autowired
    private YeCaiDataConfig yeCaiDataConfig;
    @Autowired
    private ZhiShuVendorClient zhiShuVendorClient;
    @Autowired
    private FeishuBitableClient feishuBitableClient;
    @Autowired
    private FeiShuBitableConfig feiShuBitableConfig;
    @Autowired
    private FeiShuApiClient feiShuApiClient;

    @Override
//    @Transactional(rollbackFor = Exception.class)
    public CreateContractResultDTO createContract(CreateContractDTO dto) {
        log.info("========== 创建合同开始 ==========");
        CreateContractResultDTO result = new CreateContractResultDTO();
        log.info("接收业财系统创建合同请求, 单据编号: {}, 单据类型: {}, 创建人ID: {}",
                dto.getDocumentNumber(), dto.getDocumentType(), dto.getCreateUserId());
        log.debug("创建合同请求详细参数: {}", JSON.toJSONString(dto));

        if(dto.getDocumentType()!=3){//创建采购合同、创建订单合同，两个接口，不创建合同了，直接返回合同 url  https://contract.qfei.cn/
            result.setDraftUrl("https://contract.qfei.cn/");
            result.setPcUrl("https://contract.qfei.cn/");
            result.setMobileUrl("https://contract.qfei.cn/");
            return result;
        }

        String contractId = UUID.randomUUID().toString().replace("-", "");
        log.info("生成中间件合同ID: {}", contractId);

        ZhishuCreateContractRequest request = buildZhishuRequest(dto);
        log.info("组装智书合同创建请求完成");
        log.debug("智书合同创建请求详细参数: {}", JSON.toJSONString(request));
        if(request!=null){
            ZhishuCreateContractResponse response;
            try {
                log.info("请求对象: {}", JSON.toJSONString(request));

                log.info("开始调用zhishuContractClient.createContractV2方法...");
                response = zhishuContractClient.createContractV2(request);
                log.info("智书API响应: code={}, msg={}",
                        response != null ? response.getCode() : null,
                        response != null ? response.getMsg() : null);
                log.info("响应对象: {}", response != null ? JSON.toJSONString(response) : "null");
            } catch (Exception e) {
                log.error("调用智书合同创建API异常: {}", e.getMessage(), e);
                saveSyncLog(contractId, "CREATE", "YUECAI_TO_ZHISHU", "FAIL",
                        JSON.toJSONString(dto), null, "智书API调用异常: " + e.getMessage());
                throw new BusinessException("智书合同创建失败: " + e.getMessage());
            }

            if (response == null || !response.isSuccess()) {
                String errorMsg = response != null ? response.getMsg() : "智书API响应为空";
                log.error("智书合同创建失败: {}", errorMsg);
                saveSyncLog(contractId, "CREATE", "YUECAI_TO_ZHISHU", "FAIL",
                        JSON.toJSONString(dto), JSON.toJSONString(response), errorMsg);
                throw new BusinessException("智书合同创建失败: " + errorMsg);
            }

            if (response.getData() == null) {
                String errorMsg = "智书API响应数据为空";
                log.error("智书合同创建失败: {}", errorMsg);
                saveSyncLog(contractId, "CREATE", "YUECAI_TO_ZHISHU", "FAIL",
                        JSON.toJSONString(dto), JSON.toJSONString(response), errorMsg);
                throw new BusinessException("智书合同创建失败: " + errorMsg);
            }

            ZhishuCreateContractResponse.ContractInfo contractInfo = response.getData().getContract();

            if (contractInfo == null) {
                String errorMsg = "智书API响应合同信息为空";
                log.error("智书合同创建失败: {}", errorMsg);
                saveSyncLog(contractId, "CREATE", "YUECAI_TO_ZHISHU", "FAIL",
                        JSON.toJSONString(dto), JSON.toJSONString(response), errorMsg);
                throw new BusinessException("智书合同创建失败: " + errorMsg);
            }

            String zhishuContractId = contractInfo.getContractId();
            String contractNumber = contractInfo.getContractNumber();
            log.info("智书合同创建成功, 智书合同ID: {}, 合同编号: {}", zhishuContractId, contractNumber);

            String pcUrl = null;
            String mobileUrl = null;
            if (contractInfo.getMultiUrl() != null) {
                pcUrl = contractInfo.getMultiUrl().getPcUrl();
                mobileUrl = contractInfo.getMultiUrl().getMobileUrl();
                log.info("智书返回草稿页链接 - PC端: {}, 移动端: {}", pcUrl, mobileUrl);
            }

            String draftUrl = zhishuApiClient.buildDraftPageUrl(zhishuContractId);
            log.info("中间件拼接草稿页链接: {}", draftUrl);

            Contract contract = new Contract();
            contract.setContractId(contractId);
            contract.setContractName("合同-" + dto.getDocumentNumber());
            contract.setContractStatus("DRAFT");
            contract.setSourceType(String.valueOf(dto.getDocumentType()));
            contract.setSourceId(dto.getDocumentNumber());
            contract.setSourceNo(dto.getDocumentNumber());
            contract.setOperatorId(dto.getCreateUserId());
            contract.setZhishuContractId(zhishuContractId);
            contract.setDraftUrl(draftUrl);

            contractMapper.insert(contract);
            log.info("合同信息保存到数据库成功, contractId: {}", contractId);

            saveSyncLog(contractId, "CREATE", "YUECAI_TO_ZHISHU", "SUCCESS",
                    JSON.toJSONString(dto), JSON.toJSONString(response), null);


            result.setContractId(contractId);
            result.setZhishuContractId(zhishuContractId);
            result.setContractNumber(contractNumber);
            result.setContractName("合同-" + dto.getDocumentNumber());
            result.setContractStatus("DRAFT");
            result.setDraftUrl(draftUrl);
            result.setPcUrl(pcUrl);
            result.setMobileUrl(mobileUrl);

            log.info("========== 创建合同完成 ==========");
            log.info("返回结果: contractId={}, zhishuContractId={}, draftUrl={}",
                    contractId, zhishuContractId, draftUrl);
        }else{
            log.info("合同请求对象组装失败：{}", JSON.toJSONString(request));
            result.setErrMessage("合同创建失败！");
        }
        return result;
    }

    @Override
    public CreateAntiBriberyContractResultDTO createAntiBriberyContract(QueryAllVendorResponse.Item item) {
        log.info("========== 创建反贿赂协议合同开始 ==========");
        log.info("反贿赂协议合同创建入参: {}", JSON.toJSONString(item));
        if (item == null) {
            return buildAntiBriberyContractFailResult("交易方信息不能为空");
        }
        String counterPartyCode = trimToNull(item.getVendor());
        if (counterPartyCode == null) {
            return buildAntiBriberyContractFailResult("交易方编码不能为空");
        }
        String templateNumber = trimToNull(yeCaiDataConfig.getTemplateFHLXY());
        if (templateNumber == null) {
            return buildAntiBriberyContractFailResult("反贿赂协议合同模板编码未配置");
        }
        String createUserId = trimToNull(yeCaiDataConfig.getUserId());
        if (createUserId == null) {
            return buildAntiBriberyContractFailResult("智书合同创建人ID未配置");
        }

        try {
            CreateTemplateInstanceRequest templateInstanceRequest = new CreateTemplateInstanceRequest();
            templateInstanceRequest.setCreateUserid(createUserId);
            templateInstanceRequest.setTemplateNumber(templateNumber);
            log.info("创建反贿赂协议合同模板实例请求: {}", JSON.toJSONString(templateInstanceRequest));
            CreateTemplateInstanceResponse templateInstance = zhishuContractClient.createTemplateInstance(templateInstanceRequest);
            log.info("创建反贿赂协议合同模板实例返回: {}", JSON.toJSONString(templateInstance));
            if (templateInstance == null || trimToNull(templateInstance.getTemplateInstanceid()) == null) {
                return buildAntiBriberyContractFailResult("创建反贿赂协议合同模板实例失败");
            }

            ZhishuCreateContractRequest request = buildAntiBriberyContractRequest(item, createUserId,
                    templateInstance.getTemplateInstanceid());
            log.info("创建反贿赂协议合同请求: {}", JSON.toJSONString(request));
            ZhishuCreateContractResponse response = zhishuContractClient.createContractV2(request);
            log.info("创建反贿赂协议合同返回: {}", JSON.toJSONString(response));
            if (response == null || !response.isSuccess()) {
                String errorMsg = response == null ? "智书API响应为空" : response.getMsg();
                return buildAntiBriberyContractFailResult("创建反贿赂协议合同失败: " + errorMsg);
            }
            if (response.getData() == null || response.getData().getContract() == null) {
                return buildAntiBriberyContractFailResult("创建反贿赂协议合同失败: 智书API响应合同信息为空");
            }

            ZhishuCreateContractResponse.ContractInfo contractInfo = response.getData().getContract();
            CreateAntiBriberyContractResultDTO result = new CreateAntiBriberyContractResultDTO();
            result.setSuccess(true);
            result.setZhishuContractId(contractInfo.getContractId());
            result.setContractNumber(contractInfo.getContractNumber());
            result.setContractName(firstNotBlank(contractInfo.getContractName(), request.getContractName()));
            if (contractInfo.getMultiUrl() != null) {
                result.setPcUrl(contractInfo.getMultiUrl().getPcUrl());
                result.setMobileUrl(contractInfo.getMultiUrl().getMobileUrl());
            }
            log.info("========== 创建反贿赂协议合同完成 ==========");
            return result;
        } catch (Exception e) {
            log.error("创建反贿赂协议合同异常: {}", e.getMessage(), e);
            return buildAntiBriberyContractFailResult("创建反贿赂协议合同异常: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class, noRollbackFor = BusinessException.class)
    public void syncContractFromZhishu(ContractSyncDTO dto) {
        String contractId = dto.getContractId();
        if(contractId==null||contractId.isEmpty()){
//            log.info("合同id为空！！！");
            return;
        }
        log.info("智书合同同步至业财系统: {}", JSON.toJSONString(dto));
        Map<String, Object> params = new HashMap<>();
        params.put("user_id_type", "user_id");
        ContractResponse contractInfo = zhishuContractClient.getContract(dto.getContractId(),params);
        ContractQueryResponse contractQueryInfo = null;
        if(contractInfo!=null){
            Map<String, Object> data = contractInfo.getData();
            contractQueryInfo = JSONObject.parseObject(String.valueOf(data.get("contract")), ContractQueryResponse.class);
        }else{
            log.info("回调接口：id = {}未查询到合同信息", contractId);
            return;
        }
//        contractQueryInfo.setContractStatusCode(9);
        Integer contractStatusCode = contractQueryInfo.getContractStatusCode();
        //只处理2：已撤回、3：审批中、4：已拒绝、9：已归档
        if(3!=contractStatusCode && 9!=contractStatusCode && 2!=contractStatusCode && 4!=contractStatusCode && 11!=contractStatusCode){
            log.info("当前状态不用同步合同信息：id = {} status = {}", contractId, contractStatusCode);
            return;
        }
        //获取form表单数据信息
        Map<String, Object> formData = getContractFormData(contractQueryInfo);

        log.info("开始组装向业财同步的合同信息......");
        ContSyncRequest request = makeContSyncRequest(contractQueryInfo,formData);
        request.setContractId(dto.getContractId());
        String detailUrl = contractQueryInfo.getMultiUrl().getPcUrl();
        if(detailUrl==null){
            detailUrl = zhishuApiClient.buildDetailPageUrl(dto.getContractId());
        }
        request.setContUrl(detailUrl);
        log.info("向业财同步合同组装后的请求对象：{}", JSON.toJSONString(request));

        YuecaiResponse response = yuecaiContractClient.syncContract(request);

        if (!isYuecaiContractSyncSuccess(response)) {
            String errorMessage = buildYuecaiContractSyncErrorMessage(response);
            saveSyncLog(dto.getContractId(), "SYNC", "ZHISHU_TO_YUECAI", "FAIL",
                    JSON.toJSONString(dto), JSON.toJSONString(response), errorMessage);
            throw new BusinessException("业财系统合同同步失败: " + errorMessage);
        }

        saveSyncLog(dto.getContractId(), "SYNC", "ZHISHU_TO_YUECAI", "SUCCESS",
                JSON.toJSONString(dto), JSON.toJSONString(response), null);

        String htType = contractQueryInfo.getParentContractCategoryAbbreviation();//合同类别-一级
        if("ZB".equals(htType) && 9==contractStatusCode){
            log.info("更新主播卡片---------------------------------------------开始");
            UpdateAnchorCardRequest updateAnchorCardRequest = makeAnchorCardInfo(contractQueryInfo,formData);
            log.info("组装后更新主播信息对象：{}",JSONObject.toJSONString(updateAnchorCardRequest));
            Map<String, Object> resultMap = yuecaiContractClient.updateAnchorCard(updateAnchorCardRequest);
            if(!resultMap.get("code").toString().equals("0")){
                log.info("主播信息更新失败：{}", JSON.toJSONString(resultMap.get("message")));
            }
            log.info("更新主播卡片---------------------------------------------结束");
        }

        log.info("智书合同同步至业财系统成功");
    }

    boolean isYuecaiContractSyncSuccess(YuecaiResponse response) {
        return response != null
                && Integer.valueOf(200).equals(response.getCode())
                && !Boolean.FALSE.equals(response.getSuccess());
    }

    String buildYuecaiContractSyncErrorMessage(YuecaiResponse response) {
        if (response == null) {
            return "业财API响应为空";
        }
        if (response.getMessage() != null && !response.getMessage().trim().isEmpty()) {
            return response.getMessage();
        }
        return "业财API返回失败：" + JSON.toJSONString(response);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateContractFromZhishu(ContractSyncDTO dto) {
        log.info("智书合同变更同步至业财系统: {}", JSON.toJSONString(dto));

        YuecaiUpdateContractRequest request = new YuecaiUpdateContractRequest();
        request.setContractId(dto.getContractId());
        request.setContractName(dto.getContractName());
        request.setContractType(dto.getContractType());
        request.setPartyA(dto.getPartyA());
        request.setPartyB(dto.getPartyB());
        request.setFormData(dto.getFormData());

        YuecaiResponse response = yuecaiContractClient.updateContract(dto.getContractId(), request);

        if (response == null || !Boolean.TRUE.equals(response.getSuccess())) {
            saveSyncLog(dto.getContractId(), "UPDATE", "ZHISHU_TO_YUECAI", "FAIL",
                    JSON.toJSONString(dto), null, response != null ? response.getMessage() : "业财API响应为空");
            throw new BusinessException("业财系统合同更新失败: " + (response != null ? response.getMessage() : "响应为空"));
        }

        saveSyncLog(dto.getContractId(), "UPDATE", "ZHISHU_TO_YUECAI", "SUCCESS",
                JSON.toJSONString(dto), JSON.toJSONString(response), null);

        log.info("智书合同变更同步至业财系统成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateContractFromYuecai(ContractSyncDTO dto) {
        log.info("业财系统合同变更同步至智书: {}", JSON.toJSONString(dto));

        UpdateContractRequest request = new UpdateContractRequest();
        request.setContractId(dto.getContractId());
        request.setContractName(dto.getContractName());
        request.setContractType(dto.getContractType());
        request.setPartyA(dto.getPartyA());
        request.setPartyB(dto.getPartyB());
        request.setFormData(dto.getFormData());

        ContractResponse response = zhishuContractClient.updateContract(dto.getContractId(), request);

        if (response == null || response.getCode() != 200) {
            saveSyncLog(dto.getContractId(), "UPDATE", "YUECAI_TO_ZHISHU", "FAIL",
                    JSON.toJSONString(dto), null, response != null ? response.getMessage() : "智书API响应为空");
            throw new BusinessException("智书合同更新失败: " + (response != null ? response.getMessage() : "响应为空"));
        }

        saveSyncLog(dto.getContractId(), "UPDATE", "YUECAI_TO_ZHISHU", "SUCCESS",
                JSON.toJSONString(dto), JSON.toJSONString(response), null);

        log.info("业财系统合同变更同步至智书成功");
    }

    @Override
    public Result receiptsList(ContractSyncDTO dto) {

        return null;
    }

    @Override
    public Map<String, Object> getContractFormData(ContractQueryResponse contractQueryInfo) {
        Map<String, Object> formData = new HashMap<>();
        if (contractQueryInfo == null || contractQueryInfo.getForm() == null || contractQueryInfo.getForm().trim().isEmpty()) {
            return formData;
        }

        String form = contractQueryInfo.getForm();
        List<ContractFormQueryResponse.FormAttribute> formAttributes = JSONObject.parseArray(form, ContractFormQueryResponse.FormAttribute.class);
        for (ContractFormQueryResponse.FormAttribute formAttribute : formAttributes) {
            if (formAttribute == null || formAttribute.getAttributeCode() == null) {
                continue;
            }
            String attributeType = formAttribute.getAttributeType();
            Object attributeValue = formAttribute.getAttributeValue();
            Object value;

            if (attributeType == null) {
                value = attributeValue;
            } else if(attributeType.equals(FormAttributeTypeEnum.SINGLELINE_TEXT.getCode())
                    ||attributeType.equals(FormAttributeTypeEnum.MULTILINE_TEXT.getCode())
                    ||attributeType.equals(FormAttributeTypeEnum.STRING.getCode())){
                value = attributeValue;
            }else if(attributeType.equals(FormAttributeTypeEnum.DATE.getCode())){
                value = attributeValue;
            }else if(attributeType.equals(FormAttributeTypeEnum.NUMBER.getCode())){
                value = attributeValue;
            } else if (attributeType.equals(FormAttributeTypeEnum.AMOUNT.getCode())) {
                value = getObjectValue(attributeValue, "amount");
            } else if (attributeType.equals(FormAttributeTypeEnum.DATE_RANGE.getCode())) {
                value = getDateRangeValue(attributeValue);
            } else if (attributeType.equals(FormAttributeTypeEnum.RADIO.getCode())) {
                value = getRadioNameValue(attributeValue);
            } else if (attributeType.equals(FormAttributeTypeEnum.DROPDOWN_RADIO.getCode())
                    || attributeType.equals(FormAttributeTypeEnum.TREE_RADIO.getCode())) {
                value = getObjectValue(attributeValue, "key");
            } else if (attributeType.equals(FormAttributeTypeEnum.CHECKBOX.getCode())
                    || attributeType.equals(FormAttributeTypeEnum.DROPDOWN_OPTION.getCode())
                    || attributeType.equals(FormAttributeTypeEnum.TREE_OPTION.getCode())) {
                value = getListOrObjectValue(attributeValue, "key");
            } else if (attributeType.equals(FormAttributeTypeEnum.EMPLOYEE.getCode())) {
                value = getListOrObjectValue(attributeValue, "user_id");
            } else if (attributeType.equals(FormAttributeTypeEnum.DEPARTMENT.getCode())) {
                value = getDepartmentValue(attributeValue);
            } else if (attributeType.equals(FormAttributeTypeEnum.HYPERLINK.getCode())) {
                value = getObjectValue(attributeValue, "url");
            } else if (attributeType.equals(FormAttributeTypeEnum.COUNTRY_OR_REGION.getCode())) {
                value = getListOrObjectValue(attributeValue, "country_code");
            } else if (attributeType.equals(FormAttributeTypeEnum.CALCULATION.getCode())) {
                value = attributeValue;
            } else if (attributeType.equals(FormAttributeTypeEnum.FILE.getCode())) {
                value = getListOrObjectValue(attributeValue, "file_id");
            } else if (attributeType.equals(FormAttributeTypeEnum.FEISHU_APPROVAL.getCode())) {
                value = getApprovalContentValue(attributeValue);
            } else if (attributeType.equals(FormAttributeTypeEnum.ARRAY.getCode())
                    || attributeType.equals(FormAttributeTypeEnum.COMMON_ARRAY.getCode())) {
                value = getArrayValue(attributeValue);
                addArrayRowsToFormData(formData, value);
            } else {
                value = attributeValue;
            }
            formData.put(formAttribute.getAttributeCode(), value);
            if (attributeType != null && attributeType.equals(FormAttributeTypeEnum.DROPDOWN_RADIO.getCode())) {
                formData.put(formAttribute.getAttributeCode() + "_name", getDropdownRadioNameValue(attributeValue));
            }
            if (attributeType != null && attributeType.equals(FormAttributeTypeEnum.DATE_RANGE.getCode())) {
                formData.put(formAttribute.getAttributeCode() + "_start_date", getDateRangePartValue(value, "start_date"));
                formData.put(formAttribute.getAttributeCode() + "_end_date", getDateRangePartValue(value, "end_date"));
            }
        }
        log.info("获取合同form表单信息：{}", JSON.toJSONString(formData));
        return formData;
    }

    @Override
    public Map<String, Object> calculateAmount(Map<String, Object> paramMap) {
        Map<String,Object> result = new HashMap<>();
        //合同id
        String contractId = (String) paramMap.get("contractId");
        ContractQueryResponse contractQueryInfo = getContractInfo(contractId);
        if(contractQueryInfo==null){
            log.info("获取订单信息时，查询合同信息：id = {}未查询到合同信息", contractId);
            result.put("code", 1);
            result.put("msg", "fail");
            return result;
        }
//        Map<String, Object> formData = getContractFormData(contractQueryInfo);
        //获取履约计划
        List<ContractQueryResponse.PaymentPlan> paymentPlanList = contractQueryInfo.getPaymentPlanList();
        BigDecimal payAmountBig = new BigDecimal(0);
        BigDecimal depositBig = new BigDecimal(0);
        if(paymentPlanList!=null && !paymentPlanList.isEmpty()){
            for (ContractQueryResponse.PaymentPlan paymentPlan : paymentPlanList) {
//                Double paymentAmount = paymentPlan.getPaymentAmount();//付款金额
                String paymentNature = null;
                JSONArray paymentArr = JSONArray.parseArray(paymentPlan.getPaymentCustomAttributes());
                for(int i = 0; i<paymentArr.size(); i++){
                    JSONObject payment = paymentArr.getJSONObject(i);
                    if("付款性质".equals(payment.getString("attribute_name"))){
                        paymentNature = payment.getJSONObject("attribute_value").getString("name");
                    }
                }

                BigDecimal paymentAmountBig = paymentPlan.getPaymentAmount();
                if("押金/保证金".equals(paymentNature)){
                    depositBig = depositBig.add(paymentAmountBig);
                }else if("一般付款".equals(paymentNature)){
                    payAmountBig = payAmountBig.add(paymentAmountBig);

                }
            }
        }
        result.put("code", 0);
        result.put("msg", "success");
        result.put("deposit", makeAmountData(depositBig,1));//押金/保证金
        result.put("payAmount", makeAmountData(payAmountBig,1));//合同总金额
        return result;
    }

    @Override
    public Map<String, Object> specCategoryMapping(Map<String, Object> paramMap) {
        Map<String,Object> result = new HashMap<>();
        //合同id
        String contractId = (String) paramMap.get("contractId");
        String specialCategory = (String) paramMap.get("specialCategory");
        ContractQueryResponse contractQueryInfo = getContractInfo(contractId);
        if(contractQueryInfo==null){
            log.info("获取订单信息时，查询合同信息：id = {}未查询到合同信息", contractId);
            return null;
        }
        result.put("code", 1);
        result.put("msg", "success");
        result.put("acceptedFlag", specCategoryMapping(specialCategory));
        return result;
    }

    @Override
    public Map<String, Object> specCategoryMapping(String specialCategory) {
        Map<String,Object> dataMap=new HashMap<>();
        List<Map<String,String>> selectList = new ArrayList<>();
        Map<String,String> selectMap=new HashMap<>();
        selectMap.put("label", "是");
        selectMap.put("value", "cmp0y2rse004e3b716tihbjhf");
        selectList.add(selectMap);
        selectMap=new HashMap<>();
        selectMap.put("label", "否");
        selectMap.put("value", "cmp0y2rse004f3b71rrqup04i");
        selectList.add(selectMap);
        //如果为取到值就给否
        dataMap.put("value",SpecializedCategoriesEnum.getByCode(specialCategory)==null?"cmp0y2rse004f3b71rrqup04i":SpecializedCategoriesEnum.getByCode(specialCategory).getYesOrNo());
        dataMap.put("options",selectList);
        return dataMap;
    }

    @Override
    public ContractCheckResultDTO submitCheck(Map<String, Object> paramMap) {
        ContractCheckResultDTO checkResultDTO = new ContractCheckResultDTO();
        checkResultDTO.setCode("0");
        checkResultDTO.setSuccess(true);
        String contractId = (String) paramMap.get("contractId");

        String acceptedFlag = (String) paramMap.get("acceptedFlag");//是否签署反贿赂协议
        String sumDeposit = paramMap.get("sumDeposit")==null||"".equals(paramMap.get("sumDeposit"))?"0":paramMap.get("sumDeposit").toString();//总押金/保证金
        if(!"0".equals(sumDeposit)){
            sumDeposit = JSONObject.parseObject(sumDeposit).getString("amount")==null?"0":JSONObject.parseObject(sumDeposit).getString("amount");
        }
        ContractQueryResponse contractQueryInfo = getContractInfo(contractId);
        Integer payTypeCode = contractQueryInfo.getPayTypeCode();
//        Double sumPayAmount = contractQueryInfo.getAmount();//合同总额
        BigDecimal sumDepositBig = new BigDecimal(sumDeposit);
        BigDecimal sumPayAmountBig = contractQueryInfo.getAmount();
        //获取履约计划
        List<ContractQueryResponse.PaymentPlan> paymentPlanList = contractQueryInfo.getPaymentPlanList();
        BigDecimal payAmountBig = new BigDecimal(0);//履约计划一般付款总金额
        BigDecimal depositBig = new BigDecimal(0);//履约计划押金/保证金总金额
        boolean isAcceptedExit = true;
        boolean isTimeExit = true;
        if(paymentPlanList!=null && !paymentPlanList.isEmpty()){
            for (ContractQueryResponse.PaymentPlan paymentPlan : paymentPlanList) {
//                Double paymentAmount = paymentPlan.getPaymentAmount();//付款金额
                BigDecimal paymentAmountBig = paymentPlan.getPaymentAmount();
                if(2==payTypeCode){//只有支出类型校验金额、押金保证金
                    JSONArray paymentArr = JSONArray.parseArray(paymentPlan.getPaymentCustomAttributes());
                    String paymentNature = null;
                    for(int i = 0; i<paymentArr.size(); i++){
                        JSONObject payment = paymentArr.getJSONObject(i);
                        if("付款性质".equals(payment.getString("attribute_name"))){
                            paymentNature = payment.getJSONObject("attribute_value").getString("name");
                        }
                    }
                    if("押金/保证金".equals(paymentNature)){
                        depositBig = depositBig.add(paymentAmountBig);
                    }else if("一般付款".equals(paymentNature)){
                        payAmountBig = payAmountBig.add(paymentAmountBig);
                    }
                }
                if(2==payTypeCode||3==payTypeCode){//只有支出类和有收有支类，需要校验
                    if("是".equals(acceptedFlag)||"cmp0y2rse004e3b716tihbjhf".equals(acceptedFlag)){//当合同基本信息中“是否需要验收”为“是”时
//                    JSONArray paymentPlanJson = JSONObject.parseArray(paymentPlan.getPaymentCustomAttributes());
//                    for (int i = 0; i < paymentPlanJson.size(); i++) {
//                        JSONObject paymentPlanObj = paymentPlanJson.getJSONObject(i);
//                        if("是否需要验收".equals(paymentPlanObj.getString("attribute_name"))){
//                            if("是".equals(paymentPlanObj.getJSONObject("attribute_value").getString("name"))){
//                                isAcceptedExit = false;
//                            }
//                        }
//                    }
                        String paymentIntervalDays = paymentPlan.getPaymentIntervalDays();
                        if(paymentIntervalDays!=null&&Integer.parseInt(paymentIntervalDays)>0){
                            isTimeExit = false;
                        }
                    }
                }
            }
        }
        if(2==payTypeCode) {//只有支出类型校验金额、押金保证金
            if (sumPayAmountBig.compareTo(payAmountBig) != 0) {//一般付款
                checkResultDTO.setCode("500");
                checkResultDTO.setSuccess(false);
                checkResultDTO.setErrorMessage("合同总额:"+sumPayAmountBig+"与履约计划中一般付款总金额:"+payAmountBig+"不相等，请确认！");
                return checkResultDTO;
            }
            if (sumDepositBig.compareTo(depositBig) != 0) {//押金/保证金
                checkResultDTO.setCode("500");
                checkResultDTO.setSuccess(false);
                checkResultDTO.setErrorMessage("押金/保证金:"+sumDepositBig+"与履约计划中押金/保证金总和"+depositBig+"不相等，请确认！");
                return checkResultDTO;
            }
        }

        if(2==payTypeCode||3==payTypeCode) {//只有支出类和有收有支类，需要校验
            if("是".equals(acceptedFlag)||"cmp0y2rse004e3b716tihbjhf".equals(acceptedFlag)){//当合同基本信息中“是否需要验收”为“是”时
//            if(isAcceptedExit){
//                checkResultDTO.setCode("500");
//                checkResultDTO.setSuccess(false);
//                checkResultDTO.setErrorMessage("“付款计划”中至少有一行的“是否需要验收”为“是”");
//                return checkResultDTO;
//            }

                if(isTimeExit){
                    checkResultDTO.setCode("500");
                    checkResultDTO.setSuccess(false);
                    checkResultDTO.setErrorMessage("“付款计划”中至少有一行的“付款时间”为“履约事项完成后 X 日”");
                    return checkResultDTO;
                }
            }
        }

        return checkResultDTO;
    }

    @Override
    public ContractCheckResultDTO checkOppositeSide(Map<String, Object> paramMap) {
        ContractCheckResultDTO checkResultDTO = new ContractCheckResultDTO();

        String contractId = (String) paramMap.get("contractId");
        String type = (String) paramMap.get("type");
        Object oppositeSide = paramMap.get("oppositeSide");//交易方信息
        log.info("被校验交易方信息= {}",JSON.toJSONString(oppositeSide));
        JSONArray oSideArr = JSONArray.parseArray(JSON.toJSONString(oppositeSide));
        String vendorName = "";
        String vendorType = "";
        for (Object oSide : oSideArr) {
            JSONObject oSideObj = JSONObject.parseObject(String.valueOf(oSide));
            String tradingPartySourceId = oSideObj.getString("tradingPartySourceId");
            //获取交易方信息
            VendorInfoResponse vendorInfo = zhiShuVendorClient.getVendorV2(tradingPartySourceId);
            vendorType = vendorInfo.getVendorType();
            String vendorNature = vendorInfo.getVendorNature();
            vendorName = vendorInfo.getVendorText();
            String country = vendorInfo.getAdCountry();
            log.info("智书交易方信息= {}",JSON.toJSONString(vendorInfo));
            Map<String,String> extendMap = new HashMap<>();
            List<VendorInfoResponse.ExtendInfo> extendInfos = vendorInfo.getExtendInfo();
            for (VendorInfoResponse.ExtendInfo extendField : extendInfos) {
                extendMap.put(extendField.getFieldCode(), extendField.getFieldValue());
            }
            String isSign = extendMap.get("VBI00100001");//是否已签署反贿赂协议
            String isVendor = extendMap.get("VBI00102002");//是否外部供应商

            if (!"CN".equals(country)||"是".equals(isSign)||"否".equals(isVendor)||!"0".equals(vendorNature)||!"2".equals(vendorType)){
                checkResultDTO.setCode("0");
                checkResultDTO.setSuccess(true);
                return checkResultDTO;
            }
        }
//        ContractQueryResponse contractQueryInfo = getContractInfo(contractId);
//        Double sumPayAmount = contractQueryInfo.getAmount();//合同总额
        if(vendorType.contains("2")){//包含供应商信息校验反贿赂协议是否签署
            checkResultDTO.setCode("500");
            checkResultDTO.setSuccess(false);
            if (type.equals("guidang")){
                checkResultDTO.setErrorMessage("此合同供应商["+vendorName+"]，未完成反贿赂协议签署。此时不允许归档操作。");
            }else {
                checkResultDTO.setErrorMessage("此合同供应商【"+vendorName+"】,未完成反贿赂协议签署。请确认是否提交合同？");
            }
            return checkResultDTO;
        }else{
            checkResultDTO.setCode("0");
            checkResultDTO.setSuccess(true);
            return checkResultDTO;
        }
    }

    @Override
    public Map<String,Object> getTaxData(Map<String, Object> paramMap) throws Exception {
        Map<String,Object> result = new HashMap<>();

        //合同id
        String contractId = (String) paramMap.get("contractId");
        Map<String, Object> params = new HashMap<>();
        params.put("user_id_type", "user_id");
        ContractResponse contractInfo = zhishuContractClient.getContract(contractId, params);
        ContractQueryResponse contractQueryInfo = null;
        if(contractInfo!=null){
            Map<String, Object> data = contractInfo.getData();
            contractQueryInfo = JSONObject.parseObject(String.valueOf(data.get("contract")), ContractQueryResponse.class);
        }else{
            log.info("获取采购申请详情时，查询合同信息：id = {}未查询到合同信息", contractId);
            result.put("code", 1);
            result.put("msg", "fail");
            return result;
        }

        Long itemType = contractQueryInfo.getTemplateNumber()!=null?Long.parseLong(contractQueryInfo.getTemplateNumber()):0L;

        String itemName = paramMap.get("itemName")==null?"":paramMap.get("itemName").toString();
        Map<String, Set<Object>> taxDataMap = getTaxData(itemType, itemName);
        Set<Object> taxData = new HashSet<>();
        Set<Object> receiptSet = new HashSet<>();
        Set<Object> paymentSet = new HashSet<>();
        if(taxDataMap.get("itemName")!=null){//税目
            taxData = taxDataMap.get("itemName");
            Map<String,Object> optionsResult = new HashMap<>();
            List<Map<String,String>> optionsList = new ArrayList<>();
            for (Object taxDatum : taxData) {
                Map<String,String> optionsMap = new HashMap<>();
                String taxDatumStr = taxDatum.toString();
                optionsMap.put("value", taxDatumStr);
                optionsMap.put("label", taxDatumStr);
                optionsList.add(optionsMap);
            }
            optionsResult.put("value", "");
            optionsResult.put("options", optionsList);
            result.put("optionsResult",optionsResult);
        } else {//收入支出税率
            //收入税率
            receiptSet = taxDataMap.get("receiptSet");
            Map<String,Object> optionsResult = new HashMap<>();
            List<Map<String,String>> optionsList = new ArrayList<>();
            for (Object taxDatum : receiptSet) {
                if(taxDatum==null){
                    break;
                }
                Map<String,String> optionsMap = new HashMap<>();
                String taxDatumStr = taxDatum.toString();
                BigDecimal taxDatumBig = new BigDecimal(taxDatumStr).multiply(new BigDecimal(100));
                taxDatumStr = taxDatumBig + "%";
                optionsMap.put("value", taxDatumStr);
                optionsMap.put("label", taxDatumStr);
                optionsList.add(optionsMap);
            }
            optionsResult.put("value", "");
            optionsResult.put("options", optionsList);
            result.put("receiptResult",optionsResult);
            //支出税率
            paymentSet = taxDataMap.get("paymentSet");
            optionsResult = new HashMap<>();
            optionsList = new ArrayList<>();
            for (Object taxDatum : paymentSet) {
                if(taxDatum==null){
                    break;
                }
                Map<String,String> optionsMap = new HashMap<>();
                String taxDatumStr = taxDatum.toString();
                BigDecimal taxDatumBig = new BigDecimal(taxDatumStr).multiply(new BigDecimal(100));
                taxDatumStr = taxDatumBig + "%";
                optionsMap.put("value", taxDatumStr);
                optionsMap.put("label", taxDatumStr);
                optionsList.add(optionsMap);
            }
            optionsResult.put("value", "");
            optionsResult.put("options", optionsList);
            result.put("paymentResult",optionsResult);
        }

        result.put("code", 0);
        result.put("msg", "success");
        return result;
    }

    /**
     * 获取多维表格税目税率
     *
     * @param type     合同类型
     * @param itemName 税目
     * @return
     * @throws Exception
     */
    public Map<String, Set<Object>> getTaxData(Long type, String itemName) throws Exception {
        Map<String, Set<Object>> resultMap = new HashMap<>();
        JSONObject jsonObject = new JSONObject();
        if (itemName.isEmpty()) {
            if(0L==type){
                jsonObject = null;
            }else{
                jsonObject
                        .fluentPut("conjunction", "and")
                        .fluentPut("conditions", new JSONArray().fluentAdd(new JSONObject()
                                .fluentPut("fieldName", "模板编码")
                                .fluentPut("operator", "is")
                                .fluentPut("value", new JSONArray().fluentAdd(type))
                        ))
                ;
            }

            AppTableRecord[] recs = feishuBitableClient.searchAppTableRecordSample(jsonObject, feiShuBitableConfig.getAppToken(), feiShuBitableConfig.getTaxTableId(), 500);
            if(recs.length==0){//如果没查询到就查询全部税目
                recs = feishuBitableClient.searchAppTableRecordSample(null, feiShuBitableConfig.getAppToken(), feiShuBitableConfig.getTaxTableId(), 500);
            }
            log.info("获取到的模板税目信息：{}",JSONObject.toJSONString(recs));
            Set<Object> set = new HashSet<>();
            for (AppTableRecord rec : recs) {
                if(rec.getFields().containsKey("税目")){
                    Object str = ((List<Map>)rec.getFields().get("税目")).get(0).get("text");
                    set.add(str);
                }
            }
            resultMap.put("itemName", set);
            return resultMap;
        } else {
            if(0L==type){
                jsonObject
                        .fluentPut("conjunction", "and")
                        .fluentPut("conditions", new JSONArray().
                                fluentAdd(new JSONObject()
                                        .fluentPut("fieldName", "税目")
                                        .fluentPut("operator", "is")
                                        .fluentPut("value", new JSONArray().fluentAdd(itemName)))
                        )
                ;
            }else{
                jsonObject
                        .fluentPut("conjunction", "and")
                        .fluentPut("conditions", new JSONArray().fluentAdd(new JSONObject()
                                        .fluentPut("fieldName", "模板编码")
                                        .fluentPut("operator", "is")
                                        .fluentPut("value", new JSONArray().fluentAdd(type))
                                ).fluentAdd(new JSONObject()
                                        .fluentPut("fieldName", "税目")
                                        .fluentPut("operator", "is")
                                        .fluentPut("value", new JSONArray().fluentAdd(itemName)))
                        )
                ;
            }
            AppTableRecord[] recs = feishuBitableClient.searchAppTableRecordSample(jsonObject, feiShuBitableConfig.getAppToken(), feiShuBitableConfig.getTaxTableId(), 500);
            if(recs.length==0){
                jsonObject
                        .fluentPut("conjunction", "and")
                        .fluentPut("conditions", new JSONArray().
                                fluentAdd(new JSONObject()
                                        .fluentPut("fieldName", "税目")
                                        .fluentPut("operator", "is")
                                        .fluentPut("value", new JSONArray().fluentAdd(itemName)))
                        )
                ;
                recs = feishuBitableClient.searchAppTableRecordSample(jsonObject, feiShuBitableConfig.getAppToken(), feiShuBitableConfig.getTaxTableId(), 500);
            }
            log.info("获取到的模板税率信息：{}",JSONObject.toJSONString(recs));
            Set<Object> receiptSet = new HashSet<>();
            Set<Object> paymentSet = new HashSet<>();
            for (AppTableRecord rec : recs) {
                if(rec.getFields().containsKey("收入税率")){
                    Object receiptStr = rec.getFields().get("收入税率");
                    Object paymentStr = rec.getFields().get("支出税率");
                    receiptSet.add(receiptStr);
                    paymentSet.add(paymentStr);

                }
            }
            resultMap.put("receiptSet", receiptSet);
            resultMap.put("paymentSet", paymentSet);
            return resultMap;
        }
    }

    private ZhishuCreateContractRequest buildAntiBriberyContractRequest(QueryAllVendorResponse.Item item,
                                                                        String createUserId,
                                                                        String templateInstanceId) {
        String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        String counterPartyCode = trimToNull(item.getVendor());
        String counterPartyName = firstNotBlank(item.getVendorText(), item.getShortText(), counterPartyCode);

        ZhishuCreateContractRequest request = new ZhishuCreateContractRequest();
        request.setCreateUserId(createUserId);
        request.setTemplateInstanceId(templateInstanceId);
        request.setContractName("反贿赂协议-" + counterPartyName);
        request.setContractCategoryAbbreviation("DEFAULT");
        request.setContractStatusCode("0");
        request.setBusinessTypeCode(0);
        // TODO 确认反贿赂协议无金额合同的收支类型、计价方式、币种和金额口径
        request.setPayTypeCode(4);
        request.setPropertyTypeCode(2);
        request.setAmount(BigDecimal.ZERO);
        request.setCurrencyCode("CNY");
        // TODO 确认反贿赂协议合同有效期口径
        request.setFixedValidityCode(1);
        request.setStartDate(today);
        request.setEndDate(today);
        request.setOurPartyList(buildAntiBriberyOurPartyList(null));
        request.setCounterPartyList(Collections.singletonList(buildAntiBriberyCounterParty(item)));
        return request;
    }

    private List<ZhishuCreateContractRequest.OurPartyInfo> buildAntiBriberyOurPartyList(String ourPartyCode) {
        ZhishuCreateContractRequest.OurPartyInfo ourParty = new ZhishuCreateContractRequest.OurPartyInfo();
        // TODO 确认反贿赂协议合同我方主体编码
        if(ourPartyCode!=null){
            ourParty.setOurPartyCode("L00100001");
        }
        ZhishuCreateContractRequest.SignInfoResource signInfoResource = new ZhishuCreateContractRequest.SignInfoResource();
        signInfoResource.setEnable(false);
        ourParty.setOurPartySignInfoResource(signInfoResource);
        return Collections.singletonList(ourParty);
    }

    private ZhishuCreateContractRequest.CounterPartyInfo buildAntiBriberyCounterParty(QueryAllVendorResponse.Item item) {
        ZhishuCreateContractRequest.CounterPartyInfo counterParty = new ZhishuCreateContractRequest.CounterPartyInfo();
        counterParty.setCounterPartyCode(item.getVendor());
        ZhishuCreateContractRequest.SignInfoResource signInfoResource = new ZhishuCreateContractRequest.SignInfoResource();
        signInfoResource.setEnable(false);
        counterParty.setCounterPartySignInfoResource(signInfoResource);
        counterParty.setBusinessAddressInfo(buildAntiBriberyBusinessAddressInfo(item));
        counterParty.setBankAccountInfo(buildAntiBriberyBankAccountInfo(item));
        counterParty.setContactInfo(buildAntiBriberyContactInfo(item));
        return counterParty;
    }

    private ZhishuCreateContractRequest.AddressInfo buildAntiBriberyBusinessAddressInfo(QueryAllVendorResponse.Item item) {
        if (item.getVendorAddresses() != null && !item.getVendorAddresses().isEmpty()) {
            QueryAllVendorResponse.VendorAddress address = item.getVendorAddresses().get(0);
            ZhishuCreateContractRequest.AddressInfo addressInfo = new ZhishuCreateContractRequest.AddressInfo();
            addressInfo.setId(trimToNull(address.getId()));
            addressInfo.setValue(firstNotBlank(address.getAddress(),
                    joinNonBlank(address.getCountry(), address.getProvince(), address.getCity(),
                            address.getCounty(), address.getAddress())));
            return hasAddressInfoValue(addressInfo) ? addressInfo : null;
        }
        String addressValue = firstNotBlank(item.getAddress(),
                joinNonBlank(item.getAdCountry(), item.getAdProvince(), item.getAdCity(), item.getAddress()));
        if (addressValue == null) {
            return null;
        }
        ZhishuCreateContractRequest.AddressInfo addressInfo = new ZhishuCreateContractRequest.AddressInfo();
        addressInfo.setValue(addressValue);
        return addressInfo;
    }

    private ZhishuCreateContractRequest.BankAccountInfo buildAntiBriberyBankAccountInfo(QueryAllVendorResponse.Item item) {
        if (item.getVendorAccounts() == null || item.getVendorAccounts().isEmpty()) {
            return null;
        }
        QueryAllVendorResponse.VendorAccount account = item.getVendorAccounts().get(0);
        ZhishuCreateContractRequest.BankAccountInfo bankAccountInfo = new ZhishuCreateContractRequest.BankAccountInfo();
        bankAccountInfo.setId(trimToNull(account.getId()));
        bankAccountInfo.setValue(firstNotBlank(account.getAccount(), account.getAccountName(), account.getBankName()));
        return hasBankAccountInfoValue(bankAccountInfo) ? bankAccountInfo : null;
    }

    private ZhishuCreateContractRequest.ContactInfo buildAntiBriberyContactInfo(QueryAllVendorResponse.Item item) {
        if (item.getVendorContacts() != null && !item.getVendorContacts().isEmpty()) {
            QueryAllVendorResponse.VendorContact contact = item.getVendorContacts().get(0);
            ZhishuCreateContractRequest.ContactInfo contactInfo = new ZhishuCreateContractRequest.ContactInfo();
            contactInfo.setId(trimToNull(contact.getId()));
            contactInfo.setValue(firstNotBlank(contact.getName(), contact.getPhone(), contact.getEmail()));
            return hasContactInfoValue(contactInfo) ? contactInfo : null;
        }
        String contactValue = firstNotBlank(item.getContactPerson(), item.getContactMobilePhone(), item.getContactTelephone());
        if (contactValue == null) {
            return null;
        }
        ZhishuCreateContractRequest.ContactInfo contactInfo = new ZhishuCreateContractRequest.ContactInfo();
        contactInfo.setValue(contactValue);
        return contactInfo;
    }

    private boolean hasAddressInfoValue(ZhishuCreateContractRequest.AddressInfo addressInfo) {
        return addressInfo != null && (trimToNull(addressInfo.getId()) != null || trimToNull(addressInfo.getValue()) != null);
    }

    private boolean hasBankAccountInfoValue(ZhishuCreateContractRequest.BankAccountInfo bankAccountInfo) {
        return bankAccountInfo != null && (trimToNull(bankAccountInfo.getId()) != null || trimToNull(bankAccountInfo.getValue()) != null);
    }

    private boolean hasContactInfoValue(ZhishuCreateContractRequest.ContactInfo contactInfo) {
        return contactInfo != null && (trimToNull(contactInfo.getId()) != null || trimToNull(contactInfo.getValue()) != null);
    }

    private CreateAntiBriberyContractResultDTO buildAntiBriberyContractFailResult(String errMessage) {
        log.warn("反贿赂协议合同创建失败: {}", errMessage);
        CreateAntiBriberyContractResultDTO result = new CreateAntiBriberyContractResultDTO();
        result.setSuccess(false);
        result.setErrMessage(errMessage);
        return result;
    }

    private String firstNotBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String trimValue = trimToNull(value);
            if (trimValue != null) {
                return trimValue;
            }
        }
        return null;
    }

    private String joinNonBlank(String... values) {
        StringBuilder builder = new StringBuilder();
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String trimValue = trimToNull(value);
            if (trimValue != null) {
                builder.append(trimValue);
            }
        }
        return builder.length() == 0 ? null : builder.toString();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimValue = value.trim();
        return trimValue.isEmpty() ? null : trimValue;
    }

    private ZhishuCreateContractRequest buildZhishuRequest(CreateContractDTO dto) {
        ZhishuCreateContractRequest request = new ZhishuCreateContractRequest();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String nowDate = sdf.format(new Date());
        String userId = dto.getCreateUserId();
        request.setAmount(new BigDecimal(0));
        request.setContractName("合同-" + dto.getDocumentNumber());
        request.setContractCategoryAbbreviation("DEFAULT");
        request.setCreateUserId(userId);
        request.setContractStatusCode("0");
        request.setSourceId(dto.getDocumentNumber());
        String templateId = dto.getTemplateId();
        ContractFormCreatResponse contractFormCreatResponse = new ContractFormCreatResponse();
        List<ContractFormCreatResponse.FormAttribute> formAttributes = new ArrayList<>();
        Map<String, Object> documentInfo = new HashMap<>();//单据信息
        Map<String,Object> documentParams = new HashMap<>();
        List<ZhishuCreateContractRequest.CounterPartyInfo> counterPartyList = new ArrayList<>();
        documentParams.put("page", 0);
        documentParams.put("size", 1);
        if(dto.getDocumentType()==1){//采购申请
            Map<String,Object> specializedCategoryMap = new HashMap<>();
            List<Map<String,Object>> options = new ArrayList<>();
            Double procurementAmount = 0.0;
            String specializedCategory = null;
            Map<String,Object> optionsMap = new HashMap<>();
            if(templateId==null||templateId.isEmpty()){
                templateId = yeCaiDataConfig.getTemplateProcurement();
            }
            MasterDataRes masterDataRes = yuecaiContractClient.getProcurement(documentParams,dto.getDocumentNumber());
            if(masterDataRes!=null&&masterDataRes.getContent()!=null && !masterDataRes.getContent().isEmpty()){
                ContractFormCreatResponse contractFormCreat = new ContractFormCreatResponse();
                List<Object> content = masterDataRes.getContent();
                ProcurementResponse documentData = JSONObject.parseObject(content.get(0).toString(), ProcurementResponse.class);
                //TODO 合同需要前置单据相关信息，从这里获取存入documentInfo信息中
                List<ProcurementResponse.ProcurementLineRes> lineResultDTOS = documentData.getLineResultDTOS();
                if(!lineResultDTOS.isEmpty()){
                    for (ProcurementResponse.ProcurementLineRes lineResultDTO : lineResultDTOS) {
                        if (lineResultDTO.getAttribute31() != null) {
                            procurementAmount += lineResultDTO.getAttribute31();
                        }
                        specializedCategory = lineResultDTO.getSpecializedCategory();
                        if(SpecializedCategoriesEnum.getByName(specializedCategory)!=null){
//                            Map<String, Object> acceptedFlagMap = specCategoryMapping(SpecializedCategoriesEnum.getByName(specializedCategory).getCode());
                            addContractFormAttribute(contractFormCreatResponse,ZhishuAndYecaiFiledEnum.ACCEPTANCE_REQUIRED.getZhishuFiled(),FormAttributeTypeEnum.RADIO.getCode(),SpecializedCategoriesEnum.getByName(specializedCategory).getYesOrNo()==null?"cmp0y2rse004f3b71rrqup04i":SpecializedCategoriesEnum.getByName(specializedCategory).getYesOrNo());//是否需要检验
                        }
                    }
                    addContractFormAttribute(contractFormCreatResponse,ZhishuAndYecaiFiledEnum.SPECIAL_CATEGORY.getZhishuFiled(),FormAttributeTypeEnum.DROPDOWN_RADIO.getCode(),specializedCategory);//专项品类
                    addContractFormAttribute(contractFormCreatResponse,ZhishuAndYecaiFiledEnum.CAST_AMOUNT.getZhishuFiled(),FormAttributeTypeEnum.AMOUNT.getCode(),procurementAmount);//采购金额
                    request.setForm(JSON.toJSONString(contractFormCreatResponse.getForm()));
                }
            }else{
                log.info("未查询到采购申请，编号为：DocumentNumber = {}", dto.getDocumentNumber());
                return null;
            }
        }else if(dto.getDocumentType()==2){//订单信息
            if(templateId==null||templateId.isEmpty()){
                templateId = yeCaiDataConfig.getTemplateOrder();
            }
            documentParams.put("prjDimOrderValue", dto.getDocumentNumber());
            documentParams.put("dataType", "ORDER");
            documentParams.put("startTime", URLUtil.encode(yeCaiDataConfig.getStartTime()));
            MasterDataRes masterDataRes = yuecaiContractClient.getOrderInfo(documentParams);
            if(masterDataRes!=null&&masterDataRes.getContent()!=null && !masterDataRes.getContent().isEmpty()){
                List<Object> content = masterDataRes.getContent();
                OrderInfoResponse documentData = JSONObject.parseObject(content.get(0).toString(), OrderInfoResponse.class);
                //TODO 合同需要前置单据相关信息，从这里获取存入documentInfo信息中
                ContractFormCreatResponse contractFormCreat = new ContractFormCreatResponse();
                addContractFormAttribute(contractFormCreat,ZhishuAndYecaiFiledEnum.ORDERHT_DOCUMENT_NUMBER.getZhishuFiled(),FormAttributeTypeEnum.SINGLELINE_TEXT.getCode(),documentData.getOrderHeaderId());
                addContractFormAttribute(contractFormCreat,ZhishuAndYecaiFiledEnum.ORDERHT_DOCUMENT_NAME.getZhishuFiled(),FormAttributeTypeEnum.SINGLELINE_TEXT.getCode(),documentData.getOrderTitle());
                addContractFormAttribute(contractFormCreat,ZhishuAndYecaiFiledEnum.ORDERHT_COST_CENTER.getZhishuFiled(),FormAttributeTypeEnum.DROPDOWN_OPTION.getCode(),documentData.getCostCenter());
                Date orderStartDate = documentData.getOrderStartDate();
                Date orderEndDate = documentData.getOrderEndDate();
                JSONObject orderDate = new JSONObject();
                orderDate.put("startDate", orderStartDate);
                orderDate.put("endDate", orderEndDate);
                addContractFormAttribute(contractFormCreat,ZhishuAndYecaiFiledEnum.ORDERHT_ORDER_DATE.getZhishuFiled(),FormAttributeTypeEnum.DATE_RANGE.getCode(),orderDate);
                addContractFormAttribute(contractFormCreat,ZhishuAndYecaiFiledEnum.ORDERHT_ORDER_TYPE.getZhishuFiled(),FormAttributeTypeEnum.DROPDOWN_RADIO.getCode(),documentData.getOrderType());
                List<OrderInfoResponse.Member> memberList = documentData.getMemberList();
                List<String> project_managerList = new ArrayList<>();
                List<String> expense_groupList = new ArrayList<>();
                List<String> project_acceptanceList = new ArrayList<>();
                List<String> project_budgetList = new ArrayList<>();
                List<String> project_sponosorList = new ArrayList<>();
                if(memberList!=null&& !memberList.isEmpty()){
                    for (OrderInfoResponse.Member member : memberList) {
                        String roleCode = member.getRoleCode();
                        String memberUserId = member.getUserId();
                        if(memberUserId!=null&&!memberUserId.isEmpty()){//订单信息的userId不为空，则查询用户信息
                            FeishuUserInfoResponse userInfo = feiShuApiClient.getUserInfo(member.getUserId());
                            if(userInfo!=null){
                                Boolean resigned = userInfo.getUser().getStatus().getResigned();
                                if(!resigned){
                                    if("10".equals(roleCode)){//项目经理A
                                        project_managerList.add(memberUserId);
                                    }else if("40".equals(roleCode)){
                                        expense_groupList.add(memberUserId);
                                    }else if("50".equals(roleCode)){
                                        project_acceptanceList.add(memberUserId);
                                    }else if("60".equals(roleCode)){
                                        project_budgetList.add(memberUserId);
                                    }else if("30".equals(roleCode)){//项目经理B add by lidongliang 20260702
                                        project_sponosorList.add(memberUserId);
                                    }
                                }else{
                                    log.info("前置单据-采购订单用户已离职，resigned状态：{}",resigned);
                                }
                            }else{
                                log.info("前置单据-采购订单未获取到用户信息：{}",member.getUserId());
                            }
                        }else{
                            log.info("前置单据-采购订单返回的用户信息userId为空");
                        }
                    }

                }
                if(!project_managerList.isEmpty()){//项目经理
                    addContractFormAttribute(contractFormCreat,ZhishuAndYecaiFiledEnum.ORDERHT_ORDER_TYPE.getZhishuFiled(),FormAttributeTypeEnum.EMPLOYEE.getCode(),project_managerList);
                }
                if(!expense_groupList.isEmpty()){//日常费用组
                    addContractFormAttribute(contractFormCreat,ZhishuAndYecaiFiledEnum.ORDERHT_ORDER_TYPE.getZhishuFiled(),FormAttributeTypeEnum.EMPLOYEE.getCode(),expense_groupList);
                }
                if(!project_acceptanceList.isEmpty()){//项目验收岗
                    addContractFormAttribute(contractFormCreat,ZhishuAndYecaiFiledEnum.ORDERHT_ORDER_TYPE.getZhishuFiled(),FormAttributeTypeEnum.EMPLOYEE.getCode(),project_acceptanceList);
                }
                if(!project_budgetList.isEmpty()){//项目预算岗
                    addContractFormAttribute(contractFormCreat,ZhishuAndYecaiFiledEnum.ORDERHT_ORDER_TYPE.getZhishuFiled(),FormAttributeTypeEnum.EMPLOYEE.getCode(),project_budgetList);
                }
                if(!project_sponosorList.isEmpty()){//项目Sponsor
                    addContractFormAttribute(contractFormCreat,ZhishuAndYecaiFiledEnum.ORDERHT_PROJECT_SPONOSOR.getZhishuFiled(),FormAttributeTypeEnum.EMPLOYEE.getCode(),project_sponosorList);
                }

                addContractFormAttribute(contractFormCreatResponse,ZhishuAndYecaiFiledEnum.ORDERHT_DOCUMENT_INFO.getZhishuFiled(),FormAttributeTypeEnum.COMMON_ARRAY.getCode(),contractFormCreat.getForm());
                request.setForm(JSON.toJSONString(contractFormCreatResponse.getForm()));
            }else{
                log.info("未查询到订单信息，订单编号为：DocumentNumber = {}", dto.getDocumentNumber());
                return null;
            }
        }else if(dto.getDocumentType()==3){//主播卡片
            if(templateId==null||templateId.isEmpty()){
                templateId = yeCaiDataConfig.getTemplateAnchor();
            }
            MasterDataRes masterDataRes = yuecaiContractClient.getAnchorCard(documentParams,dto.getDocumentNumber(),"id");
            if(masterDataRes!=null&&masterDataRes.getContent()!=null && !masterDataRes.getContent().isEmpty()){
                List<Object> content = masterDataRes.getContent();
                AnchorCardResponse documentData = JSONObject.parseObject(content.get(0).toString(), AnchorCardResponse.class);
                //TODO 合同需要前置单据相关信息，从这里获取存入documentInfo信息中
                //设置前置单据
                ContractFormCreatResponse.FormAttribute formAttribute = new ContractFormCreatResponse.FormAttribute();
                formAttribute.setAttributeCode(ZhishuAndYecaiFiledEnum.ANCHOR_DOCUMENT_NUMBER.getZhishuFiled());
                formAttribute.setAttributeKey(ZhishuAndYecaiFiledEnum.ANCHOR_DOCUMENT_NUMBER.getZhishuFiled());
                formAttribute.setModuleName("相关单据");
                formAttribute.setAttributeName(ZhishuAndYecaiFiledEnum.ANCHOR_DOCUMENT_NUMBER.getName());
                formAttribute.setAttributeType(FormAttributeTypeEnum.FEISHU_APPROVAL.getCode());
                formAttribute.setApprovalType(FormAttributeTypeEnum.THIRD_PARTY_APPROVAL.getCode());//第三方单据
                PrecedingDocResponse.Receipts receipts = new PrecedingDocResponse.Receipts();
                receipts.setId(String.valueOf(documentData.getHeaderId()));
                receipts.setTitle(documentData.getRealName());
                receipts.setContent(documentData.getId());
                receipts.setPcAppLink("http://link.heroesports.com/hfbs/anchor-doc/document");
                receipts.setMobileAppLink("http://link.heroesports.com/hfbs/anchor-doc/document");
                formAttribute.setAttributeValue(Arrays.asList(receipts));
                formAttributes.add(formAttribute);
                //设置主播姓名
                addContractFormAttribute(contractFormCreatResponse,ZhishuAndYecaiFiledEnum.ANCHOR_NAME.getZhishuFiled(),FormAttributeTypeEnum.SINGLELINE_TEXT.getCode(),documentData.getRealName());
                List<AnchorCardResponse.AnchorCardLineRes> lineResultDTOS = documentData.getLineResultDTOS();
                if(lineResultDTOS!=null&& !lineResultDTOS.isEmpty()){
                    AnchorCardResponse.AnchorCardLineRes anchorCardLineRes = lineResultDTOS.get(0);
                    for (AnchorCardResponse.AnchorCardLineRes lineResultDTO : lineResultDTOS) {
                        if(Objects.equals(lineResultDTO.getId(), dto.getAnchorLineId())){
                            anchorCardLineRes = lineResultDTO;
                        }
                    }

                    //主播昵称
                    addContractFormAttribute(contractFormCreatResponse,ZhishuAndYecaiFiledEnum.ANCHOR_NICKNAME.getZhishuFiled(),FormAttributeTypeEnum.SINGLELINE_TEXT.getCode(),anchorCardLineRes.getAnchorNickname());
                    //房间号主播id
                    addContractFormAttribute(contractFormCreatResponse,ZhishuAndYecaiFiledEnum.ANCHOR_ROOM_ID.getZhishuFiled(),FormAttributeTypeEnum.SINGLELINE_TEXT.getCode(),anchorCardLineRes.getAnchorId());
                    //战队名称
                    addContractFormAttribute(contractFormCreatResponse,ZhishuAndYecaiFiledEnum.ANCHOR_TEAM_NAME.getZhishuFiled(),FormAttributeTypeEnum.SINGLELINE_TEXT.getCode(),anchorCardLineRes.getTeamName());
                    //所属平台
                    addContractFormAttribute(contractFormCreatResponse,ZhishuAndYecaiFiledEnum.PLATFORM.getZhishuFiled(),FormAttributeTypeEnum.DROPDOWN_RADIO.getCode(),anchorCardLineRes.getPlatform());
                    //直播品类
                    addContractFormAttribute(contractFormCreatResponse,ZhishuAndYecaiFiledEnum.LIVE_CATEGORY.getZhishuFiled(),FormAttributeTypeEnum.DROPDOWN_RADIO.getCode(),anchorCardLineRes.getLiveCategory());
                }
                //主播身份证号
                addContractFormAttribute(contractFormCreatResponse,ZhishuAndYecaiFiledEnum.ANCHOR_ID_CARD.getZhishuFiled(),FormAttributeTypeEnum.SINGLELINE_TEXT.getCode(),documentData.getCertificateNumber());
                //获取直播对应交易方信息
                VendorInfoResponse vendorInfoResponse = zhiShuVendorClient.getVendorByCertificationId(documentData.getCertificateNumber());
                if(vendorInfoResponse!=null){
//                    List<ZhishuCreateContractRequest.CounterPartyInfo> counterPartyList = new ArrayList<>();
                    ZhishuCreateContractRequest.CounterPartyInfo counterPartyInfo = new ZhishuCreateContractRequest.CounterPartyInfo();
                    counterPartyInfo.setCounterPartyCode(vendorInfoResponse.getVendor());
                    ZhishuCreateContractRequest.SignInfoResource counterSignInfo = new ZhishuCreateContractRequest.SignInfoResource();
                    counterSignInfo.setEnable(false);
                    counterPartyInfo.setCounterPartySignInfoResource(counterSignInfo);
                    counterPartyList.add(counterPartyInfo);
//                    request.setCounterPartyList(counterPartyList);
                }else{
                    log.info("证件id={}未查询到交易方信息！！！", documentData.getCertificateNumber());
                }
                //关联前置单据
//                PrecedingDocResponse.Receipts receipts = new PrecedingDocResponse.Receipts();
//                receipts.setId(String.valueOf(documentData.getHeaderId()));
//                receipts.setTitle(documentData.getRealName());
//                receipts.setContent(documentData.getId());
//                contractFormCreatResponse.getForm().add(buildContractFormAttribute(ZhishuAndYecaiFiledEnum.ANCHOR_DOCUMENT_NUMBER.getZhishuFiled(),ZhishuAndYecaiFiledEnum.ANCHOR_DOCUMENT_NUMBER.getZhishuFiled(),"相关单据",FormAttributeTypeEnum.SINGLELINE_TEXT.getCode(),FormAttributeTypeEnum.THIRD_PARTY_APPROVAL.getCode(),receipts));
                contractFormCreatResponse.getForm().add(formAttribute);
                request.setForm(JSON.toJSONString(contractFormCreatResponse.getForm()));
            }else{
                log.info("未查询到主播卡片，订单编号为：DocumentNumber = {}", dto.getDocumentNumber());
                return null;
            }
        }else{
            log.info("请确认单据类型是否正确：DocumentType = {}", dto.getDocumentType());
            return null;
        }

//        Map<String, Object> params = new HashMap<>();
//        params.put("user_id",userId);
//        params.put("user_id_type","user_id");
//        QueryTemplateResponse templateInfo = zhishuContractClient.getTemplate(templateId, params);
//        String templateNumber = templateInfo.getTemplateNumber();
        CreateTemplateInstanceRequest createTemplateInstanceRequest = new CreateTemplateInstanceRequest();
        createTemplateInstanceRequest.setCreateUserid(userId);
        createTemplateInstanceRequest.setTemplateNumber(templateId);
        CreateTemplateInstanceResponse templateInstance = zhishuContractClient.createTemplateInstance(createTemplateInstanceRequest);
        request.setTemplateInstanceId(templateInstance.getTemplateInstanceid());
//        request.setTextFileId("1110770860756566380");

        // 确保必填参数不为空
        if (dto.getCurrencyCode() == null) {
            request.setCurrencyCode("CNY");
        } else {
            request.setCurrencyCode(dto.getCurrencyCode());
        }

        request.setStartDate(nowDate);
        request.setEndDate(nowDate);

        if (dto.getFixedValidityCode() == null) {
            request.setFixedValidityCode(1);
        } else {
            request.setFixedValidityCode(dto.getFixedValidityCode());
        }

        if (dto.getPayTypeCode() == null) {
            request.setPayTypeCode(1);
        } else {
            request.setPayTypeCode(dto.getPayTypeCode());
        }

        //仅收支类型为收入类或支出类时必填
        if((1==request.getPayTypeCode()||2==request.getPayTypeCode()) && dto.getPropertyTypeCode() != null){
            request.setPropertyTypeCode(dto.getPropertyTypeCode());
        }else{
            request.setPropertyTypeCode(0);
        }

        List<ZhishuCreateContractRequest.OurPartyInfo> ourPartyList = new ArrayList<>();
        if (dto.getOurPartyList() != null) {
            for (CreateContractDTO.OurPartyDTO ourPartyDTO : dto.getOurPartyList()) {
                ZhishuCreateContractRequest.OurPartyInfo ourParty = new ZhishuCreateContractRequest.OurPartyInfo();
                ourParty.setOurPartyCode(ourPartyDTO.getOurPartyCode());
                ZhishuCreateContractRequest.SignInfoResource ourSignInfo = new ZhishuCreateContractRequest.SignInfoResource();
                ourSignInfo.setEnable(false);
                ourParty.setOurPartySignInfoResource(ourSignInfo);
                ourPartyList.add(ourParty);
            }
        }
//        else{
//            ZhishuCreateContractRequest.OurPartyInfo ourParty = new ZhishuCreateContractRequest.OurPartyInfo();
//            ourParty.setOurPartyCode("L00100001");
//            ZhishuCreateContractRequest.SignInfoResource ourSignInfo = new ZhishuCreateContractRequest.SignInfoResource();
//            ourSignInfo.setEnable(false);
//            ourParty.setOurPartySignInfoResource(ourSignInfo);
//            ourPartyList.add(ourParty);
//        }
        request.setOurPartyList(ourPartyList);


        if (dto.getCounterPartyList() != null) {
            for (CreateContractDTO.CounterPartyDTO counterPartyDTO : dto.getCounterPartyList()) {
                ZhishuCreateContractRequest.CounterPartyInfo counterParty = new ZhishuCreateContractRequest.CounterPartyInfo();
                counterParty.setCounterPartyCode(counterPartyDTO.getCounterPartyCode());
                ZhishuCreateContractRequest.SignInfoResource counterSignInfo = new ZhishuCreateContractRequest.SignInfoResource();
                counterSignInfo.setEnable(false);
                counterParty.setCounterPartySignInfoResource(counterSignInfo);
                counterPartyList.add(counterParty);
            }
        }
//        else{
//            ZhishuCreateContractRequest.CounterPartyInfo counterParty = new ZhishuCreateContractRequest.CounterPartyInfo();
//            counterParty.setCounterPartyCode("V00100001");
//            ZhishuCreateContractRequest.SignInfoResource counterSignInfo = new ZhishuCreateContractRequest.SignInfoResource();
//            counterSignInfo.setEnable(false);
//            counterParty.setCounterPartySignInfoResource(counterSignInfo);
//            counterPartyList.add(counterParty);
//        }
        request.setCounterPartyList(counterPartyList);

        log.info("构建智书合同创建请求: {}", JSON.toJSONString(request));
        return request;
    }

    private ContractFormCreatResponse.FormAttribute buildContractFormAttribute(String attributeCode,
                                                                               String attributeType,
                                                                               Object attributeValue) {
        return buildContractFormAttribute(attributeCode, attributeCode, null, attributeType, null, attributeValue);
    }

    private ContractFormCreatResponse.FormAttribute buildContractFormAttribute(String attributeCode,
                                                                               String attributeKey,
                                                                               String moduleName,
                                                                               String attributeType,
                                                                               String approvalType,
                                                                               Object attributeValue) {
        ContractFormCreatResponse.FormAttribute formAttribute = new ContractFormCreatResponse.FormAttribute();
        formAttribute.setAttributeCode(attributeCode);
        formAttribute.setAttributeKey(attributeKey != null ? attributeKey : attributeCode);
        formAttribute.setModuleName(moduleName);
        formAttribute.setAttributeType(attributeType);
        formAttribute.setApprovalType(approvalType);
        formAttribute.setAttributeValue(buildContractFormAttributeValue(attributeType, attributeValue));
        return formAttribute;
    }

    private void addContractFormAttribute(ContractFormCreatResponse contractFormCreatResponse,
                                          String attributeCode,
                                          String attributeType,
                                          Object attributeValue) {
        if (contractFormCreatResponse == null || attributeCode == null || attributeCode.trim().isEmpty()|| attributeValue==null) {
            return;
        }
        if (contractFormCreatResponse.getForm() == null) {
            contractFormCreatResponse.setForm(new ArrayList<>());
        }
        contractFormCreatResponse.getForm().add(buildContractFormAttribute(attributeCode, attributeType, attributeValue));
    }

    private Object buildContractFormAttributeValue(String attributeType, Object attributeValue) {
        if (attributeValue == null || attributeType == null) {
            return attributeValue;
        }
        if (attributeType.equals(FormAttributeTypeEnum.SINGLELINE_TEXT.getCode())
                || attributeType.equals(FormAttributeTypeEnum.MULTILINE_TEXT.getCode())
                || attributeType.equals(FormAttributeTypeEnum.STRING.getCode())
                || attributeType.equals(FormAttributeTypeEnum.DATE.getCode())
                || attributeType.equals(FormAttributeTypeEnum.NUMBER.getCode())
                || attributeType.equals(FormAttributeTypeEnum.CALCULATION.getCode())) {
            return attributeValue.toString();
        }
        if (attributeType.equals(FormAttributeTypeEnum.AMOUNT.getCode())) {
            return buildAmountAttributeValue(attributeValue);
        }
        if (attributeType.equals(FormAttributeTypeEnum.DATE_RANGE.getCode())) {
            return buildDateRangeAttributeValue(attributeValue);
        }
        if (attributeType.equals(FormAttributeTypeEnum.RADIO.getCode())
                || attributeType.equals(FormAttributeTypeEnum.DROPDOWN_RADIO.getCode())
                || attributeType.equals(FormAttributeTypeEnum.TREE_RADIO.getCode())) {
            return buildKeyNameAttributeValue(attributeValue);
        }
        if (attributeType.equals(FormAttributeTypeEnum.CHECKBOX.getCode())
                || attributeType.equals(FormAttributeTypeEnum.DROPDOWN_OPTION.getCode())
                || attributeType.equals(FormAttributeTypeEnum.TREE_OPTION.getCode())) {
            return buildKeyNameAttributeValueList(attributeValue);
        }
        if (attributeType.equals(FormAttributeTypeEnum.EMPLOYEE.getCode())) {
            return buildEmployeeAttributeValueList(attributeValue);
        }
        if (attributeType.equals(FormAttributeTypeEnum.DEPARTMENT.getCode())) {
            return buildDepartmentAttributeValueList(attributeValue);
        }
        if (attributeType.equals(FormAttributeTypeEnum.HYPERLINK.getCode())) {
            return buildHyperlinkAttributeValue(attributeValue);
        }
        if (attributeType.equals(FormAttributeTypeEnum.COUNTRY_OR_REGION.getCode())) {
            return buildCountryOrRegionAttributeValue(attributeValue);
        }
        if (attributeType.equals(FormAttributeTypeEnum.ADDRESS.getCode())) {
            return buildAddressAttributeValue(attributeValue);
        }
        if (attributeType.equals(FormAttributeTypeEnum.FILE.getCode())) {
            return buildFileAttributeValueList(attributeValue);
        }
        if (attributeType.equals(FormAttributeTypeEnum.FEISHU_APPROVAL.getCode())
                || attributeType.equals(FormAttributeTypeEnum.ARRAY.getCode())
                || attributeType.equals(FormAttributeTypeEnum.COMMON_ARRAY.getCode())) {
            return Arrays.asList(attributeValue);
        }
        return attributeValue;
    }

    private ContractFormCreatResponse.AmountValue buildAmountAttributeValue(Object value) {
        if (value instanceof ContractFormCreatResponse.AmountValue) {
            return (ContractFormCreatResponse.AmountValue) value;
        }
        ContractFormCreatResponse.AmountValue amountValue = new ContractFormCreatResponse.AmountValue();
        Object amount = getMapOrBeanValue(value, "amount");
        Object currency = getMapOrBeanValue(value, "currency");
        Object currencyName = getMapOrBeanValue(value, "currency_name");
        if (amount == null) {
            amount = value;
        }
        if (amount != null && !String.valueOf(amount).trim().isEmpty()) {
            amountValue.setAmount(new BigDecimal(String.valueOf(amount)));
        }
        amountValue.setCurrency(currency == null ? "CNY" : String.valueOf(currency));
        amountValue.setCurrencyName(currencyName == null ? null : String.valueOf(currencyName));
        return amountValue;
    }

    private Map<String, Object> buildDateRangeAttributeValue(Object value) {
        Map<String, Object> dateRange = new LinkedHashMap<>();
        if (value instanceof List) {
            List<?> values = (List<?>) value;
            dateRange.put("start_date", values.size() > 0 ? values.get(0) : null);
            dateRange.put("end_date", values.size() > 1 ? values.get(1) : null);
            return dateRange;
        }
        Object startDate = getMapOrBeanValue(value, "start_date");
        Object endDate = getMapOrBeanValue(value, "end_date");
        if (startDate == null) {
            startDate = getMapOrBeanValue(value, "startDate");
        }
        if (endDate == null) {
            endDate = getMapOrBeanValue(value, "endDate");
        }
        dateRange.put("start_date", DateUtils.convertDateToString((Date) startDate,"yyyy-MM-dd"));
        dateRange.put("end_date", DateUtils.convertDateToString((Date) endDate,"yyyy-MM-dd"));
        return dateRange;
    }

    private Object buildKeyNameAttributeValue(Object value) {
        if (value instanceof Map || value instanceof ContractFormCreatResponse.TreeNodeValue
                || value instanceof ContractFormCreatResponse.OptionValue) {
            return value;
        }
        Map<String, Object> optionValue = new LinkedHashMap<>();
        optionValue.put("name", "");
        optionValue.put("key", value);
        return optionValue;
    }

    private Object buildKeyNameAttributeValueList(Object value) {
        if (value instanceof Map || value instanceof ContractFormCreatResponse.TreeNodeValue
                || value instanceof ContractFormCreatResponse.OptionValue) {
            return value;
        }
        Map<String, Object> optionValue = new LinkedHashMap<>();
        optionValue.put("key", Arrays.asList(value));
        return optionValue;
    }

    private List<ContractFormCreatResponse.EmployeeValue> buildEmployeeAttributeValueList(Object value) {
        List<ContractFormCreatResponse.EmployeeValue> values = new ArrayList<>();
        for (Object item : toObjectList(value)) {
            ContractFormCreatResponse.EmployeeValue employeeValue = new ContractFormCreatResponse.EmployeeValue();
            Object userId = getMapOrBeanValue(item, "user_id");
            Object userIdType = getMapOrBeanValue(item, "user_id_type");
            if (userId == null) {
                userId = item;
            }
            employeeValue.setUserId(userId == null ? null : String.valueOf(userId));
            employeeValue.setUserIdType(userIdType == null ? "lark_user_id" : String.valueOf(userIdType));
            values.add(employeeValue);
        }
        return values;
    }

    private List<ContractFormCreatResponse.DepartmentValue> buildDepartmentAttributeValueList(Object value) {
        List<ContractFormCreatResponse.DepartmentValue> values = new ArrayList<>();
        for (Object item : toObjectList(value)) {
            ContractFormCreatResponse.DepartmentValue departmentValue = new ContractFormCreatResponse.DepartmentValue();
            Object departmentId = getMapOrBeanValue(item, "department_id");
            Object departmentIdType = getMapOrBeanValue(item, "department_id_type");
            Object openDepartmentId = getMapOrBeanValue(item, "open_department_id");
            Object larkDepartmentId = getMapOrBeanValue(item, "lark_department_id");
            if (departmentId == null) {
                departmentId = item;
            }
            departmentValue.setDepartmentId(departmentId == null ? null : String.valueOf(departmentId));
            departmentValue.setDepartmentIdType(departmentIdType == null ? "department_id" : String.valueOf(departmentIdType));
            departmentValue.setOpenDepartmentId(openDepartmentId == null ? null : String.valueOf(openDepartmentId));
            departmentValue.setLarkDepartmentId(larkDepartmentId == null ? null : String.valueOf(larkDepartmentId));
            values.add(departmentValue);
        }
        return values;
    }

    private ContractFormCreatResponse.HyperlinkValue buildHyperlinkAttributeValue(Object value) {
        if (value instanceof ContractFormCreatResponse.HyperlinkValue) {
            return (ContractFormCreatResponse.HyperlinkValue) value;
        }
        ContractFormCreatResponse.HyperlinkValue hyperlinkValue = new ContractFormCreatResponse.HyperlinkValue();
        Object title = getMapOrBeanValue(value, "title");
        Object url = getMapOrBeanValue(value, "url");
        hyperlinkValue.setTitle(title == null ? null : String.valueOf(title));
        hyperlinkValue.setUrl(url == null ? String.valueOf(value) : String.valueOf(url));
        return hyperlinkValue;
    }

    private ContractFormCreatResponse.CountryOrRegionValue buildCountryOrRegionAttributeValue(Object value) {
        if (value instanceof ContractFormCreatResponse.CountryOrRegionValue) {
            return (ContractFormCreatResponse.CountryOrRegionValue) value;
        }
        ContractFormCreatResponse.CountryOrRegionValue countryOrRegionValue = new ContractFormCreatResponse.CountryOrRegionValue();
        Object countryCode = getMapOrBeanValue(value, "country_code");
        if (countryCode == null) {
            countryCode = value;
        }
        countryOrRegionValue.setCountryCode(countryCode == null ? null : String.valueOf(countryCode));
        return countryOrRegionValue;
    }

    private ContractFormCreatResponse.AddressValue buildAddressAttributeValue(Object value) {
        if (value instanceof ContractFormCreatResponse.AddressValue) {
            return (ContractFormCreatResponse.AddressValue) value;
        }
        ContractFormCreatResponse.AddressValue addressValue = new ContractFormCreatResponse.AddressValue();
        Object countryCode = getMapOrBeanValue(value, "country_code");
        Object regionCode = getMapOrBeanValue(value, "region_code");
        Object cityCode = getMapOrBeanValue(value, "city_code");
        Object address = getMapOrBeanValue(value, "address");
        addressValue.setCountryCode(countryCode == null ? null : String.valueOf(countryCode));
        addressValue.setRegionCode(regionCode == null ? null : String.valueOf(regionCode));
        addressValue.setCityCode(cityCode == null ? null : String.valueOf(cityCode));
        addressValue.setAddress(address == null ? null : String.valueOf(address));
        return addressValue;
    }

    private List<ContractFormCreatResponse.FileValue> buildFileAttributeValueList(Object value) {
        List<ContractFormCreatResponse.FileValue> values = new ArrayList<>();
        for (Object item : toObjectList(value)) {
            ContractFormCreatResponse.FileValue fileValue = new ContractFormCreatResponse.FileValue();
            Object fileId = getMapOrBeanValue(item, "file_id");
            Object fileName = getMapOrBeanValue(item, "file_name");
            Object fileSize = getMapOrBeanValue(item, "file_size");
            Object mime = getMapOrBeanValue(item, "mime");
            if (fileId == null) {
                fileId = item;
            }
            fileValue.setFileId(fileId == null ? null : String.valueOf(fileId));
            fileValue.setFileName(fileName == null ? null : String.valueOf(fileName));
            fileValue.setFileSize(fileSize == null ? null : String.valueOf(fileSize));
            fileValue.setMime(mime == null ? null : String.valueOf(mime));
            values.add(fileValue);
        }
        return values;
    }

    private List<Object> toObjectList(Object value) {
        if (value == null) {
            return Collections.emptyList();
        }
        if (value instanceof Collection) {
            return new ArrayList<>((Collection<?>) value);
        }
        return Collections.singletonList(value);
    }

    private Object getMapOrBeanValue(Object source, String fieldName) {
        if (source == null || fieldName == null) {
            return null;
        }
        if (source instanceof Map) {
            Object value = ((Map<?, ?>) source).get(fieldName);
            if (value == null) {
                value = ((Map<?, ?>) source).get(snakeToCamel(fieldName));
            }
            return value;
        }
        if (source instanceof JSONObject) {
            Object value = ((JSONObject) source).get(fieldName);
            return value == null ? ((JSONObject) source).get(snakeToCamel(fieldName)) : value;
        }
        return getBeanFieldValue(source, snakeToCamel(fieldName));
    }

    private UpdateAnchorCardRequest makeAnchorCardInfo(ContractQueryResponse contractQueryInfo,Map<String, Object> formData){
        UpdateAnchorCardRequest request = new UpdateAnchorCardRequest();
        request.setId(toLongValue(getFormDataValue(formData, ZhishuAndYecaiFiledEnum.ANCHOR_DOCUMENT_NUMBER)));
        request.setAnchorId(toStringValue(getFormDataValue(formData, ZhishuAndYecaiFiledEnum.ANCHOR_ROOM_ID)));
        request.setPlatform(getAnchorCardPlatform(formData));
        request.setTeamName(toStringValue(getFormDataValue(formData, ZhishuAndYecaiFiledEnum.ANCHOR_TEAM_NAME)));
        request.setContractStartDate(contractQueryInfo == null ? null : contractQueryInfo.getStartDate());
        request.setContractEndDate(contractQueryInfo == null ? null : contractQueryInfo.getEndDate());
        request.setOfficialSigningBonusIncome(toDoubleValue(getFormDataValue(formData, ZhishuAndYecaiFiledEnum.OFFICIAL_SIGNING_FEE)));
        Double officialSigningBonusRatio = toDoubleValue(getFormDataValue(formData, ZhishuAndYecaiFiledEnum.OFFICIAL_SIGNING_FEE_SHARE_RATIO));
        request.setOfficialSigningBonusRatio(officialSigningBonusRatio!=null?officialSigningBonusRatio*100:null);
        request.setCompanySigningBonus(toDoubleValue(getFormDataValue(formData, ZhishuAndYecaiFiledEnum.COMPANY_SIGNING_FEE)));
        request.setFixedBaseSalary(toDoubleValue(getFormDataValue(formData, ZhishuAndYecaiFiledEnum.FIXED_BASE_SALARY_PER_MONTH)));
        Double salaryRatio = toDoubleValue(getFormDataValue(formData, ZhishuAndYecaiFiledEnum.LIVE_PLATFORM_BASIC_COOPERATION_FEE));
        request.setSalaryRatio(salaryRatio!=null?salaryRatio*100:null);
        Double giftRatio = toDoubleValue(getFormDataValue(formData, ZhishuAndYecaiFiledEnum.GIFT_BASIC_SHARE_RATIO));
        request.setGiftRatio(giftRatio!=null?giftRatio*100:null);
        Double businessRatio = toDoubleValue(getFormDataValue(formData, ZhishuAndYecaiFiledEnum.SELF_MEDIA_BUSINESS_INCOME));
        request.setBusinessRatio(businessRatio!=null?businessRatio*100:null);
        Double selfMediaRatio = toDoubleValue(getFormDataValue(formData, ZhishuAndYecaiFiledEnum.SELF_MEDIA_ACCOUNT_INCOME));
        request.setSelfMediaRatio(selfMediaRatio!=null?selfMediaRatio*100:null);
        request.setOtherInfo(toStringValue(getFormDataValue(formData, ZhishuAndYecaiFiledEnum.OTHER_FEE)));
        return request;
    }

    private Object getFormDataValue(Map<String, Object> formData, ZhishuAndYecaiFiledEnum fieldEnum) {
        if (formData == null || fieldEnum == null) {
            return null;
        }
        return formData.get(fieldEnum.getZhishuFiled());
    }

    private String getAnchorCardPlatform(Map<String, Object> formData) {
        String platform = toStringValue(getFormDataValue(formData, ZhishuAndYecaiFiledEnum.PLATFORM));
        if (hasValue(platform)) {
            return platform;
        }
        if (formData == null) {
            return null;
        }
        String platformName = toStringValue(formData.get(ZhishuAndYecaiFiledEnum.PLATFORM.getZhishuFiled() + "_name"));
        return PlatformEnum.getCodeByName(platformName);
    }

    private String toStringValue(Object value) {
        Object joinedValue = joinListValue(value);
        return hasValue(joinedValue) ? String.valueOf(joinedValue) : null;
    }

    private Long toLongValue(Object value) {
        Object actualValue = getFirstNonBlankValue(value);
        if (!hasValue(actualValue)) {
            return null;
        }
        try {
            return new BigDecimal(String.valueOf(actualValue).trim()).longValue();
        } catch (NumberFormatException e) {
            log.warn("转换Long失败, value={}", actualValue, e);
            return null;
        }
    }

    private Double toDoubleValue(Object value) {
        Object actualValue = getFirstNonBlankValue(value);
        if (!hasValue(actualValue)) {
            return null;
        }
        try {
            return new BigDecimal(String.valueOf(actualValue).trim()).doubleValue();
        } catch (NumberFormatException e) {
            log.warn("转换Double失败, value={}", actualValue, e);
            return null;
        }
    }

    private Object getFirstNonBlankValue(Object value) {
        if (!(value instanceof Collection)) {
            return value;
        }
        for (Object item : (Collection<?>) value) {
            if (hasValue(item)) {
                return item;
            }
        }
        return null;
    }

    private ContSyncRequest makeContSyncRequest(ContractQueryResponse contractQueryInfo,Map<String, Object> formData) {


        ContSyncRequest request = new ContSyncRequest();
        ContSyncLineRequest lineRequest = new ContSyncLineRequest();
        boolean hasLineValue = false;
        hasLineValue = setMappedValue(request, lineRequest, hasLineValue, ZhishuAndYecaiFiledEnum.CONTRACT_NAME, contractQueryInfo, formData); // 合同名称
        hasLineValue = setMappedValue(request, lineRequest, hasLineValue, ZhishuAndYecaiFiledEnum.CONTRACT_CATEGORY_ABBREVIATION, contractQueryInfo, formData); // 合同类型
        String previousContractNumber = contractQueryInfo.getPreviousContractNumber();//旧合同编码
        if(trimToNull(previousContractNumber)!=null){//如果存在旧合同编码就需要按照旧合同编码进行变更数据同步至业财
            hasLineValue = setMappedValue(request, lineRequest, hasLineValue, ZhishuAndYecaiFiledEnum.PREVIOUS_CONTRACT_NUMBER, contractQueryInfo, formData); // 合同编号
        }else{
            hasLineValue = setMappedValue(request, lineRequest, hasLineValue, ZhishuAndYecaiFiledEnum.CONTRACT_NUMBER, contractQueryInfo, formData); // 合同编号
        }
        hasLineValue = setMappedValue(request, lineRequest, hasLineValue, ZhishuAndYecaiFiledEnum.CONTRACT_EXECUTOR, contractQueryInfo, formData); // 合同执行人
        hasLineValue = setMappedValue(request, lineRequest, hasLineValue, ZhishuAndYecaiFiledEnum.ORDER_INFO, contractQueryInfo, formData); // 订单编号
        hasLineValue = setMappedValue(request, lineRequest, hasLineValue, ZhishuAndYecaiFiledEnum.COST_CENTER, contractQueryInfo, formData); // 成本中心
        hasLineValue = setMappedValue(request, lineRequest, hasLineValue, ZhishuAndYecaiFiledEnum.PURCHASE_REQUEST, contractQueryInfo, formData); // 采购申请
        hasLineValue = setMappedValue(request, lineRequest, hasLineValue, ZhishuAndYecaiFiledEnum.SPECIAL_CATEGORY, contractQueryInfo, formData); // 专项分类
        hasLineValue = setMappedValue(request, lineRequest, hasLineValue, ZhishuAndYecaiFiledEnum.EFFECTIVE_TIME, contractQueryInfo, formData); // 有效时间
        hasLineValue = setDateRangeMappedValue(request, lineRequest, hasLineValue, ZhishuAndYecaiFiledEnum.LOAN_DATE_RANGE, formData); // 借款起止日
        hasLineValue = setMappedValue(request, lineRequest, hasLineValue, ZhishuAndYecaiFiledEnum.ACCEPTANCE_REQUIRED, contractQueryInfo, formData); // 是否需要验收
        hasLineValue = setMappedValue(request, lineRequest, hasLineValue, ZhishuAndYecaiFiledEnum.PAY_TYPE, contractQueryInfo, formData); // 收支类型
        hasLineValue = setMappedValue(request, lineRequest, hasLineValue, ZhishuAndYecaiFiledEnum.PROPERTY_TYPE_CODE, contractQueryInfo, formData); // 计价方式
        hasLineValue = setMappedValue(request, lineRequest, hasLineValue, ZhishuAndYecaiFiledEnum.TOTAL_AMOUNT, contractQueryInfo, formData); // 合同总额
//        hasLineValue = setMappedValue(request, lineRequest, hasLineValue, ZhishuAndYecaiFiledEnum.CURRENCY_CODE, contractQueryInfo, formData); // 合同总额
        hasLineValue = setMappedValue(request, lineRequest, hasLineValue, ZhishuAndYecaiFiledEnum.FUNCTION_AMOUNT, contractQueryInfo, formData); // 本币金额
        hasLineValue = setMappedValue(request, lineRequest, hasLineValue, ZhishuAndYecaiFiledEnum.ESTIMATED_AMOUNT, contractQueryInfo, formData); // 预估金额
        hasLineValue = setMappedValue(request, lineRequest, hasLineValue, ZhishuAndYecaiFiledEnum.FIXED_VALIDITY, contractQueryInfo, formData); // 合同期限类型
//        hasLineValue = setMappedValue(request, lineRequest, hasLineValue, ZhishuAndYecaiFiledEnum.DATE_RANGE, contractQueryInfo, formData); // 合同期限
        hasLineValue = setMappedValue(request, lineRequest, hasLineValue, ZhishuAndYecaiFiledEnum.MAJOR_CONTRACT_FLAG, contractQueryInfo, formData); // 是否涉及重大权利义务条款
        hasLineValue = setMappedValue(request, lineRequest, hasLineValue, ZhishuAndYecaiFiledEnum.BANK_CHARGE_PAYER, contractQueryInfo, formData); // 银行手续费承担方
        hasLineValue = setMappedValue(request, lineRequest, hasLineValue, ZhishuAndYecaiFiledEnum.INVOICE_TYPE, contractQueryInfo, formData); // 发票种类
        hasLineValue = setMappedValue(request, lineRequest, hasLineValue, ZhishuAndYecaiFiledEnum.ORDERHT_DOCUMENT_NUMBER, contractQueryInfo, formData); // 单据编码
        hasLineValue = setMappedValue(request, lineRequest, hasLineValue, ZhishuAndYecaiFiledEnum.PLATFORM, contractQueryInfo, formData); // 所属平台
        hasLineValue = setMappedValue(request, lineRequest, hasLineValue, ZhishuAndYecaiFiledEnum.REQUISITION_DATE, contractQueryInfo, formData); // 签署日期
        Integer payTypeCode = contractQueryInfo.getPayTypeCode();//收支类型:1:收入类;2:支出类;3:既收又支;4:无金额
//        String dimension2Code = request.getDimension2Code();
//        if(dimension2Code==null){
//            hasLineValue = setMappedValue(request, lineRequest, hasLineValue, ZhishuAndYecaiFiledEnum.ORDER_INFO, contractQueryInfo, formData);
//        }
        String contractNumber = request.getContractNumber();
        if(contractNumber==null){
            String contNumber = contractQueryInfo.getContractNumber();
            String substr = contNumber.substring(0, 5);
            if("H-P20".equals(substr)){
                request.setContractNumber("A02-2026-011-M001");
            }
        }
        request.setContSyncLines(makeContSyncLines(contractQueryInfo, formData));
        request.setStartDate(DateUtils.convertStringToDate(contractQueryInfo.getStartDate(),"yyyy-MM-dd"));//合同期限-起始日期
        request.setEndDate(DateUtils.convertStringToDate(contractQueryInfo.getEndDate(),"yyyy-MM-dd"));//合同期限-终止日期
        request.setMagOrgCode("1000");//管理组织-设置默认值：1000
        String htType = contractQueryInfo.getParentContractCategoryAbbreviation();//合同类别-一级
        if("ZB".equals(htType)){
            hasLineValue = setMappedValue(request, lineRequest, hasLineValue, ZhishuAndYecaiFiledEnum.ANCHORHT_DOCUMENT_NUMBER, contractQueryInfo, formData); //单据编码
            hasLineValue = setMappedValue(request, lineRequest, hasLineValue, ZhishuAndYecaiFiledEnum.ROOM_ID, contractQueryInfo, formData); //主播id
            String roomId = request.getRoomId();
            Map<String,Object> documentParams = new HashMap<>();
            documentParams.put("page", 0);
            documentParams.put("size", 1);
            MasterDataRes masterDataRes = yuecaiContractClient.getAnchorCard(documentParams, roomId,"id");
            if(masterDataRes!=null&&masterDataRes.getContent()!=null && !masterDataRes.getContent().isEmpty()){
                List<Object> content = masterDataRes.getContent();
                AnchorCardResponse documentData = JSONObject.parseObject(content.get(0).toString(), AnchorCardResponse.class);
                List<AnchorCardResponse.AnchorCardLineRes> lineResultDTOS = documentData.getLineResultDTOS();
                if(lineResultDTOS!=null&& !lineResultDTOS.isEmpty()){//TODO 暂时先默认获取第一行
                    AnchorCardResponse.AnchorCardLineRes anchorCardLineRes = lineResultDTOS.get(0);
                    request.setRoomId(anchorCardLineRes.getAnchorId());//直播id
                    request.setAnchorNickname(anchorCardLineRes.getAnchorNickname());//主播昵称
                    request.setAnchorIdCard(documentData.getCertificateNumber());//证件号码
                }else{
                    log.info("当前主播卡片id={}没有明细行信息",roomId);
                }
                request.setPlatform(PlatformEnum.getCodeByName(String.valueOf(formData.get(ZhishuAndYecaiFiledEnum.PLATFORM.getZhishuFiled()+"_name"))));//平台
            }else{
                log.info("未查询到主播卡片，订单编号为：roomId = {}", roomId);
            }
        }else{
            request.setBankChargePayer(BankChargePayerEnum.getYecaiCodeByZhishuCode(request.getBankChargePayer()));//银行手续费承担方
            request.setInvoiceType(InvoiceTypeEnum.getYecaiCodeByZhishuCode(request.getInvoiceType()));//发票类型
        }
        //我方信息
        List<ContractQueryResponse.OurParty> ourPartyList = contractQueryInfo.getOurPartyList();
        StringBuilder ourPartyCodes = new StringBuilder();
        for (ContractQueryResponse.OurParty ourParty : ourPartyList) {
            if(ourPartyCodes.length()==0){
                ourPartyCodes.append(ourParty.getOurPartyCode());
            }else{
                ourPartyCodes.append(",").append(ourParty.getOurPartyCode());
            }
        }
        request.setEntityCodes(ourPartyCodes.toString());
        //对方信息
        List<ContractQueryResponse.CounterParty> counterPartyList = contractQueryInfo.getCounterPartyList();
        StringBuilder counterPartyCodes = new StringBuilder();
        VendorTypeEnum partnerDirection = getPartnerDirectionByPayType(payTypeCode);
        if (counterPartyList != null) {
            for (ContractQueryResponse.CounterParty counterParty : counterPartyList) {
                String counterPartyCode = counterParty.getCounterPartyCode();
                String counterPartyId = counterParty.getCounterPartyId();
                VendorInfoResponse vendorInfo = getVendorInfo(counterPartyId, null);
                String vendorType = vendorInfo == null ? null : vendorInfo.getVendorType();
                if (vendorInfo == null) {
                    log.info("交易方id = {} 交易方编码 = {} 未获取到交易方信息，请确认", counterPartyId, counterPartyCode);
                }
                PartnerValue partnerValue = partnerDirection == null
                        ? resolvePartnerValueByVendorType(counterPartyCode, vendorType)
                        : resolvePartnerValue(counterPartyCode, vendorType, partnerDirection);
                if (!partnerValue.isValid()) {
                    log.info("合同主表交易方编码解析失败，交易方id = {}，交易方编码 = {}，交易方类型 = {}，收支类型 = {}",
                            new Object[]{counterPartyId, counterPartyCode, vendorType, payTypeCode});
                    continue;
                }
                appendPartnerCode(counterPartyCodes, partnerValue.getPartnerCode());
                if (trimToNull(partnerValue.getPartnerCategory()) != null) {
                    request.setPartnerCategory(partnerValue.getPartnerCategory());
                }
            }
        }
        request.setPartnerCode(counterPartyCodes.toString());
        request.setCompanyCode("HeroEsports");//公司-固定值HeroEsports
        request.setEmployeeCode(contractQueryInfo.getCreateUserId());
//        request.setEmployeeCode("e8d58ag6");
//        request.setRespEmployeeCode("e8d58ag6");
        request.setResponsibilityCenterCode("RC001");
        request.setCurrencyCode(contractQueryInfo.getCurrencyCode()==null?"CNY":contractQueryInfo.getCurrencyCode());//币别
        request.setExchangeRate(new BigDecimal("1.0"));
        if(3 == contractQueryInfo.getContractStatusCode()){
            request.setStatus("SUBMITTED");//提交
        }else if(9 == contractQueryInfo.getContractStatusCode()||11 == contractQueryInfo.getContractStatusCode()){
            request.setStatus("CONFIRM");//确认*
        }else if(2 == contractQueryInfo.getContractStatusCode()||4 == contractQueryInfo.getContractStatusCode()){
            request.setStatus("APPROVAL_RETURN");//审批退回
        }
        request.setTenantId(0L);//租户ID-固定值：0
        request.setAttachmentClass(0);//附件类型-固定值：0
        return request;
    }

    private List<ContSyncLineRequest> makeContSyncLines(ContractQueryResponse contractQueryInfo, Map<String, Object> formData) {
        List<ContSyncLineRequest> lineRequests = new ArrayList<>();
        int lineCount = getContSyncLineCount(contractQueryInfo, formData);
        Map<String, VendorInfoResponse> vendorInfoCache = new HashMap<>();
        int lineNum = 1;
        if(contractQueryInfo.getPaymentPlanList()!=null){
            for (int i = 0; i < contractQueryInfo.getPaymentPlanList().size(); i++) {
                ContSyncLineRequest lineRequest = new ContSyncLineRequest();
                boolean hasLineValue = false;
                hasLineValue = setLineMappedValue(lineRequest, hasLineValue, ZhishuAndYecaiFiledEnum.PAYMENT_AMOUNT, contractQueryInfo, formData, i); // 支出金额
//            hasLineValue = setLineMappedValue(lineRequest, hasLineValue, ZhishuAndYecaiFiledEnum.PAYMENT_AMOUNT2, contractQueryInfo, formData, i); // 支出金额
                hasLineValue = setLineMappedValue(lineRequest, hasLineValue, ZhishuAndYecaiFiledEnum.PAYMENT_DATE, contractQueryInfo, formData, i); // 支出日期
                hasLineValue = setLineMappedValue(lineRequest, hasLineValue, ZhishuAndYecaiFiledEnum.PAYMENT_CURRENCY_CODE, contractQueryInfo, formData, i); // 支出币种
                hasLineValue = setLineMappedValue(lineRequest, hasLineValue, ZhishuAndYecaiFiledEnum.CURRENCY_CODE, contractQueryInfo, formData, i); // 币种
//            hasLineValue = setLineMappedValue(lineRequest, hasLineValue, ZhishuAndYecaiFiledEnum.PAYMENT_ACCEPTED_FLAG, contractQueryInfo, formData, i); // 是否需要验收
                hasLineValue = setLineMappedValue(lineRequest, hasLineValue, ZhishuAndYecaiFiledEnum.ACCEPTANCE_TIME_AGREEMENT, contractQueryInfo, formData, i); // 验收时间（天）
//                hasLineValue = setLineMappedValue(lineRequest, hasLineValue, ZhishuAndYecaiFiledEnum.PAYMENT_INTERVAL_DAYS, contractQueryInfo, formData, i); // 验收时间（天）
                hasLineValue = setLineMappedValue(lineRequest, hasLineValue, ZhishuAndYecaiFiledEnum.PAYMENT_NODE_TYPE, contractQueryInfo, formData, i); // 付款节点属性
                lineRequest.setLineNumber(Long.valueOf(lineNum++));
                String revenueExpenditure = "2";//支出
                lineRequest.setRevenueExpenditure(revenueExpenditure);
                lineRequest.setPay2reqExchangeType("EX_PERIOD");
                lineRequest.setPay2reqExchangeRate(new BigDecimal("1.0"));
                lineRequest.setReq2payExchangeType("EX_PERIOD");
                lineRequest.setReq2payExchangeRate(new BigDecimal("1.0"));
                lineRequest.setPaymentRatioFlag(true);
                lineRequest.setLandmarkPhase(lineRequest.getPaymentNodeType());
                lineRequest.setPaymentNodeType("押金/保证金".equals(lineRequest.getPaymentNodeType())?"DEPOSIT":"");//是否为押金/保证金
                hasLineValue = setPaymentCounterPartyValue(lineRequest, hasLineValue, contractQueryInfo, i, vendorInfoCache);
                lineRequest.setPaymentNodeFlag("Y");//是否付款-固定值：Y
                ContractQueryResponse.PaymentPlan paymentPlan = contractQueryInfo.getPaymentPlanList().get(i);
                lineRequest.setZsLineId(paymentPlan.getUuid());
                String paymentIntervalDays = paymentPlan.getPaymentIntervalDays();
                if(paymentIntervalDays!=null&&Integer.parseInt(paymentIntervalDays)>0){
                    lineRequest.setAcceptanceTimeAgreement(paymentIntervalDays);
                    lineRequest.setAcceptedFlag("Y");
                }else {
                    lineRequest.setAcceptedFlag("N");
                }
                String partnerCode = lineRequest.getPartnerCode();
                List<ContractQueryResponse.CounterParty> counterPartyList = contractQueryInfo.getCounterPartyList();
                for (ContractQueryResponse.CounterParty counterParty : counterPartyList) {
                    String counterPartyCode = counterParty.getCounterPartyCode();
                    if(counterPartyCode.equals(partnerCode)){
                        List<ContractQueryResponse.BankAccount> bankAccounts = counterParty.getBankAccounts();
                        if(bankAccounts!=null&&!bankAccounts.isEmpty()){
                            ContractQueryResponse.BankAccount bankAccount = bankAccounts.get(0);
                            lineRequest.setBankAccountNumber(bankAccount.getAccount());
                        }
                    }
                }
//            if("2".equals(revenueExpenditure)){//支出
//            } else {
//                hasLineValue = setCollectionCounterPartyValue(lineRequest, hasLineValue, contractQueryInfo, i, vendorInfoCache);
//                ContractQueryResponse.CollectionPlan collectionPlan = contractQueryInfo.getCollectionPlanList().get(i);
//                lineRequest.setZsLineId(collectionPlan.getCollectionPlanId());
//            }

                if (hasLineValue) {
                    lineRequests.add(lineRequest);
                }
            }
        }
        if(contractQueryInfo.getCollectionPlanList()!=null){
            for (int i = 0; i < contractQueryInfo.getCollectionPlanList().size(); i++) {
                ContSyncLineRequest lineRequest = new ContSyncLineRequest();
                boolean hasLineValue = false;
                hasLineValue = setLineMappedValue(lineRequest, hasLineValue, ZhishuAndYecaiFiledEnum.COLLECTION_AMOUNT, contractQueryInfo, formData, i); // 收入金额
//            hasLineValue = setLineMappedValue(lineRequest, hasLineValue, ZhishuAndYecaiFiledEnum.COLLECTION_AMOUNT2, contractQueryInfo, formData, i); // 收入金额
                hasLineValue = setLineMappedValue(lineRequest, hasLineValue, ZhishuAndYecaiFiledEnum.COLLECTION_RMB_AMOUNT, contractQueryInfo, formData, i); // 收入人民币金额
                hasLineValue = setLineMappedValue(lineRequest, hasLineValue, ZhishuAndYecaiFiledEnum.COLLECTION_DATE, contractQueryInfo, formData, i); // 收款日期
                lineRequest.setLineNumber(Long.valueOf(lineNum++));
                String revenueExpenditure = "1";//收入
                lineRequest.setRevenueExpenditure(revenueExpenditure);
                lineRequest.setPay2reqExchangeType("EX_PERIOD");
                lineRequest.setPay2reqExchangeRate(new BigDecimal("1.0"));
                lineRequest.setReq2payExchangeType("EX_PERIOD");
                lineRequest.setReq2payExchangeRate(new BigDecimal("1.0"));
                lineRequest.setPaymentRatioFlag(true);
                lineRequest.setLandmarkPhase("一般收款");//给定默认值描述
                lineRequest.setPaymentNodeType("押金/保证金".equals(lineRequest.getPaymentNodeType())?"DEPOSIT":"");//是否为押金/保证金
//            if("2".equals(revenueExpenditure)){//支出
//                hasLineValue = setPaymentCounterPartyValue(lineRequest, hasLineValue, contractQueryInfo, i, vendorInfoCache);
//                lineRequest.setPaymentNodeFlag("Y");//是否付款-固定值：Y
//                ContractQueryResponse.PaymentPlan paymentPlan = contractQueryInfo.getPaymentPlanList().get(i);
//                lineRequest.setZsLineId(paymentPlan.getUuid());
//                String paymentIntervalDays = paymentPlan.getPaymentIntervalDays();
//                if(paymentIntervalDays!=null&&Integer.parseInt(paymentIntervalDays)>0){
//                    lineRequest.setAcceptedFlag("Y");
//                }else {
//                    lineRequest.setAcceptedFlag("N");
//                }
//            } else {
//            }
                hasLineValue = setCollectionCounterPartyValue(lineRequest, hasLineValue, contractQueryInfo, i, vendorInfoCache);
                ContractQueryResponse.CollectionPlan collectionPlan = contractQueryInfo.getCollectionPlanList().get(i);
                lineRequest.setZsLineId(collectionPlan.getCollectionPlanId());
                lineRequest.setCurrencyCode(collectionPlan.getCurrencyCode()==null?"CNY":collectionPlan.getCurrencyCode());

                if (hasLineValue) {
                    lineRequests.add(lineRequest);
                }
            }
        }
        return lineRequests.isEmpty() ? null : lineRequests;
    }

    private String getRevenueExpenditure(ContractQueryResponse contractQueryInfo, int lineIndex) {
        if (contractQueryInfo == null) {
            return null;
        }
        if (contractQueryInfo.getPaymentPlanList() != null
                && lineIndex >= 0
                && lineIndex < contractQueryInfo.getPaymentPlanList().size()) {
            return "2";
        }
        if (contractQueryInfo.getCollectionPlanList() != null
                && lineIndex >= 0
                && lineIndex < contractQueryInfo.getCollectionPlanList().size()) {
            return "1";
        }
        return contractQueryInfo.getPayTypeCode() == null ? null : contractQueryInfo.getPayTypeCode().toString();
    }

    private VendorTypeEnum getPartnerDirectionByPayType(Integer payTypeCode) {
        if (Integer.valueOf(3).equals(payTypeCode) || Integer.valueOf(2).equals(payTypeCode)) {
            return VendorTypeEnum.VENDOR;
        }
        if (Integer.valueOf(1).equals(payTypeCode)) {
            return VendorTypeEnum.CUSTOMER;
        }
        log.info("合同收支类型为空或无需选择交易方方向，payTypeCode={}", payTypeCode);
        return null;
    }

    private PartnerValue resolvePartnerValueByVendorType(String counterPartyCode, String vendorType) {
        String code = trimToNull(counterPartyCode);
        if (code == null) {
            return PartnerValue.invalid();
        }
        if (isMixedVendorType(vendorType)) {
            log.info("交易方类型同时包含客户和供应商，但当前业务方向不明确，交易方编码={}，交易方类型={}", counterPartyCode, vendorType);
            return PartnerValue.invalid();
        }
        if (containsVendorType(vendorType, VendorTypeEnum.VENDOR)) {
            return resolvePartnerValue(counterPartyCode, vendorType, VendorTypeEnum.VENDOR);
        }
        if (containsVendorType(vendorType, VendorTypeEnum.CUSTOMER)) {
            return resolvePartnerValue(counterPartyCode, vendorType, VendorTypeEnum.CUSTOMER);
        }
        if (code.contains(";")) {
            log.info("交易方编码为混合编码，但交易方类型和业务方向都不明确，交易方编码={}，交易方类型={}", counterPartyCode, vendorType);
            return PartnerValue.invalid();
        }
        return PartnerValue.of(code, null);
    }

    private PartnerValue resolvePartnerValue(String counterPartyCode, String vendorType, VendorTypeEnum direction) {
        String code = trimToNull(counterPartyCode);
        if (code == null) {
            log.info("交易方编码为空，无法解析交易方方向，direction={}，vendorType={}", direction, vendorType);
            return PartnerValue.invalid();
        }
        if (direction == null) {
            log.info("交易方业务方向无效，direction={}，counterPartyCode={}，vendorType={}", new Object[]{direction, counterPartyCode, vendorType});
            return PartnerValue.invalid();
        }
        boolean mixed = isMixedVendorType(vendorType) || code.contains(";");
        String partnerCode = mixed ? getMixedPartnerCode(code, direction) : code;
        if (trimToNull(partnerCode) == null) {
            log.info("混合交易方编码缺少对应方向编码，交易方编码={}，交易方类型={}，业务方向={}", new Object[]{counterPartyCode, vendorType, direction.getZhishuCode()});
            return PartnerValue.invalid();
        }
        if (trimToNull(vendorType) != null && !isMixedVendorType(vendorType) && !containsVendorType(vendorType, direction)) {
            log.info("交易方类型与业务方向不一致，将按业务方向同步，交易方编码={}，交易方类型={}，业务方向={}",
                    new Object[]{counterPartyCode, vendorType, direction.getZhishuCode()});
        }
        return PartnerValue.of(partnerCode, direction.getYecaiCode());
    }

    private String getMixedPartnerCode(String counterPartyCode, VendorTypeEnum direction) {
        String[] counterPartyCodes = counterPartyCode.split(";", -1);
        if (VendorTypeEnum.VENDOR == direction) {
            return counterPartyCodes.length > 0 ? trimToNull(counterPartyCodes[0]) : null;
        }
        if (VendorTypeEnum.CUSTOMER == direction) {
            return counterPartyCodes.length > 1 ? trimToNull(counterPartyCodes[1]) : null;
        }
        return null;
    }

    private boolean isMixedVendorType(String vendorType) {
        return containsVendorType(vendorType, VendorTypeEnum.CUSTOMER)
                && containsVendorType(vendorType, VendorTypeEnum.VENDOR);
    }

    private boolean containsVendorType(String vendorType, VendorTypeEnum direction) {
        String cleanVendorType = trimToNull(vendorType);
        return cleanVendorType != null && direction != null && cleanVendorType.contains(direction.getZhishuCode());
    }

    private void appendPartnerCode(StringBuilder builder, String partnerCode) {
        String code = trimToNull(partnerCode);
        if (code == null) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(",");
        }
        builder.append(code);
    }

    private VendorInfoResponse getVendorInfo(String counterPartyId, Map<String, VendorInfoResponse> vendorInfoCache) {
        String id = trimToNull(counterPartyId);
        if (id == null) {
            return null;
        }
        if (vendorInfoCache == null) {
            return zhiShuVendorClient.getVendorV2(id);
        }
        if (!vendorInfoCache.containsKey(id)) {
            vendorInfoCache.put(id, zhiShuVendorClient.getVendorV2(id));
        }
        return vendorInfoCache.get(id);
    }

    private boolean setPaymentCounterPartyValue(ContSyncLineRequest lineRequest, boolean hasLineValue,
                                                ContractQueryResponse contractQueryInfo, int lineIndex,
                                                Map<String, VendorInfoResponse> vendorInfoCache) {
        ContractQueryResponse.PaymentCounterParty counterParty = getPaymentCounterParty(contractQueryInfo, lineIndex);
        if (counterParty == null) {
            return hasLineValue;
        }
        String counterPartyCode = counterParty.getCounterPartyCode();
        String counterPartyId = counterParty.getCounterPartyId();
        VendorInfoResponse vendorInfo = getVendorInfo(counterPartyId, vendorInfoCache);
        String vendorType = vendorInfo == null ? null : vendorInfo.getVendorType();
        if (vendorInfo == null) {
            log.info("付款计划交易方id = {} 交易方编码 = {} 未获取到交易方信息，请确认", counterPartyId, counterPartyCode);
        }
        PartnerValue partnerValue = resolvePartnerValue(counterPartyCode, vendorType, VendorTypeEnum.VENDOR);
        if (partnerValue.isValid()) {
            lineRequest.setPartnerCode(partnerValue.getPartnerCode());
            lineRequest.setPartnerCategory(partnerValue.getPartnerCategory());
            hasLineValue = true;
        } else {
            log.info("付款计划交易方编码解析失败，交易方id = {}，交易方编码 = {}，交易方类型 = {}", new Object[]{counterPartyId, counterPartyCode, vendorType});
        }
        return hasLineValue;
    }

    private ContractQueryResponse.PaymentCounterParty getPaymentCounterParty(ContractQueryResponse contractQueryInfo, int lineIndex) {
        if (contractQueryInfo == null
                || contractQueryInfo.getPaymentPlanList() == null
                || lineIndex < 0
                || lineIndex >= contractQueryInfo.getPaymentPlanList().size()) {
            return null;
        }
        ContractQueryResponse.PaymentPlan paymentPlan = contractQueryInfo.getPaymentPlanList().get(lineIndex);
        return paymentPlan == null ? null : paymentPlan.getPaymentCounterParty();
    }

    private boolean setCollectionCounterPartyValue(ContSyncLineRequest lineRequest, boolean hasLineValue,
                                                   ContractQueryResponse contractQueryInfo, int lineIndex,
                                                   Map<String, VendorInfoResponse> vendorInfoCache) {
        ContractQueryResponse.CollectionCounterParty counterParty = getCollectionCounterParty(contractQueryInfo, lineIndex);
        if (counterParty == null) {
            return hasLineValue;
        }
        String counterPartyCode = counterParty.getCounterPartyCode();
        String counterPartyId = counterParty.getCounterPartyId();
        VendorInfoResponse vendorInfo = getVendorInfo(counterPartyId, vendorInfoCache);
        String vendorType = vendorInfo == null ? null : vendorInfo.getVendorType();
        if (vendorInfo == null) {
            log.info("收款计划交易方id = {} 交易方编码 = {} 未获取到交易方信息，请确认", counterPartyId, counterPartyCode);
        }
        PartnerValue partnerValue = resolvePartnerValue(counterPartyCode, vendorType, VendorTypeEnum.CUSTOMER);
        if (partnerValue.isValid()) {
            lineRequest.setPartnerCode(partnerValue.getPartnerCode());
            lineRequest.setPartnerCategory(partnerValue.getPartnerCategory());
            hasLineValue = true;
        } else {
            log.info("收款计划交易方编码解析失败，交易方id = {}，交易方编码 = {}，交易方类型 = {}", new Object[]{counterPartyId, counterPartyCode, vendorType});
        }
        return hasLineValue;
    }

    private ContractQueryResponse.CollectionCounterParty getCollectionCounterParty(ContractQueryResponse contractQueryInfo, int lineIndex) {
        if (contractQueryInfo == null
                || contractQueryInfo.getCollectionPlanList() == null
                || lineIndex < 0
                || lineIndex >= contractQueryInfo.getCollectionPlanList().size()) {
            return null;
        }
        ContractQueryResponse.CollectionPlan collectionPlan = contractQueryInfo.getCollectionPlanList().get(lineIndex);
        return collectionPlan == null ? null : collectionPlan.getCollectionCounterParty();
    }

    private static class PartnerValue {
        private final String partnerCode;
        private final String partnerCategory;
        private final boolean valid;

        private PartnerValue(String partnerCode, String partnerCategory, boolean valid) {
            this.partnerCode = partnerCode;
            this.partnerCategory = partnerCategory;
            this.valid = valid;
        }

        private static PartnerValue of(String partnerCode, String partnerCategory) {
            return new PartnerValue(partnerCode, partnerCategory, true);
        }

        private static PartnerValue invalid() {
            return new PartnerValue(null, null, false);
        }

        private String getPartnerCode() {
            return partnerCode;
        }

        private String getPartnerCategory() {
            return partnerCategory;
        }

        private boolean isValid() {
            return valid;
        }
    }

    private boolean setDateRangeMappedValue(ContSyncRequest request, ContSyncLineRequest lineRequest, boolean hasLineValue,
                                            ZhishuAndYecaiFiledEnum fieldMapping, Map<String, Object> formData) {
        return setDateRangeMappedValue(request, lineRequest, hasLineValue, fieldMapping.getZhishuFiled(),
                fieldMapping.getYecaiFiled(), fieldMapping.getYecaiEndFiled(), formData);
    }

    private boolean setDateRangeMappedValue(ContSyncRequest request, ContSyncLineRequest lineRequest, boolean hasLineValue,
                                            String zhishuField, String startDateField, String endDateField,
                                            Map<String, Object> formData) {
        Object startDate = getDateRangePartValue(formData.get(zhishuField), "start_date");
        Object endDate = getDateRangePartValue(formData.get(zhishuField), "end_date");
        if (startDate != null) {
            hasLineValue = setFieldValue(request, startDateField, startDate)
                    || setFieldValue(lineRequest, startDateField, startDate)
                    || hasLineValue;
        }
        if (endDate != null) {
            hasLineValue = setFieldValue(request, endDateField, endDate)
                    || setFieldValue(lineRequest, endDateField, endDate)
                    || hasLineValue;
        }
        return hasLineValue;
    }

    private Object getDateRangePartValue(Object value, String fieldName) {
        if (value instanceof Map) {
            return ((Map<?, ?>) value).get(fieldName);
        }
        return getObjectValue(value, fieldName);
    }

    private int getContSyncLineCount(ContractQueryResponse contractQueryInfo, Map<String, Object> formData) {
        int lineCount = 0;
        if (contractQueryInfo != null && contractQueryInfo.getPaymentPlanList() != null) {
            lineCount = Math.max(lineCount, contractQueryInfo.getPaymentPlanList().size());
        }
        if (contractQueryInfo != null && contractQueryInfo.getCollectionPlanList() != null) {
            lineCount = Math.max(lineCount, contractQueryInfo.getCollectionPlanList().size());
        }
        lineCount = Math.max(lineCount, getListSize(formData.get(ZhishuAndYecaiFiledEnum.PAYMENT_ACCEPTED_FLAG.getZhishuFiled())));
        lineCount = Math.max(lineCount, getListSize(formData.get(ZhishuAndYecaiFiledEnum.ACCEPTANCE_TIME_AGREEMENT.getZhishuFiled())));
        return lineCount;
    }

    private int getListSize(Object value) {
        return value instanceof List ? ((List<?>) value).size() : 0;
    }

    private boolean setLineMappedValue(ContSyncLineRequest lineRequest, boolean hasLineValue,
                                       ZhishuAndYecaiFiledEnum fieldMapping, ContractQueryResponse contractQueryInfo,
                                       Map<String, Object> formData, int lineIndex) {
        return setLineMappedValue(lineRequest, hasLineValue, fieldMapping.getZhishuFiled(), fieldMapping.getYecaiFiled(),
                contractQueryInfo, formData, lineIndex);
    }

    private boolean setLineMappedValue(ContSyncLineRequest lineRequest, boolean hasLineValue,
                                       String zhishuField, String yuecaiField, ContractQueryResponse contractQueryInfo,
                                       Map<String, Object> formData, int lineIndex) {
        Object value = getZhishuMappedLineValue(zhishuField, contractQueryInfo, formData, lineIndex);
        if (value == null || yuecaiField == null || yuecaiField.trim().isEmpty()) {
            return hasLineValue;
        }
        return setFieldValue(lineRequest, yuecaiField, value) || hasLineValue;
    }

    private boolean setMappedValue(ContSyncRequest request, ContSyncLineRequest lineRequest, boolean hasLineValue,
                                   ZhishuAndYecaiFiledEnum fieldMapping, ContractQueryResponse contractQueryInfo,
                                   Map<String, Object> formData) {
        return setMappedValue(request, lineRequest, hasLineValue, fieldMapping.getZhishuFiled(), fieldMapping.getYecaiFiled(),
                contractQueryInfo, formData);
    }

    private boolean setMappedValue(ContSyncRequest request, ContSyncLineRequest lineRequest, boolean hasLineValue,
                                   String zhishuField, String yuecaiField, ContractQueryResponse contractQueryInfo,
                                   Map<String, Object> formData) {
        Object value = getZhishuMappedValue(zhishuField, contractQueryInfo, formData);
        if (value == null || yuecaiField == null || yuecaiField.trim().isEmpty()) {
            return hasLineValue;
        }
        if (setFieldValue(request, yuecaiField, value)) {
            return hasLineValue;
        }
        if (setFieldValue(lineRequest, yuecaiField, value)) {
            return true;
        }
        return hasLineValue;
    }

    private Object getZhishuMappedValue(String zhishuField, ContractQueryResponse contractQueryInfo, Map<String, Object> formData) {
        if (zhishuField == null || zhishuField.trim().isEmpty() || contractQueryInfo == null) {
            return null;
        }
        if (zhishuField.startsWith("custom_")) {
            return joinListValue(formData.get(zhishuField));
        }
        if (ZhishuAndYecaiFiledEnum.TOTAL_AMOUNT.getZhishuFiled().equals(zhishuField)) {
            return contractQueryInfo.getAmount();
        }
        if (ZhishuAndYecaiFiledEnum.CONTRACT_CATEGORY_ABBREVIATION.getZhishuFiled().equals(zhishuField)) {
            return contractQueryInfo.getContractCategoryAbbreviation();
        }
        if (ZhishuAndYecaiFiledEnum.PAY_TYPE.getZhishuFiled().equals(zhishuField)) {
            return contractQueryInfo.getPayTypeCode();
        }
        if (ZhishuAndYecaiFiledEnum.FIXED_VALIDITY.getZhishuFiled().equals(zhishuField)) {
            return contractQueryInfo.getFixedValidityCode();
        }
        if (ZhishuAndYecaiFiledEnum.DATE_RANGE.getZhishuFiled().equals(zhishuField)) {
            return contractQueryInfo.getStartDate();
        }
        if (ZhishuAndYecaiFiledEnum.PAYMENT_AMOUNT.getZhishuFiled().equals(zhishuField)||ZhishuAndYecaiFiledEnum.PAYMENT_AMOUNT2.getZhishuFiled().equals(zhishuField)) {
            return getFirstPaymentPlanValue(contractQueryInfo, "paymentAmount");
        }
        if (ZhishuAndYecaiFiledEnum.PAYMENT_DATE.getZhishuFiled().equals(zhishuField)) {
            return getFirstPaymentPlanValue(contractQueryInfo, "paymentDate");
        }
        if (ZhishuAndYecaiFiledEnum.COLLECTION_AMOUNT.getZhishuFiled().equals(zhishuField)||ZhishuAndYecaiFiledEnum.COLLECTION_AMOUNT2.getZhishuFiled().equals(zhishuField)) {
            return getFirstCollectionPlanValue(contractQueryInfo, "collectionAmount");
        }
        if ("legal_entity".equals(zhishuField)) {
            return contractQueryInfo.getOurPartyList() != null && !contractQueryInfo.getOurPartyList().isEmpty()
                    ? contractQueryInfo.getOurPartyList().get(0).getOurPartyCode() : null;
        }
        if ("trading_party".equals(zhishuField)) {
            return contractQueryInfo.getCounterPartyList() != null && !contractQueryInfo.getCounterPartyList().isEmpty()
                    ? contractQueryInfo.getCounterPartyList().get(0).getCounterPartyCode() : null;
        }
        return getBeanFieldValue(contractQueryInfo, snakeToCamel(zhishuField));
    }

    private Object joinListValue(Object value) {
        if (!(value instanceof Collection)) {
            return value;
        }
        List<String> values = new ArrayList<>();
        for (Object item : (Collection<?>) value) {
            if (item != null && !String.valueOf(item).trim().isEmpty()) {
                values.add(String.valueOf(item));
            }
        }
        return values.isEmpty() ? null : String.join(",", values);
    }

    private Object getZhishuMappedLineValue(String zhishuField, ContractQueryResponse contractQueryInfo,
                                            Map<String, Object> formData, int lineIndex) {
        if (zhishuField == null || zhishuField.trim().isEmpty() || contractQueryInfo == null) {
            return null;
        }
        if (zhishuField.startsWith("custom_")) {
            Object value = getLineValue(formData.get(zhishuField), lineIndex);
            if (value != null) {
                return value;
            }
            return getPaymentCustomAttributeValue(contractQueryInfo, lineIndex, zhishuField);
        }
        if (ZhishuAndYecaiFiledEnum.PAYMENT_AMOUNT.getZhishuFiled().equals(zhishuField)) {
            return getPaymentPlanValue(contractQueryInfo, lineIndex, "paymentAmount");
        }
        if (ZhishuAndYecaiFiledEnum.PAYMENT_DATE.getZhishuFiled().equals(zhishuField)) {
            return getPaymentPlanValue(contractQueryInfo, lineIndex, "paymentDate");
        }
        if (ZhishuAndYecaiFiledEnum.CURRENCY_CODE.getZhishuFiled().equals(zhishuField)) {
            Object value = getPaymentPlanValue(contractQueryInfo, lineIndex, "currencyCode");
            if (value == null) {
                value = getCollectionPlanValue(contractQueryInfo, lineIndex, "currencyCode");
            }
            return value != null ? value : contractQueryInfo.getCurrencyCode();
        }
        if (ZhishuAndYecaiFiledEnum.COLLECTION_AMOUNT.getZhishuFiled().equals(zhishuField)) {
            return getCollectionPlanValue(contractQueryInfo, lineIndex, "collectionAmount");
        }
        if (ZhishuAndYecaiFiledEnum.COLLECTION_DATE.getZhishuFiled().equals(zhishuField)) {
            return getCollectionPlanValue(contractQueryInfo, lineIndex, "collectionDate");
        }
        return getLineValue(getZhishuMappedValue(zhishuField, contractQueryInfo, formData), lineIndex);
    }

    private Object getPaymentCustomAttributeValue(ContractQueryResponse contractQueryInfo, int lineIndex, String zhishuField) {
        if (ZhishuAndYecaiFiledEnum.PAYMENT_ACCEPTED_FLAG.getZhishuFiled().equals(zhishuField)) {
            return getPaymentCustomAttributeValueByName(contractQueryInfo, lineIndex, "是否需要验收");
        }
        if (ZhishuAndYecaiFiledEnum.ACCEPTANCE_TIME_AGREEMENT.getZhishuFiled().equals(zhishuField)) {
            return getPaymentCustomAttributeValueByName(contractQueryInfo, lineIndex, "验收时间（天）");
        }
        if (ZhishuAndYecaiFiledEnum.PAYMENT_NODE_TYPE.getZhishuFiled().equals(zhishuField)) {
            return getPaymentCustomAttributeValueByName(contractQueryInfo, lineIndex, "付款性质");
        }
        return null;
    }

    private Object getPaymentCustomAttributeValueByName(ContractQueryResponse contractQueryInfo, int lineIndex, String attributeName) {
        Object customAttributes = getPaymentPlanValue(contractQueryInfo, lineIndex, "paymentCustomAttributes");
        if (customAttributes == null || String.valueOf(customAttributes).trim().isEmpty()) {
            return null;
        }
        JSONArray attributes;
        try {
            attributes = JSONArray.parseArray(String.valueOf(customAttributes));
        } catch (Exception e) {
            log.warn("解析付款计划自定义字段失败, lineIndex={}, attributes={}", lineIndex, customAttributes, e);
            return null;
        }
        if (attributes == null) {
            return null;
        }
        for (Object item : attributes) {
            JSONObject attribute = JSONObject.parseObject(JSON.toJSONString(item));
            if (!attributeName.equals(attribute.getString("attribute_name"))) {
                continue;
            }
            Object attributeValue = attribute.get("attribute_value");
            if (attributeValue instanceof JSONObject && ((JSONObject) attributeValue).containsKey("name")) {
                return getRadioNameValue(attributeValue);
            }
            return attributeValue;
        }
        return null;
    }

    private Object getLineValue(Object value, int lineIndex) {
        if (value instanceof List) {
            List<?> values = (List<?>) value;
            return lineIndex >= 0 && lineIndex < values.size() ? values.get(lineIndex) : null;
        }
        return lineIndex == 0 ? value : null;
    }

    private Object getPaymentPlanValue(ContractQueryResponse contractQueryInfo, int lineIndex, String fieldName) {
        if (contractQueryInfo.getPaymentPlanList() == null
                || lineIndex < 0
                || lineIndex >= contractQueryInfo.getPaymentPlanList().size()) {
            return null;
        }
        return getBeanFieldValue(contractQueryInfo.getPaymentPlanList().get(lineIndex), fieldName);
    }

    private Object getCollectionPlanValue(ContractQueryResponse contractQueryInfo, int lineIndex, String fieldName) {
        if (contractQueryInfo.getCollectionPlanList() == null
                || lineIndex < 0
                || lineIndex >= contractQueryInfo.getCollectionPlanList().size()) {
            return null;
        }
        return getBeanFieldValue(contractQueryInfo.getCollectionPlanList().get(lineIndex), fieldName);
    }

    private Object getFirstPaymentPlanValue(ContractQueryResponse contractQueryInfo, String fieldName) {
        if (contractQueryInfo.getPaymentPlanList() == null || contractQueryInfo.getPaymentPlanList().isEmpty()) {
            return null;
        }
        return getBeanFieldValue(contractQueryInfo.getPaymentPlanList().get(0), fieldName);
    }

    private Object getFirstCollectionPlanValue(ContractQueryResponse contractQueryInfo, String fieldName) {
        if (contractQueryInfo.getCollectionPlanList() == null || contractQueryInfo.getCollectionPlanList().isEmpty()) {
            return null;
        }
        return getBeanFieldValue(contractQueryInfo.getCollectionPlanList().get(0), fieldName);
    }

    private Object getBeanFieldValue(Object bean, String fieldName) {
        if (bean == null || fieldName == null || fieldName.isEmpty()) {
            return null;
        }
        try {
            String methodName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            return bean.getClass().getMethod(methodName).invoke(bean);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean setFieldValue(Object bean, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = getField(bean.getClass(), fieldName);
            if (field == null) {
                return false;
            }
            field.setAccessible(true);
            field.set(bean, convertValue(value, field.getType()));
            return true;
        } catch (Exception e) {
            log.warn("字段映射赋值失败, target={}, field={}, value={}", bean.getClass().getSimpleName(), fieldName, value, e);
            return false;
        }
    }

    private java.lang.reflect.Field getField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private Object convertValue(Object value, Class<?> targetType) throws Exception {
        if (value == null) {
            return null;
        }
        if (targetType.isAssignableFrom(value.getClass())) {
            return value;
        }
        if (value instanceof List && !((List<?>) value).isEmpty()) {
            value = ((List<?>) value).get(0);
        }
        if (value instanceof Map && targetType == Date.class) {
            Object startDate = ((Map<?, ?>) value).get("start_date");
            return startDate == null ? null : parseDateValue(String.valueOf(startDate));
        }
        String text = String.valueOf(value);
        if (targetType == String.class) {
            return text;
        }
        if (targetType == BigDecimal.class) {
            return new BigDecimal(text);
        }
        if (targetType == Long.class || targetType == Long.TYPE) {
            return Long.valueOf(text);
        }
        if (targetType == Integer.class || targetType == Integer.TYPE) {
            return Integer.valueOf(text);
        }
        if (targetType == Boolean.class || targetType == Boolean.TYPE) {
            return Boolean.valueOf(text);
        }
        if (targetType == Date.class) {
            return parseDateValue(text);
        }
        return value;
    }

    private Date parseDateValue(String text) throws Exception {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        String dateText = text.trim();
        if (dateText.contains(",")) {
            dateText = dateText.substring(0, dateText.indexOf(",")).trim();
        }
        if (dateText.contains("~")) {
            dateText = dateText.substring(0, dateText.indexOf("~")).trim();
        }
        if (dateText.contains("\u81F3")) {
            dateText = dateText.substring(0, dateText.indexOf("\u81F3")).trim();
        }
        List<String> patterns = Arrays.asList("yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd", "yyyy/MM/dd HH:mm:ss", "yyyy/MM/dd");
        for (String pattern : patterns) {
            try {
                return new SimpleDateFormat(pattern).parse(dateText);
            } catch (Exception ignored) {
            }
        }
        return new Date(Long.parseLong(dateText));
    }

    private String snakeToCamel(String value) {
        StringBuilder builder = new StringBuilder();
        boolean upperNext = false;
        for (char c : value.toCharArray()) {
            if (c == '_') {
                upperNext = true;
            } else if (upperNext) {
                builder.append(Character.toUpperCase(c));
                upperNext = false;
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }




    private Map<String,Object> makeAmountData(BigDecimal depositBig,int currencyCode){
        Map<String,Object> result = new HashMap<>();
        result.put("amount", String.valueOf(depositBig));
        result.put("currency", currencyCode);
        return result;
    }

    private Map<String, Object> getDateRangeValue(Object attributeValue) {
        Map<String, Object> dateRange = new LinkedHashMap<>();
        dateRange.put("start_date", getObjectValue(attributeValue, "start_date"));
        dateRange.put("end_date", getObjectValue(attributeValue, "end_date"));
        return dateRange;
    }

    @SuppressWarnings("unchecked")
    private void addArrayRowsToFormData(Map<String, Object> formData, Object arrayValue) {
        if (!(arrayValue instanceof List)) {
            return;
        }
        for (Object rowValue : (List<?>) arrayValue) {
            if (!(rowValue instanceof Map)) {
                continue;
            }
            Map<String, Object> rowData = (Map<String, Object>) rowValue;
            for (Map.Entry<String, Object> entry : rowData.entrySet()) {
                Object existingValue = formData.get(entry.getKey());
                List<Object> rowValues;
                if (existingValue instanceof List) {
                    rowValues = (List<Object>) existingValue;
                } else {
                    rowValues = new ArrayList<>();
                    if (existingValue != null) {
                        rowValues.add(existingValue);
                    }
                    formData.put(entry.getKey(), rowValues);
                }
                rowValues.add(entry.getValue());
            }
        }
    }

    private Object getRadioNameValue(Object attributeValue) {
        Object name = getObjectValue(attributeValue, "name");
        if (name == null) {
            Object key = getObjectValue(attributeValue, "key");
            name = getOptionNameByKey(attributeValue, key);
        }
        if (name == null) {
            return null;
        }
        String text = String.valueOf(name);
        if ("是".equals(text)) {
            return "Y";
        }
        if ("否".equals(text)) {
            return "N";
        }
        return name;
    }

    private Object getOptionNameByKey(Object attributeValue, Object key) {
        if (attributeValue == null || key == null) {
            return null;
        }
        Object options = getObjectValue(attributeValue, "outbound_options");
        JSONArray optionArray = null;
        if (options instanceof JSONArray) {
            optionArray = (JSONArray) options;
        } else if (options instanceof List) {
            optionArray = JSONArray.parseArray(JSON.toJSONString(options));
        }
        if (optionArray == null) {
            return null;
        }
        return findOptionName(optionArray, String.valueOf(key));
    }

    private Object findOptionName(JSONArray optionArray, String key) {
        for (Object option : optionArray) {
            JSONObject optionObject = JSONObject.parseObject(JSON.toJSONString(option));
            if (key.equals(String.valueOf(optionObject.get("key")))) {
                Object name = optionObject.get("name");
                return name != null ? name : optionObject.get("value");
            }
            Object children = optionObject.get("children");
            if (children != null) {
                Object childName = findOptionName(JSONArray.parseArray(JSON.toJSONString(children)), key);
                if (childName != null) {
                    return childName;
                }
            }
        }
        return null;
    }

    private Object getDepartmentValue(Object attributeValue) {
        Object value = getListOrObjectValue(attributeValue, "department_id");
        if (hasValue(value)) {
            return value;
        }
        value = getListOrObjectValue(attributeValue, "open_department_id");
        if (hasValue(value)) {
            return value;
        }
        return getListOrObjectValue(attributeValue, "lark_department_id");
    }

    private Object getApprovalContentValue(Object attributeValue) {
        Object content = getListOrObjectValue(attributeValue, "content");
        return hasValue(content) ? content : attributeValue;
    }

    private boolean hasValue(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Collection) {
            for (Object item : (Collection<?>) value) {
                if (item != null && !String.valueOf(item).trim().isEmpty()) {
                    return true;
                }
            }
            return false;
        }
        return !String.valueOf(value).trim().isEmpty();
    }

    private Object getObjectValue(Object attributeValue, String fieldName) {
        if (attributeValue == null) {
            return null;
        }
        if (attributeValue instanceof JSONObject) {
            return ((JSONObject) attributeValue).get(fieldName);
        }
        Object json = JSONObject.toJSON(attributeValue);
        if (json instanceof JSONObject) {
            return ((JSONObject) json).get(fieldName);
        }
        return attributeValue;
    }

    private Object getListOrObjectValue(Object attributeValue, String fieldName) {
        if (attributeValue == null) {
            return null;
        }
        if (attributeValue instanceof JSONArray) {
            return getListValue((JSONArray) attributeValue, fieldName);
        }
        if (attributeValue instanceof List) {
            JSONArray array = JSONArray.parseArray(JSON.toJSONString(attributeValue));
            return getListValue(array, fieldName);
        }
        Object json = JSONObject.toJSON(attributeValue);
        if (json instanceof JSONArray) {
            return getListValue((JSONArray) json, fieldName);
        }
        return getObjectValue(attributeValue, fieldName);
    }

    private List<Object> getListValue(JSONArray array, String fieldName) {
        List<Object> values = new ArrayList<>();
        for (Object item : array) {
            if (item instanceof JSONObject) {
                values.add(((JSONObject) item).get(fieldName));
            } else {
                Object json = JSONObject.toJSON(item);
                if (json instanceof JSONObject) {
                    values.add(((JSONObject) json).get(fieldName));
                } else {
                    values.add(item);
                }
            }
        }
        return values;
    }

    private List<Map<String, Object>> getArrayValue(Object attributeValue) {
        List<Map<String, Object>> rows = new ArrayList<>();
        if (attributeValue == null) {
            return rows;
        }
        JSONArray rowArray = JSONArray.parseArray(JSON.toJSONString(attributeValue));
        for (Object row : rowArray) {
            Map<String, Object> rowData = new HashMap<>();
            JSONArray columns = JSONArray.parseArray(JSON.toJSONString(row));
            for (Object column : columns) {
                ContractFormQueryResponse.FormAttribute formAttribute =
                        JSONObject.parseObject(JSON.toJSONString(column), ContractFormQueryResponse.FormAttribute.class);
                if (formAttribute == null || formAttribute.getAttributeCode() == null) {
                    continue;
                }
                rowData.put(formAttribute.getAttributeCode(), getFormAttributeValue(formAttribute));
                if (FormAttributeTypeEnum.DROPDOWN_RADIO.getCode().equals(formAttribute.getAttributeType())) {
                    rowData.put(formAttribute.getAttributeCode() + "_name",
                            getDropdownRadioNameValue(formAttribute.getAttributeValue()));
                }
            }
            rows.add(rowData);
        }
        return rows;
    }

    private Object getDropdownRadioNameValue(Object attributeValue) {
        Object name = getObjectValue(attributeValue, "name");
        if (name != null) {
            return name;
        }
        Object key = getObjectValue(attributeValue, "key");
        return getOptionNameByKey(attributeValue, key);
    }

    private Object getFormAttributeValue(ContractFormQueryResponse.FormAttribute formAttribute) {
        String attributeType = formAttribute.getAttributeType();
        Object attributeValue = formAttribute.getAttributeValue();
        if (attributeType == null) {
            return attributeValue;
        }
        if (attributeType.equals(FormAttributeTypeEnum.AMOUNT.getCode())) {
            return getObjectValue(attributeValue, "amount");
        }
        if (attributeType.equals(FormAttributeTypeEnum.DATE_RANGE.getCode())) {
            return getDateRangeValue(attributeValue);
        }
        if (attributeType.equals(FormAttributeTypeEnum.RADIO.getCode())) {
            return getRadioNameValue(attributeValue);
        }
        if (attributeType.equals(FormAttributeTypeEnum.DROPDOWN_RADIO.getCode())
                || attributeType.equals(FormAttributeTypeEnum.TREE_RADIO.getCode())) {
            return getObjectValue(attributeValue, "key");
        }
        if (attributeType.equals(FormAttributeTypeEnum.CHECKBOX.getCode())
                || attributeType.equals(FormAttributeTypeEnum.DROPDOWN_OPTION.getCode())
                || attributeType.equals(FormAttributeTypeEnum.TREE_OPTION.getCode())) {
            return getListOrObjectValue(attributeValue, "key");
        }
        if (attributeType.equals(FormAttributeTypeEnum.EMPLOYEE.getCode())) {
            return getListOrObjectValue(attributeValue, "user_id");
        }
        if (attributeType.equals(FormAttributeTypeEnum.DEPARTMENT.getCode())) {
            return getDepartmentValue(attributeValue);
        }
        if (attributeType.equals(FormAttributeTypeEnum.HYPERLINK.getCode())) {
            return getObjectValue(attributeValue, "url");
        }
        if (attributeType.equals(FormAttributeTypeEnum.COUNTRY_OR_REGION.getCode())) {
            return getListOrObjectValue(attributeValue, "country_code");
        }
        if (attributeType.equals(FormAttributeTypeEnum.FILE.getCode())) {
            return getListOrObjectValue(attributeValue, "file_id");
        }
        if (attributeType.equals(FormAttributeTypeEnum.FEISHU_APPROVAL.getCode())) {
            return getApprovalContentValue(attributeValue);
        }
        if (attributeType.equals(FormAttributeTypeEnum.ARRAY.getCode())
                || attributeType.equals(FormAttributeTypeEnum.COMMON_ARRAY.getCode())) {
            return getArrayValue(attributeValue);
        }
        return attributeValue;
    }

    public void saveSyncLog(String contractId, String syncType, String syncDirection,
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

    public ContractQueryResponse getContractInfo(String contractId){
        //合同id
        Map<String, Object> params = new HashMap<>();
        params.put("user_id_type", "user_id");
        ContractResponse contractInfo = zhishuContractClient.getContract(contractId, params);
        ContractQueryResponse contractQueryInfo = null;
        if(contractInfo!=null){
            Map<String, Object> data = contractInfo.getData();
            contractQueryInfo = JSONObject.parseObject(String.valueOf(data.get("contract")), ContractQueryResponse.class);
        }else{
            log.info("查询合同信息：id = {}，未查询到合同信息", contractId);
        }
        return contractQueryInfo;
    }



}
