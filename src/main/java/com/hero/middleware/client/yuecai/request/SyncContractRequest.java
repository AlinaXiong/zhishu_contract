package com.hero.middleware.client.yuecai.request;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class SyncContractRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String contractId;

    private String contractName;

    private String contractType;

    private String contractStatus;

    private String partyA;

    private String partyB;

    private Map<String, Object> formData;

}
