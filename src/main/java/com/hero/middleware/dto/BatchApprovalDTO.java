package com.hero.middleware.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.List;

@Data
public class BatchApprovalDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotEmpty(message = "合同ID列表不能为空")
    private List<String> contractIds;

    @NotBlank(message = "审批人不能为空")
    private String approval;

    @NotBlank(message = "审批状态不能为空")
    private String approvalStatus;

    private String approvalOpinion;

}
