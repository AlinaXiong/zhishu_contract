package com.hero.middleware.client.feishu;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hero.middleware.client.feishu.request.FeiShuMessageSendRequest;
import com.hero.middleware.client.feishu.response.FeiShuMessageSendResponse;
import com.hero.middleware.client.feishu.response.FeishuUserBatchInfoResponse;
import com.hero.middleware.client.feishu.response.FeishuUserInfoResponse;
import com.hero.middleware.config.FeiShuApiConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class FeiShuApiClient {

    private static final int USER_INFO_BATCH_SIZE = 50;

    /**
     * 发送消息
     */
    private static final String SEND_MESSAGE_URL = "/open-apis/im/v1/messages?receive_id_type=:receive_id_type";

    private static final String GET_USER_INFO_URL = "/open-apis/contact/v3/users/:user_id?user_id_type=:user_id_type&department_id_type=:department_id_type";

    private static final String GET_USER_INFO_BATCH_URL = "/open-apis/contact/v3/users/batch";

    @Autowired
    private FeiShuTokenManager feiShuTokenManager;

    @Autowired
    private FeiShuApiConfig feiShuApiConfig;

    public FeiShuMessageSendResponse sendMessage(FeiShuMessageSendRequest request) {
        log.info("发送飞书消息-请求信息：{}", JSON.toJSONString(request));
        String tenantAccessToken = feiShuTokenManager.getAccessToken();
        String receiveIdType = request.getReceiveIdType() == null || request.getReceiveIdType().trim().isEmpty()
                ? "open_id" : request.getReceiveIdType();
        String url = feiShuApiConfig.getBaseUrl() + SEND_MESSAGE_URL.replace(":receive_id_type", receiveIdType);
        try {
            HttpResponse response = HttpRequest.post(url)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + tenantAccessToken)
                    .body(JSON.toJSONString(request))
                    .timeout(feiShuApiConfig.getTimeout())
                    .execute();
            String responseBody = response.body();
            log.info("发送飞书消息-响应状态码：{}", response.getStatus());
            log.info("发送飞书消息-返回信息：{}", responseBody);
            return JSON.parseObject(responseBody, FeiShuMessageSendResponse.class);
        } catch (Exception e) {
            log.error("发送飞书消息异常：{}", e.getMessage(), e);
            throw new RuntimeException("发送飞书消息失败：" + e.getMessage());
        }
    }

    public FeishuUserInfoResponse getUserInfo(String userId) {
        return getUserInfo(userId, "user_id", "open_department_id");
    }

    public FeishuUserBatchInfoResponse getUserInfoBatch(List<String> userIds) {
        return getUserInfoBatch(userIds, "user_id", "open_department_id");
    }

    public FeishuUserBatchInfoResponse getUserInfoBatch(List<String> userIds, String userIdType, String departmentIdType) {
        FeishuUserBatchInfoResponse resultResponse = new FeishuUserBatchInfoResponse();
        List<FeishuUserInfoResponse.User> allUsers = new ArrayList<>();
        resultResponse.setItems(allUsers);
        List<String> cleanUserIds = getCleanUserIds(userIds);
        if (cleanUserIds.isEmpty()) {
            return resultResponse;
        }
        String tenantAccessToken = feiShuTokenManager.getAccessToken();
        String finalUserIdType = userIdType == null || userIdType.trim().isEmpty() ? "user_id" : userIdType;
        String finalDepartmentIdType = departmentIdType == null || departmentIdType.trim().isEmpty()
                ? "open_department_id" : departmentIdType;
        for (int start = 0; start < cleanUserIds.size(); start += USER_INFO_BATCH_SIZE) {
            int end = Math.min(start + USER_INFO_BATCH_SIZE, cleanUserIds.size());
            List<String> batchUserIds = cleanUserIds.subList(start, end);
            FeishuUserBatchInfoResponse batchResponse = doGetUserInfoBatch(batchUserIds, finalUserIdType,
                    finalDepartmentIdType, tenantAccessToken);
            if (batchResponse != null && batchResponse.getItems() != null) {
                allUsers.addAll(batchResponse.getItems());
            }
        }
        return resultResponse;
    }

    private FeishuUserBatchInfoResponse doGetUserInfoBatch(List<String> userIds, String userIdType,
                                                           String departmentIdType, String tenantAccessToken) {
        String url = buildGetUserInfoBatchUrl(userIds, userIdType, departmentIdType);
        log.info("批量获取飞书用户信息 请求用户数量：{}，用户ID类型：{}，部门ID类型：{}", userIds.size(), userIdType, departmentIdType);
        try {
            HttpResponse response = HttpRequest.get(url)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + tenantAccessToken)
                    .timeout(feiShuApiConfig.getTimeout())
                    .execute();
            String responseBody = response.body();
            log.info("批量获取飞书用户信息 响应状态码：{}", response.getStatus());
            log.info("批量获取飞书用户信息 返回信息：{}", responseBody);
            JSONObject result = JSON.parseObject(responseBody);
            if (result != null && result.getInteger("code") != null && result.getInteger("code") == 0) {
                JSONObject data = result.getJSONObject("data");
                if (data == null) {
                    FeishuUserBatchInfoResponse emptyResponse = new FeishuUserBatchInfoResponse();
                    emptyResponse.setItems(new ArrayList<>());
                    return emptyResponse;
                }
                return JSON.parseObject(data.toJSONString(), FeishuUserBatchInfoResponse.class);
            }
            throw new RuntimeException("批量获取飞书用户信息失败：" + responseBody);
        } catch (Exception e) {
            log.error("批量获取飞书用户信息异常：{}", e.getMessage(), e);
            throw new RuntimeException("批量获取飞书用户信息失败：" + e.getMessage());
        }
    }

    private List<String> getCleanUserIds(List<String> userIds) {
        List<String> cleanUserIds = new ArrayList<>();
        if (userIds == null || userIds.isEmpty()) {
            return cleanUserIds;
        }
        for (String userId : userIds) {
            if (userId == null || userId.trim().isEmpty()) {
                continue;
            }
            cleanUserIds.add(userId.trim());
        }
        return cleanUserIds;
    }

    private String buildGetUserInfoBatchUrl(List<String> userIds, String userIdType, String departmentIdType) {
        StringBuilder urlBuilder = new StringBuilder(feiShuApiConfig.getBaseUrl())
                .append(GET_USER_INFO_BATCH_URL)
                .append("?user_id_type=").append(encodeQueryValue(userIdType))
                .append("&department_id_type=").append(encodeQueryValue(departmentIdType));
        for (String userId : userIds) {
            urlBuilder.append("&user_ids=").append(encodeQueryValue(userId));
        }
        return urlBuilder.toString();
    }

    private String encodeQueryValue(String value) {
        try {
            return URLEncoder.encode(value == null ? "" : value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("URL参数编码失败：" + e.getMessage());
        }
    }

    public FeishuUserInfoResponse getUserInfo(String userId, String userIdType, String departmentIdType) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("userId不能为空");
        }
        log.info("获取飞书用户信息 请求用户ID：{}，用户ID类型：{}，部门ID类型：{}", userId, userIdType, departmentIdType);
        String tenantAccessToken = feiShuTokenManager.getAccessToken();
        String finalUserIdType = userIdType == null || userIdType.trim().isEmpty() ? "open_id" : userIdType;
        String finalDepartmentIdType = departmentIdType == null || departmentIdType.trim().isEmpty()
                ? "open_department_id" : departmentIdType;
        String url = feiShuApiConfig.getBaseUrl() + GET_USER_INFO_URL
                .replace(":user_id_type", finalUserIdType)
                .replace(":department_id_type", finalDepartmentIdType)
                .replace(":user_id", userId);
        try {
            HttpResponse response = HttpRequest.get(url)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + tenantAccessToken)
                    .timeout(feiShuApiConfig.getTimeout())
                    .execute();
            String responseBody = response.body();
            log.info("获取飞书用户信息 响应状态码：{}", response.getStatus());
            log.info("获取飞书用户信息 返回信息：{}", responseBody);
            JSONObject result = JSON.parseObject(responseBody);
            if (result != null && result.getInteger("code") != null && result.getInteger("code") == 0) {
                return JSON.parseObject(result.getString("data"), FeishuUserInfoResponse.class);
            }
            throw new RuntimeException("获取飞书用户信息失败：" + responseBody);
        } catch (Exception e) {
            log.error("获取飞书用户信息异常：{}", e.getMessage(), e);
            throw new RuntimeException("获取飞书用户信息失败：" + e.getMessage());
        }
    }
}
