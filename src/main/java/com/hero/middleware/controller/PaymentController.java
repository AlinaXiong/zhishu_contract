package com.hero.middleware.controller;

import com.hero.middleware.common.Result;
import com.hero.middleware.dto.PaymentSyncDTO;
import com.hero.middleware.service.PaymentService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Api(tags = "付款记录管理")
@Slf4j
@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @ApiOperation("付款记录同步 - 业财系统付款成功后同步至智书合同")
    @PostMapping("/sync")
    public Result<Void> syncPayment(@Validated @RequestBody PaymentSyncDTO dto) {
        log.info("接收业财系统付款记录同步请求, 合同ID: {}, 业务编号: {}, 付款金额: {}", 
                dto.getContractId(), dto.getBusinessCode(), dto.getPaymentAmount());
        paymentService.syncPayment(dto);
        log.info("付款记录同步成功");
        return Result.success();
    }

}
