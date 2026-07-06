package com.hero.middleware.service;

import com.hero.middleware.dto.ApiLogEvent;

public interface ApiLogService {

    void record(ApiLogEvent event);
}
