package com.hero.middleware.client.zhishu.response;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ContractListResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer code;

    private String message;

    private Long total;

    private List<ContractResponse> list;

}
