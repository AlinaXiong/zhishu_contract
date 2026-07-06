package com.hero.middleware.client.yuecai.response;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class DocumentListResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer code;

    private String message;

    private Long total;

    private List<DocumentItem> list;

    @Data
    public static class DocumentItem implements Serializable {

        private static final long serialVersionUID = 1L;

        private String documentId;

        private String documentName;

        private String documentType;

        private String documentStatus;

    }

}
