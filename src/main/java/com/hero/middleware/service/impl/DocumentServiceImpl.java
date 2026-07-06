package com.hero.middleware.service.impl;

import cn.hutool.core.util.URLUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hero.middleware.annotation.SkipApiLogTable;
import com.hero.middleware.client.feishu.FeiShuApiClient;
import com.hero.middleware.client.feishu.response.FeishuUserBatchInfoResponse;
import com.hero.middleware.client.feishu.response.FeishuUserInfoResponse;
import com.hero.middleware.client.yuecai.YuecaiContractClient;
import com.hero.middleware.client.yuecai.response.*;
import com.hero.middleware.client.zhishu.ZhiShuVendorClient;
import com.hero.middleware.client.zhishu.ZhishuContractClient;
import com.hero.middleware.client.zhishu.request.PrecedingDocRequest;
import com.hero.middleware.client.zhishu.response.*;
import com.hero.middleware.config.YeCaiDataConfig;
import com.hero.middleware.config.YuecaiApiConfig;
import com.hero.middleware.dto.DocumentQueryDTO;
import com.hero.middleware.enums.MasterDataTypeEnum;
import com.hero.middleware.enums.SpecializedCategoriesEnum;
import com.hero.middleware.enums.ZhishuAndYecaiFiledEnum;
import com.hero.middleware.service.ContractService;
import com.hero.middleware.service.DocumentService;
import com.hero.middleware.service.YeCaiToZhiShuService;
import com.hero.middleware.utils.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@SkipApiLogTable
public class DocumentServiceImpl implements DocumentService {

    @Autowired
    private YuecaiContractClient yuecaiContractClient;
    @Autowired
    private YeCaiDataConfig yeCaiDataConfig;
    @Autowired
    private YuecaiApiConfig yuecaiApiConfig;
    @Autowired
    private YeCaiToZhiShuService yeCaiToZhiShuService;
    @Autowired
    private ContractService contractService;
    @Autowired
    private FeiShuApiClient feiShuApiClient;
    @Autowired
    private ZhiShuVendorClient zhiShuVendorClient;
    private static final String BASE_URL = "http://link.heroesports.com";
    private static final String DOCUMENT_LIST_URL = "/exp/requisition/list";//采购申请
    private static final String ORDER_INFO_URL = "/project/order-query/list";//订单信息
    private static final String ANCHOR_CARD_URL = "/hfbs/anchor-doc/document";//主播卡片

    @Override
    public DocumentListResponse getDocumentList(DocumentQueryDTO dto) {
        log.info("获取业财系统单据列表: {}", dto);

        Map<String, Object> params = new HashMap<>();
        if (dto.getDocumentType() != null) {
            params.put("documentType", dto.getDocumentType());
        }
        if (dto.getDocumentStatus() != null) {
            params.put("documentStatus", dto.getDocumentStatus());
        }
        if (dto.getPageNum() != null) {
            params.put("pageNum", dto.getPageNum());
        }
        if (dto.getPageSize() != null) {
            params.put("pageSize", dto.getPageSize());
        }

        DocumentListResponse response = yuecaiContractClient.getDocumentList(params);

        log.info("获取业财系统单据列表完成, total: {}", response != null ? response.getTotal() : 0);
        return response;
    }

    @Override
    public ResultResponse getOrderInfo(PrecedingDocRequest request) {
        ResultResponse response = new ResultResponse();
        String keywords = request.getKeywords();//搜索词
//        String userId = request.getUserId();//搜索词
        String userId = yeCaiDataConfig.getDocumentUserId();
        Map<String,Object> params = new HashMap<>();
        params.put("page", 0);
        params.put("size", 100);//查100条
        params.put("dataType", "ORDER");
        params.put("startTime", URLUtil.encode(yeCaiDataConfig.getStartTime()));
        if(keywords!=null&& !keywords.isEmpty()){
            try {
//                params.put("prjDimOrderValue", URLEncoder.encode(keywords, String.valueOf(StandardCharsets.UTF_8)));
                params.put("keyword", URLEncoder.encode(keywords, String.valueOf(StandardCharsets.UTF_8)));
            } catch (UnsupportedEncodingException e) {
//                params.put("prjDimOrderValue", keywords);
                params.put("keyword", keywords);
            }
        }
        MasterDataRes masterDataRes = yuecaiContractClient.getOrderInfo(params);
        List<PrecedingDocResponse.Receipts> resultList = new ArrayList<>();
        PrecedingDocResponse resultDto = new PrecedingDocResponse();
        resultDto.setTotal(Math.min(masterDataRes.getTotalElements(), 100));
        if(masterDataRes.getSize()>0){
//            Map<String, String> employeeCodeAndUserIdMap = yeCaiToZhiShuService.getEmployeeCodeAndUserIdMap();
            List<Object> content = masterDataRes.getContent();
            for(int i = 0;i<content.size();i++){
                PrecedingDocResponse.Receipts receipts = new PrecedingDocResponse.Receipts();
                OrderInfoResponse data = JSONObject.parseObject(content.get(i).toString(), OrderInfoResponse.class);
                data.setMemberList(new ArrayList<>());
//                receipts.setId(String.valueOf(data.getOrderHeaderId()));
                receipts.setId(String.valueOf(data.getPrjDimOrderValue()));
                receipts.setTitle(data.getOrderTitle());
                //TODO 待定字段 start
                receipts.setContent(data.getPrjDimOrderValue());//项目维度订单值
                receipts.setCreateTime(new Date().getTime());
                receipts.setMobileAppLink(BASE_URL+ORDER_INFO_URL);
                receipts.setPcAppLink(BASE_URL+ORDER_INFO_URL);
                //TODO 待定字段 end
//                if(data.getUserId()!=null){
//                    userId = data.getUserId();
//                }else{
//                    userId = yeCaiDataConfig.getDocumentUserId();
//                }
                receipts.setSponsor(userId);
                resultList.add(receipts);
            }
            response.setCode(0);
            response.setMsg("success");
        }else{
            response.setCode(0);
            response.setMsg("fail:未查询到数据信息");
        }
        resultDto.setReceipts(resultList);
        response.setData(resultDto);
        return response;
    }

