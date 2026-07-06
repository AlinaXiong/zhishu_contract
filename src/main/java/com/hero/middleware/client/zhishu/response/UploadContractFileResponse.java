package com.hero.middleware.client.zhishu.response;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.io.Serializable;

@Data
public class UploadContractFileResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer code;

    private String msg;

    private String message;

    private DataInfo data;

    public boolean isSuccess() {
        return code != null && code == 0;
    }

    @Data
    public static class DataInfo implements Serializable {

        private static final long serialVersionUID = 1L;

        @JSONField(name = "file_id")
        private String fileId;
    }
}
