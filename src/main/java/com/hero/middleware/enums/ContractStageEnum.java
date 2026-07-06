package com.hero.middleware.enums;

import java.util.Arrays;

public enum ContractStageEnum {
    /**
     * 枚举类型
     */
    CREATE(0, "合同创建时"),
    AUDIT_START(5, "审批发起时"),
    AUDIT_FINISH(6, "审批完成时"),
    STAMP(7, "盖章发起时"),
    ARCHIVE_START(9, "归档发起时"),
    ARCHIVE_FINISH(10, "归档完成时"),
    NODE_PASS(11, "节点通过时"),
    NODE_NOT_PASS(14, "节点拒绝时"),
    NODE_BACK(15, "节点撤回时"),
    RE_SUBMIT(20, "合同重新提交时"),
    CANCEL(21, "合同作废时"),
    TASKS_DONE(108, "任务待办时"),
    TASKS_COMPLETED(109, "任务已办时"),
    TASKS_CC(110, "任务抄送时"),
    ;

    ContractStageEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    private Integer code;
    private String name;

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static ContractStageEnum getEnum(final Integer code) {
        if(code == null) {
            return null;
        }
        return Arrays.stream(ContractStageEnum.values()).filter(z -> z.code.equals(code)).findFirst().orElse(null);
    }
}
