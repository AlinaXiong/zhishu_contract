package com.hero.middleware.client.yuecai.response;

import lombok.Data;

import java.io.Serializable;

@Data
public class YuecaiResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer code;

    private String message;

    private Boolean success;

}
