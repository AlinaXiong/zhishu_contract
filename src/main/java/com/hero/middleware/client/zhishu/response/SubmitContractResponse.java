package com.hero.middleware.client.zhishu.response;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.io.Serializable;

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
    }
}
