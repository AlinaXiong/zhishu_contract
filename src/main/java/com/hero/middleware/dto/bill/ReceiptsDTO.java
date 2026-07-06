package com.hero.middleware.dto.bill;

import lombok.Data;

@Data
public class ReceiptsDTO {
    private String id;
    private String title;
    private String content;
    private Long createTime;
    private String mobileAppLink;
    private String pcAppLink;
}
