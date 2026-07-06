package com.hero.middleware.enums;

/**
 * 审批操作枚举
 * 通过第一个参数获取对应的第二个参数值
 */
public enum ApprovalEnum {

    /**
     * 审批同意 - 对应操作: 审批同意
     */
    APPROVE("APPROVE", "general", "审批同意"),

    /**
     * 审批拒绝 - 对应操作: 审批拒绝
     */
    REJECTBACK("REJECT", "rollBack", "审批拒绝");


    /**
     * 第一个参数 - 操作码
     */
    private final String code;

    /**
     * 第二个参数 - 智书合同操作编码
     */
    private final String commandType;

    /**
     * 操作描述
     */
    private final String description;


    ApprovalEnum(String code, String commandType, String description) {
        this.code = code;
        this.commandType = commandType;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getCommandType() {
        return commandType;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据第一个参数(code)获取对应的枚举实例
     *
     * @param code 第一个参数
     * @return 对应的枚举实例
     * @throws IllegalArgumentException 如果找不到对应的枚举
     */
    public static ApprovalEnum getByCode(String code) {
        for (ApprovalEnum action : values()) {
            if (action.getCode().equals(code)) {
                return action;
            }
        }
        throw new IllegalArgumentException("未找到对应的审批操作，code: " + code);
    }

    /**
     * 根据第一个参数(code)获取第二个参数(commandType)
     *
     * @param code 第一个参数
     * @return 第二个参数值
     */
    public static String getCommandTypeByCode(String code) {
        return getByCode(code).getCommandType();
    }

    /**
     * 根据第一个参数(code)获取第三个参数(description)
     *
     * @param code 第一个参数
     * @return 第三个参数值
     */
    public static String getDescriptionByCode(String code) {
        return getByCode(code).getDescription();
    }

    @Override
    public String toString() {
        return String.format("ApprovalEnum{code='%s', commandType='%s', description='%s'}",
                code, commandType, description);
    }
}
