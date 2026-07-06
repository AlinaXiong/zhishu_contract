package com.hero.middleware.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Collection;

@Data
public class YeCaiContractSyncDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 要同步业财的合同编码集合。
     */
    private Collection<String> contractNumbers;

    /**
     * 要同步业财的合同编码txt文件地址，每行一个合同编码。
     */
    private String contractNumberFilePath;

    /**
     * 同步线程数，不传默认5。
     */
    private Integer threadCount;
}