    @Override
    public Map<String,Object> getOrderDetail(Map<String, Object> paramMap) {
        Map<String,Object> result = new HashMap<>();
//        try {
//            Thread.sleep(3000);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
        //合同id
//        String contractId = (String) paramMap.get("contractId");
        String paramMapStr = JSONObject.toJSONString(paramMap);
        JSONArray orderInfoList = JSONObject.parseArray(JSONObject.parseObject(paramMapStr).getString("orderInfoList"));
//        Map<String, Object> params = new HashMap<>();
//        params.put("user_id_type", "user_id");
//        ContractResponse contractInfo = zhishuContractClient.getContract(contractId, params);
//        ContractQueryResponse contractQueryInfo = null;
//        if(contractInfo!=null){
//            Map<String, Object> data = contractInfo.getData();
//            contractQueryInfo = JSONObject.parseObject(String.valueOf(data.get("contract")), ContractQueryResponse.class);
//        }else{
//            log.info("获取订单信息时，查询合同信息：id = {}未查询到合同信息", contractId);
//            result.put("code", 1);
//            result.put("msg", "fail");
//            return result;
//        }
//        Map<String, Object> formData = contractService.getContractFormData(contractQueryInfo);
//        String orderNumber = String.valueOf(formData.get("custom_1024_90a78c8120994f95b2dbfedd297c7d81")==null?"[]":formData.get("custom_1024_90a78c8120994f95b2dbfedd297c7d81"));
//        if(orderNumber==null||orderNumber.isEmpty()){
//            log.info("合同信息中为获取到订单信息，请确认：{}",JSONObject.toJSON(contractInfo));
//            result.put("code", 1);
//            result.put("msg", "fail");
//            return result;
//        }
//        List orderNumberList = (List<String>)formData.get("custom_1024_90a78c8120994f95b2dbfedd297c7d81");
//        JSONArray orderNumberArr = JSONObject.parseArray(orderNumber);
        Map<String,Object> resultMap = null;
        List<Map<String,Object>> orderDetailList = new ArrayList<>();
        if(orderInfoList!=null && !orderInfoList.isEmpty()){
//            Map<String, Map<String,Object>> specCategoryAllMap = new LinkedHashMap<>();
            Map<String,Object> userStatusMap = new HashMap<>();
            for(int i=0;i<orderInfoList.size();i++){
//            String orderNumber = String.valueOf(orderNumberList.get(i));
                String orderNumber = orderInfoList.getJSONObject(i).getString("serialId");
                PrecedingDocRequest request = new PrecedingDocRequest();
                request.setKeywords(orderNumber);
                Map<String,Object> queryParams = new HashMap<>();
                queryParams.put("page", 0);
                queryParams.put("size", 20);
                queryParams.put("dataType", "ORDER");
                queryParams.put("startTime", URLUtil.encode(yeCaiDataConfig.getStartTime()));
                queryParams.put("prjDimOrderValue", orderNumber);
                MasterDataRes masterDataRes = yuecaiContractClient.getOrderInfo(queryParams);
                List<Object> content = masterDataRes.getContent();
                OrderInfoResponse data = JSONObject.parseObject(content.get(0).toString(), OrderInfoResponse.class);
                List<String> orderDate = new ArrayList<>();
                orderDate.add(DateUtils.convertDateToString(data.getOrderStartDate(),"yyyy-MM-dd"));
                orderDate.add(DateUtils.convertDateToString(data.getOrderEndDate(),"yyyy-MM-dd"));
                resultMap = new HashMap<>();
                resultMap.put("orderTitle",data.getOrderTitle());//订单名称
                resultMap.put("prjDimOrderValue",data.getPrjDimOrderValue());//订单编号
                resultMap.put("costCenter", Collections.singletonList(data.getCostCenter()));//成本中心
                resultMap.put("orderDate",orderDate);//订单周期
                List<OrderInfoResponse.Member> memberList = data.getMemberList();
                Set<String> userIds = new HashSet<>();
                List<Map<String,String>> project_managerList = new ArrayList<>();
                List<Map<String,String>> expense_groupList = new ArrayList<>();
                List<Map<String,String>> project_acceptanceList = new ArrayList<>();
                List<Map<String,String>> project_budgetList = new ArrayList<>();
                List<Map<String,String>> project_sponosorList = new ArrayList<>();
                if(memberList!=null&& !memberList.isEmpty()){
                    for (OrderInfoResponse.Member member : memberList) {
                        if(userStatusMap.get(member.getUserId())==null){
                            userIds.add(member.getUserId());
                        }
//                        String roleCode = member.getRoleCode();
//                        Map<String,String> userMap = new HashMap<>();
//                        userMap.put("type","userId");
//                        userMap.put("userId",member.getUserId());
//                        FeishuUserInfoResponse userInfo = null;
//                        if(userStatusMap.get(member.getUserId())==null){
//                            userInfo = feiShuApiClient.getUserInfo(member.getUserId());
//                        }else{
//                            userInfo = (FeishuUserInfoResponse) userStatusMap.get(member.getUserId());
//                        }
//                        if(userInfo!=null){
//                            Boolean resigned = userInfo.getUser().getStatus().getResigned();
//                            userStatusMap.put(member.getUserId(),userInfo);
//                            if(!resigned){
//                                if("10".equals(roleCode)||"30".equals(roleCode)){//项目经理A、B
//                                    project_managerList.add(userMap);
//                                }else if("40".equals(roleCode)){
//                                    expense_groupList.add(userMap);
//                                }else if("50".equals(roleCode)){
//                                    project_acceptanceList.add(userMap);
//                                }else if("60".equals(roleCode)){
//                                    project_budgetList.add(userMap);
//                                }else if("70".equals(roleCode)){
//                                    project_sponosorList.add(userMap);
//                                }
//                            }else{
//                                log.info("前置单据-采购订单用户已离职，resigned状态：{}",resigned);
//                            }
//                        }else{
//                            log.info("前置单据-采购订单未获取到用户信息：{}",member.getUserId());
//                        }
                    }
                    if(!userIds.isEmpty()){
                        FeishuUserBatchInfoResponse userInfoBatch = feiShuApiClient.getUserInfoBatch(new ArrayList<>(userIds));
                        for (FeishuUserInfoResponse.User item : userInfoBatch.getItems()) {
                            userStatusMap.put(item.getUserId(),item);
                        }
                    }
                    for (OrderInfoResponse.Member member : memberList) {
                        String roleCode = member.getRoleCode();
                        Map<String,String> userMap = new HashMap<>();
                        userMap.put("type","userId");
                        userMap.put("userId",member.getUserId());
                        FeishuUserInfoResponse.User userInfo = null;
                        if(member.getUserId()!=null&&userStatusMap.get(member.getUserId())==null){
                            FeishuUserInfoResponse userInfoResponse = feiShuApiClient.getUserInfo(member.getUserId());
                            userInfo = userInfoResponse==null?null:userInfoResponse.getUser();
                        }else{
                            userInfo = (FeishuUserInfoResponse.User) userStatusMap.get(member.getUserId());
                        }
                        if(userInfo!=null){
                            Boolean resigned = userInfo.getStatus().getResigned();
                            userStatusMap.put(member.getUserId(),userInfo);
                            if(!resigned){
                                if("10".equals(roleCode)){//项目经理A
                                    project_managerList.add(userMap);
                                }else if("40".equals(roleCode)){
                                    expense_groupList.add(userMap);
                                }else if("50".equals(roleCode)){
                                    project_acceptanceList.add(userMap);
                                }else if("60".equals(roleCode)){
                                    project_budgetList.add(userMap);
                                }else if("30".equals(roleCode)){//项目经理B  add by lidongliang 20260702
                                    project_sponosorList.add(userMap);
                                }
                            }else{
                                log.info("前置单据-采购订单用户{}-{}-已离职，resigned状态：{}",userInfo.getUserId(),userInfo.getName(),resigned);
                            }
                        }else{
                            log.info("前置单据-采购订单未获取到用户信息：{}",member.getUserId());
                        }
                    }
                }
                resultMap.put("project_manager",project_managerList);//项目经理
                resultMap.put("expense_group",expense_groupList);//日常费用组
                resultMap.put("project_acceptance",project_acceptanceList);//项目验收岗
                resultMap.put("project_budget",project_budgetList);//项目预算岗
                resultMap.put("project_sponosor",project_sponosorList);//项目Sponosor
//                Map<String,Object> dropdown_radio = new HashMap<>();
//                List<Map<String,String>> mapList = new ArrayList<>();
//                Map<String,String> dropdown_radioMap = new HashMap<>();
//                dropdown_radioMap.put("label",data.getOrderType());
//                dropdown_radioMap.put("value","cmoi8em8e00513b71e61co28e");
//                mapList.add(dropdown_radioMap);
//                dropdown_radio.put("value",data.getOrderType());
//                dropdown_radio.put("options",mapList);
                resultMap.put("orderType",data.getProjectType());//订单类型
//                addSpecCategoryOptions(specCategoryAllMap, getSpecCategoryOptionsByOrderType(data.getProjectType()));
                orderDetailList.add(resultMap);
            }
//            Map<String,Object> specializedCategoryMap = new HashMap<>();
//            specializedCategoryMap.put("value","");
//            specializedCategoryMap.put("options",new ArrayList<>(specCategoryAllMap.values()));
//            result.put("specCategoryAllList", specializedCategoryMap);
        }
        log.info("订单信息详情获取明细行信息：{}", JSONObject.toJSON(orderDetailList));
        result.put("code", 0);
        result.put("msg", "success");
        result.put("orderDetailList", orderDetailList);
        return result;
    }

