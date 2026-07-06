package com.hero.middleware.controller;

import com.hero.middleware.common.Result;
import com.hero.middleware.dto.ContractStatusDTO;
import com.hero.middleware.service.ContractSyncService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Api(tags = "合同状态同步")
@Slf4j
@RestController
@RequestMapping("/api/contract/status")
public class ContractStatusController {

    @Autowired
    private ContractSyncService contractSyncService;

    @ApiOperation("监听智书合同状态变更事件")
    @PostMapping("/sync")
    public Result<Void> syncContractStatus(@RequestBody ContractStatusDTO dto) {
        log.info("接收智书合同状态变更事件: {}", dto);
        contractSyncService.syncContractStatus(dto);
        return Result.success();
    }

}
