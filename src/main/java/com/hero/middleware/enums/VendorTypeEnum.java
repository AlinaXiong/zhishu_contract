package com.hero.middleware.enums;

/**
 * 交易方类型枚举
 */
public enum VendorTypeEnum {

    CUSTOMER("1", "CUSTOMER", "客户"),
    VENDOR("2", "VENDER", "供应商");

    /**
     * 智书交易方类型
     */
    private final String zhishuCode;

    /**
     * 业财交易方类型
     */
    private final String yecaiCode;

    /**
     * 名称
     */
    private final String name;

    VendorTypeEnum(String zhishuCode, String yecaiCode, String name) {
        this.zhishuCode = zhishuCode;
        this.yecaiCode = yecaiCode;
        this.name = name;
    }

    public String getZhishuCode() {
        return zhishuCode;
    }

    public String getYecaiCode() {
        return yecaiCode;
    }

    public String getName() {
        return name;
    }

    public static VendorTypeEnum getByZhishuCode(String zhishuCode) {
        for (VendorTypeEnum vendorTypeEnum : values()) {
            if (vendorTypeEnum.getZhishuCode().equals(zhishuCode)) {
                return vendorTypeEnum;
            }
        }
        return null;
    }

    public static String getYecaiCodeByZhishuCode(String zhishuCode) {
        VendorTypeEnum vendorTypeEnum = getByZhishuCode(zhishuCode);
        return vendorTypeEnum == null ? null : vendorTypeEnum.getYecaiCode();
    }
}
