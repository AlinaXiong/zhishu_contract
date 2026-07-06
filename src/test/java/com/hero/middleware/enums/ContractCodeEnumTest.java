package com.hero.middleware.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContractCodeEnumTest {

    @Test
    void shouldContainAllDocumentedBusinessTypes() {
        assertBusinessType(0, "合同申请");
        assertBusinessType(2, "合同变更（补充协议）");
        assertBusinessType(3, "合同终止");
        assertBusinessType(10, "合同组申请");
        assertBusinessType(12, "合同信息修改");
        assertBusinessType(13, "合同原始信息");
        assertBusinessType(14, "合同到期");
    }

    @Test
    void shouldContainAllDocumentedContractStages() {
        assertContractStage(0, "合同创建时");
        assertContractStage(5, "审批发起时");
        assertContractStage(6, "审批完成时");
        assertContractStage(7, "盖章发起时");
        assertContractStage(9, "归档发起时");
        assertContractStage(10, "归档完成时");
        assertContractStage(11, "节点通过时");
        assertContractStage(14, "节点拒绝时");
        assertContractStage(15, "节点撤回时");
        assertContractStage(20, "合同重新提交时");
        assertContractStage(21, "合同作废时");
        assertContractStage(108, "任务待办时");
        assertContractStage(109, "任务已办时");
        assertContractStage(110, "任务抄送时");
    }

    private void assertBusinessType(int code, String name) {
        assertEquals(name, ContractBusinessTypeEnum.getEnum(code).getName());
    }

    private void assertContractStage(int code, String name) {
        assertEquals(name, ContractStageEnum.getEnum(code).getName());
    }
}
