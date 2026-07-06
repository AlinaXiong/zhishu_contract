package com.hero.middleware.client.yuecai.request;

import lombok.Data;

import java.util.Map;

@Data
public class ProcessActionRequest {
    /**
     * 动作类型
     * APPROVE 同意
     * REJECT 驳回
     */
    private String actionType;

    /**
     * 是否显示意见输入框，默认不隐藏
     * 0-隐藏，1-显示，默认0
     */
    private Integer commentHideFlag = 0;

    /**
     * 审批意见是否必输，默认必输
     * 0-非必输，1-必输，默认1
     */
    private Integer commentRequiredFlag = 1;

    /**
     * 动作回调地址
     */
    private String actionBackUrl;

    /**
     * 回调token
     */
    private String actionBackToken;

    /**
     * 回调额外参数
     * 现在为固定值
     */
    private Map<String, Object> actionBackParam;
}
