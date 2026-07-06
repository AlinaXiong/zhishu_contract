package com.hero.middleware.client.zhishu.response;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.util.List;

@Data
public class PrecedingDocResponse {
    private int total;//总共数量
    private List<Receipts> receipts;//单据
    @Data
    public static class Receipts{
        private String id;//唯一标识
        private String title;//标题
        private String content;//内容
        private Long createTime;//创建时间
        @JSONField(name = "mobile_app_link")
        private String mobileAppLink;//手机端访问链接
        @JSONField(name = "pc_app_link")
        private String pcAppLink;//pc 端访问链接
        private String sponsor;//实际是需要使用飞书的userId
    }
}
