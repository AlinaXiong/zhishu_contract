package com.hero.middleware.client.zhishu.response;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class QueryTemplateListResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer code;

    private String msg;

    private String message;

    private DataInfo data;

    public boolean isSuccess() {
        return code != null && code == 0;
    }

    @Data
    public static class DataInfo implements Serializable {

        private static final long serialVersionUID = 1L;

        @JSONField(name = "has_more")
        private Boolean hasMore;

        @JSONField(name = "page_token")
        private String pageToken;

        @JSONField(name = "next_page_token")
        private String nextPageToken;

        @JSONField(name = "template_brief_infos")
        private List<TemplateBriefInfo> templateBriefInfos;
    }

    @Data
    public static class TemplateBriefInfo implements Serializable {

        private static final long serialVersionUID = 1L;

        @JSONField(name = "template_id")
        private String templateId;

        @JSONField(name = "template_number")
        private String templateNumber;

        @JSONField(name = "template_name")
        private String templateName;

        @JSONField(name = "contract_category_number")
        private String contractCategoryNumber;

        @JSONField(name = "contract_category_name")
        private String contractCategoryName;
    }
}
