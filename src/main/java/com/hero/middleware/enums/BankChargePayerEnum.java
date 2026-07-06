package com.hero.middleware.enums;

/**
 * 审批操作枚举
 * 通过第一个参数获取对应的第二个参数值
 */
public enum BankChargePayerEnum {

    OTHER_BEAR("cmpdgqgq900193b711asaj2kf", "1", "对方承担"),
    OUR_BEAR("cmpdgqgq9001b3b71e4glorpu", "2", "我方承担"),
    RESPECTIVE_BEAR("cmpdgqgq9001a3b7138ly4di8", "3", "各自承担");


    /**
     * 第一个参数 - 智书编码
     */
    private final String zhishuCode;

    /**
     * 第二个参数 - 业财编码
     */
    private final String yecaiCode;

    /**
     * 操作描述
     */
    private final String description;


    BankChargePayerEnum(String zhishuCode, String yecaiCode, String description) {
        this.zhishuCode = zhishuCode;
        this.yecaiCode = yecaiCode;
        this.description = description;
    }

    public String getZhishuCode() {
        return zhishuCode;
    }

    public String getYecaiCode() {
        return yecaiCode;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据第一个参数(zhishuCode)获取对应的枚举实例
     *
     * @param zhishuCode 第一个参数
     * @return 对应的枚举实例
     * @throws IllegalArgumentException 如果找不到对应的枚举
     */
    public static BankChargePayerEnum getByZhishuCode(String zhishuCode) {
        for (BankChargePayerEnum action : values()) {
            if (action.getZhishuCode().equals(zhishuCode)) {
                return action;
            }
        }
        throw new IllegalArgumentException("未找到对应的承担方编码，zhishuCode: " + zhishuCode);
    }

    /**
     * 根据第一个参数(code)获取第二个参数(commandType)
     *
     * @param zhishuCode 第一个参数
     * @return 第二个参数值
     */
    public static String getYecaiCodeByZhishuCode(String zhishuCode) {
        if(zhishuCode==null||zhishuCode.isEmpty()){
            return null;
        }
        return getByZhishuCode(zhishuCode).getYecaiCode();
    }

    public static BankChargePayerEnum getByDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            return null;
        }
        for (BankChargePayerEnum action : values()) {
            if (action.getDescription().equals(description.trim())) {
                return action;
            }
        }
        return null;
    }

    public static String getZhishuCodeByDescription(String description) {
        BankChargePayerEnum bankChargePayerEnum = getByDescription(description);
        return bankChargePayerEnum == null ? null : bankChargePayerEnum.getZhishuCode();
    }

    /**
     * 根据第一个参数(code)获取第三个参数(description)
     *
     * @param zhishuCode 第一个参数
     * @return 第三个参数值
     */
    public static String getDescriptionByZhishuCode(String zhishuCode) {
        return getByZhishuCode(zhishuCode).getDescription();
    }

    @Override
    public String toString() {
        return String.format("ApprovalEnum{code='%s', yecaiCode='%s', description='%s'}",
                zhishuCode, yecaiCode, description);
    }
}
