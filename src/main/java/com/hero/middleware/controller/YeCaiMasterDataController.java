package com.hero.middleware.controller;

import com.alibaba.fastjson.JSONObject;
import com.hero.middleware.common.Result;
import com.hero.middleware.dto.CustomerMasterDataSyncDTO;
import com.hero.middleware.dto.MasterDataSyncByTypeDTO;
import com.hero.middleware.dto.VendorMasterDataSyncDTO;
import com.hero.middleware.enums.MasterDataTypeEnum;
import com.hero.middleware.service.YeCaiToZhiShuService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;

@Api(tags = "业财主数据同步1")
@Slf4j
@RestController
@RequestMapping("/api/yecai/master-data")
public class YeCaiMasterDataController {

    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final String DATE_TIME_REGEX = "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}";
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss").withResolverStyle(ResolverStyle.STRICT);

    @Autowired
    private YeCaiToZhiShuService yeCaiToZhiShuService;

    @ApiOperation("同步供应商主数据到智书交易方")
    @PostMapping("/vendor/sync")
    public Result<Void> syncVendor(@RequestBody(required = false) VendorMasterDataSyncDTO dto) {
        log.info("同步供应商主数据到智书交易方-入参：{}", JSONObject.toJSONString(dto));
        return syncMasterData(
                MasterDataTypeEnum.VENDER,
                dto == null ? null : dto.getCertificationId(),
                dto == null ? null : dto.getStartTime(),
                dto == null ? null : dto.getEndTime(),
                dto == null ? null : dto.getPage(),
                dto == null ? null : dto.getSize());
    }

    @ApiOperation("同步客户主数据到智书交易方")
    @PostMapping("/customer/sync")
    public Result<Void> syncCustomer(@RequestBody(required = false) CustomerMasterDataSyncDTO dto) {
        log.info("同步客户主数据到智书交易方-入参：{}", JSONObject.toJSONString(dto));
        return syncMasterData(
                MasterDataTypeEnum.CUSTOMER,
                dto == null ? null : dto.getCertificationId(),
                dto == null ? null : dto.getStartTime(),
                dto == null ? null : dto.getEndTime(),
                dto == null ? null : dto.getPage(),
                dto == null ? null : dto.getSize());
    }

    @ApiOperation("按交易方编码同步供应商主数据到智书交易方")
    @PostMapping("/vendor/sync-by-code")
    public Result<Void> syncVendorByCode(@RequestBody(required = false) VendorMasterDataSyncDTO dto) {
        log.info("按交易方编码同步供应商主数据到智书交易方-入参：{}", JSONObject.toJSONString(dto));
        return syncMasterDataByVendorCode(
                MasterDataTypeEnum.VENDER,
                dto == null ? null : dto.getCertificationId(),
                dto == null ? null : dto.getStartTime(),
                dto == null ? null : dto.getEndTime(),
                dto == null ? null : dto.getPage(),
                dto == null ? null : dto.getSize());
    }

    @ApiOperation("按交易方编码同步客户主数据到智书交易方")
    @PostMapping("/customer/sync-by-code")
    public Result<Void> syncCustomerByCode(@RequestBody(required = false) CustomerMasterDataSyncDTO dto) {
        log.info("按交易方编码同步客户主数据到智书交易方-入参：{}", JSONObject.toJSONString(dto));
        return syncMasterDataByVendorCode(
                MasterDataTypeEnum.CUSTOMER,
                dto == null ? null : dto.getCertificationId(),
                dto == null ? null : dto.getStartTime(),
                dto == null ? null : dto.getEndTime(),
                dto == null ? null : dto.getPage(),
                dto == null ? null : dto.getSize());
    }

    @ApiOperation("Sync vendor or customer master data by type and time range")
    @PostMapping("/sync-by-code")
    public Result<Void> syncByCode(@RequestBody(required = false) MasterDataSyncByTypeDTO dto,
                                   @RequestParam(required = false) String type,
                                   @RequestParam(required = false) String businessType,
                                   @RequestParam(required = false) String certificationId,
                                   @RequestParam(required = false) String startTime,
                                   @RequestParam(required = false) String endTime,
                                   @RequestParam(required = false) Integer page,
                                   @RequestParam(required = false) Integer size) {
        MasterDataSyncByTypeDTO request = buildSyncByCodeRequest(
                dto, type, businessType, certificationId, startTime, endTime, page, size);
        log.info("Sync master data by type and time range, request: {}", JSONObject.toJSONString(request));
        MasterDataTypeEnum businessTypeEnum = resolveTradePartyType(firstNotBlank(request.getType(), request.getBusinessType()));
        if (businessTypeEnum == null) {
            return Result.error(400, "同步类型仅支持 VENDER/VENDOR 或 CUSTOMER");
        }
        return syncMasterDataByVendorCode(
                businessTypeEnum,
                request.getCertificationId(),
                request.getStartTime(),
                request.getEndTime(),
                request.getPage(),
                request.getSize());
    }

