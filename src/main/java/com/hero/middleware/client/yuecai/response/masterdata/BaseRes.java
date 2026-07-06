package com.hero.middleware.client.yuecai.response.masterdata;

import lombok.Data;

import java.util.Date;

@Data
public class BaseRes {
    // 审计字段
    private Date creationDate;          // 创建日期
    private Long createdBy;              // 创建人
    private Date lastUpdateDate;         // 最后更新日期
    private Long lastUpdatedBy;           // 最后更新人
    private Long objectVersionNumber;    // 版本号
    private String _token;                // 令牌
}
