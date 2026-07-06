package com.hero.middleware.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;

public enum SpecializedCategoriesEnum {

    THREE_D_AND_GRAPHIC_DESIGN("3D及平面设计", "3D及平面设计", "cmp0y2rse004e3b716tihbjhf", "A02,B02,B03,B04,B07,B08,B10,C02,C04"),
    SECURITY_CLEANING_MEDICAL_STAFF_EQUIPMENT("安保保洁医护人员及设备", "安保保洁医护人员及设备", "cmp0y2rse004e3b716tihbjhf", "B02,B04,B09,C02,C04"),
    OFFICE_FURNITURE_APPLIANCE_PURCHASE_REPAIR("办公家具及电器采购及维修", "办公家具及电器采购及维修", "cmp0y2rse004e3b716tihbjhf", "A01"),
    OFFICE_SOFTWARE_COPYRIGHT("办公软件版权", "办公软件版权", "cmp0y2rse004e3b716tihbjhf", "A02"),
    OFFICE_EQUIPMENT_PURCHASE_REPAIR("办公设备采购及维修", "办公设备采购及维修", "cmp0y2rse004e3b716tihbjhf", "A01"),
    OFFICE_NETWORK_COMMUNICATION("办公网络及通讯", "办公网络及通讯", "cmp0y2rse004e3b716tihbjhf", "A02,A04"),
    OFFICE_SUPPLIES("办公用品", "办公用品", "cmp0y2rse004e3b716tihbjhf", "A02,A04"),
    VENUE_RENTAL("场地租赁", "场地租赁", "cmp0y2rse004f3b71rrqup04i", "B02,B04,B06,B09,C02,C04"),
    VENUE_NETWORK_COMMUNICATION("场馆网络及通讯", "场馆网络及通讯", "cmp0y2rse004e3b716tihbjhf", "B02,B04,B09,C02,C04"),
    THIRD_PARTY_PERSONNEL_SERVICE("第三方人员服务", "第三方人员服务", "cmp0y2rse004e3b716tihbjhf", "B01,B02,B04,B05,B08,B09,C02,C03,C04"),
    PUBLIC_RELATIONS_MONITORING_SERVICE("公关及舆情监测服务", "公关及舆情监测服务", "cmp0y2rse004e3b716tihbjhf", "A02,B03,B07,B10"),
    BROADCAST_NETWORK_OTHER_EQUIPMENT_PURCHASE_REPAIR("广电/网络/其他设备采购及维修", "广电/网络/其他设备采购及维修", "cmp0y2rse004e3b716tihbjhf", "B01"),
    BROADCAST_NETWORK_OTHER_EQUIPMENT_RENTAL("广电/网络/其他设备租赁", "广电/网络/其他设备租赁", "cmp0y2rse004e3b716tihbjhf", "B02,B05,B08,B09,C02"),
    ADMINISTRATIVE_SERVICE("行政服务", "行政服务", "cmp0y2rse004e3b716tihbjhf", "A02,A04"),
    EXPRESS_TRANSPORT_SERVICE("快递及运输服务", "快递及运输服务", "cmp0y2rse004e3b716tihbjhf", "A02,A04,B01,B02,B04,B05,B09,C02,C04"),
    GIFT_PURCHASE("礼品采买", "礼品采买", "cmp0y2rse004e3b716tihbjhf", "B02,B03,B07,B10,C02"),
    BUILDING_EQUIPMENT_MAINTENANCE("楼宇设备及维护", "楼宇设备及维护", "cmp0y2rse004e3b716tihbjhf", "A07"),
    MEDIA_ASSET_SOFTWARE_OR_SYSTEM("媒资软件或系统", "媒资软件或系统", "cmp0y2rse004e3b716tihbjhf", "B01"),
    CONTENT_PRODUCTION("内容制作", "内容制作", "cmp0y2rse004e3b716tihbjhf", "A02,B02,B03,B04,B07,B08,B10,C02,C04"),
    TICKETING_SERVICE("票务服务", "票务服务", "cmp0y2rse004e3b716tihbjhf", "B02,C02"),
    OTHER_HR_SERVICE("其他人事服务", "其他人事服务", "cmp0y2rse004e3b716tihbjhf", "A08"),
    ENTERPRISE_CULTURE("企业文化", "企业文化", "cmp0y2rse004e3b716tihbjhf", "A08"),
    EVENT_ACTIVITY_PLANNING_PACKAGE("赛事/活动策划及整包", "赛事/活动策划及整包", "cmp0y2rse004e3b716tihbjhf", "A04,A09,B02,B03,B04,B07,B09,B10,C02,C04"),
    MARKET_RESEARCH_DATA_ANALYSIS("市场调研及数据分析", "市场调研及数据分析", "cmp0y2rse004e3b716tihbjhf", "B03,B07,B10"),
    NETWORK_CONSTRUCTION_OPERATION_MAINTENANCE("网络建设及运维", "网络建设及运维", "cmp0y2rse004e3b716tihbjhf", "A01"),
    STAGE_ART_CONSTRUCTION("舞美搭建", "舞美搭建", "cmp0y2rse004e3b716tihbjhf", "B02,B04,B05,B08,B09,C02,C03,C04"),
    PROJECT_PRODUCTION_ARTIST_COORDINATION("项目制片及艺人统筹", "项目制片及艺人统筹", "cmp0y2rse004e3b716tihbjhf", "A02,A04,B01,B02,B03,B04,B05,B06,B07,B08,B09,B10,C02,C03,C04,C05"),
    SYSTEM_DEVELOPMENT_OPERATION_MAINTENANCE("系统开发及运维", "系统开发及运维", "cmp0y2rse004e3b716tihbjhf", "A01"),
    DERIVATIVE_PURCHASE_PRODUCTION("衍生品采购及制作", "衍生品采购及制作", "cmp0y2rse004e3b716tihbjhf", "B04,C04"),
    AUDIO_VIDEO_FONT_COPYRIGHT_PURCHASE("音频/视频/字体版权采买", "音频/视频/字体版权采买", "cmp0y2rse004e3b716tihbjhf", "A02,B02,B03,B04,B07,B08,B10,C02,C04"),
    EMPLOYEE_BENEFITS("员工福利", "员工福利", "cmp0y2rse004e3b716tihbjhf", "A08"),
    EMPLOYEE_TRAINING_EVALUATION("员工培训及测评", "员工培训及测评", "cmp0y2rse004e3b716tihbjhf", "A08"),
    CLOUD_SERVICE("云服务", "云服务", "cmp0y2rse004e3b716tihbjhf", "A02,A04,B02,B04,B09,C02,C04"),
    OPERATION_MATERIAL_PRODUCTION("运营及物料制作", "运营及物料制作", "cmp0y2rse004e3b716tihbjhf", "A02,B02,B03,B04,B07,B10,C02,C04"),
    OPERATION_INTEGRATED_MARKETING_SERVICE("运营及整合营销服务", "运营及整合营销服务", "cmp0y2rse004e3b716tihbjhf", "A02,B02,B03,B04,B07,B09,B10,C02,C04"),
    RECRUITMENT_CHANNEL("招聘渠道", "招聘渠道", "cmp0y2rse004e3b716tihbjhf", "A08"),
    DECORATION_RENOVATION_SERVICE("装修及改造服务", "装修及改造服务", "cmp0y2rse004e3b716tihbjhf", "A07,B06,B09"),
    BROADCAST_TRANSPORT_VEHICLE_PURCHASE_REPAIR("转播运输车辆采购及维修", "转播运输车辆采购及维修", "cmp0y2rse004e3b716tihbjhf", "B01"),
    CATERING_SERVICE_EQUIPMENT("餐饮服务及设备", "餐饮服务及设备", "cmp0y2rse004e3b716tihbjhf", "C05"),
    TRAVEL_SERVICE("差旅服务", "差旅服务", "cmp0y2rse004e3b716tihbjhf", "A02,A04,B02,B03,B04,B06,B07,B08,B09,B10,C02,C03,C04"),
    ECOMMERCE_OPERATION_SERVICE("电商运营服务", "电商运营服务", "cmp0y2rse004e3b716tihbjhf", "C03"),
    HOUSE_WATER_ELECTRIC_ENERGY("房屋水电及能耗", "房屋水电及能耗", "cmp0y2rse004e3b716tihbjhf", "A07"),
    HOUSE_RENTAL("房屋租赁", "房屋租赁", "cmp0y2rse004f3b71rrqup04i", "A07"),
    ADVERTISING_MEDIA_PLACEMENT("广告与媒介投放", "广告与媒介投放", "cmp0y2rse004e3b716tihbjhf", "B03,B07,B10,C01"),
    COMMENTATOR_GUEST_LABOR_BONUS("解说/嘉宾劳务及赛事奖金", "解说/嘉宾劳务及赛事奖金", "cmp0y2rse004e3b716tihbjhf", "B02,B03,B04,B06,B07,B08,B09,B10,C02,C03,C04"),
    TECHNICAL_SERVICE("技术服务", "技术服务", "cmp0y2rse004e3b716tihbjhf", "A05,C01"),
    CLUB_INFLUENCER_COOPERATION("俱乐部及达人合作", "俱乐部及达人合作", "cmp0y2rse004f3b71rrqup04i", "B03,B07,B10"),
    SIGNING_TRANSFER_FEE("签约金及转会费", "签约金及转会费", "cmp0y2rse004f3b71rrqup04i", "B06"),
    DAILY_OPERATION_VEHICLE_PURCHASE_REPAIR("日常运营车辆采购及维修", "日常运营车辆采购及维修", "cmp0y2rse004e3b716tihbjhf", "A01,A02,A04"),
    BUSINESS_IP_COOPERATION("商务及IP合作", "商务及IP合作", "cmp0y2rse004e3b716tihbjhf", "B02,B03,B04,B07,B10,C02,C04"),
    INVESTMENT_FINANCING_STRATEGIC_COOPERATION("投融资及战略合作", "投融资及战略合作", "cmp0y2rse004f3b71rrqup04i", "A06"),
    PROPERTY_SERVICE("物业服务", "物业服务", "cmp0y2rse004f3b71rrqup04i", "A07"),
    PAYROLL_SERVICE("薪酬服务", "薪酬服务", "cmp0y2rse004f3b71rrqup04i", "A03"),
    INVESTMENT_ATTRACTION_AGENT("招商代理", "招商代理", "cmp0y2rse004f3b71rrqup04i", "B03,B07,B08,B10"),
    LIVE_STREAMING_OPERATION("直播运营", "直播运营", "cmp0y2rse004f3b71rrqup04i", "B06"),
    PROFESSIONAL_SERVICE("专业服务", "专业服务", "cmp0y2rse004f3b71rrqup04i", "A02,A04,A06");

    private final String code;

    @Getter
    private final String name;

    @Getter
    private final String yesOrNo;

    @Getter
    private final String orderType;

    SpecializedCategoriesEnum(String code, String name, String yesOrNo, String orderType) {
        this.code = code;
        this.name = name;
        this.yesOrNo = yesOrNo;
        this.orderType = orderType;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static SpecializedCategoriesEnum fromCode(String code) {
        return getByCode(code);
    }

    public static SpecializedCategoriesEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (SpecializedCategoriesEnum type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }

    public static SpecializedCategoriesEnum getByName(String name) {
        if (name == null) {
            return null;
        }
        for (SpecializedCategoriesEnum type : values()) {
            if (type.name.equals(name)) {
                return type;
            }
        }
        return null;
    }

    public static Map<String, String> toMap() {
        Map<String, String> map = new LinkedHashMap<>();
        for (SpecializedCategoriesEnum type : values()) {
            map.put(type.code, type.name);
        }
        return map;
    }
}
