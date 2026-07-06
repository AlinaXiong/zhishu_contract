package com.hero.middleware.client.zhishu.response;

import lombok.Data;

import java.io.Serializable;

@Data
public class ZhishuTokenResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer code;

    private String msg;

    private String tenant_access_token;

    private Integer expire;

    public boolean isSuccess() {
        return code != null && code == 0;
    }

}
