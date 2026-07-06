package com.hero.middleware.client.zhishu.response;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class QueryContractCategoryResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer code;

    private String msg;

    private DataInfo data;

    public boolean isSuccess() {
        return code == null || code == 0;
    }

    @Data
    public static class DataInfo implements Serializable {

        private static final long serialVersionUID = 1L;

        @JSONField(name = "categoryResources")
        private List<CategoryResource> categoryResources;

        @JSONField(name = "contract_category_resource_vo")
        private ContractCategoryResourceVo contractCategoryResourceVo;

        public List<CategoryResource> getCategoryResources() {
            if (categoryResources != null) {
                return categoryResources;
            }
            return contractCategoryResourceVo == null ? null : contractCategoryResourceVo.getCategoryResources();
        }
    }

    @Data
    public static class ContractCategoryResourceVo implements Serializable {

        private static final long serialVersionUID = 1L;

        @JSONField(name = "category_resources")
        private List<CategoryResource> categoryResources;
    }

    @Data
    public static class CategoryResource implements Serializable {

        private static final long serialVersionUID = 1L;

        private String name;

        private String number;

        private String abbreviation;

        private String id;

        private String description;

        private List<CategoryResource> children;
    }
}
