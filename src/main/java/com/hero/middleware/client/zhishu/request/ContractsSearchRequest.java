package com.hero.middleware.client.zhishu.request;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ContractsSearchRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 合同编号
     */
    @JSONField(name = "contract_number")
    private String contractNumber;

    /**
     * 分页大小
     */
    @JSONField(name = "page_size")
    private Integer pageSize;

    /**
     * 分页标识
     */
    @JSONField(name = "page_token")
    private String pageToken;

    /**
     * 组合查询条件
     */
    @JSONField(name = "combine_condition")
    private CombineCondition combineCondition;

    @Data
    public static class CombineCondition implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 用户ID
         */
        @JSONField(name = "user_id")
        private String userId;

        /**
         * 权限
         */
        @JSONField(name = "permission")
        private Integer permission;

        /**
         * 合同状态
         */
        @JSONField(name = "contract_status")
        private Integer contractStatus;

        /**
         * 收支类型
         */
        @JSONField(name = "pay_type")
        private Integer payType;

        /**
         * 合同类型名称
         */
        @JSONField(name = "contract_category_name")
        private String contractCategoryName;

        /**
         * 合同名称
         */
        @JSONField(name = "contract_name")
        private String contractName;

        /**
         * 合同编号
         */
        @JSONField(name = "contract_number")
        private String contractNumber;

        /**
         * 归档编号
         */
        @JSONField(name = "archive_number")
        private String archiveNumber;

        /**
         * 创建开始时间
         */
        @JSONField(name = "create_time_start")
        private String createTimeStart;

        /**
         * 创建结束时间
         */
        @JSONField(name = "create_time_end")
        private String createTimeEnd;

        /**
         * 更新开始时间
         */
        @JSONField(name = "update_time_start")
        private String updateTimeStart;

        /**
         * 更新结束时间
         */
        @JSONField(name = "update_time_end")
        private String updateTimeEnd;

        /**
         * 提交开始时间
         */
        @JSONField(name = "submited_time_start")
        private String submitedTimeStart;

        /**
         * 提交结束时间
         */
        @JSONField(name = "submited_time_end")
        private String submitedTimeEnd;

        /**
         * 归档开始时间
         */
        @JSONField(name = "archived_time_start")
        private String archivedTimeStart;

        /**
         * 归档结束时间
         */
        @JSONField(name = "archived_time_end")
        private String archivedTimeEnd;

        /**
         * 合同归属人用户ID
         */
        @JSONField(name = "owner_user_id")
        private String ownerUserId;

        /**
         * 表单字段查询条件
         */
        @JSONField(name = "form")
        private List<FormCondition> form;

        /**
         * 合同状态集合，多个状态用英文逗号拼接
         */
        @JSONField(name = "contract_status_in")
        private String contractStatusIn;

        /**
         * 合同类型缩写
         */
        @JSONField(name = "contract_category_abbreviation")
        private String contractCategoryAbbreviation;
    }

    @Data
    public static class FormCondition implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 字段名称
         */
        @JSONField(name = "attribute_name")
        private String attributeName;

        /**
         * 字段值
         */
        @JSONField(name = "attribute_value")
        private String attributeValue;

        /**
         * 模块名称
         */
        @JSONField(name = "module_name")
        private String moduleName;
    }
}
