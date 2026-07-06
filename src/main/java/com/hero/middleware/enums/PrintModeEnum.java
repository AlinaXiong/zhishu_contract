package com.hero.middleware.enums;

/**
 * 智书打印模式枚举
 */
public enum PrintModeEnum {

    BLACK_WHITE_DOUBLE("cmnyeghfi004e3b71kz8iaz5m", "黑白双面打印"),
    BLACK_WHITE_SINGLE("cmnyeghfi004f3b71en8fc9ct", "黑白单面打印"),
    COLOR_DOUBLE("cmnyeghfi004g3b71haty2oh0", "彩色双面打印"),
    COLOR_SINGLE("cmnyeigat004h3b71pqu6f34c", "彩色单面打印");

    private final String code;

    private final String name;

    PrintModeEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static PrintModeEnum getByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        for (PrintModeEnum printModeEnum : values()) {
            if (printModeEnum.getName().equals(name.trim())) {
                return printModeEnum;
            }
        }
        return null;
    }

    public static String getCodeByName(String name) {
        PrintModeEnum printModeEnum = getByName(name);
        return printModeEnum == null ? null : printModeEnum.getCode();
    }
}
