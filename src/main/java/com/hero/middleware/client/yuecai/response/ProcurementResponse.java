package com.hero.middleware.client.yuecai.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
public class ProcurementResponse {
    //头id
    private Long expRequisitionHeaderId;

    //员工代码
    private String employeeCode;

    //员工名称
    private String employeeName;

    //单据申请日期
    private Date requisitionDate;

    //申请单编号
    private String expRequisitionNumber;

    //单据类型id
    private Long moExpReqTypeId;

    //单据类型
    private String moExpReqTypeName;

    //申请单状态
    private String requisitionStatus;

    //提单人部门
    private String unitCode;

    //部门名称
    private String unitName;

    //核算主体代码
    private String accEntityCode;

    private String accEntityName;

    //需求人对接人Code
    private String demandContactPersonCode;
    //需求人对接人
    private String demandContactPerson;

    //需求人部门Code
    private String requestingDepartmentCode;

    //需求人部门
    private String requestingDepartment;

    //项目国家Code
    private String countryCode;

    //项目国家
    private String country;

    //项目省份Id
    private Long provincesId;

    //项目省份
    private String provinces;

    //项目城市Id
    private Long cicitytyId;

    //项目城市
    private String cicityty;

    //责任中心Code
    private String responsibilityCenterCode;
    private String responsibilityCenterName;

    private String attribute1;//项目名称
    private String attribute2;//项目详细地址
    private String attribute21;//立项时间

    //采购供应商dto
    private List<ProcurementLineRes> lineResultDTOS;

    //采购明细dto
    private List<ProcurementDetailsRes> detailsResultDTOS;

    @Data
    public static class ProcurementLineRes{
        //头id
        private Long expRequisitionHeaderId;

        //专项品类ID
        private Long pecializedCategoryId;

        //专项品类
        private String specializedCategory;

        private Double attribute31;

        private BigDecimal businessAmount;

        private String businessCurrencyCode;

        private Date departureDate;

        private Date arrivalDate;

        //采购员Code
        private String purchaserCode;

        //采购员
        private String purchaser;

        //供应商名称Code
        private String supplierCode;

        //供应商名称
        private String supplier;
    }
    @Data
    public static class ProcurementDetailsRes{
        //头id
        private Long expRequisitionHeaderId;
        //采购内容
        private String procurementContent;

        //单位
        private String unit;

        //数量
        private Integer quantity;

        //其他补充
        private String note;
    }
}
