package com.hero.middleware.enums;

/**
 * 智书合同状态枚举
 */
public enum ContractStatusEnum {

    EDITING(0, "编辑中"),
    VOIDED(1, "已作废"),
    WITHDRAWN(2, "已撤回"),
    APPROVING(3, "审批中"),
    REJECTED(4, "已拒绝"),
    SIGNING(6, "签订中"),
    ARCHIVING(8, "归档中"),
    ARCHIVED(9, "已归档"),
    CHANGING(10, "变更中"),
    CHANGED(11, "已变更"),
    OUR_SIGNED(12, "签订中，我方已签约"),
    COUNTERPARTY_SIGNED(13, "签订中，对方已签约"),
    TERMINATING(16, "终止中"),
    TERMINATED(17, "已终止"),
    PROCESS_INTERRUPTED(19, "审批流程被干预中止"),
    COMPLETED(23, "已完成"),
    NEGOTIATING(30, "协商中"),
    NEGOTIATION_DRAFT_EDITING(31, "协商草稿编辑中");

    private final Integer code;

    private final String name;

    ContractStatusEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public Integer getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static ContractStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ContractStatusEnum statusEnum : values()) {
            if (statusEnum.getCode().equals(code)) {
                return statusEnum;
            }
        }
        return null;
    }

    public static String getNameByCode(Integer code) {
        ContractStatusEnum statusEnum = getByCode(code);
        return statusEnum == null ? "未知状态" : statusEnum.getName();
    }
}
