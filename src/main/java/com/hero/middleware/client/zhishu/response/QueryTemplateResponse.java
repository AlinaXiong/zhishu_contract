package com.hero.middleware.client.zhishu.response;

import lombok.Data;
@Data
public class QueryTemplateResponse {
    /**
     * 合同二级分类名称
     */
    private String contractCategoryName;
    /**
     * 创建人姓名
     */
    private String createEmployeeName;
    /**
     * 模板描述
     */
    private String description;
    /**
     * 发布人姓名
     */
    private String publishEmployeeName;
    /**
     * 发布时间
     */
    private String publishTime;
    /**
     * 是否已发布
     */
    private boolean published;
    /**
     * 模板字段信息列表
     */
    private TemplateField[] templateFields;
    /**
     * 模板id
     */
    private String templateid;
    /**
     * 模板名称
     */
    private String templateName;
    /**
     * 模板编号
     */
    private String templateNumber;

    @Data
    public class TemplateField {
        /**
         * 是否可为空
         */
        private Boolean allowNull;
        /**
         * 默认取值
         */
        private String[] defaultValue;
        /**
         * 字段描述
         */
        private String description;
        /**
         * 字段code
         */
        private String fieldCode;
        /**
         * 字段id
         */
        private String fieldid;
        /**
         * 字段名称
         */
        private String fieldName;
        /**
         * 字段类型
         */
        private Long fieldType;
        /**
         * 字段取值范围
         */
        private ValueScope[] valueScopes;

    }
    @Data
    public static class ValueScope {
        /**
         * 属性可选值的外显名称
         */
        private String label;
        /**
         * 属性可选值的存储值
         */
        private String value;

    }
}
