package com.hero.middleware.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class CreateAntiBriberyContractResultDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Boolean success;

    private String errMessage;

    private String zhishuContractId;

    private String contractNumber;

    private String contractName;

    private String pcUrl;

    private String mobileUrl;
}
