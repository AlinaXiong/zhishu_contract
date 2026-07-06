package com.hero.middleware.client.feishu.request;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.io.Serializable;

@Data
public class FeiShuTokenRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @JSONField(name = "app_id")
    private String appId;

    @JSONField(name = "app_secret")
    private String appSecret;

    public FeiShuTokenRequest() {
    }

    public FeiShuTokenRequest(String appId, String appSecret) {
        this.appId = appId;
        this.appSecret = appSecret;
    }
}
