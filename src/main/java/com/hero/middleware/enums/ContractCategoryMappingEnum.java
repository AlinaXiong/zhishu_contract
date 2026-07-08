package com.hero.middleware.enums;

/**
 * 合同类别映射枚举
 */
public enum ContractCategoryMappingEnum {

    ZBJJ("ZBJJ", "主播专项-主播经纪"),
    PTJJ("PTJJ", "主播专项-平台经纪"),
    ZBZM("ZBZM", "主播专项-主播招募"),
    ZBGK("ZBGK", "主播专项-主播挂靠"),
    ZBZR("ZBZR", "主播专项-主播转让"),
    MLZB("MLZB", "主播专项-马来直播"),
    INZB("INZB", "主播专项-印尼直播"),
    ZBQT("ZBQT", "主播专项-其他"),
    NDA("NDA", "其他-保密协议"),
    FSYHL("FSYHL", "其他-反商业贿赂协议"),
    BC("BC", "其他-补充/变更协议"),
    MOU("MOU", "其他-战略合作协议"),
    QTLX("QTLX", "其他-其他类型"),
    NBJZ("NBJZ", "内部合同-内部结转"),
    LC("LC", "内部合同-理财产品"),
    DK("DK", "内部合同-银行贷款"),
    GNDF("GNDF", "单次支出-国内赛事及活动支出"),
    XZDF("XZDF", "单次支出-行政运营及人力支出"),
    JYDF("JYDF", "单次支出-简易支出"),
    TA("TA", "单次支出-其他支出"),
    HLWDF("HLWDF", "单次支出-互联网产品支出"),
    DCDF("DCDF", "单次支出-地产运营支出"),
    DSDF("DSDF", "单次支出-电商运营支出"),
    ENDF("ENDF", "单次支出-海外赛事业及活动支出"),
    GRDF("GRDF", "单次支出-个人合作支出"),
    JJDF("JJDF", "单次支出-奖金补贴支出"),
    ZCDF("ZCDF", "单次支出-资产采购租赁及经营支出"),
    GGDF("GGDF", "单次支出-广告赞助支出"),
    ZHDF("ZHDF", "单次支出-整合营销支出"),
    IPDF("IPDF", "单次支出-衍生品支出"),
    SXDF("SXDF", "单次支出-视效支出"),
    SSDS("SSDS", "单次收入-赛事及活动收入"),
    DCDS("DCDS", "单次收入-地产运营收入"),
    DSDS("DSDS", "单次收入-电商运营收入"),
    ZCDS("ZCDS", "单次收入-资产采购租赁及经营收入"),
    GGDS("GGDS", "单次收入-广告赞助收入"),
    ZHDS("ZHDS", "单次收入-整合营销收入"),
    IPDS("IPDS", "单次收入-衍生品收入"),
    SXDS("SXDS", "单次收入-视效收入"),
    XZDS("XZDS", "单次收入-行政运营及人力收入"),
    QTDS("QTDS", "单次收入-其他收入"),
    HLWDS("HLWDS", "单次收入-互联网产品收入"),
    TYDJ("TYDJ", "单次收支-通用单次收支"),
    SSKF("SSKF", "框架支出-赛事及活动支出框架"),
    DCKF("DCKF", "框架支出-地产运营支出框架"),
    DSKF("DSKF", "框架支出-电商运营支出框架"),
    ZCKF("ZCKF", "框架支出-资产采购租赁及经营支出框架"),
    GGKF("GGKF", "框架支出-广告赞助支出框架"),
    ZHKF("ZHKF", "框架支出-整合营销支出框架"),
    IPKF("IPKF", "框架支出-衍生品支出框架"),
    SXKF("SXKF", "框架支出-视效支出框架"),
    XZKF("XZKF", "框架支出-行政运营及人力支出框架"),
    QTKF("QTKF", "框架支出-其他支出框架"),
    HLWKF("HLWKF", "框架支出-互联网产品支出框架"),
    SSKS("SSKS", "框架收入-赛事及活动收入框架"),
    DCKS("DCKS", "框架收入-地产运营收入框架"),
    DSKS("DSKS", "框架收入-电商运营收入框架"),
    ZCKS("ZCKS", "框架收入-资产采购租赁及经营收入框架"),
    GGKS("GGKS", "框架收入-广告赞助收入"),
    ZHKS("ZHKS", "框架收入-整合营销收入框架"),
    IPKS("IPKS", "框架收入-衍生品收入"),
    SXKS("SXKS", "框架收入-视效收入框架"),
    XZKS("XZKS", "框架收入-行政运营及人力收入框架"),
    QTKS("QTKS", "框架收入-其他收入框架"),
    HLWKS("HLWKS", "框架收入-互联网产品收入框架"),
    MCNKS("MCNKS", "框架收入-MCN平台收入框架"),
    TYKJ("TYKJ", "框架收支-通用框架收支"),
    SSOF("SSOF", "订单支出-赛事及活动支出订单"),
    DCOF("DCOF", "订单支出-地产运营支出订单"),
    DSOF("DSOF", "订单支出-电商运营支出订单"),
    ZCOF("ZCOF", "订单支出-资产采购租赁及经营支出订单"),
    GGOF("GGOF", "订单支出-广告赞助支出订单"),
    ZHOF("ZHOF", "订单支出-整合营销支出订单"),
    IPOF("IPOF", "订单支出-衍生品支出订单"),
    SXOF("SXOF", "订单支出-视效支出订单"),
    XZOF("XZOF", "订单支出-行政运营及人力支出订单"),
    QTOF("QTOF", "订单支出-其他支出订单"),
    HLWOF("HLWOF", "订单支出-互联网产品支出订单"),
    SSOS("SSOS", "订单收入-赛事及活动收入订单"),
    DCOS("DCOS", "订单收入-地产运营收入订单"),
    DSOS("DSOS", "订单收入-电商运营订单收入"),
    ZCOS("ZCOS", "订单收入-资产采购租赁及经营收入订单"),
    GGOS("GGOS", "订单收入-广告赞助收入订单"),
    ZHOS("ZHOS", "订单收入-整合营销收入订单"),
    IPOS("IPOS", "订单收入-衍生品收入订单"),
    SXOS("SXOS", "订单收入-视效订单收入"),
    XZOS("XZOS", "订单收入-行政运营及人力收入订单"),
    QTOS("QTOS", "订单收入-其他收入订单"),
    HLWOS("HLWOS", "订单收入-互联网产品收入订单"),
    MCNOS("MCNOS", "订单收入-MCN平台收入订单"),
    TZ("TZ", "资本专项-投资协议"),
    RZ("RZ", "资本专项-融资协议"),
    JK("JK", "资本专项-借款协议"),
    QTCP("QTCP", "资本专项-其他");

    private final String code;

    private final String name;

    ContractCategoryMappingEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static ContractCategoryMappingEnum getByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        for (ContractCategoryMappingEnum category : values()) {
            if (category.name.equals(name.trim())) {
                return category;
            }
        }
        return null;
    }

    public static String getCodeByName(String name) {
        ContractCategoryMappingEnum category = getByName(name);
        return category == null ? null : category.getCode();
    }
}
