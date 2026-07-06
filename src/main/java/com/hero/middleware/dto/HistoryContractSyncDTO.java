package com.hero.middleware.dto;

import lombok.Data;

import java.util.Collection;

@Data
public class HistoryContractSyncDTO {

    /**
     * 合同编码集合。为空时同步文件中的全部合同。
     */
    private Collection<String> contractNumbers;

    /**
     * Contract number txt file path. One contract number per line.
     */
    private String contractNumberFilePath;

    /**
     * 历史合同Excel文件路径。为空时使用默认文件路径。
     */
    private String filePath;

    /**
     * 兼容调用方使用excelPath作为文件路径字段。
     */
    private String excelPath;

    /**
     * Contract file fallback root path used when uploading contract files.
     */
    private String contractFileFallbackRoot;

    /**
     * Contract status code. Defaults to 9 when blank.
     */
    private String contractStatusCode;

    /**
     * Multi-thread sync worker count.
     */
    private Integer threadCount;

    /**
     * Multi-thread sync batch size.
     */
    private Integer batchSize;

    public String getResolvedFilePath() {
        if (filePath != null && !filePath.trim().isEmpty()) {
            return filePath;
        }
        return excelPath;
    }
}
