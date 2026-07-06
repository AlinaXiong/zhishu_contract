package com.hero.middleware.client.zhishu.response;

import lombok.Data;

@Data
public class ResultResponse {
    private Integer code;
    private String msg;
    private Object data;
}
