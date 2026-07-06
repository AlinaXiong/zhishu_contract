package com.hero.middleware.service;

import com.hero.middleware.dto.PaymentSyncDTO;

public interface PaymentService {

    void syncPayment(PaymentSyncDTO dto);

}
