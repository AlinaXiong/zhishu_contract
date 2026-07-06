package com.hero.middleware.client.yuecai.response;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class OrderInfoResponse {
    /**
     * 订单头ID
     */
    private Long orderHeaderId;

    /**
     * 项目维度订单值
     */
    private String prjDimOrderValue;

    /**
     * 订单标题
     */
    private String orderTitle;

    /**
     * 订单类型
     */
    private String orderType;

    /**
     * 订单阶段
     */
    private String orderPhase;

    /**
     * 项目维度值
     */
    private String prjDimValue;

    /**
     * 项目名称
     */
    private String projectName;

    /**
     * 收入主体
     */
    private String revenueEntity;

    /**
     * 支出主体
     */
    private String expenditureEntity;

    /**
     * 成本中心
     */
    private String costCenter;

    /**
     * 业务单元
     */
    private String businessUnit;

    /**
     * 创建日期
     */
    private Date creationDate;

    /**
     * 项目开始日期
     */
    private Date projectStartDate;

    /**
     * 项目结束日期
     */
    private Date projectEndDate;

    /**
     * 订单开始日期
     */
    private Date orderStartDate;

    /**
     * 订单结束日期
     */
    private Date orderEndDate;

    /**
     * 订单摘要
     */
    private String orderSummary;

    /**
     * 订单币种
     */
    private String orderCurrency;

    /**
     * 游戏名称
     */
    private String gameName;

    /**
     * 夜间打车标识
     */
    private String nightRideFlag;

    /**
     * 打车开始时间
     */
    private Date rideStartTime;

    /**
     * 打车结束时间
     */
    private Date rideEndTime;

    /**
     * 夜间用车类型
     * 1:同事业部可见 2:仅项目成员 3:同事业部&项目成员
     */
    private String nightRideType;

    /**
     * 订单类型
     */
    private String projectType;

    /**
     * 项目成员列表
     */
    private List<Member> memberList;

    /**
     * 飞书user_id
     */
    private String userId;

    /**
     * 内部类：项目成员
     */
    @Data
    public static class Member {
        /**
         * 员工姓名
         */
        private String employeeCode;

        /**
         * 角色
         */
        private String roleCode;

        /**
         * 飞书user_id
         */
        private String userId;
    }
}
