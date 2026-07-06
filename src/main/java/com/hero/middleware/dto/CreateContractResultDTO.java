package com.hero.middleware.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class CreateContractResultDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String contractId;

    private String zhishuContractId;

    private String contractNumber;

    private String contractName;

    private String contractStatus;

    private String draftUrl;

    private String pcUrl;

    private String mobileUrl;

    private String errMessage;
}
