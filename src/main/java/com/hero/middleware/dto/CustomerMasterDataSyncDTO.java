package com.hero.middleware.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class CustomerMasterDataSyncDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 客户证件 ID
     */
    private String certificationId;

    /**
     * 查询起始时间，格式：yyyy-MM-dd HH:mm:ss
     */
    private String startTime;

    /**
     * 查询终止时间，格式：yyyy-MM-dd HH:mm:ss
     */
    private String endTime;

    /**
     * 查询页码，从 0 开始
     */
    private Integer page;

    /**
     * 每页数量
     */
    private Integer size;
}
