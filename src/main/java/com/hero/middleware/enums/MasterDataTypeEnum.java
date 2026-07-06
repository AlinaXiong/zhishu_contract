package com.hero.middleware.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

public enum MasterDataTypeEnum {
    COMPANY("COMPANY", "公司"),
    UNIT("UNIT", "部门"),
    EMPLOYEE("EMPLOYEE", "员工"),
    EMPLOYEE_ACCOUNT("EMPLOYEE_ACCOUNT", "员工账号"),
    VENDER("VENDER", "供应商"),
    VENDER_ACCOUNT("VENDER_ACCOUNT", "供应商账号"),
    CUSTOMER("CUSTOMER", "客户"),
    CUSTOMER_ACCOUNT("CUSTOMER_ACCOUNT", "客户账号"),
    BANK("BANK", "银行"),
    CNAPS("CNAPS", "银行分行"),
    CURRENCY("CURRENCY", "币种"),
    EXCHANGE_RATE("EXCHANGE_RATE", "汇率"),
    RESP_CENTER("RESP_CENTER", "成本中心"),
    ORDER("ORDER", "项目订单");

    private final String code;
    @Getter
    private final String name;

    MasterDataTypeEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    /**
     * 序列化时使用code
     */
    @JsonValue
    public String getCode() {
        return code;
    }

    /**
     * 反序列化时根据code创建枚举
     */
    @JsonCreator
    public static MasterDataTypeEnum fromCode(String code) {
        return getByCode(code);
    }

    public static MasterDataTypeEnum getByCode(String code) {
        for (MasterDataTypeEnum type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }

    public static MasterDataTypeEnum getByName(String name) {
        for (MasterDataTypeEnum type : values()) {
            if (type.name.equals(name)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 转换为键值对格式
     */
    public static java.util.Map<String, String> toMap() {
        java.util.Map<String, String> map = new java.util.LinkedHashMap<>();
        for (MasterDataTypeEnum type : values()) {
            map.put(type.code, type.name);
        }
        return map;
    }
}
