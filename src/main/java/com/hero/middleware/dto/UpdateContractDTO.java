package com.hero.middleware.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Map;

@Data
public class UpdateContractDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "合同ID不能为空")
    private String contractId;

    private String contractName;

    private String contractType;

    private String partyA;

    private String partyB;

    private Map<String, Object> formData;

}
