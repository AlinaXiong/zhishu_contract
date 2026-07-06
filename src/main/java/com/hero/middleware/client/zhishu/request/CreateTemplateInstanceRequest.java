package com.hero.middleware.client.zhishu.request;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.util.List;

@Data
public class CreateTemplateInstanceRequest {
    /**
     * 创建员工工号（与create_user_id二选一，优先取create_user_id）
     */
    @JSONField(name = "create_employee_code")
    private String createEmployeeCode;
    /**
     * 创建用户id，默认open_id（与create_employee_code二选一，优先取create_user_id）
     */
    @JSONField(name = "create_user_id")
    private String createUserid;
    /**
     * 外部系统来源单据id
     */
    @JSONField(name = "source_id")
    private String sourceid;
    /**
     * 模板字段信息列表，创建补充协议时需要将所需字段全部填写，不会继承原合同的模板自定义字段信息
     */
    @JSONField(name = "template_field_list")
    private List<TemplateFieldList> templateFieldList;
    /**
     * 模板编号，可通过飞书合同模板管理页面查询得到
     */
    @JSONField(name = "template_number")
    private String templateNumber;

    @Data
    public static class TemplateFieldList {
        /**
         * 是否禁止用户修改字段值
         */
        @JSONField(name = "edit_disabled")
        private Boolean editDisabled;
        /**
         * 字段key  模板字段编码
         */
        @JSONField(name = "field_key")
        private String fieldKey;
        /**
         * 字段值，使用JSON字符串，文本：{"content":"名称"}；数值：{"content":10086}；日期：{"content":"2022-04-21"}，格式=yyyy-MM-dd；
         * field_value说明
         * - field_value为string类型，存储Content的JSON字符串
         * - Content中引用一个object对象，根据不同的字段类型存储具体的字段值
         * class Content {
         * Object content;
         * }
         * [
         * // 文本类型
         * {
         * "content" : "名称"
         * },
         * // 数值类型
         * {
         * "content" : 10086
         * },
         * // 日期类型 格式=yyyy-MM-dd
         * {
         * "content" : "2022-04-21"
         * },
         * // 日期区间 日期格式=yyyy-MM-dd
         * {
         * "content" : ["2022-04-21", "2022-04-21"]
         * },
         * // 单选
         * {
         * "content" : "value"
         * },
         * // 多选
         * {
         * "content" : ["value"]
         * },
         * // 附件
         * {
         * "content" : ["1231313123"]
         * }
         * ]
         */
        @JSONField(name = "field_value")
        private String fieldValue;
    }
}
