package com.hero.middleware.client.zhishu.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class ZhishuTokenRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String appId;

    private String appSecret;

    public ZhishuTokenRequest() {
    }

    public ZhishuTokenRequest(String appId, String appSecret) {
        this.appId = appId;
        this.appSecret = appSecret;
    }

}
