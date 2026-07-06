package com.hero.middleware.client.yuecai.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class SyncContractStatusRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String contractId;

    private String contractStatus;

    private String statusDesc;

}
