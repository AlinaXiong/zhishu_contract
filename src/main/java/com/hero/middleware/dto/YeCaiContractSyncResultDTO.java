package com.hero.middleware.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class YeCaiContractSyncResultDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer totalCount = 0;

    private Integer successCount = 0;

    private Integer failCount = 0;

    private Long elapsedMillis = 0L;

    private List<Item> items = new ArrayList<>();

    public void addItem(Item item) {
        items.add(item);
        refreshCount();
    }

    public void refreshCount() {
        int success = 0;
        int fail = 0;
        for (Item item : items) {
            if (item == null) {
                continue;
            }
            if ("成功".equals(item.getResult())) {
                success++;
            } else if ("失败".equals(item.getResult())) {
                fail++;
            }
        }
        successCount = success;
        failCount = fail;
        totalCount = items.size();
    }

    @Data
    public static class Item implements Serializable {

        private static final long serialVersionUID = 1L;

        private Integer index;

        private String contractNumber;

        private String zhishuContractId;

        private String result;

        private String errorMessage;

        /** 处理结果说明，例如跳过或失败的具体原因。 */
        private String remark;

        private LocalDateTime startTime;

        private LocalDateTime endTime;
    }
}
