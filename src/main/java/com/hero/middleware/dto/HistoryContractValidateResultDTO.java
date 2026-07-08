package com.hero.middleware.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class HistoryContractValidateResultDTO implements Serializable {

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

    public void addFailure(String contractNumber, String flowType, List<String> errors) {
        Failure failure = new Failure();
        failure.setContractNumber(contractNumber);
        failure.setFlowType(flowType);
        failure.setErrors(errors == null ? new ArrayList<>() : new ArrayList<>(errors));
        failures.add(failure);
        failCount = failures.size();
        refreshTotalCount();
    }

    public void addFailure(String contractNumber, String flowType, String error) {
        List<String> errors = new ArrayList<>();
        errors.add(error);
        addFailure(contractNumber, flowType, errors);
    }

    public void refreshTotalCount() {
        totalCount = successContractNumbers.size() + failures.size();
    }

    @Data
    public static class Failure implements Serializable {

        private static final long serialVersionUID = 1L;

        private String contractNumber;

        private String flowType;

        private List<String> errors = new ArrayList<>();
    }
}
