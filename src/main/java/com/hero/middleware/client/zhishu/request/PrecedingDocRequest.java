package com.hero.middleware.client.zhishu.request;

import lombok.Data;

@Data
public class PrecedingDocRequest {
    private String userId;
    private String keywords;
    private ContractType contractTypes;
    @Data
    public static class ContractType{
        private String level1Name;
        private String level1Id;
        private String level2Name;
        private String level2Id;
    }
}
