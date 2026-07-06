package com.hero.middleware.enums;

/**
 * 智书税目枚举
 */
public enum TaxItemEnum {

    PRODUCTION_LIFE_SERVICE("cmpdgp0vj00123b711vd1i527", "生产生活服务", "cmqktxxfs000w3571qt3lw5cj"),
    CLOTHING("cmpdgp0vj00133b71m2deeo9q", "服装", "cmqktxxfs000x3571ed5il4kv"),
    INTANGIBLE_ASSETS("cmq9g64c60008597d4nswcumu", "无形资产", "cmqktxxfs000y3571sh2ks65g"),
    COMPUTER("cmq9g662p0009597dvn0hrcv1", "电子计算机", "cmqktyotl000z35715qt89h9g"),
    REPAIR_SERVICE("cmq9g66oa000a597dipp46uk8", "修理修配服务", "cmqktyq9a00103571lpriwmth"),
    RADIO_TELEVISION_EQUIPMENT("cmq9h5z3x0025597dj86wja1v", "广播电视设备", "cmqktyq9a00103571lpriwmth"),
    PLASTIC_PRODUCTS("cmq9h689u0026597dssyp46dj", "塑料制品", "cmqktyyg300123571nf626hqn"),
    TOY("cmq9h6fa40027597dv0823owy", "玩具", "cmqktz2nv001335718xjp3znc"),
    TRANSPORT_SERVICE("cmq9h6k4k0028597d8v8t3vc0", "运输服务", null),
    OVERSEAS_TAX_ITEM("cmq9h7p7f002h597dt8xcbgyv", "海外税目", null);

    private final String expenseCode;

    private final String name;

    private final String incomeCode;

    TaxItemEnum(String expenseCode, String name, String incomeCode) {
        this.expenseCode = expenseCode;
        this.name = name;
        this.incomeCode = incomeCode;
    }

    public String getExpenseCode() {
        return expenseCode;
    }

    public String getName() {
        return name;
    }

    public String getIncomeCode() {
        return incomeCode;
    }

    public static TaxItemEnum getByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        for (TaxItemEnum taxItemEnum : values()) {
            if (taxItemEnum.getName().equals(name.trim())) {
                return taxItemEnum;
            }
        }
        return null;
    }

    public static String getExpenseCodeByName(String name) {
        TaxItemEnum taxItemEnum = getByName(name);
        return taxItemEnum == null ? null : taxItemEnum.getExpenseCode();
    }

    public static String getIncomeCodeByName(String name) {
        TaxItemEnum taxItemEnum = getByName(name);
        return taxItemEnum == null ? null : taxItemEnum.getIncomeCode();
    }
}
