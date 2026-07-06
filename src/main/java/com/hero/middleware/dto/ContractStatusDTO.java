package com.hero.middleware.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class ContractStatusDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String contractId;

    private String contractStatus;

    private String statusDesc;

}