    @Override
    public ResultResponse getAnchorCardInfo(PrecedingDocRequest request) {
        ResultResponse response = new ResultResponse();
        String keywords = request.getKeywords();//搜索词
//        String userId = request.getUserId();//搜索词
        String userId = yeCaiDataConfig.getDocumentUserId();
        Map<String,Object> params = new HashMap<>();
        params.put("page", 0);
        params.put("size", 100);
//        MasterDataRes masterDataRes = yuecaiContractClient.getAnchorCard(params,keywords,"id");
        MasterDataRes masterDataRes = yuecaiContractClient.getAnchorCard(params,keywords,"keyword");
        List<PrecedingDocResponse.Receipts> resultList = new ArrayList<>();
        PrecedingDocResponse resultDto = new PrecedingDocResponse();
        resultDto.setTotal(Math.min(masterDataRes.getTotalElements(), 100));
        if(masterDataRes.getSize()>0){
            Map<String, String> employeeCodeAndUserIdMap = yeCaiToZhiShuService.getEmployeeCodeAndUserIdMap();
            List<Object> content = masterDataRes.getContent();
            for(int i = 0;i<content.size();i++){
                PrecedingDocResponse.Receipts receipts = new PrecedingDocResponse.Receipts();
                AnchorCardResponse data = JSONObject.parseObject(content.get(i).toString(), AnchorCardResponse.class);
                receipts.setId(String.valueOf(data.getHeaderId()));
                receipts.setTitle(data.getRealName());
                //TODO 待定字段 start
                receipts.setContent(data.getId());//订单id
                receipts.setCreateTime(new Date().getTime());
                receipts.setMobileAppLink(BASE_URL+ANCHOR_CARD_URL);
                receipts.setPcAppLink(BASE_URL+ANCHOR_CARD_URL);
                //TODO 待定字段 end
//                if(employeeCodeAndUserIdMap.get(data.getCreatorId())!=null
//                    &&!employeeCodeAndUserIdMap.get(data.getCreatorId()).isEmpty()){
//                    userId = employeeCodeAndUserIdMap.get(data.getCreatorId());
//                }else{
//                    userId = yeCaiDataConfig.getDocumentUserId();
//                }
                receipts.setSponsor(userId);
                resultList.add(receipts);
            }
            response.setCode(0);
            response.setMsg("success");
        }else{
            response.setCode(0);
            response.setMsg("fail:未查询到数据信息");
        }
        resultDto.setReceipts(resultList);
        response.setData(resultDto);
        return response;
    }

