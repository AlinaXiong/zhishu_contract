package com.hero.middleware.client.zhishu.response;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

@Data
public class CreateTemplateInstanceResponse {
    /**
     * 外部系统单据id
     */
    @JSONField(name = "source_id")
    private String sourceid;

    /**
     * 模板id
     */
    @JSONField(name = "template_id")
    private String templateid;

    /**
     * 模板实例id
     */
    @JSONField(name = "template_instance_id")
    private String templateInstanceid;

    /**
     * 模板编号
     */
    @JSONField(name = "template_number")
    private String templateNumber;
}
