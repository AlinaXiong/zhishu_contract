package com.hero.middleware.controller;

import com.hero.middleware.common.Result;
import com.hero.middleware.dto.ContractSyncDTO;
import com.hero.middleware.service.ContractService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Api(tags = "业财系统合同同步")
@Slf4j
@RestController
@RequestMapping("/api/yuecai/contract")
public class YuecaiContractController {

    @Autowired
    private ContractService contractService;

    @ApiOperation("业财系统合同变更同步至智书")
    @PutMapping("/sync")
    public Result<Void> updateContractFromYuecai(@RequestBody ContractSyncDTO dto) {
        log.info("接收业财系统合同变更同步请求: {}", dto);
        contractService.updateContractFromYuecai(dto);
        return Result.success();
    }

}
