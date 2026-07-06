package com.hero.middleware.job;

import com.hero.middleware.enums.MasterDataTypeEnum;
import com.hero.middleware.service.YeCaiToZhiShuService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class ZhiShuSynJobTest {

    private static final String THREE_HOUR_CRON = "0 0 0/3 * * ?";
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private YeCaiToZhiShuService yeCaiToZhiShuService;
    private ZhiShuSynJob job;

    @BeforeEach
    void setUp() {
        yeCaiToZhiShuService = mock(YeCaiToZhiShuService.class);
        job = new ZhiShuSynJob();
        ReflectionTestUtils.setField(job, "yeCaiToZhiShuService", yeCaiToZhiShuService);
    }

    @Test
    void synVendorRunsEveryThreeHours() throws Exception {
        Scheduled scheduled = ZhiShuSynJob.class.getMethod("synVendor").getAnnotation(Scheduled.class);

        assertNotNull(scheduled);
        assertEquals(THREE_HOUR_CRON, scheduled.cron());
    }

    @Test
    void synCustomerRunsEveryThreeHours() throws Exception {
        Scheduled scheduled = ZhiShuSynJob.class.getMethod("synCustomer").getAnnotation(Scheduled.class);

        assertNotNull(scheduled);
        assertEquals(THREE_HOUR_CRON, scheduled.cron());
    }

    @Test
    void synVendorUsesRecentThreeHourWindowAndByCodeSync() {
        job.synVendor();

        TimeRange timeRange = verifyByCodeSyncAndCaptureTimeRange(MasterDataTypeEnum.VENDER.getCode());
        assertEquals(Duration.ofHours(3), Duration.between(timeRange.startTime, timeRange.endTime));
        verify(yeCaiToZhiShuService, never()).synMasterData(
                eq(MasterDataTypeEnum.VENDER.getCode()), anyString(), anyString());
        verifyNoMoreInteractions(yeCaiToZhiShuService);
    }

    @Test
    void synCustomerUsesRecentThreeHourWindowAndByCodeSync() {
        job.synCustomer();

        TimeRange timeRange = verifyByCodeSyncAndCaptureTimeRange(MasterDataTypeEnum.CUSTOMER.getCode());
        assertEquals(Duration.ofHours(3), Duration.between(timeRange.startTime, timeRange.endTime));
        verify(yeCaiToZhiShuService, never()).synMasterData(
                eq(MasterDataTypeEnum.CUSTOMER.getCode()), anyString(), anyString());
        verifyNoMoreInteractions(yeCaiToZhiShuService);
    }

    private TimeRange verifyByCodeSyncAndCaptureTimeRange(String businessType) {
        ArgumentCaptor<String> startTimeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> endTimeCaptor = ArgumentCaptor.forClass(String.class);

        verify(yeCaiToZhiShuService).synMasterDataByVendorCode(
                eq(businessType),
                isNull(),
                startTimeCaptor.capture(),
                endTimeCaptor.capture(),
                isNull(),
                isNull());

        return new TimeRange(
                LocalDateTime.parse(startTimeCaptor.getValue(), DATE_TIME_FORMATTER),
                LocalDateTime.parse(endTimeCaptor.getValue(), DATE_TIME_FORMATTER));
    }

    private static class TimeRange {
        private final LocalDateTime startTime;
        private final LocalDateTime endTime;

        private TimeRange(LocalDateTime startTime, LocalDateTime endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
        }
    }
}
