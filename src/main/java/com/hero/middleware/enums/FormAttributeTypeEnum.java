package com.hero.middleware.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;

public enum FormAttributeTypeEnum {

    SINGLELINE_TEXT("singleline_text", "单行文本"),
    MULTILINE_TEXT("multiline_text", "多行文本"),
    STRING("string", "多行文本"),
    NUMBER("number", "数字"),
    DATE("date", "日期"),
    DATE_RANGE("date_range", "日期区间"),
    RADIO("radio", "单选-radio"),
    CHECKBOX("checkbox", "多选-checkbox"),
    DROPDOWN_RADIO("dropdown_radio", "下拉单选"),
    DROPDOWN_OPTION("dropdown_option", "下拉多选"),
    EMPLOYEE("employee", "人员"),
    DEPARTMENT("department", "部门"),
    AMOUNT("amount", "金额"),
    HYPERLINK("hyperlink", "超链接"),
    FEISHU_APPROVAL("feishu_approval", "关联前置单据"),
    THIRD_PARTY_APPROVAL("third_party_approval", "第三方单据"),
    COUNTRY_OR_REGION("country_or_region", "国家地区"),
    ADDRESS("address", "地址"),
    TREE_RADIO("tree_radio", "树形单选"),
    TREE_OPTION("tree_option", "树形多选"),
    CALCULATION("calculation", "公式"),
    FILE("file", "附件"),
    ARRAY("array", "明细字段-array"),
    COMMON_ARRAY("common_array", "明细字段-common_array");

    private final String code;

    @Getter
    private final String name;

    FormAttributeTypeEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static FormAttributeTypeEnum fromCode(String code) {
        return getByCode(code);
    }

    public static FormAttributeTypeEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (FormAttributeTypeEnum type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return null;
    }

    public static Map<String, String> toMap() {
        Map<String, String> map = new LinkedHashMap<>();
        for (FormAttributeTypeEnum type : values()) {
            map.put(type.code, type.name);
        }
        return map;
    }
}
