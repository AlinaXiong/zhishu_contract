package com.hero.middleware.client.yuecai.response;

import lombok.Data;

import java.util.List;

@Data
public class ProcessInstanceResponse {
    private Long instanceId;
    private String businessKey;
    private Long tenantId;
    private List<ProcessTaskResponse> taskList;

    @Data
    public static class ProcessTaskResponse {
        private String taskCode;
        private String taskId;
    }
}
