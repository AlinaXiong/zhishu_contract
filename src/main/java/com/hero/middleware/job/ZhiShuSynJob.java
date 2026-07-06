package com.hero.middleware.job;

import com.hero.middleware.enums.MasterDataTypeEnum;
import com.hero.middleware.service.YeCaiToZhiShuService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class ZhiShuSynJob {

    private static final ZoneId SHANGHAI_ZONE_ID = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private YeCaiToZhiShuService yeCaiToZhiShuService;

    /**
     * 每小时同步最近一小时供应商信息
     */
//    @Scheduled(cron = "0 0 * * * ?")
    public void synVendor() {
        String[] timeRange = buildRecentOneHourTimeRange();
        String startTime = timeRange[0];
        String endTime = timeRange[1];
        log.info("供应商一小时增量同步开始，查询起始时间：{}，查询终止时间：{}", startTime, endTime);
        yeCaiToZhiShuService.synMasterDataByVendorCode(
                MasterDataTypeEnum.VENDER.getCode(), null, startTime, endTime, null, null);
        log.info("供应商一小时增量同步结束，查询起始时间：{}，查询终止时间：{}", startTime, endTime);
    }

    /**
     * 每小时同步最近一小时客户信息
     */
//    @Scheduled(cron = "0 0 * * * ?")
    public void synCustomer() {
        String[] timeRange = buildRecentOneHourTimeRange();
        String startTime = timeRange[0];
        String endTime = timeRange[1];
        log.info("客户一小时增量同步开始，查询起始时间：{}，查询终止时间：{}", startTime, endTime);
        yeCaiToZhiShuService.synMasterDataByVendorCode(
                MasterDataTypeEnum.CUSTOMER.getCode(), null, startTime, endTime, null, null);
        log.info("客户一小时增量同步结束，查询起始时间：{}，查询终止时间：{}", startTime, endTime);
    }

    /**
     * 每天早上10点同步前一天所有汇率信息
     */
//    @Scheduled(cron = "0 0 10 * * ?")
    public void synExchangeRate() {
        String[] timeRange = buildPreviousDayTimeRange();
        String startTime = timeRange[0];
        String endTime = timeRange[1];
        log.info("汇率前一天增量同步开始，查询起始时间：{}，查询终止时间：{}", startTime, endTime);
        yeCaiToZhiShuService.synMasterData(
                MasterDataTypeEnum.EXCHANGE_RATE.getCode(), null, startTime, endTime, null, null);
        log.info("汇率前一天增量同步结束，查询起始时间：{}，查询终止时间：{}", startTime, endTime);
    }

    private String[] buildRecentOneHourTimeRange() {
        LocalDateTime endTime = LocalDateTime.now(SHANGHAI_ZONE_ID);
        LocalDateTime startTime = endTime.minusHours(1);
        return new String[]{startTime.format(DATE_TIME_FORMATTER), endTime.format(DATE_TIME_FORMATTER)};
    }

    private String[] buildPreviousDayTimeRange() {
        LocalDate previousDay = LocalDateTime.now(SHANGHAI_ZONE_ID).toLocalDate().minusDays(1);
        LocalDateTime startTime = previousDay.atStartOfDay();
        LocalDateTime endTime = previousDay.atTime(23, 59, 59);
        return new String[]{startTime.format(DATE_TIME_FORMATTER), endTime.format(DATE_TIME_FORMATTER)};
    }
}
