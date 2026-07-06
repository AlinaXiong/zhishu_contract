package com.hero.middleware.controller;

import com.hero.middleware.common.Result;
import com.hero.middleware.dto.ApproveContractToNodeDTO;
import com.hero.middleware.dto.ApproveContractToNodeResultDTO;
import com.hero.middleware.dto.DeleteDraftContractsResultDTO;
import com.hero.middleware.dto.HistoryContractSyncDTO;
import com.hero.middleware.dto.HistoryContractSyncResultDTO;
import com.hero.middleware.dto.YeCaiContractSyncDTO;
import com.hero.middleware.dto.YeCaiContractSyncResultDTO;
import com.hero.middleware.service.ZhiShuSynService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Api(tags = "智书历史合同同步")
@Slf4j
@RestController
@RequestMapping("/api/contract/syn")
public class ContractSynController {

    @Autowired
    private ZhiShuSynService zhiShuSynService;

    @ApiOperation("同步历史合同到智书")
    @PostMapping("/history")
    public Result<HistoryContractSyncResultDTO> syncHistoryContracts(
            @RequestBody(required = false) HistoryContractSyncDTO request) {
        log.info("接收智书历史合同同步请求，请求参数：{}", request);
        HistoryContractSyncResultDTO result = zhiShuSynService.syncHistoryContracts(request);
        log.info("智书历史合同同步请求处理完成，结果：{}", result);
        return Result.success(result);
    }

    @ApiOperation("多线程同步历史合同到智书")
    @PostMapping("/history/multi-thread")
    public Result<HistoryContractSyncResultDTO> syncHistoryContractsMultiThread(
            @RequestBody(required = false) HistoryContractSyncDTO request) {
        log.info("接收智书历史合同多线程同步请求，请求参数：{}", request);
        String validateMessage = validateMultiThreadHistorySyncRequest(request);
        if (validateMessage != null) {
            log.warn("智书历史合同多线程同步请求参数错误：{}", validateMessage);
            return Result.error(400, validateMessage);
        }
        HistoryContractSyncResultDTO result = zhiShuSynService.syncHistoryContractsMultiThread(request);
        log.info("智书历史合同多线程同步请求处理完成，结果：{}", result);
        return Result.success(result);
    }

    @ApiOperation("按智书合同编码同步业财")
    @PostMapping("/yecai")
    public Result<YeCaiContractSyncResultDTO> syncYeCaiContracts(
            @RequestBody(required = false) YeCaiContractSyncDTO request) {
        log.info("接收按智书合同编码同步业财请求，请求参数：{}", request);
        String validateMessage = validateYeCaiContractSyncRequest(request);
        if (validateMessage != null) {
            log.warn("按智书合同编码同步业财请求参数错误：{}", validateMessage);
            return Result.error(400, validateMessage);
        }
        YeCaiContractSyncResultDTO result = zhiShuSynService.syncYeCaiContracts(request);
        log.info("按智书合同编码同步业财请求处理完成，结果：{}", result);
        return Result.success(result);
    }

    @ApiOperation("智书合同审核到指定节点")
    @PostMapping("/approve-to-node")
    public Result<ApproveContractToNodeResultDTO> approveContractsToNode(
            @RequestBody(required = false) ApproveContractToNodeDTO request) {
        log.info("接收智书合同审核到指定节点请求，请求参数：{}", request);
        ApproveContractToNodeResultDTO result = zhiShuSynService.approveContractsToNode(request);
        log.info("智书合同审核到指定节点请求处理完成，结果：{}", result);
        return Result.success(result);
    }

    @ApiOperation("按节点分组审核合同到指定节点")
    @PostMapping("/approve-to-node/map")
    public Result<Map<String, ApproveContractToNodeResultDTO>> approveContractsToNodeByMap(
            @RequestBody(required = false) Map<String, List<String>> request) {
        log.info("接收按节点分组审核合同请求，请求参数：{}", request);
        String validateMessage = validateApproveToNodeMapRequest(request);
        if (validateMessage != null) {
            log.warn("按节点分组审核合同请求参数错误：{}", validateMessage);
            return Result.error(400, validateMessage);
        }
        Map<String, ApproveContractToNodeResultDTO> result = zhiShuSynService.approveContractsToNode(request);
        log.info("按节点分组审核合同请求处理完成，结果：{}", result);
        return Result.success(result);
    }

    @ApiOperation("删除智书草稿合同")
    @PostMapping("/draft-contracts/delete")
    public Result<DeleteDraftContractsResultDTO> deleteDraftContracts() {
        log.info("接收删除智书草稿合同请求");
        DeleteDraftContractsResultDTO result = zhiShuSynService.deleteAllDraftContracts();
        log.info("删除智书草稿合同请求处理完成，结果：{}", result);
        return Result.success(result);
    }

    private String validateMultiThreadHistorySyncRequest(HistoryContractSyncDTO request) {
        if (request == null) {
            return "请求参数不能为空";
        }
        boolean hasContractNumbers = request.getContractNumbers() != null && !request.getContractNumbers().isEmpty();
        boolean hasContractNumberFile = trimToNull(request.getContractNumberFilePath()) != null;
        if (!hasContractNumbers && !hasContractNumberFile) {
            return "执行编码集合和执行编码txt文件地址至少需要存在一个";
        }
        if (trimToNull(request.getResolvedFilePath()) == null) {
            return "导入模板地址不能为空";
        }
        if (trimToNull(request.getContractFileFallbackRoot()) == null) {
            return "合同附件查询根路径不能为空";
        }
        return null;
    }

    private String validateYeCaiContractSyncRequest(YeCaiContractSyncDTO request) {
        if (request == null) {
            return "请求参数不能为空";
        }
        boolean hasContractNumbers = request.getContractNumbers() != null && !request.getContractNumbers().isEmpty();
        boolean hasContractNumberFile = trimToNull(request.getContractNumberFilePath()) != null;
        if (!hasContractNumbers && !hasContractNumberFile) {
            return "执行编码集合和执行编码txt文件地址至少需要存在一个";
        }
        return null;
    }

    private String validateApproveToNodeMapRequest(Map<String, List<String>> request) {
        if (request == null || request.isEmpty()) {
            return "节点审核参数不能为空";
        }
        for (Map.Entry<String, List<String>> entry : request.entrySet()) {
            String nodeName = trimToNull(entry.getKey());
            if (nodeName == null) {
                return "节点名称不能为空";
            }
            List<String> contractNumbers = entry.getValue();
            if (contractNumbers == null || contractNumbers.isEmpty()) {
                return "节点[" + nodeName + "]的合同编号集合不能为空";
            }
            boolean hasContractNumber = false;
            for (String contractNumber : contractNumbers) {
                if (trimToNull(contractNumber) != null) {
                    hasContractNumber = true;
                    break;
                }
            }
            if (!hasContractNumber) {
                return "节点[" + nodeName + "]的合同编号集合不能为空";
            }
        }
        return null;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimValue = value.trim();
        return trimValue.isEmpty() ? null : trimValue;
    }
}