    @Override
    public Map<String, Object> getAnchorCardDetail(Map<String, Object> paramMap) {
        Map<String,Object> result = new HashMap<>();
//        try {
//            Thread.sleep(3000);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
        //合同id
//        String contractId = (String) paramMap.get("contractId");
        String paramMapStr = JSONObject.toJSONString(paramMap);
        JSONArray anchorCardList = JSONObject.parseArray(JSONObject.parseObject(paramMapStr).getString("anchorCardList"));
//        Map<String, Object> params = new HashMap<>();
//        params.put("user_id_type", "user_id");
//        ContractResponse contractInfo = zhishuContractClient.getContract(contractId, params);
//        ContractQueryResponse contractQueryInfo = null;
//        if(contractInfo!=null){
//            Map<String, Object> data = contractInfo.getData();
//            contractQueryInfo = JSONObject.parseObject(String.valueOf(data.get("contract")), ContractQueryResponse.class);
//        }else{
//            log.info("获取主播卡片详情时，查询合同信息：id = {}未查询到合同信息", contractId);
//            result.put("code", 1);
//            result.put("msg", "fail");
//            return result;
//        }
//        Map<String, Object> formData = contractService.getContractFormData(contractQueryInfo);
//        Object anchorCardNumberValue = formData.get(ZhishuAndYecaiFiledEnum.ANCHOR_DOCUMENT_NUMBER.getZhishuFiled());
//        List<?> anchorCardNumberList;
//        if (anchorCardNumberValue instanceof List) {
//            anchorCardNumberList = (List<?>) anchorCardNumberValue;
//        } else if (anchorCardNumberValue == null || String.valueOf(anchorCardNumberValue).isEmpty()) {
//            log.info("获取主播卡片详情时，合同id = {}未获取到主播卡片单据编码", contractId);
//            result.put("code", 1);
//            result.put("msg", "fail");
//            return result;
//        } else {
//            anchorCardNumberList = Collections.singletonList(anchorCardNumberValue);
//        }
        if(anchorCardList!=null&& !anchorCardList.isEmpty()){
            for(int i=0;i<anchorCardList.size();i++){
//                String anchorCardNumber = String.valueOf(anchorCardNumberList.get(i));
                String anchorCardNumber = anchorCardList.getJSONObject(i).getString("serialId");
                Map<String,Object> queryParams = new HashMap<>();
                queryParams.put("page", 0);
                queryParams.put("size", 20);
                MasterDataRes masterDataRes = yuecaiContractClient.getAnchorCard(queryParams,anchorCardNumber,"headerId");
                if(masterDataRes != null && masterDataRes.getSize()>0 && masterDataRes.getContent() != null){
                    List<Object> content = masterDataRes.getContent();
                    String certificateId = null;
                    for(int j = 0;j<content.size();j++){
                        AnchorCardResponse data = JSONObject.parseObject(content.get(j).toString(), AnchorCardResponse.class);
                        certificateId = data.getCertificateNumber();
                        result.put("realName",data.getRealName());
                        result.put("creatorId",data.getCreatorId());
                        result.put("certificateNumber",certificateId);
                        List<AnchorCardResponse.AnchorCardLineRes> lineResultDTOS = data.getLineResultDTOS();
                        if(lineResultDTOS!=null&& !lineResultDTOS.isEmpty()){
                            AnchorCardResponse.AnchorCardLineRes anchorCardLineRes = lineResultDTOS.get(0);
                            result.put("anchorNickname",anchorCardLineRes.getAnchorNickname());
                            result.put("anchorId",anchorCardLineRes.getAnchorId());
                            result.put("teamName",anchorCardLineRes.getTeamName());
                            result.put("platform",anchorCardLineRes.getPlatform());//所属平台
                            result.put("liveCategory",anchorCardLineRes.getLiveCategory());//直播品类
                        }
                    }
                    List<Map<String,String>> partyList = new ArrayList<>();
                    Map<String,String> tradingMap = new HashMap<>();
                    VendorInfoResponse vendorInfoResponse = zhiShuVendorClient.getVendorByCertificationId(certificateId);
                    if(vendorInfoResponse!=null){
                        tradingMap.put("type","trading_party");
                        tradingMap.put("code",vendorInfoResponse.getVendor());
                        partyList.add(tradingMap);
                        result.put("tradingMap",partyList);
                    }else{
                        result.put("tradingMap",partyList);
                        log.info("证件id={}未查询到交易方信息！！！", certificateId);
                    }
                }
            }
        }else{
            result.put("realName"," ");
            result.put("creatorId"," ");
            result.put("certificateNumber"," ");
            result.put("anchorNickname"," ");
            result.put("anchorId"," ");
            result.put("teamName"," ");
            result.put("platform"," ");//所属平台
            result.put("liveCategory"," ");//直播品类
            List<Map<String,String>> partyList = new ArrayList<>();
            Map<String,String> tradingMap = new HashMap<>();
            tradingMap.put("type","trading_party");
            tradingMap.put("code","");
            partyList.add(tradingMap);
            result.put("tradingMap",partyList);
        }
        result.put("code", 0);
        result.put("msg", "success");
        log.info("关联主播卡片返回结果：{}", JSONObject.toJSON(result));
        return result;
    }

