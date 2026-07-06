package com.hero.middleware.job;

import com.hero.middleware.service.YeCaiToZhiShuService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GetYeCaiInfoJob {

    @Autowired
    private YeCaiToZhiShuService yeCaiToZhiShuService;

    /**
     * 每天凌晨1点刷新业财员工编码与飞书用户ID缓存
     */
//    @Scheduled(cron = "0 0 1 * * ?")
    public void refreshEmployeeCodeAndUserIdMap() {
        log.info("开始执行业财员工编码与飞书用户ID缓存刷新定时任务");
        yeCaiToZhiShuService.refreshEmployeeCodeAndUserIdMap();
        log.info("业财员工编码与飞书用户ID缓存刷新定时任务执行完成");
    }
}
