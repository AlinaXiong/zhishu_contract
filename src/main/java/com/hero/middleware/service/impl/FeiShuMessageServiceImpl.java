package com.hero.middleware.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hero.middleware.client.feishu.FeiShuApiClient;
import com.hero.middleware.client.feishu.request.FeiShuMessageSendRequest;
import com.hero.middleware.client.feishu.response.FeiShuMessageSendResponse;
import com.hero.middleware.client.zhishu.ZhishuContractClient;
import com.hero.middleware.client.zhishu.request.ContractsSearchRequest;
import com.hero.middleware.client.zhishu.response.ContractQueryResponse;
import com.hero.middleware.client.zhishu.response.ContractResponse;
import com.hero.middleware.client.zhishu.response.ContractsSearchResponse;
import com.hero.middleware.enums.ZhishuAndYecaiFiledEnum;
import com.hero.middleware.service.FeiShuMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
public class FeiShuMessageServiceImpl implements FeiShuMessageService {

    private static final int CONTRACT_SEARCH_PAGE_SIZE = 100;

    private static final Set<Long> REMIND_DAYS = new HashSet<>(Arrays.asList(30L, 20L, 10L, 0L));

    @Autowired
    private ZhishuContractClient zhishuContractClient;

    @Autowired
    private FeiShuApiClient feiShuApiClient;

    @Override
    public void sendContractExpireRemindMessage() {
        Set<String> sentUserDayKeys = new HashSet<>();
        String pageToken = null;
        while (true) {
            ContractsSearchRequest request = new ContractsSearchRequest();
            request.setPageSize(CONTRACT_SEARCH_PAGE_SIZE);
            request.setPageToken(pageToken);
            ContractsSearchRequest.CombineCondition combineCondition = new ContractsSearchRequest.CombineCondition();
            combineCondition.setContractStatus(9);
            request.setCombineCondition(combineCondition);
            ContractsSearchResponse response = zhishuContractClient.searchContracts(request);
            if (response == null || !response.isSuccess() || response.getData() == null) {
                log.info("搜索智书合同失败或返回为空：{}", JSON.toJSONString(response));
                return;
            }

            List<ContractQueryResponse> contracts = response.getData().getItems();
            if (contracts != null && !contracts.isEmpty()) {
                for (ContractQueryResponse contract : contracts) {
                    handleContractRemind(contract, sentUserDayKeys);
                }
            }

            if (!Boolean.TRUE.equals(response.getData().getHasMore())) {
                break;
            }
            String nextPageToken = response.getData().getNextPageToken();
            if (!hasText(nextPageToken)) {
                nextPageToken = response.getData().getPageToken();
            }
            if (!hasText(nextPageToken) || nextPageToken.equals(pageToken)) {
                log.info("搜索智书合同返回hasMore=true，但未返回有效下一页pageToken，结束分页查询");
                break;
            }
            pageToken = nextPageToken;
        }
    }

    private void handleContractRemind(ContractQueryResponse contract, Set<String> sentUserDayKeys) {
        if (contract == null) {
            return;
        }
        ContractQueryResponse contractInfo = contract;
        if (!hasText(contractInfo.getForm()) || !hasText(contractInfo.getEndDate())) {
            ContractQueryResponse detail = getContractDetail(contractInfo.getContractId());
            if (detail != null) {
                contractInfo = mergeContractInfo(contractInfo, detail);
            }
        }

        long remainDays = getRemainDays(contractInfo.getEndDate());
        log.info("合同：{}还剩{}天", contractInfo.getContractNumber(), remainDays);
        if (!REMIND_DAYS.contains(remainDays)) {
            return;
        }

        Set<String> projectManagers = getProjectManagers(contractInfo.getForm());
        if (projectManagers.isEmpty() && contractInfo.getContractId() != null) {
            ContractQueryResponse detail = getContractDetail(contractInfo.getContractId());
            if (detail != null) {
                if (hasText(detail.getEndDate()) && !hasText(contractInfo.getEndDate())) {
                    contractInfo.setEndDate(detail.getEndDate());
                }
                if (hasText(detail.getForm())) {
                    contractInfo.setForm(detail.getForm());
                }
                projectManagers = getProjectManagers(contractInfo.getForm());
            }
        }
        if (projectManagers.isEmpty()) {
            log.info("合同id = {} 未获取到项目经理字段，不发送飞书提醒", contractInfo.getContractId());
            return;
        }

        for (String openId : projectManagers) {
            String sentKey = openId + ":" + remainDays;
            if (!sentUserDayKeys.add(sentKey)) {
                log.info("项目经理 {} 剩余 {} 日提醒已发送，本次跳过重复发送", openId, remainDays);
                continue;
            }
            FeiShuMessageSendRequest request = new FeiShuMessageSendRequest();
            request.setReceiveId(openId);
            request.setMsgType("interactive");
            request.setContent(JSON.toJSONString(buildMessageCard("测试发送时间" + remainDays)));
            FeiShuMessageSendResponse feiShuMessageSendResponse = feiShuApiClient.sendMessage(request);
        }
    }

