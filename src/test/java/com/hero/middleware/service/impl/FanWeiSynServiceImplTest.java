package com.hero.middleware.service.impl;

import com.hero.middleware.service.FanWeiSynService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@SpringBootTest
public class FanWeiSynServiceImplTest {

    @Autowired
    private FanWeiSynService fanWeiSynService;

    @Test
    public void fanWeiToZhiShuContract() {
        Map<String,Object> params = new HashMap<>();
        params.put("old_main_id","");
        params.put("templateNumber","202604220006");
        fanWeiSynService.fanWeiToZhiShuContract(params);
    }

}