    @Override
    public ResultResponse getProcurementInfo(PrecedingDocRequest request) {
        ResultResponse response = new ResultResponse();
        String keywords = request.getKeywords();//搜索词
//        String userId = request.getUserId();//搜索词
        String userId = yeCaiDataConfig.getDocumentUserId();
        Map<String,Object> params = new HashMap<>();
        params.put("page", 0);
        params.put("size", 100);
        MasterDataRes masterDataRes = yuecaiContractClient.getProcurement(params,keywords);
        List<PrecedingDocResponse.Receipts> resultList = new ArrayList<>();
        PrecedingDocResponse resultDto = new PrecedingDocResponse();
        resultDto.setTotal(Math.min(masterDataRes.getTotalElements(), 100));
        if(masterDataRes.getSize()>0){
            Map<String, String> employeeCodeAndUserIdMap = yeCaiToZhiShuService.getEmployeeCodeAndUserIdMap();
            List<Object> content = masterDataRes.getContent();
            for(int i = 0;i<content.size();i++){
                PrecedingDocResponse.Receipts receipts = new PrecedingDocResponse.Receipts();
                ProcurementResponse data = JSONObject.parseObject(content.get(i).toString(), ProcurementResponse.class);
                receipts.setId(String.valueOf(data.getExpRequisitionNumber()));
                receipts.setTitle(data.getAttribute1());//项目名称
                //TODO 待定字段 start
                receipts.setContent(data.getExpRequisitionNumber());//申请单号编码
                receipts.setCreateTime(DateUtils.convertStringToDate(data.getAttribute21(),null).getTime());
                receipts.setMobileAppLink(BASE_URL+DOCUMENT_LIST_URL);
                receipts.setPcAppLink(BASE_URL+DOCUMENT_LIST_URL);
                //TODO 待定字段 end
//                if(employeeCodeAndUserIdMap.get(data.getEmployeeCode())!=null
//                    &&!employeeCodeAndUserIdMap.get(data.getEmployeeCode()).isEmpty()){
//                    userId = employeeCodeAndUserIdMap.get(data.getEmployeeCode());
//                }else{
//                    userId = yeCaiDataConfig.getDocumentUserId();
//                }
                receipts.setSponsor(userId);
                resultList.add(receipts);
            }
            response.setCode(0);
            response.setMsg("success");
        }else{
            response.setCode(0);
            response.setMsg("fail:未查询到数据信息");
        }
        resultDto.setReceipts(resultList);
        response.setData(resultDto);

        return response;
    }

