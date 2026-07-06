package com.hero.middleware.client.zhishu.request;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class UpdateContractRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String contractId;

    private String contractName;

    private String contractType;

    private String partyA;

    private String partyB;

    private Map<String, Object> formData;

}
