package com.hero.middleware.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class MasterDataSyncByTypeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Master data type. Supported values: VENDER/VENDOR, CUSTOMER.
     */
    private String type;

    /**
     * Alias of type for callers that use existing business type naming.
     */
    private String businessType;

    /**
     * Optional certification ID filter.
     */
    private String certificationId;

    /**
     * Query start time, format: yyyy-MM-dd HH:mm:ss.
     */
    private String startTime;

    /**
     * Query end time, format: yyyy-MM-dd HH:mm:ss.
     */
    private String endTime;

    /**
     * Query page, starting from 0.
     */
    private Integer page;

    /**
     * Page size.
     */
    private Integer size;
}
