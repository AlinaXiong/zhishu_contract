package com.hero.middleware.client.yuecai.response.masterdata;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
public class UnitRes extends BaseRes {
    /**
     * 部门表主键 ID
     */
    private Long unitId;

    /**
     * 管理公司代码
     */
    private String companyCode;

    /**
     * 部门代码
     */
    private String unitCode;

    /**
     * 部门类型代码
     */
    private String unitTypeCode;

    /**
     * 管理组织代码
     */
    private String magOrgCode;

    /**
     * 描述
     */
    private String description;

    /**
     * 上级部门代码
     */
    private String parentUnitCode;

    /**
     * 主岗位代码
     */
    private String chiefPositionCode;

    /**
     * 部门级别代码
     */
    private String unitLevelCode;

    /**
     * 启用标志
     */
    private Boolean enabledFlag;
}