    @Override
    public Map<String, Object> getProcurementDetail(Map<String, Object> paramMap) {
        Map<String,Object> result = new HashMap<>();
//        try {
//            Thread.sleep(3000);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
        //合同id
//        String contractId = (String) paramMap.get("contractId");
        String paramMapStr = JSONObject.toJSONString(paramMap);
        JSONArray procurementList = JSONObject.parseArray(JSONObject.parseObject(paramMapStr).getString("procurementList"));
//        Map<String, Object> params = new HashMap<>();
//        params.put("user_id_type", "user_id");
//        ContractResponse contractInfo = zhishuContractClient.getContract(contractId, params);
//        ContractQueryResponse contractQueryInfo = null;
//        if(contractInfo!=null){
//            Map<String, Object> data = contractInfo.getData();
//            contractQueryInfo = JSONObject.parseObject(String.valueOf(data.get("contract")), ContractQueryResponse.class);
//        }else{
//            log.info("获取采购申请详情时，查询合同信息：id = {}未查询到合同信息", contractId);
//            result.put("code", 1);
//            result.put("msg", "fail");
//            return result;
//        }
//        Map<String, Object> formData = contractService.getContractFormData(contractQueryInfo);
//        Object procurementNumberValue = formData.get(ZhishuAndYecaiFiledEnum.PROCUREMENT_DOCUMENT_NUMBER.getZhishuFiled());
//        List<?> procurementNumberList;
//        if (procurementNumberValue instanceof List) {
//            procurementNumberList = (List<?>) procurementNumberValue;
//        } else if (procurementNumberValue == null || String.valueOf(procurementNumberValue).isEmpty()) {
//            log.info("获取采购申请详情时，合同id = {}未获取到采购申请单据编码", contractId);
//            result.put("code", 1);
//            result.put("msg", "fail");
//            return result;
//        } else {
//            procurementNumberList = Collections.singletonList(procurementNumberValue);
//        }
        ProcurementSpecCategoryResult specCategoryResult = getProcurementSpecCategoryResult(procurementList);
        Map<String,Object> specializedCategoryMap = new HashMap<>();
        List<Map<String,Object>> options = specCategoryResult.getOptions();
        if(options.isEmpty()){
            options = getAllSpecCategoryOptions();
        }
        specializedCategoryMap.put("value", specCategoryResult.getValue());
        result.put("acceptedFlag", specCategoryResult.getAcceptedFlagMap());
        specializedCategoryMap.put("options",options);
        Map<String,Object> amountMap = new HashMap<>();
        amountMap.put("currency", 1);
        amountMap.put("amount", specCategoryResult.getProcurementAmount());
        result.put("code", 0);
        result.put("msg", "success");
        result.put("procurementAmount",amountMap);
        result.put("specializedCategory",specializedCategoryMap);
        log.info("关联采购申请单返回结果：{}", JSONObject.toJSON(result));
        return result;
    }

