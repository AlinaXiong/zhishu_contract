package com.hero.middleware.enums;

/**
 * 平台编码枚举
 */
public enum PlatformEnum {

    DOU_YU("1", "斗鱼"),
    HU_YA("2", "虎牙"),
    BILIBILI("3", "B站"),
    WEI_SHI("4", "微视"),
    KUAI_SHOU("5", "快手"),
    WANG_YI_CC("6", "网易CC"),
    WEI_BO("7", "微博"),
    DOU_YIN("8", "抖音"),
    QIE_E_HAO("9", "企鹅号"),
    QQ_KAN_DIAN("10", "QQ看点"),
    HUA_JIAO("11", "花椒"),
    BAI_DU("12", "百度"),
    YOUTUBE("13", "YouTube"),
    DA_YU_HAO("14", "大鱼号"),
    QQ_LIVE("15", "QQ直播"),
    QI_XIU("16", "奇秀"),
    AI_QI_YI("17", "爱奇艺"),
    YING_SHI_DA_QUAN("18", "影视大全"),
    TOU_TIAO_HAO("19", "头条号"),
    BAI_HUA_HAO("20", "百花号"),
    YING_HUO_JI_HUA("21", "萤火计划"),
    QQ_MUSIC("22", "QQ音乐"),
    BAI_JIA_HAO("23", "百家号"),
    MI_GU("24", "咪咕"),
    BO_DONG("25", "波洞"),
    XIAO_HONG_SHU("26", "小红书"),
    SHI_PIN_HAO("27", "视频号"),
    YY("28", "YY"),
    OFFLINE("29", "线下"),
    OTHER("30", "其他"),
    TIKTOK("31", "TikTok");

    /**
     * 平台编码
     */
    private final String code;

    /**
     * 平台名称
     */
    private final String name;

    PlatformEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static PlatformEnum getByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        for (PlatformEnum platformEnum : values()) {
            if (platformEnum.getName().equals(name.trim())) {
                return platformEnum;
            }
        }
        return null;
    }

    public static String getCodeByName(String name) {
        PlatformEnum platformEnum = getByName(name);
        return platformEnum == null ? null : platformEnum.getCode();
    }
}
