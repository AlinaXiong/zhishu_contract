package com.hero.middleware.enums;

import java.util.Arrays;

public enum ContractBusinessTypeEnum {

    CONTRACT_APPLICATION(0, "合同申请"),
    CONTRACT_CHANGE(2, "合同变更（补充协议）"),
    CONTRACT_TERMINATION(3, "合同终止"),
    CONTRACT_GROUP_APPLICATION(10, "合同组申请"),
    CONTRACT_INFO_CHANGE(12, "合同信息修改"),
    CONTRACT_ORIGINAL_INFO(13, "合同原始信息"),
    CONTRACT_EXPIRATION(14, "合同到期"),
    ;

    private final Integer code;

    private final String name;

    ContractBusinessTypeEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public Integer getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static ContractBusinessTypeEnum getEnum(final Integer code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(ContractBusinessTypeEnum.values())
                .filter(item -> item.code.equals(code))
                .findFirst()
                .orElse(null);
    }
}
