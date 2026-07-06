package com.hero.middleware.client.zhishu.response;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class ContractFormQueryResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<FormAttribute> form;

    @Data
    public static class FormAttribute implements Serializable {

        private static final long serialVersionUID = 1L;

        @JSONField(name = "attribute_key")
        private String attributeKey;

        @JSONField(name = "attribute_code")
        private String attributeCode;

        @JSONField(name = "attribute_name")
        private String attributeName;

        @JSONField(name = "attribute_type")
        private String attributeType;

        @JSONField(name = "module_name")
        private String moduleName;

        /**
         * Different form components return different shapes:
         * String, Number, JSONObject, List, or nested List<List<FormAttribute>>.
         */
        @JSONField(name = "attribute_value")
        private Object attributeValue;
    }

    @Data
    public static class OptionValue implements Serializable {

        private static final long serialVersionUID = 1L;

        private Object key;

        @JSONField(name = "outbound_options")
        private List<OptionNode> outboundOptions;
    }

    @Data
    public static class OptionNode implements Serializable {

        private static final long serialVersionUID = 1L;

        private String key;

        private String name;

        private String value;

        @JSONField(name = "labelCn")
        private String labelCn;

        @JSONField(name = "labelEn")
        private String labelEn;

        @JSONField(name = "labelJp")
        private String labelJp;

        private List<OptionNode> children;
    }

    @Data
    public static class AmountValue implements Serializable {

        private static final long serialVersionUID = 1L;

        private BigDecimal amount;

        private String currency;

        @JSONField(name = "currency_name")
        private String currencyName;
    }

    @Data
    public static class HyperlinkValue implements Serializable {

        private static final long serialVersionUID = 1L;

        private String title;

        private String url;
    }

    @Data
    public static class CountryOrRegionValue implements Serializable {

        private static final long serialVersionUID = 1L;

        @JSONField(name = "country_code")
        private String countryCode;
    }

    @Data
    public static class TreeNodeValue implements Serializable {

        private static final long serialVersionUID = 1L;

        private String key;

        private String name;
    }

    @Data
    public static class FileValue implements Serializable {

        private static final long serialVersionUID = 1L;

        @JSONField(name = "file_id")
        private String fileId;

        @JSONField(name = "file_name")
        private String fileName;

        @JSONField(name = "file_size")
        private String fileSize;

        private String mime;
    }

    @Data
    public static class EmployeeValue implements Serializable {

        private static final long serialVersionUID = 1L;

        @JSONField(name = "user_id_type")
        private String userIdType;

        @JSONField(name = "user_id")
        private String userId;
    }

    @Data
    public static class DepartmentValue implements Serializable {

        private static final long serialVersionUID = 1L;
        @JSONField(name = "department_id_type")
        private String departmentIdType;

        @JSONField(name = "department_id")
        private String departmentId;

        @JSONField(name = "open_department_id")
        private String openDepartmentId;

        @JSONField(name = "lark_department_id")
        private String larkDepartmentId;
    }

    @Data
    public static class ApprovalValue implements Serializable {

        private static final long serialVersionUID = 1L;

        private String id;

        private String title;

        private String content;

        private Map<String, Object> links;
    }

    @Data
    public static class AddressValue implements Serializable {

        private static final long serialVersionUID = 1L;

        @JSONField(name = "country_code")
        private String countryCode;

        @JSONField(name = "region_code")
        private String regionCode;

        @JSONField(name = "city_code")
        private String cityCode;

        private String address;

        private Map<String, Object> extra;
    }
}
