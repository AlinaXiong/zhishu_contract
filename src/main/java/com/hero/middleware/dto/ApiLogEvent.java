package com.hero.middleware.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApiLogEvent {

    private String targetSystem;

    private String action;

    private String httpMethod;

    private String url;

    private String requestParams;

    private String responseBody;

    private Integer httpStatus;

    private String exceptionMessage;

    private Long startTime;

    private Long durationMs;
}
