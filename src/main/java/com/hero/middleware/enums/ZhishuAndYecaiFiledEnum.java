package com.hero.middleware.enums;

public enum ZhishuAndYecaiFiledEnum {

    CONTRACT_NAME("contract_name", "documentDesc", null, "合同名称"),
    CONTRACT_CATEGORY_ABBREVIATION("contract_category_abbreviation", "contractType", null, "合同类型"),
    CONTRACT_NUMBER("contract_number", "documentNumber", null, "合同编号"),
    PREVIOUS_CONTRACT_NUMBER("previous_contract_number", "documentNumber", null, "合同编号"),
    CONTRACT_EXECUTOR("custom_1001_948719050bfe402ab083c98e52fa71b2", "respEmployeeCode", null, "合同执行人"),
    PRINT_MODE("custom_15_78cf503c57194e4fb8ad03ded1c4ad60", "", null, "打印模式"),
    SIGN_DATE("custom_10_9a2a0e99771346c98bfb6cfb893e1bee", "", null, "签署日期"),
    ORDER_INFO("custom_1024_90a78c8120994f95b2dbfedd297c7d81", "dimension2Code", null, "订单编号"),
    COST_CENTER("custom_16_0b772134a1ee46499128178d63190b69", "responsibilityCenterCode", null, "成本中心"),
    PURCHASE_REQUEST("custom_1024_7db9a8ee2b3d4a3f9d9835dd9fee69df", "purchaseRequest", null, "采购申请"),
    SPECIAL_CATEGORY("custom_15_de8944334b104d52b28d9472ab0584ef", "specialCategory", null, "专项分类"),
    CAST_AMOUNT("custom_1012_cec7052f613b465980f23f7004e2f82c", "", null, "采购金额"),
    OUT_INCOME_TAX_RATE("custom_15_b83a9d26fdb741319698a45994517558", "", null, "支出税率"),
    IN_INCOME_TAX_RATE("custom_15_4cc0342691164d26aaa851f0f2dba7cd", "", null, "收入税率"),
    OUT_TAX_ITEM("custom_15_ef15f3fac0db4abca6831352c7252e0c", "", null, "支出税目"),
    IN_TAX_ITEM("custom_15_e4a7839dca744aaf82965b27837a51ba", "", null, "收入税目"),
    DEPOSIT_AMOUNT("custom_1012_7e2c970e63f648268eaefbd13d6bfc8f", "", null, "押金/保证金"),
    FIRST_PAYMENT_AMOUNT("custom_1012_7c79ac40b9ec4efb8be367a43f480d01", "", null, "首款金额"),
    FINAL_PAYMENT_AMOUNT("custom_1012_98dfe42395274c53926c2c37b2dbd9a9", "", null, "尾款金额"),
    EFFECTIVE_TIME("custom_12_b6e6d91d406b45a3868d0ecf948de4b8", "effectiveTime", null, "有效时间"),
    LOAN_DATE_RANGE("custom_12_4ab4c13cc8d24521a86a086dd124bea0", "loanStartDate", "loanEndDate", "借款起止日"),
    ACCEPTANCE_REQUIRED("custom_13_c9805a6fe9f245ebbfeea13407277306", "acceptanceRequired", null, "是否需要验收"),
    PAYMENT_NODE_TYPE("custom_15_071a641657e94f2faf65bf973850166e", "paymentNodeType", null, "付款性质-付款节点属性"),
    PAY_TYPE("pay_type", "incomeExpenseType", null, "收支类型"),
    PROPERTY_TYPE_CODE("property_type_code", "valuationMethod", null, "计价方式"),
    TOTAL_AMOUNT("total_amount", "amount", null, "合同总额"),
    FUNCTION_AMOUNT("total_amount", "functionAmount", null, "本币金额"),
    ESTIMATED_AMOUNT("estimated_amount", "estimatedAmount", null, "预估金额"),
    FIXED_VALIDITY("fixed_validity", "contractTermType", null, "合同期限类型"),
    DATE_RANGE("date_range", "startDate", null, "合同期限"),
    MAJOR_CONTRACT_FLAG("custom_16_e32be022424c4c27af75c42ba997e337", "majorContractFlag", null, "是否涉及重大权利义务条款"),
    BANK_CHARGE_PAYER("custom_15_fd4b04ae59064d6fb5df6b5ad4f626df", "bankChargePayer", null, "银行手续费承担方"),
    INVOICE_TYPE("custom_15_ad035716cc97463a9e72ae78259c218e", "invoiceType", null, "发票种类"),
    PLATFORM("custom_15_92d6805fe16b4f40aa31609a6e1530c3", "platform", null, "所属平台"),
    REQUISITION_DATE("custom_10_9a2a0e99771346c98bfb6cfb893e1bee", "requisitionDate", null, "签署日期"),
    LIVE_CATEGORY("custom_15_f0b4b1f6e5184d359055745be54a3f37", "liveCategory", null, "直播品类"),
    PAYMENT_AMOUNT("payment_amount", "amount", null, "支出金额"),
    PAYMENT_AMOUNT2("payment_amount", "paymentAmount", null, "支出金额"),
    PAYMENT_DATE("payment_date", "dueDate", null, "支出日期"),
    PAYMENT_CURRENCY_CODE("currency_code", "paymentCurrencyCode", null, "支出币种"),
    CURRENCY_CODE("currency_code", "currencyCode", null, "币种"),
    PAYMENT_ACCEPTED_FLAG("custom_13_c9805a6fe9f245ebbfeea13407277306", "acceptedFlag", null, "是否需要验收"),
    ACCEPTANCE_TIME_AGREEMENT("custom_5_c9431df7da064e94a32d6a0c3bdfb83a", "acceptanceTimeAgreement", null, "验收时间（天）"),
    PAYMENT_INTERVAL_DAYS("payment_interval_days", "acceptanceTimeAgreement", null, "履约条件完成到付款日之间的间隔时间"),
    COLLECTION_AMOUNT("collection_amount", "amount", null, "收入金额"),
    COLLECTION_AMOUNT2("collection_amount", "paymentAmount", null, "收入金额"),
    COLLECTION_RMB_AMOUNT("collection_amount", "rmb_amount", null, "收入人民币金额"),

