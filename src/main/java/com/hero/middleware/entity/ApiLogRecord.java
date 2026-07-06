package com.hero.middleware.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("t_api_log")
public class ApiLogRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String traceId;

    private String apiName;

    private String apiDescription;

    private String requestMethod;

    private String requestUrl;

    private String requestParams;

    private String responseParams;

    private String requestIp;

    private String userAgent;

    private Long executeTime;

    private Integer httpStatus;

    private Integer status;

    private String errorMessage;

    private String operatorId;

    private String operatorName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

}
