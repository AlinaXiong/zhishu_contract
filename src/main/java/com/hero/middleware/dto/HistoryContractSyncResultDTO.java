package com.hero.middleware.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class HistoryContractSyncResultDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer totalCount = 0;

    private Integer successCount = 0;

    private Integer failCount = 0;

    private Long elapsedMillis = 0L;

    private List<String> successContractNumbers = new ArrayList<>();

    private List<Failure> failures = new ArrayList<>();

    public void addSuccess(String contractNumber) {
        successContractNumbers.add(contractNumber);
        successCount = successContractNumbers.size();
        refreshTotalCount();
    }

    public void addFailure(String contractNumber, String flowType, String reason) {
        Failure failure = new Failure();
        failure.setContractNumber(contractNumber);
        failure.setFlowType(flowType);
        failure.setReason(reason);
        failures.add(failure);
        failCount = failures.size();
        refreshTotalCount();
    }

    public void refreshTotalCount() {
        totalCount = successContractNumbers.size() + failures.size();
    }

    @Data
    public static class Failure implements Serializable {

        private static final long serialVersionUID = 1L;

        private String contractNumber;

        private String flowType;

        private String reason;
    }
}
