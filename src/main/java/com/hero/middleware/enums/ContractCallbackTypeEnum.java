package com.hero.middleware.enums;

import java.util.Arrays;

public enum ContractCallbackTypeEnum {
    /**
     * 枚举类型
     */
    COOPERATION("contract.contract.cooperation_v1", "协商事件"),
    INFO_CHANGE("contract.contract.info_change_v1", "合同信息变更"),
    STATUS_CHANGE("contract.contract.change_v1", "合同状态变更"),
    PAYMENT_CREATE("contract.contract.payment.create_v1", "合同新建付款"),
    VENDOR_CHANGE("mdm.vendor.change_v1", "供应商信息变更")
    ;

    ContractCallbackTypeEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    private String code;
    private String name;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static ContractCallbackTypeEnum getEnum(final String code) {
        if(code == null) {
            return null;
        }
        return Arrays.stream(ContractCallbackTypeEnum.values()).filter(z -> z.code.equals(code)).findFirst().orElse(null);
    }
}
