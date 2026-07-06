package com.hero.middleware.enums;

/**
 * 是否枚举
 */
public enum YesOrNoEnum {

    YES("1", "是"),
    NO("0", "否");

    private final String code;

    private final String name;

    YesOrNoEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static YesOrNoEnum getByCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }
        for (YesOrNoEnum yesOrNoEnum : values()) {
            if (yesOrNoEnum.getCode().equals(code.trim())) {
                return yesOrNoEnum;
            }
        }
        return null;
    }

    public static String getNameByCode(String code) {
        YesOrNoEnum yesOrNoEnum = getByCode(code);
        return yesOrNoEnum == null ? "否" : yesOrNoEnum.getName();
    }
}
