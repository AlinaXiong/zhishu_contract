package com.hero.middleware.client.feishu.response;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class FeishuUserBatchInfoResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<FeishuUserInfoResponse.User> items;
}
