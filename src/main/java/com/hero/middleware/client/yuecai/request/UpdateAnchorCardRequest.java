package com.hero.middleware.client.yuecai.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class UpdateAnchorCardRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;//是 主播正式档案表 anchor_profile_list.id
    private String anchorNickname;//否 主播昵称；为空时默认取正式档案姓名
    private String anchorId;//是 主播ID/房间号
    private String platform;//是 平台编码
    private String accountType;//否 账号状态编码
    private String liveCategory;//否 直播品类编码；未传时保留原值
    private String liveSection;//否 直播板块编码；未传时保留原值
    private String teamName;//否 战队名称
    private String contractStartDate;//是 签约开始日期，格式：yyyy-MM-dd
    private String contractEndDate;//是 签约结束日期，格式：yyyy-MM-dd
    private Double officialSigningBonusIncome;//否 官签收入
    private Double officialSigningBonusRatio;//否 官签分成比例
    private Double officialSignDurationRatio;//否 官签时长分成比例，最多 6 位小数
    private Double companySigningBonus;//否 公司签约费
    private Double fixedBaseSalary;//否 固定底薪
    private Double salaryRatio;//否 薪资比例
    private Double giftRatio;//否 礼物分成比例
    private Double businessRatio;//否 商务分成比例
    private Double selfMediaRatio;//否 自媒体分成比例
    private String otherInfo;//否 其他说明

}
