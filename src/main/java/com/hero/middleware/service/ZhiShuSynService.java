package com.hero.middleware.service;

import com.hero.middleware.dto.ApproveContractToNodeDTO;
import com.hero.middleware.dto.ApproveContractToNodeResultDTO;
import com.hero.middleware.dto.DeleteDraftContractsResultDTO;
import com.hero.middleware.dto.HistoryContractSyncDTO;
import com.hero.middleware.dto.HistoryContractSyncResultDTO;
import com.hero.middleware.dto.HistoryContractValidateResultDTO;
import com.hero.middleware.dto.YeCaiContractSyncDTO;
import com.hero.middleware.dto.YeCaiContractSyncResultDTO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ZhiShuSynService {

    /**
     * 同步历史合同到智书。
     *
     * @param request 同步请求；合同编码为空时同步Excel中的全部合同。
     * @return 同步结果。
     */
    HistoryContractSyncResultDTO syncHistoryContracts(HistoryContractSyncDTO request);

    HistoryContractSyncResultDTO syncHistoryContractsMultiThread(HistoryContractSyncDTO request);

    HistoryContractValidateResultDTO validateHistoryContractsMultiThread(HistoryContractSyncDTO request);

    YeCaiContractSyncResultDTO syncYeCaiContracts(YeCaiContractSyncDTO request);

    /**
     * 同步历史反商业贿赂协议合同到智书。
     *
     * @param request 同步请求；合同编码为空时同步Excel中的全部反商业贿赂协议合同。
     * @return 同步结果。
     */
    HistoryContractSyncResultDTO syncHistoryAntiBriberyContracts(HistoryContractSyncDTO request);

    /**
     * 同步历史合同到智书。
     *
     * @param contractNumbers 合同编码集合，为空时同步Excel中的全部合同。
     * @return 同步结果。
     */
    HistoryContractSyncResultDTO syncHistoryContracts(Collection<String> contractNumbers);

    /**
     * 从指定Excel文件同步历史合同到智书。
     *
     * @param contractNumbers 合同编码集合，为空时同步Excel中的全部合同。
     * @param filePath Excel文件路径，为空时使用默认文件路径。
     * @return 同步结果。
     */
    HistoryContractSyncResultDTO syncHistoryContracts(Collection<String> contractNumbers, String filePath);

    /**
     * 按合同编码查询智书合同，未查询到的合同编码导出到file目录下的Excel。
     *
     * @param contractNumber 合同编码。
     * @return 生成的Excel文件路径。
     */
    String exportNotFoundContracts(String contractNumber);

    /**
     * 按合同编码批量查询智书合同，未查询到的合同编码导出到file目录下的Excel。
     *
     * @param contractNumbers 合同编码集合。
     * @return 生成的Excel文件路径。
     */
    String exportNotFoundContracts(Collection<String> contractNumbers);

    /**
     * 将草稿合同审核到指定节点。
     *
     * @param request 审核请求。
     * @return 审核结果。
     */
    ApproveContractToNodeResultDTO approveContractsToNode(ApproveContractToNodeDTO request);

    /**
     * 将草稿合同审核到指定节点。
     *
     * @param contractNumbers 合同编码集合。
     * @param nodeName 目标节点名称。
     * @return 审核结果。
     */
    ApproveContractToNodeResultDTO approveContractsToNode(Collection<String> contractNumbers, String nodeName);

    /**
     * 按节点名称分组，将合同审核到指定节点。
     *
     * @param contractNumbersByNodeName key 为节点名称，value 为合同编号集合。
     * @return 按节点名称分组的审核结果。
     */
    Map<String, ApproveContractToNodeResultDTO> approveContractsToNode(Map<String, List<String>> contractNumbersByNodeName);

    DeleteDraftContractsResultDTO deleteAllDraftContracts();
}
