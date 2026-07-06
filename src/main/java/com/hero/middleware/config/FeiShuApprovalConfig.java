package com.hero.middleware.config;

import lombok.Getter;

public enum FeiShuApprovalConfig {
    SUCCESS_STATE("节点成功状态码","11"),

    /**
     * 节点对应，多维表格字段
     */
    START_NODE("UserTask_approve_0:1", "上传扫描件"),
    NODE_1("UserTask_approve_1778471075026:1", "交接纸质文件"),
    NODE_2("UserTask_approve_1778471160099:1", "法务审核");

    @Getter
    private final String nodeId;

    @Getter
    private final String tableName;

    FeiShuApprovalConfig(String nodeId, String tableName) {
        this.nodeId = nodeId;
        this.tableName = tableName;
    }

    public static String getTableName(String nodeId) {
        for (FeiShuApprovalConfig e : values()) {
            if (e.getNodeId().equals(nodeId)) {
                return e.getTableName();
            }
        }
        return null;
    }
}
