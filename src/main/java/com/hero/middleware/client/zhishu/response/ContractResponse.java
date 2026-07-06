package com.hero.middleware.client.zhishu.response;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class ContractResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer code;

    private String message;

    private String contractId;

    private String contractName;

    private String contractStatus;

    private Map<String, Object> data;

}
