package com.hero.middleware.controller;

import com.hero.middleware.common.Result;
import com.hero.middleware.dto.ReceiptSyncDTO;
import com.hero.middleware.exception.BusinessException;
import com.hero.middleware.service.ReceiptService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Api(tags = "收款记录管理")
@Slf4j
@RestController
@RequestMapping("/api/receipt")
public class ReceiptController {

    @Autowired
    private ReceiptService receiptService;

    @ApiOperation("收款记录同步")
    @PostMapping("/sync")
    public Result<Void> syncReceipt(@Validated @RequestBody ReceiptSyncDTO dto) {
        log.info("接收业财系统收款记录同步请求: {}", dto);
        try{
            receiptService.syncReceipt(dto);
        }catch (BusinessException e){
            return Result.error(e.getCode(), e.getMessage());
        }
        return Result.success();
    }

}
