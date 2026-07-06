package com.hero.middleware.job;

import com.hero.middleware.service.FeiShuMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FeiShuMessageJob {

    @Autowired
    private FeiShuMessageService feiShuMessageService;

    /**
     * 每天上午10点发送合同到期提醒
     */
//    @Scheduled(cron = "0 0 10 * * ?")
    public void sendContractExpireRemindMessage() {
        log.info("开始执行飞书合同到期提醒定时任务");
        feiShuMessageService.sendContractExpireRemindMessage();
        log.info("飞书合同到期提醒定时任务执行完成");
    }
}
