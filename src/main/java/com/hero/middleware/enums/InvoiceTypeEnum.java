package com.hero.middleware.enums;

/**
 * 审批操作枚举
 * 通过第一个参数获取对应的第二个参数值
 */
public enum InvoiceTypeEnum {

    VAT_SPECIAL_PAPER("cmpdgdrns000n3b71gx6z03fq", "VAT_SPECIAL_PAPER", "增值税专用发票"),
    INVOICE("cmpdgdrns000o3b71ur1ttte8", "VAT_SPECIAL_PAPER", "普通发票"),
    ORDINARY_VAT("cmpdgdrns000p3b71fore5ecy", "VAT_SPECIAL_PAPER", "普通发票"),
    SPECIAL_VAT("cmpdgefh9000q3b715zz0tpiu", "VAT_SPECIAL_PAPER", "专用发票");


    /**
     * 第一个参数 - 智书编码
     */
    private final String zhishuCode;

    /**
     * 第二个参数 - 业财编码
     */
    private final String yecaiCode;

    /**
     * 操作描述
     */
    private final String description;


    InvoiceTypeEnum(String zhishuCode, String yecaiCode, String description) {
        this.zhishuCode = zhishuCode;
        this.yecaiCode = yecaiCode;
        this.description = description;
    }

    public String getZhishuCode() {
        return zhishuCode;
    }

    public String getYecaiCode() {
        return yecaiCode;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据第一个参数(zhishuCode)获取对应的枚举实例
     *
     * @param zhishuCode 第一个参数
     * @return 对应的枚举实例
     * @throws IllegalArgumentException 如果找不到对应的枚举
     */
    public static InvoiceTypeEnum getByZhishuCode(String zhishuCode) {
        for (InvoiceTypeEnum action : values()) {
            if (action.getZhishuCode().equals(zhishuCode)) {
                return action;
            }
        }
        throw new IllegalArgumentException("未找到对应的发票编码，zhishuCode: " + zhishuCode);
    }

    /**
     * 根据第一个参数(code)获取第二个参数(commandType)
     *
     * @param zhishuCode 第一个参数
     * @return 第二个参数值
     */
    public static String getYecaiCodeByZhishuCode(String zhishuCode) {
        if(zhishuCode==null){
            return null;
        }
        return getByZhishuCode(zhishuCode).getYecaiCode();
    }

    public static InvoiceTypeEnum getByDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            return null;
        }
        for (InvoiceTypeEnum action : values()) {
            if (action.getDescription().equals(description.trim())) {
                return action;
            }
        }
        return null;
    }

    public static String getZhishuCodeByDescription(String description) {
        InvoiceTypeEnum invoiceTypeEnum = getByDescription(description);
        return invoiceTypeEnum == null ? null : invoiceTypeEnum.getZhishuCode();
    }

    /**
     * 根据第一个参数(code)获取第三个参数(description)
     *
     * @param zhishuCode 第一个参数
     * @return 第三个参数值
     */
    public static String getDescriptionByZhishuCode(String zhishuCode) {
        return getByZhishuCode(zhishuCode).getDescription();
    }

    @Override
    public String toString() {
        return String.format("ApprovalEnum{code='%s', yecaiCode='%s', description='%s'}",
                zhishuCode, yecaiCode, description);
    }
}