    ORDERHT_DOCUMENT_INFO("custom_1201_7abb8aec0c9a492391206bae3e3ac7ab", "", null, "订单信息"),
    ORDERHT_DOCUMENT_NUMBER("custom_1_7f977c0d30064dd199434f706470c669", "contractNumber", null, "订单-单据编码"),
    ORDERHT_DOCUMENT_NAME("custom_1_5549b19faea641eeac924deada603c11", "", null, "订单名称"),
    ORDERHT_COST_CENTER("custom_16_3171b080033943c9a98380f20e0895a8", "", null, "成本中心"),
    ORDERHT_ORDER_DATE("custom_12_d67e0d9472134b1cba5187e192bb2670", "", null, "订单周期"),
    ORDERHT_ORDER_TYPE("custom_15_622e96ab047c4f689d287a27066f7bcb", "", null, "订单类型"),
    ORDERHT_EXPENSE_GROUP("custom_1001_0a61d360dcad4265a68d2555d17e896e", "", null, "日常费用组"),
    ORDERHT_PROJECT_ACCEPTANCE("custom_1001_f8f9114f511346f9adc7fabae012f17a", "", null, "项目验收岗"),
    ORDERHT_PROJECT_BUDGET("custom_1001_8b8028ca466f4841bead801a9c7fedf2", "", null, "项目预算岗"),
    ORDERHT_PROJECT_SPONOSOR("custom_1001_9072dcb2126e4051854da3927f742ab9", "", null, "项目Sponsor"),

    ANCHORHT_DOCUMENT_NUMBER("custom_1024_61820798c0f348658d8daa64f8b2aef9", "contractNumber", null, "主播-单据编码"),
    ORDER_DOCUMENT_NUMBER("custom_1024_90a78c8120994f95b2dbfedd297c7d81", "contractNumber", null, "订单-单据编码"),
    ANCHOR_DOCUMENT_NUMBER("custom_1024_61820798c0f348658d8daa64f8b2aef9", "contractNumber", null, "主播卡片"),
    PROCUREMENT_DOCUMENT_NUMBER("custom_1024_7db9a8ee2b3d4a3f9d9835dd9fee69df", "contractNumber", null, "采购-单据编码"),
    COLLECTION_DATE("collection_date", "dueDate", null, "收款日期"),
    ROOM_ID("custom_1024_61820798c0f348658d8daa64f8b2aef9", "roomId", null, "房间号/主播ID"),//目前用途是主播卡片id暂存至房间号/主播ID字段，后边通过这个字段查询主播信息，会重新为主播卡片相关信息赋值
    PROJECT_MANAGER("custom_1001_b193503253664cf28b2ca1c3f57b68b3", "", null, "项目经理"),
    ANCHOR_NAME("custom_1_ab6f99ee02e549469ec5b2d4a5a98452", "", null, "主播姓名"),
    ANCHOR_NICKNAME("custom_1_4fa3c71e706c489e94977935b512b0f6", "", null, "主播昵称"),
    ANCHOR_ROOM_ID("custom_1_543d4d9106f34c31bf3f9397ded6ef28", "", null, "房间号/主播ID"),
    ANCHOR_ID_CARD("custom_1_c97a63f71e1048aea384680a64aa3573", "", null, "主播身份证号码"),
    ANCHOR_TEAM_NAME("custom_1_ba05d6fb71bc4a778b1eca63423a38bc", "", null, "战队名称"),