    private Result<Void> syncMasterData(MasterDataTypeEnum businessType,
                                        String certificationId,
                                        String startTime,
                                        String endTime,
                                        Integer page,
                                        Integer size) {
        return syncMasterDataInternal(businessType, certificationId, startTime, endTime, page, size, false);
    }

    private Result<Void> syncMasterDataByVendorCode(MasterDataTypeEnum businessType,
                                                    String certificationId,
                                                    String startTime,
                                                    String endTime,
                                                    Integer page,
                                                    Integer size) {
        return syncMasterDataInternal(businessType, certificationId, startTime, endTime, page, size, true);
    }

    private Result<Void> syncMasterDataInternal(MasterDataTypeEnum businessType,
                                                String certificationId,
                                                String startTime,
                                                String endTime,
                                                Integer page,
                                                Integer size,
                                                boolean byVendorCode) {
        certificationId = normalize(certificationId);
        startTime = normalize(startTime);
        endTime = normalize(endTime);
        if ((startTime == null) != (endTime == null)) {
            return Result.error(400, "查询起始时间和查询终止时间必须同时传入");
        }

        if ((page == null) != (size == null)) {
            return Result.error(400, "查询页码和每页数量必须同时传入");
        }
        if (page != null && page < 0) {
            return Result.error(400, "查询页码不能小于0");
        }
        if (size != null && size <= 0) {
            return Result.error(400, "每页数量必须大于0");
        }

        if (startTime != null) {
            LocalDateTime startDateTime;
            LocalDateTime endDateTime;
            try {
                startDateTime = parseDateTime(startTime);
                endDateTime = parseDateTime(endTime);
            } catch (DateTimeParseException e) {
                return Result.error(400, "查询时间格式必须为 " + DATE_TIME_PATTERN + "，且日期时间必须有效");
            }
            if (startDateTime.isAfter(endDateTime)) {
                return Result.error(400, "查询起始时间不能晚于查询终止时间");
            }
        }

        if (byVendorCode) {
            yeCaiToZhiShuService.synMasterDataByVendorCode(
                    businessType.getCode(), certificationId, startTime, endTime, page, size);
        } else {
            yeCaiToZhiShuService.synMasterData(
                    businessType.getCode(), certificationId, startTime, endTime, page, size);
        }
        return Result.success();
    }

    private LocalDateTime parseDateTime(String value) {
        if (!value.matches(DATE_TIME_REGEX)) {
            throw new DateTimeParseException("时间格式不正确", value, 0);
        }
        return LocalDateTime.parse(value, DATE_TIME_FORMATTER);
    }

    private String normalize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private MasterDataTypeEnum resolveTradePartyType(String type) {
        type = normalize(type);
        if (type == null) {
            return null;
        }
        String normalizedType = type.toUpperCase(Locale.ROOT);
        if ("VENDOR".equals(normalizedType)) {
            normalizedType = MasterDataTypeEnum.VENDER.getCode();
        }
        MasterDataTypeEnum businessType = MasterDataTypeEnum.getByCode(normalizedType);
        if (MasterDataTypeEnum.VENDER.equals(businessType) || MasterDataTypeEnum.CUSTOMER.equals(businessType)) {
            return businessType;
        }
        return null;
    }

    private String firstNotBlank(String first, String second) {
        first = normalize(first);
        if (first != null) {
            return first;
        }
        return normalize(second);
    }

    private MasterDataSyncByTypeDTO buildSyncByCodeRequest(MasterDataSyncByTypeDTO dto,
                                                           String type,
                                                           String businessType,
                                                           String certificationId,
                                                           String startTime,
                                                           String endTime,
                                                           Integer page,
                                                           Integer size) {
        MasterDataSyncByTypeDTO request = dto == null ? new MasterDataSyncByTypeDTO() : dto;
        if (normalize(request.getType()) == null) {
            request.setType(type);
        }
        if (normalize(request.getBusinessType()) == null) {
            request.setBusinessType(businessType);
        }
        if (normalize(request.getCertificationId()) == null) {
            request.setCertificationId(certificationId);
        }
        if (normalize(request.getStartTime()) == null) {
            request.setStartTime(startTime);
        }
        if (normalize(request.getEndTime()) == null) {
            request.setEndTime(endTime);
        }
        if (request.getPage() == null) {
            request.setPage(page);
        }
        if (request.getSize() == null) {
            request.setSize(size);
        }
        return request;
    }
}
