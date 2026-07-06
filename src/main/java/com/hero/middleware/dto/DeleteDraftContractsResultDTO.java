package com.hero.middleware.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class DeleteDraftContractsResultDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer totalCount = 0;

    private Integer draftCount = 0;

    private Integer successCount = 0;

    private Integer failCount = 0;

    private Boolean completed = true;

    private String failureReason;

    private Long elapsedMillis = 0L;

    private List<Item> items = new ArrayList<>();

    public void addScannedCount(int count) {
        if (count > 0) {
            totalCount = totalCount + count;
        }
    }

    public void addSuccess(String contractId,
                           String contractNumber,
                           Integer contractStatusCode,
                           String contractStatusName) {
        Item item = buildItem(contractId, contractNumber, contractStatusCode, contractStatusName);
        item.setSuccess(true);
        items.add(item);
        refreshCounts();
    }

    public void addFailure(String contractId,
                           String contractNumber,
                           Integer contractStatusCode,
                           String contractStatusName,
                           String reason) {
        Item item = buildItem(contractId, contractNumber, contractStatusCode, contractStatusName);
        item.setSuccess(false);
        item.setReason(reason);
        items.add(item);
        refreshCounts();
    }

    public void markFailed(String reason) {
        completed = false;
        failureReason = reason;
    }

    public void refreshCounts() {
        int success = 0;
        int fail = 0;
        for (Item item : items) {
            if (Boolean.TRUE.equals(item.getSuccess())) {
                success++;
            } else {
                fail++;
            }
        }
        draftCount = items.size();
        successCount = success;
        failCount = fail;
    }

    private Item buildItem(String contractId,
                           String contractNumber,
                           Integer contractStatusCode,
                           String contractStatusName) {
        Item item = new Item();
        item.setContractId(contractId);
        item.setContractNumber(contractNumber);
        item.setContractStatusCode(contractStatusCode);
        item.setContractStatusName(contractStatusName);
        return item;
    }

    @Data
    public static class Item implements Serializable {

        private static final long serialVersionUID = 1L;

        private String contractId;

        private String contractNumber;

        private Integer contractStatusCode;

        private String contractStatusName;

        private Boolean success;

        private String reason;
    }
}
