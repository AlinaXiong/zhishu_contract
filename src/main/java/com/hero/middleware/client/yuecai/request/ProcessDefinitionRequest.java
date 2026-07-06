package com.hero.middleware.client.yuecai.request;

import lombok.Data;
import java.util.Map;

/**
 * 流程实体类
 */
@Data
public class ProcessDefinitionRequest {

    /**
     * 流程名称
     */
    private String flowName;

    /**
     * 流程编码（唯一标识）
     */
    private String flowCode;

    /**
     * 分类编码（唯一标识）
     * 判断分类是否存在，不存在则新建，同步预置分类
     */
    private String typeCode;

    /**
     * 分类名称
     */
    private String typeName;

    /**
     * 流程描述
     */
    private String description;

    /**
     * 租户ID
     * 租户ID与租户编码必输其中一个
     */
    private Long tenantId;

    /**
     * 租户编码
     * 租户ID与租户编码必输其中一个
     */
    private String tenantNum;

    /**
     * 来源系统
     */
    private String sourceSystem;

    /**
     * 名称多语言
     * Map结构，key为语言代码，value为对应的翻译文本
     */
    private Map<String, String> _tls;
}