    @Override
    public Map<String, Object> getspecCategoryList(Map<String, Object> paramMap) {
        JSONArray procurementList = getJsonArrayParam(paramMap, "procurementList");
        List<Map<String,Object>> options;
        if (hasJsonArrayValue(procurementList)) {
            options = getProcurementSpecCategoryResult(procurementList).getOptions();
        } else {
            JSONArray orderInfoList = getJsonArrayParam(paramMap, "orderInfoList");
            options = hasJsonArrayValue(orderInfoList) ? getOrderSpecCategoryOptions(orderInfoList) : getAllSpecCategoryOptions();
        }
        if (options == null || options.isEmpty()) {
            options = getAllSpecCategoryOptions();
        }
        return buildSpecCategoryResult(options);
    }

    private List<Map<String, Object>> getSpecCategoryOptionsByOrderType(String orderType) {
        List<Map<String,Object>> options = new ArrayList<>();
        if (orderType == null || orderType.trim().isEmpty()) {
            return options;
        }
        String orderTypeCode = orderType.trim();
        Map<String, Map<String,Object>> optionMap = new LinkedHashMap<>();
        for (SpecializedCategoriesEnum specializedCategoriesEnum : SpecializedCategoriesEnum.values()) {
            if (!containsOrderType(specializedCategoriesEnum.getOrderType(), orderTypeCode)) {
                continue;
            }
            addSpecCategoryOption(optionMap, specializedCategoriesEnum);
        }
        options.addAll(optionMap.values());
        return options;
    }

    private List<Map<String, Object>> getOrderSpecCategoryOptions(JSONArray orderInfoList) {
        Map<String, Map<String,Object>> optionMap = new LinkedHashMap<>();
        if (!hasJsonArrayValue(orderInfoList)) {
            return new ArrayList<>();
        }
        for(int i=0;i<orderInfoList.size();i++){
            String orderNumber = orderInfoList.getJSONObject(i).getString("serialId");
            if (orderNumber == null || orderNumber.trim().isEmpty()) {
                continue;
            }
            Map<String,Object> queryParams = new HashMap<>();
            queryParams.put("page", 0);
            queryParams.put("size", 20);
            queryParams.put("dataType", "ORDER");
            queryParams.put("startTime", URLUtil.encode(yeCaiDataConfig.getStartTime()));
            queryParams.put("prjDimOrderValue", orderNumber);
            MasterDataRes masterDataRes = yuecaiContractClient.getOrderInfo(queryParams);
            if (masterDataRes == null || masterDataRes.getContent() == null || masterDataRes.getContent().isEmpty()) {
                continue;
            }
            OrderInfoResponse data = JSONObject.parseObject(masterDataRes.getContent().get(0).toString(), OrderInfoResponse.class);
            addSpecCategoryOptions(optionMap, getSpecCategoryOptionsByOrderType(data.getProjectType()));
        }
        return new ArrayList<>(optionMap.values());
    }

    private ProcurementSpecCategoryResult getProcurementSpecCategoryResult(JSONArray procurementList) {
        ProcurementSpecCategoryResult result = new ProcurementSpecCategoryResult();
        result.setAcceptedFlagMap(contractService.specCategoryMapping(""));
        Map<String, Map<String,Object>> optionMap = new LinkedHashMap<>();
        if (!hasJsonArrayValue(procurementList)) {
            return result;
        }
        for(int i=0;i<procurementList.size();i++){
            String procurementNumber = procurementList.getJSONObject(i).getString("serialId");
            if (procurementNumber == null || procurementNumber.trim().isEmpty()) {
                continue;
            }
            Map<String,Object> queryParams = new HashMap<>();
            queryParams.put("page", 0);
            queryParams.put("size", 20);
            MasterDataRes masterDataRes = yuecaiContractClient.getProcurement(queryParams,procurementNumber);
            if(masterDataRes == null || masterDataRes.getSize() <= 0 || masterDataRes.getContent() == null){
                continue;
            }
            List<Object> content = masterDataRes.getContent();
            for(int j = 0;j<content.size();j++){
                ProcurementResponse data = JSONObject.parseObject(content.get(j).toString(), ProcurementResponse.class);
                List<ProcurementResponse.ProcurementLineRes> lineResultDTOS = data.getLineResultDTOS();
                if (lineResultDTOS == null) {
                    continue;
                }
                for (ProcurementResponse.ProcurementLineRes lineResultDTO : lineResultDTOS) {
                    if (lineResultDTO.getAttribute31() != null) {
                        result.addProcurementAmount(lineResultDTO.getAttribute31());
                    }
                    String specializedCategory = lineResultDTO.getSpecializedCategory();
                    SpecializedCategoriesEnum specializedCategoriesEnum = SpecializedCategoriesEnum.getByName(specializedCategory);
                    if(specializedCategoriesEnum == null){
                        continue;
                    }
                    result.setValue(specializedCategory);
                    result.setAcceptedFlagMap(contractService.specCategoryMapping(specializedCategoriesEnum.getCode()));
                    addSpecCategoryOption(optionMap, specializedCategoriesEnum);
                }
            }
        }
        result.setOptions(new ArrayList<>(optionMap.values()));
        return result;
    }

