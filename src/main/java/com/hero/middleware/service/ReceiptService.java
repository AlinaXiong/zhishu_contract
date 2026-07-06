package com.hero.middleware.service;

import com.hero.middleware.dto.ReceiptSyncDTO;

public interface ReceiptService {

    void syncReceipt(ReceiptSyncDTO dto);

}
