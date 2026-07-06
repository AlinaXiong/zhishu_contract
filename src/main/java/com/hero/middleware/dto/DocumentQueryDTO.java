package com.hero.middleware.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class DocumentQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String documentType;

    private String documentStatus;

    private Integer pageNum;

    private Integer pageSize;

}