    private ContractQueryResponse mergeContractInfo(ContractQueryResponse base, ContractQueryResponse detail) {
        if (!hasText(base.getForm())) {
            base.setForm(detail.getForm());
        }
        if (!hasText(base.getEndDate())) {
            base.setEndDate(detail.getEndDate());
        }
        return base;
    }

    private ContractQueryResponse getContractDetail(Long contractId) {
        if (contractId == null) {
            return null;
        }
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("user_id_type", "user_id");
            ContractResponse contractResponse = zhishuContractClient.getContract(String.valueOf(contractId), params);
            if (contractResponse == null || contractResponse.getData() == null) {
                return null;
            }
            Object contract = contractResponse.getData().get("contract");
            return JSONObject.parseObject(JSON.toJSONString(contract), ContractQueryResponse.class);
        } catch (Exception e) {
            log.warn("查询智书合同详情失败，contractId={}", contractId, e);
            return null;
        }
    }

    private long getRemainDays(String endDate) {
        if (!hasText(endDate)) {
            return -1;
        }
        try {
            String dateText = endDate.length() > 10 ? endDate.substring(0, 10) : endDate;
            return ChronoUnit.DAYS.between(LocalDate.now(ZoneId.of("Asia/Shanghai")), LocalDate.parse(dateText));
        } catch (Exception e) {
            log.warn("解析合同结束日期失败，endDate={}", endDate, e);
            return -1;
        }
    }

    private Set<String> getProjectManagers(String form) {
        Set<String> projectManagers = new LinkedHashSet<>();
        if (!hasText(form)) {
            return projectManagers;
        }
        JSONArray formAttributes;
        try {
            formAttributes = JSONArray.parseArray(form);
        } catch (Exception e) {
            log.warn("解析合同form失败，form={}", form, e);
            return projectManagers;
        }
        String projectManagerCode = ZhishuAndYecaiFiledEnum.PROJECT_MANAGER.getZhishuFiled();
        for (Object item : formAttributes) {
            addProjectManagerUserIds(projectManagers, item, projectManagerCode);
        }
        return projectManagers;
    }

    private void addProjectManagerUserIds(Set<String> userIds, Object value, String projectManagerCode) {
        if (value == null || !hasText(projectManagerCode)) {
            return;
        }
        Object json = JSON.toJSON(value);
        if (json instanceof JSONArray) {
            for (Object item : (JSONArray) json) {
                addProjectManagerUserIds(userIds, item, projectManagerCode);
            }
            return;
        }
        if (!(json instanceof JSONObject)) {
            return;
        }
        JSONObject attribute = (JSONObject) json;
        if (projectManagerCode.equals(attribute.getString("attribute_code"))
                || projectManagerCode.equals(attribute.getString("attribute_key"))) {
            addUserIds(userIds, attribute.get("attribute_value"));
            return;
        }
        if (attribute.containsKey("attribute_value")) {
            addProjectManagerUserIds(userIds, attribute.get("attribute_value"), projectManagerCode);
        }
    }

    private void addUserIds(Set<String> userIds, Object value) {
        if (value == null) {
            return;
        }
        Object json = JSON.toJSON(value);
        if (json instanceof JSONArray) {
            for (Object item : (JSONArray) json) {
                addUserIds(userIds, item);
            }
            return;
        }
        if (json instanceof JSONObject) {
            JSONObject user = (JSONObject) json;
            String userId = firstText(user.getString("open_id"), user.getString("user_id"), user.getString("id"));
            addIfHasText(userIds, userId);
            return;
        }
        addIfHasText(userIds, String.valueOf(value));
    }

    private JSONObject buildMessageCard(String content) {
        JSONObject card = new JSONObject();

        JSONObject text = new JSONObject();
        text.put("tag", "plain_text");
        text.put("content", content);

        JSONObject element = new JSONObject();
        element.put("tag", "div");
        element.put("text", text);

        JSONArray elements = new JSONArray();
        elements.add(element);
        card.put("elements", elements);
        return card;
    }

    private void addIfHasText(Set<String> values, String value) {
        if (hasText(value)) {
            values.add(value.trim());
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String firstText(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (hasText(value)) {
                return value;
            }
        }
        return null;
    }
}
