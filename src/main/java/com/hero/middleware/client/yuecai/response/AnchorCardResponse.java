package com.hero.middleware.client.yuecai.response;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class AnchorCardResponse {
    //id
    private String id;
    //头id
    private Long headerId;
    //创建人id
    private String creatorId;
    //创建人
    private String creatorName;
    //姓名
    private String realName;
    //证照类型
    private String certificateType;
    //证照号码
    private String certificateNumber;
    //是否歌舞厅
    private String isEntertainmentHall;
    //国籍
    private String nationality;
    //手机号
    private String mobile;
    //微信号
    private String wechat;
    //支付宝账号
    private String alipayAccount;
    //其他联系方式
    private String otherContact;
    //经纪人
    private String brokerId;

    //成本中心id
    private String costCenterId;

    private String costCenter;

    //签约主体
    private String signingEntity;

    private String signingEntityName;
    //主播平台信息dto
    private List<AnchorCardLineRes> lineResultDTOS;
    //主播银行信息dto
    private List<AnchorCardBackLineRes> BackLineResultDTOS;

    @Data
    public static class AnchorCardLineRes{
        private Long id;
        //头id
        private Long headerId;
        //主播昵称
        private String anchorNickname;
        //id/房间号
        private String anchorId;
        //当前平台
        private String platform;
        //账号状态
        private String accountType;
        //直播品类
        private String liveCategory;
        //直播板块
        private String liveSection;
        //签约开始日期
        private Date contractStartDate;
        //签约结束日期
        private Date contractEndDate;
        //战队名称
        private String teamName;
    }

    @Data
    public static class AnchorCardBackLineRes{
        //头id
        private Long headerId;
        //收款人名称
        private String payeeName;
        //收款人性质
        private String payeeType;
        //联系电话
        private String contactPhone;
        //开户银行
        private String bankName;
        //开户支行
        private String bankBranch;
        //银行卡号
        private String bankCardNumber;
        //身份证号
        private String payeeIdNumber;
    }

}
