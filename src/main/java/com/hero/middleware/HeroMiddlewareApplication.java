package com.hero.middleware;

import com.hero.middleware.config.YuecaiApiConfig;
import com.hero.middleware.config.ZhishuApiConfig;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@MapperScan("com.hero.middleware.mapper")
@EnableConfigurationProperties({ZhishuApiConfig.class, YuecaiApiConfig.class})
public class HeroMiddlewareApplication {

    public static void main(String[] args) {
        SpringApplication.run(HeroMiddlewareApplication.class, args);
    }

}
