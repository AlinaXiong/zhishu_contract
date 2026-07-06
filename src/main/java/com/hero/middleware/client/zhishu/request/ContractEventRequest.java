package com.hero.middleware.client.zhishu.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ContractEventRequest {
    /**
     * 合同阶段编码:0:合同创建时;5:审批发起时;6:审批完成时;7:盖章发起时;9:归档发起时;10：归档完成时;11:节点通过时;14:节点拒绝时;15:节点撤回时;20: 合同重新提交时;21: 合同作废时;
     */
    @JsonProperty(value = "contract_stage_code")
    private Integer contractStageCode;
    /**
     * 合同阶段名称
     */
    @JsonProperty(value = "contract_stage_name")
    private String contractStageName;
    /**
     * 合同业务类型编码:0：合同申请;2：合同变更（补充协议）;3：合同终止;10：合同组申请;12：合同信息修改;13：合同原始信息;14: 合同到期;
     */
    @JsonProperty(value = "business_type_code")
    private Integer businessTypeCode;
    /**
     * 上一份合同id:合同变更（补充协议）时，指发起补充协议的合同id;合同信息修改时，指发起修改的原合同id;合同终止时，指被终止的合同id;
     */
    @JsonProperty(value = "previous_id")
    private String previousId;
    /**
     * 上一份合同编号:合同变更时，指发起补充协议的合同编号;合同信息修改时，指发起修改的原合同编号;
     */
    @JsonProperty(value = "previous_contract_number")
    private String previousContractNumber;
    /**
     * 合同id
     */
    @JsonProperty(value = "contract_id")
    private String contractId;
    /**
     * 分组id
     */
    @JsonProperty(value = "group_id")
    private String groupId;
    /**
     * 合同编号
     */
    @JsonProperty(value = "contract_number")
    private String contractNumber;

    /**
     * 扩展信息
     */
    @JsonProperty(value = "extra_info")
    private ExtraInfo extraInfo;

    @Data
    public static class ExtraInfo {
        @JsonProperty(value = "node_id")
        private String nodeId;
        @JsonProperty(value = "node_name")
        private String nodeName;
    }
}
