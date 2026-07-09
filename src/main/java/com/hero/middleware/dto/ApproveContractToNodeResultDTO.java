package com.hero.middleware.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class ApproveContractToNodeResultDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer totalCount = 0;

    private Integer successCount = 0;

    private Integer failCount = 0;

    private Long elapsedMillis = 0L;

    private List<Success> successes = new ArrayList<>();

    private List<Failure> failures = new ArrayList<>();

    public void addSuccess(String contractNumber, String contractId, String processInstanceId, String currentNodeName) {
        Success success = new Success();
        success.setContractNumber(contractNumber);
        success.setContractId(contractId);
        success.setProcessInstanceId(processInstanceId);
        success.setCurrentNodeName(currentNodeName);
        successes.add(success);
        refreshTotalCount();
    }

    public void addFailure(String contractNumber, String reason) {
        addFailure(contractNumber, reason, null);
    }

    public void addFailure(String contractNumber, String reason, String contractOwner) {
        addFailure(contractNumber, reason, contractOwner, null);
    }

    public void addFailure(String contractNumber, String reason, String contractOwner, String zhishuContractType) {
        Failure failure = new Failure();
        failure.setContractNumber(contractNumber);
        failure.setReason(reason);
        failure.setContractOwner(contractOwner);
        failure.setZhishuContractType(zhishuContractType);
        failures.add(failure);
        refreshTotalCount();
    }

    public void refreshTotalCount() {
        successCount = successes.size();
        failCount = failures.size();
        totalCount = successCount + failCount;
    }

    @Data
    public static class Success implements Serializable {

        private static final long serialVersionUID = 1L;

        private String contractNumber;

        private String contractId;

        private String processInstanceId;

        private String currentNodeName;
    }

    @Data
    public static class Failure implements Serializable {

        private static final long serialVersionUID = 1L;

        private String contractNumber;

        private String contractOwner;

        private String zhishuContractType;

        private String reason;
    }
}