    private List<Map<String,Object>> getAllSpecCategoryOptions() {
        List<Map<String,Object>> options = new ArrayList<>();
        for (SpecializedCategoriesEnum specializedCategoriesEnum : SpecializedCategoriesEnum.values()) {
            options.add(buildSpecCategoryOption(specializedCategoriesEnum));
        }
        return options;
    }

    private Map<String,Object> buildSpecCategoryResult(List<Map<String,Object>> options) {
        Map<String,Object> result = new HashMap<>();
        Map<String,Object> specializedCategoryMap = new HashMap<>();
        specializedCategoryMap.put("value","");
        specializedCategoryMap.put("options",options);
        result.put("code", 0);
        result.put("msg", "success");
        result.put("specializedCategory", specializedCategoryMap);
        return result;
    }

    private void addSpecCategoryOptions(Map<String, Map<String,Object>> optionMap, List<Map<String,Object>> options) {
        if (optionMap == null || options == null) {
            return;
        }
        for (Map<String,Object> option : options) {
            if (option == null || option.get("value") == null) {
                continue;
            }
            optionMap.put(String.valueOf(option.get("value")), option);
        }
    }

    private void addSpecCategoryOption(Map<String, Map<String,Object>> optionMap, SpecializedCategoriesEnum specializedCategoriesEnum) {
        if (optionMap == null || specializedCategoriesEnum == null) {
            return;
        }
        optionMap.put(specializedCategoriesEnum.getCode(), buildSpecCategoryOption(specializedCategoriesEnum));
    }

    private Map<String,Object> buildSpecCategoryOption(SpecializedCategoriesEnum specializedCategoriesEnum) {
        Map<String,Object> optionsMap = new HashMap<>();
        optionsMap.put("label", specializedCategoriesEnum.getName());
        optionsMap.put("value", specializedCategoriesEnum.getCode());
        return optionsMap;
    }

    private JSONArray getJsonArrayParam(Map<String,Object> paramMap, String key) {
        if (paramMap == null || key == null || paramMap.get(key) == null) {
            return null;
        }
        Object value = paramMap.get(key);
        if (value instanceof JSONArray) {
            return (JSONArray) value;
        }
        if (value instanceof String) {
            String text = ((String) value).trim();
            return text.isEmpty() ? null : JSONObject.parseArray(text);
        }
        return JSONObject.parseArray(JSONObject.toJSONString(value));
    }

    private boolean hasJsonArrayValue(JSONArray jsonArray) {
        return jsonArray != null && !jsonArray.isEmpty();
    }

    private boolean containsOrderType(String orderTypes, String orderType) {
        if (orderTypes == null || orderTypes.trim().isEmpty()) {
            return false;
        }
        String[] orderTypeArray = orderTypes.split(",");
        for (String item : orderTypeArray) {
            if (orderType.equals(item.trim())) {
                return true;
            }
        }
        return false;
    }

    private static class ProcurementSpecCategoryResult {
        private Double procurementAmount = 0.0;
        private String value = "";
        private List<Map<String,Object>> options = new ArrayList<>();
        private Map<String,Object> acceptedFlagMap;

        private Double getProcurementAmount() {
            return procurementAmount;
        }

        private void addProcurementAmount(Double amount) {
            if (amount != null) {
                procurementAmount += amount;
            }
        }

        private String getValue() {
            return value;
        }

        private void setValue(String value) {
            this.value = value == null ? "" : value;
        }

        private List<Map<String, Object>> getOptions() {
            return options;
        }

        private void setOptions(List<Map<String, Object>> options) {
            this.options = options == null ? new ArrayList<>() : options;
        }

        private Map<String, Object> getAcceptedFlagMap() {
            return acceptedFlagMap;
        }

        private void setAcceptedFlagMap(Map<String, Object> acceptedFlagMap) {
            this.acceptedFlagMap = acceptedFlagMap;
        }
    }

}
