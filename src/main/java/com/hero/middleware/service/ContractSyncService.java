package com.hero.middleware.service;

import com.hero.middleware.dto.ContractStatusDTO;

public interface ContractSyncService {

    void syncContractStatus(ContractStatusDTO dto);

}