    GIFT_BASIC_SHARE_RATIO("custom_5_d6aaf62f8568491c8f5824285e72499d", "", null, "礼物基础分成比例"),
    LIVE_PLATFORM_BASIC_COOPERATION_FEE("custom_5_fc246ba4165145af92cffb2e6088c4b3", "", null, "直播平台签约后每月基本合作费"),
    SELF_MEDIA_BUSINESS_INCOME("custom_5_a8e7beeebc7b4253a9ec3c713d9d790a", "", null, "自媒体平台账号商务收入"),
    SELF_MEDIA_ACCOUNT_INCOME("custom_5_c0a993cb06774ab38ea3e75187631ab5", "", null, "自媒体平台支付的自媒体账号收入"),
    OFFICIAL_SIGNING_FEE("custom_1012_dbf82175f1964048b83b18550f3bb8d1", "", null, "官签签约金"),
    OFFICIAL_SIGNING_FEE_SHARE_RATIO("custom_5_f53355b8e15546a7a187575f07cf59ee", "", null, "官签签约金分成比例"),
    FIXED_BASE_SALARY_PER_MONTH("custom_1012_3c0986ffce8848caab9249df37e5d49e", "", null, "固定底薪（每月）"),
    COMPANY_SIGNING_FEE("custom_1012_07b5c0b4f40a414588ce86d43705f5c4", "", null, "公司签约金"),
    OTHER_FEE("custom_1012_a01fe29e8faf471a92d0f002f22aea48", "", null, "其他费用"),
    SIGNING_TERM_MONTHS("custom_5_057b181679934312a3edd122a75efb8c", "", null, "签约期限（月）"),
    ESTIMATED_PAYBACK_CYCLE_MONTHS("custom_5_de3f9a77f174445495453e444e984037", "", null, "预计回本周期（月）"),
    OTHER_DISTRIBUTABLE_INCOME("custom_5_def7270057bd4913a1fd087b4b1f128e", "", null, "本合同项下其他可分配收益"),
    GUARANTEED_FEE("custom_1012_65df3bda1aae46a1822c4d4531be5e25", "", null, "保底费用"),
    BASIC_SERVICE_FEE("custom_1012_27f3d0bf4d1d44cb82333cfe22443b65", "", null, "基础服务费"),
    ANCHOR_FEE_DETAIL("custom_1201_9ffd79c1e5da4492bfdab24bce0d93f8", "", null, "费用明细"),
    ANCHOR_FEE_ITEM("custom_15_02f24c2051cb4cc5a5d7a9bc7230f3ff", "", null, "费用项"),
    ANCHOR_MONTH_AVERAGE_INCOME("custom_1012_fba348aede86465b90d669eda6e956a7", "", null, "收入/月平均"),
    ANCHOR_MONTH_AVERAGE_COST("custom_1012_869047e5ebc3425d912890ccd9455a6e", "", null, "成本/月平均"),
    ANCHOR_MONTH_AVERAGE_GROSS_PROFIT("custom_1012_47053c50bd7742e79f39e3f37e718720", "", null, "毛利/月平均"),

    RECEIPT_INVOICE_NUMBER("custom_1_32a9f1c93e144153828eca344f4f7270", "", null, "开票编号"),
    RECEIPT_INVOICE_AMOUNT("custom_1012_b14ed42196864937b2cdd8356f1993f1", "", null, "开票金额"),
    RECEIPT_INVOICE_ENTITY("custom_1_c7f44c6730ea4a6ca69ed2a63151a864", "", null, "开票主体"),
    RECEIPT_ORDER_NUMBER("custom_1_45e12829cc1d45c5a201de8bbdbfda6e", "", null, "订单编号"),
    RECEIPT_ORDER_NAME("custom_1_60457a32f8f449469156456d13c91de8", "", null, "订单名称"),
    RECEIPT_COST_CENTER("custom_1_b5fdc619ecb14461a2ea7a4ead8c9056", "", null, "成本中心"),
    RECEIPT_CLIENT_NAME("custom_1_f1bbe1972d5b41e69f36ab2bcb7da499", "", null, "客户名称");

    private final String zhishuFiled;

    private final String yecaiFiled;

    private final String yecaiEndFiled;

    private final String name;

    ZhishuAndYecaiFiledEnum(String zhishuFiled, String yecaiFiled, String yecaiEndFiled, String name) {
        this.zhishuFiled = zhishuFiled;
        this.yecaiFiled = yecaiFiled;
        this.yecaiEndFiled = yecaiEndFiled;
        this.name = name;
    }

    public String getZhishuFiled() {
        return zhishuFiled;
    }

    public String getYecaiFiled() {
        return yecaiFiled;
    }

    public String getYecaiEndFiled() {
        return yecaiEndFiled;
    }

    public String getName() {
        return name;
    }

    public static ZhishuAndYecaiFiledEnum getByZhishuFiled(String zhishuFiled) {
        if (zhishuFiled == null || zhishuFiled.trim().isEmpty()) {
            return null;
        }
        for (ZhishuAndYecaiFiledEnum fieldEnum : values()) {
            if (zhishuFiled.equals(fieldEnum.getZhishuFiled())) {
                return fieldEnum;
            }
        }
        return null;
    }
}
