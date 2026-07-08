package com.hero.middleware.client.zhishu.response;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class SubmitContractResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer code;

    private String msg;

    private DataInfo data;

    public boolean isSuccess() {
        return code != null && code == 0;
    }

    @Data
    public static class DataInfo implements Serializable {

        private static final long serialVersionUID = 1L;

        @JSONField(name = "contract_id")
        private String contractId;

        @JSONField(name = "process_instance_id")
        private String processInstanceId;

        @JSONField(name = "invalid_attribute_list")
        private List<InvalidAttribute> invalidAttributeList;
    }

    @Data
    public static class InvalidAttribute implements Serializable {

        private static final long serialVersionUID = 1L;

        @JSONField(name = "module_name")
        private String moduleName;

        @JSONField(name = "attribute_name")
        private String attributeName;

        @JSONField(name = "group_name")
        private String groupName;

        private String reason;

        @JSONField(name = "attribute_key")
        private String attributeKey;
    }
}
