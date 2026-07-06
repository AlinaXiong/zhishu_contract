package com.hero.middleware.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class BatchApprovalResultDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer successCount;

    private Integer failCount;

    private List<ApprovalItem> successItems;

    private List<ApprovalItem> failItems;

    private String message;

    @Data
    public static class ApprovalItem implements Serializable {

        private static final long serialVersionUID = 1L;

        private String contractId;

        private String contractName;

        private Boolean success;

        private String message;

        private String nextApprover;

    }

}
