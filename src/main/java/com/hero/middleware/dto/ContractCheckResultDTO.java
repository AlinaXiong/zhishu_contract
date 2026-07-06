package com.hero.middleware.dto;

import lombok.Data;

@Data
public class ContractCheckResultDTO {
    private boolean success;
    private String code;
    private String errorMessage;
    private String errorTitle;
    private String linkUrl;
    private Object text1;
    private Object text2;
}
