package com.hero.middleware.service.impl;

import cn.hutool.core.util.URLUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.annotation.JSONField;
import com.alibaba.fastjson.JSONObject;
import com.hero.middleware.annotation.SkipApiLogTable;
import com.hero.middleware.client.feishu.FeiShuApiClient;
import com.hero.middleware.client.feishu.response.FeishuUserBatchInfoResponse;
import com.hero.middleware.client.feishu.response.FeishuUserInfoResponse;
import com.hero.middleware.client.yuecai.YuecaiContractClient;
import com.hero.middleware.client.yuecai.response.AnchorCardResponse;
import com.hero.middleware.client.yuecai.response.MasterDataRes;
import com.hero.middleware.client.yuecai.response.OrderInfoResponse;
import com.hero.middleware.client.yuecai.response.ProcurementResponse;
import com.hero.middleware.client.yuecai.response.masterdata.CustomerRes;
import com.hero.middleware.client.yuecai.response.masterdata.VenderRes;
import com.hero.middleware.client.zhishu.ZhishuContractClient;
import com.hero.middleware.client.zhishu.request.ApprovalContractRequest;
import com.hero.middleware.client.zhishu.request.ContractFormCreatResponse;
import com.hero.middleware.client.zhishu.request.ContractsSearchRequest;
import com.hero.middleware.client.zhishu.request.ZhishuCreateContractRequest;
import com.hero.middleware.client.zhishu.response.ApprovalQueryResponse;
import com.hero.middleware.client.zhishu.response.ApprovalResponse;
import com.hero.middleware.client.zhishu.response.ContractQueryResponse;
import com.hero.middleware.client.zhishu.response.ContractsSearchResponse;
import com.hero.middleware.client.zhishu.response.PrecedingDocResponse;
import com.hero.middleware.client.zhishu.response.QueryContractCategoryResponse;
import com.hero.middleware.client.zhishu.response.ResultResponse;
import com.hero.middleware.client.zhishu.response.SubmitContractResponse;
import com.hero.middleware.client.zhishu.response.UploadContractFileResponse;
import com.hero.middleware.client.zhishu.response.ZhishuCreateContractResponse;
import com.hero.middleware.config.YeCaiDataConfig;
import com.hero.middleware.config.YuecaiApiConfig;
import com.hero.middleware.context.ApiLogTableContext;
import com.hero.middleware.dto.ApproveContractToNodeDTO;
import com.hero.middleware.dto.ApproveContractToNodeResultDTO;
import com.hero.middleware.dto.DeleteDraftContractsResultDTO;
import com.hero.middleware.dto.HistoryContractSyncDTO;
import com.hero.middleware.dto.HistoryContractSyncResultDTO;
import com.hero.middleware.dto.ContractSyncDTO;
import com.hero.middleware.dto.HistoryContractValidateResultDTO;
import com.hero.middleware.dto.YeCaiContractSyncDTO;
import com.hero.middleware.dto.YeCaiContractSyncResultDTO;
import com.hero.middleware.enums.BankChargePayerEnum;
import com.hero.middleware.enums.ContractCategoryMappingEnum;
import com.hero.middleware.enums.FormAttributeTypeEnum;
import com.hero.middleware.enums.InvoiceTypeEnum;
import com.hero.middleware.enums.MasterDataTypeEnum;
import com.hero.middleware.enums.PlatformEnum;
import com.hero.middleware.enums.PrintModeEnum;
import com.hero.middleware.enums.TaxItemEnum;
import com.hero.middleware.enums.ZhishuAndYecaiFiledEnum;
import com.hero.middleware.service.ZhiShuSynService;
import com.hero.middleware.service.ContractService;
import com.hero.middleware.utils.DateUtils;
import com.hero.middleware.utils.ExcelUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@SkipApiLogTable
public class ZhiShuSynServiceImpl implements ZhiShuSynService {

    private static final String DEFAULT_EXCEL_PATH =
            "E:/lidongliang/需求文档/英雄电竞/泛微合同历史数据同步/业财项目_迁移数据集 (1)_合同数据填充_13sheet_合同库一般合同重拉_快速版.xlsx";
    private static final String DEFAULT_ANTI_BRIBERY_EXCEL_PATH =
//            "C:/Users/AAA/Downloads/签署反商业贿赂协议6.25终版.xlsx";
            "E:/lidongliang/需求文档/英雄电竞/泛微合同历史数据同步/签署反商业贿赂协议6.25终版.xlsx";
    private static final int ANTI_BRIBERY_DATA_SHEET_INDEX = 3;
    private static final String ANTI_BRIBERY_FLOW_TYPE = "反商业贿赂协议";
    private static final String ANTI_BRIBERY_HEADER_CONTRACT_NUMBER = "contract_number（合同编码）";
    private static final String ANTI_BRIBERY_HEADER_CONTRACT_NAME = "contract_name（合同名称）";
    private static final String ANTI_BRIBERY_HEADER_SUBMITTED_TIME = "签署日期";
    private static final String ANTI_BRIBERY_HEADER_CREATE_USER_ID = "合同申请人（user_id)";
    private static final String ANTI_BRIBERY_HEADER_COUNTER_PARTY_ID = "对方信息id";
    private static final String ANTI_BRIBERY_HEADER_OUR_PARTY_ID = "我方信息id";
    private static final String ANTI_BRIBERY_HEADER_REMARK = "remark（合同说明）";
    private static final int HEADER_ROW_INDEX = 0;
    private static final int DATA_START_ROW_INDEX = 1;
    private static final int DEFAULT_BATCH_SIZE = 200;
    private static final int DEFAULT_MULTI_THREAD_COUNT = 5;
    private static final int DEFAULT_MULTI_THREAD_BATCH_SIZE = 10;
    private static final String DEFAULT_HISTORY_CONTRACT_STATUS_CODE = "9";
    private static final String YECAI_SYNC_SUCCESS = "成功";
    private static final String YECAI_SYNC_FAIL = "失败";
    private static final int CONTRACT_SEARCH_PAGE_SIZE = 100;
    private static final int APPROVE_TO_NODE_MAX_STEPS = 20;
    private static final int CONTRACT_DRAFT_STATUS_CODE = 0;
    private static final String CONTRACT_DRAFT_STATUS_NAME = "editing";
    private static final String APPROVE_COMMAND_TYPE = "general";
    private static final String APPROVE_TO_NODE_COMMENT = "自动审核到指定节点";
    private static final String CONTRACT_NUMBER = "contract_number";
    private static final String CONTRACT_CATEGORY = "contractCategory";
    private static final String DEFAULT_CONTRACT_CATEGORY_ABBREVIATION = "FSYHL";
    private static final String DEFAULT_CURRENCY_CODE = "CNY";
//    private static final String DEFAULT_CONTRACT_FILE_FALLBACK_ROOT = "D:/hero/反商业贿赂协议合同附件_20260626";
    private static final String DEFAULT_CONTRACT_FILE_FALLBACK_ROOT = "D:/hero/一般流程合同附件_20260630";
//    private static final String DEFAULT_CONTRACT_FILE_FALLBACK_ROOT = "D:/hero/主播流程合同附件_20260629";
    private static final String FILE_TYPE_TEXT = "text";
    private static final String FILE_TYPE_ATTACHMENT = "attachment";
    private static final String FILE_TYPE_CAUSE = "cause";
    private static final String FILE_TYPE_SCAN = "scan";
    private static final String ANTI_BRIBERY_MAIN_FILE_FOLDER = "主文件";
    private static final String ANTI_BRIBERY_SCAN_FILE_FOLDER = "归档扫描件";
    private static final boolean NEED_CONVERT_TO_PDF = false;

    private static final String FIELD_OUR_PARTY_CODE = "our_party_code";
    private static final String FIELD_COUNTER_PARTY_CODE = "counter_party_code";
    private static final String FIELD_PAYMENT_DATE = "payment_plan_list[].payment_date";
    private static final String FIELD_PAYMENT_PREPAID = "payment_plan_list[].prepaid";
    private static final String FIELD_PAYMENT_AMOUNT = "payment_plan_list[].payment_amount";
    private static final String FIELD_PAYMENT_DESC = "payment_plan_list[].payment_desc";
    private static final String FIELD_PAYMENT_TYPE = "payment_plan_list[].payment_custom_attributes/custom_15_071a641657e94f2faf65bf973850166e";
    private static final String FIELD_PAYMENT_COUNTER_PARTY =
            "payment_plan_list[].payment_counter_party[].counter_party_code";
    private static final String FIELD_COLLECTION_DATE = "collection_plan_list[].collection_date";
    private static final String FIELD_COLLECTION_AMOUNT = "collection_plan_list[].collection_amount";
    private static final String FIELD_COLLECTION_DESC = "collection_plan_list[].collection_desc";
    private static final String FIELD_COLLECTION_COUNTER_PARTY =
            "collection_plan_list[].collection_counter_party[].counter_party_code";
    private static final String FIELD_INCOME_CURRENCY_CODE = "收入币种编码";
    private static final String FIELD_EXPENSE_CURRENCY_CODE = "支出币种编码";
    private static final String FIELD_RELATION_CONTRACTS = "relation.relation_contracts";
    private static final String FIELD_FRAMEWORK_CONTRACT_NUMBER = "框架合同编号";
    private static final String FIELD_CONTRACT_TEXT = "contract_files.contract_text";
    private static final String FIELD_CONTRACT_CAUSES = "contract_files.contract_causes";
    private static final String FIELD_CONTRACT_ATTACHMENTS = "contract_files.contract_attachments";
    private static final String FIELD_SIGN_TYPE_CODE_SEAL_PARTY = "sign_type_code#seal_party";
    private static final String FIELD_SIGN_TYPE_CODE_SIGN_FORM = "sign_type_code#sign_form";
    private static final String DOCUMENT_LIST_URL = "/exp/requisition/list";
    private static final String ORDER_INFO_URL = "/project/order-query/list";
    private static final String ANCHOR_CARD_URL = "/hfbs/anchor-doc/document";
    private static final String ACCEPTANCE_REQUIRED_YES_CODE = "cmp0y2rse004e3b716tihbjhf";
    private static final String ACCEPTANCE_REQUIRED_NO_CODE = "cmp0y2rse004f3b71rrqup04i";
    private static final String CONTRACT_NOT_FOUND_EXPORT_DIR = "file";

    private static final Pattern DATE_PATTERN = Pattern.compile("\\d{4}[-/]\\d{1,2}[-/]\\d{1,2}");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("\\d+");
    private static final Map<String, Field> CREATE_REQUEST_FIELDS = buildCreateRequestFields();

    @Autowired
    private ZhishuContractClient zhishuContractClient;

    @Autowired
    private YeCaiDataConfig yeCaiDataConfig;

    @Autowired
    private YuecaiContractClient yuecaiContractClient;

    @Autowired
    private YuecaiApiConfig yuecaiApiConfig;

    @Autowired
    private FeiShuApiClient feiShuApiClient;

    @Autowired
    private ContractService contractService;

    private final String historyContractExcelPath;
    private final int batchSize;
    private Path contractFileFallbackRoot = Paths.get(DEFAULT_CONTRACT_FILE_FALLBACK_ROOT);

    public ZhiShuSynServiceImpl() {
        this(DEFAULT_EXCEL_PATH, DEFAULT_BATCH_SIZE);
    }

    ZhiShuSynServiceImpl(String historyContractExcelPath) {
        this(historyContractExcelPath, DEFAULT_BATCH_SIZE);
    }

    ZhiShuSynServiceImpl(String historyContractExcelPath, int batchSize) {
        this.historyContractExcelPath = historyContractExcelPath;
        this.batchSize = batchSize <= 0 ? DEFAULT_BATCH_SIZE : batchSize;
    }

    @Override
    public HistoryContractSyncResultDTO syncHistoryContracts(HistoryContractSyncDTO request) {
        long startTime = System.currentTimeMillis();
        HistoryContractSyncDTO actualRequest = request == null ? new HistoryContractSyncDTO() : request;
        Set<String> contractNumberFilter = resolveContractNumberFilter(actualRequest);
        String excelPath = resolveExcelPath(actualRequest.getResolvedFilePath());
        SyncContext context = buildSyncContext(actualRequest);

        log.info("智书13Sheet历史合同同步开始，excelPath={}，过滤合同编码={}", excelPath, contractNumberFilter);
        try {
            context.setCounterPartyCodeLookup(loadCounterPartyCodeLookup());
            ContractIndex contractIndex = scanContractIndex(excelPath);
            Set<String> initialTargetKeys = resolveTargetGroupKeys(contractNumberFilter, contractIndex, context);
            Set<String> expandedTargetKeys = expandRelationDependencies(initialTargetKeys, contractIndex);
            ContractSyncOrder syncOrder = sortByDependencies(expandedTargetKeys, contractIndex);
            recordSortFailures(syncOrder, contractIndex, context);
            log.info("智书13Sheet历史合同同步待处理合同数={}，依赖展开后合同数={}，可同步合同数={}，批大小={}",
                    initialTargetKeys.size(), expandedTargetKeys.size(), syncOrder.getOrderedGroupKeys().size(), batchSize);
            syncInBatches(excelPath, syncOrder.getOrderedGroupKeys(), contractIndex, context);
        } catch (Exception e) {
            log.error("智书13Sheet历史合同同步整体异常，excelPath={}，错误={}", excelPath, e.getMessage(), e);
            context.getResult().addFailure("ALL", null, "同步整体异常：" + e.getMessage());
        }

        long elapsedMillis = System.currentTimeMillis() - startTime;
        HistoryContractSyncResultDTO result = context.getResult();
        result.setElapsedMillis(elapsedMillis);
        result.refreshTotalCount();
        log.info("智书13Sheet历史合同同步结束，总数={}，成功数={}，失败数={}，成功合同编码={}，失败明细={}，耗时={}ms",
                result.getTotalCount(), result.getSuccessCount(), result.getFailCount(),
                result.getSuccessContractNumbers(), JSON.toJSONString(result.getFailures()), elapsedMillis);
        return result;
    }

    @Override
    public HistoryContractSyncResultDTO syncHistoryContractsMultiThread(HistoryContractSyncDTO request) {
        long startTime = System.currentTimeMillis();
        HistoryContractSyncResultDTO mergedResult = new HistoryContractSyncResultDTO();
        HistoryContractSyncDTO actualRequest = request == null ? new HistoryContractSyncDTO() : request;
        Set<String> contractNumberSet;
        try {
            contractNumberSet = resolveContractNumberFilter(actualRequest);
        } catch (Exception e) {
            mergedResult.addFailure("ALL", null, e.getMessage());
            finishHistoryMultiThreadResult(mergedResult, startTime);
            return mergedResult;
        }
        String excelPath = trimToNull(actualRequest.getResolvedFilePath());
        String fallbackRoot = trimToNull(actualRequest.getContractFileFallbackRoot());
        if (contractNumberSet.isEmpty()) {
            mergedResult.addFailure("ALL", null, "合同编码集合和txt文件地址不能同时为空");
            finishHistoryMultiThreadResult(mergedResult, startTime);
            return mergedResult;
        }
        if (excelPath == null) {
            mergedResult.addFailure("ALL", null, "导入模板地址不能为空");
            finishHistoryMultiThreadResult(mergedResult, startTime);
            return mergedResult;
        }
        if (fallbackRoot == null) {
            mergedResult.addFailure("ALL", null, "合同附件查询根路径不能为空");
            finishHistoryMultiThreadResult(mergedResult, startTime);
            return mergedResult;
        }

        int threadCount = resolvePositiveInteger(actualRequest.getThreadCount(), DEFAULT_MULTI_THREAD_COUNT);
        int batchSize = resolvePositiveInteger(actualRequest.getBatchSize(), DEFAULT_MULTI_THREAD_BATCH_SIZE);
        List<String> contractNumbers = new ArrayList<>(contractNumberSet);
        List<List<String>> batches = splitContractNumberBatches(contractNumbers, batchSize);
        log.info("智书13Sheet历史合同多线程同步开始，合同数={}，批次数={}，线程数={}，批大小={}，excelPath={}，fallbackRoot={}，contractStatusCode={}",
                contractNumbers.size(), batches.size(), threadCount, batchSize, excelPath, fallbackRoot,
                resolveContractStatusCode(actualRequest));

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        List<Future<HistoryContractSyncResultDTO>> futures = new ArrayList<>();
        try {
            for (int index = 0; index < batches.size(); index++) {
                final int batchNo = index + 1;
                final List<String> batchContractNumbers = batches.get(index);
                futures.add(executorService.submit(ApiLogTableContext.wrap(() -> {
                    log.info("智书13Sheet历史合同多线程同步第{}批开始，合同数={}，合同编码={}",
                            batchNo, batchContractNumbers.size(), JSON.toJSONString(batchContractNumbers));
                    HistoryContractSyncDTO batchRequest = copyHistorySyncRequestForBatch(actualRequest, batchContractNumbers);
                    HistoryContractSyncResultDTO batchResult = syncHistoryContracts(batchRequest);
                    log.info("智书13Sheet历史合同多线程同步第{}批完成，结果={}", batchNo, JSON.toJSONString(batchResult));
                    return batchResult;
                })));
            }

            for (int index = 0; index < futures.size(); index++) {
                List<String> batchContractNumbers = batches.get(index);
                try {
                    mergeHistorySyncResult(mergedResult, futures.get(index).get());
                } catch (Exception e) {
                    log.error("智书13Sheet历史合同多线程同步第{}批异常，合同编码={}，错误={}",
                            index + 1, JSON.toJSONString(batchContractNumbers), e.getMessage(), e);
                    for (String contractNumber : batchContractNumbers) {
                        mergedResult.addFailure(contractNumber, null, "批次同步异常：" + e.getMessage());
                    }
                }
            }
        } finally {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(10, TimeUnit.MINUTES)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
                log.warn("智书13Sheet历史合同多线程同步等待线程池结束被中断，错误={}", e.getMessage(), e);
            }
        }

        finishHistoryMultiThreadResult(mergedResult, startTime);
        log.info("智书13Sheet历史合同多线程同步结束，总数={}，成功数={}，失败数={}，成功合同编码={}，失败明细={}，耗时={}ms",
                mergedResult.getTotalCount(), mergedResult.getSuccessCount(), mergedResult.getFailCount(),
                mergedResult.getSuccessContractNumbers(), JSON.toJSONString(mergedResult.getFailures()),
                mergedResult.getElapsedMillis());
        return mergedResult;
    }

    @Override
    public HistoryContractValidateResultDTO validateHistoryContractsMultiThread(HistoryContractSyncDTO request) {
        long startTime = System.currentTimeMillis();
        HistoryContractValidateResultDTO mergedResult = new HistoryContractValidateResultDTO();
        HistoryContractSyncDTO actualRequest = request == null ? new HistoryContractSyncDTO() : request;
        Set<String> contractNumberSet;
        try {
            contractNumberSet = resolveContractNumberFilter(actualRequest);
        } catch (Exception e) {
            mergedResult.addFailure("ALL", null, e.getMessage());
            finishHistoryValidateResult(mergedResult, startTime);
            return mergedResult;
        }
        String excelPath = trimToNull(actualRequest.getResolvedFilePath());
        String fallbackRoot = trimToNull(actualRequest.getContractFileFallbackRoot());
        if (contractNumberSet.isEmpty()) {
            mergedResult.addFailure("ALL", null, "合同编码集合和txt文件地址不能同时为空");
            finishHistoryValidateResult(mergedResult, startTime);
            return mergedResult;
        }
        if (excelPath == null) {
            mergedResult.addFailure("ALL", null, "导入模板地址不能为空");
            finishHistoryValidateResult(mergedResult, startTime);
            return mergedResult;
        }
        if (fallbackRoot == null) {
            mergedResult.addFailure("ALL", null, "合同附件查询根路径不能为空");
            finishHistoryValidateResult(mergedResult, startTime);
            return mergedResult;
        }

        int threadCount = resolvePositiveInteger(actualRequest.getThreadCount(), DEFAULT_MULTI_THREAD_COUNT);
        int batchSize = resolvePositiveInteger(actualRequest.getBatchSize(), DEFAULT_MULTI_THREAD_BATCH_SIZE);
        List<String> contractNumbers = new ArrayList<>(contractNumberSet);
        List<List<String>> batches = splitContractNumberBatches(contractNumbers, batchSize);
        log.info("智书13Sheet历史合同多线程校验开始，合同数={}，批次数={}，线程数={}，批大小={}，excelPath={}，fallbackRoot={}，contractStatusCode={}",
                contractNumbers.size(), batches.size(), threadCount, batchSize, excelPath, fallbackRoot,
                resolveContractStatusCode(actualRequest));

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        List<Future<HistoryContractValidateResultDTO>> futures = new ArrayList<>();
        try {
            for (int index = 0; index < batches.size(); index++) {
                final int batchNo = index + 1;
                final List<String> batchContractNumbers = batches.get(index);
                futures.add(executorService.submit(ApiLogTableContext.wrap(() -> {
                    log.info("智书13Sheet历史合同多线程校验第{}批开始，合同数={}，合同编码={}",
                            batchNo, batchContractNumbers.size(), JSON.toJSONString(batchContractNumbers));
                    HistoryContractSyncDTO batchRequest = copyHistorySyncRequestForBatch(actualRequest, batchContractNumbers);
                    HistoryContractValidateResultDTO batchResult = validateHistoryContracts(batchRequest);
                    log.info("智书13Sheet历史合同多线程校验第{}批完成，结果={}", batchNo, JSON.toJSONString(batchResult));
                    return batchResult;
                })));
            }

            for (int index = 0; index < futures.size(); index++) {
                List<String> batchContractNumbers = batches.get(index);
                try {
                    mergeHistoryValidateResult(mergedResult, futures.get(index).get());
                } catch (Exception e) {
                    log.error("智书13Sheet历史合同多线程校验第{}批异常，合同编码={}，错误={}",
                            index + 1, JSON.toJSONString(batchContractNumbers), e.getMessage(), e);
                    for (String contractNumber : batchContractNumbers) {
                        mergedResult.addFailure(contractNumber, null, "批次校验异常：" + e.getMessage());
                    }
                }
            }
        } finally {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(10, TimeUnit.MINUTES)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
                log.warn("智书13Sheet历史合同多线程校验等待线程池结束被中断，错误={}", e.getMessage(), e);
            }
        }

        finishHistoryValidateResult(mergedResult, startTime);
        log.info("智书13Sheet历史合同多线程校验结束，总数={}，成功数={}，失败数={}，成功合同编码={}，失败明细={}，耗时={}ms",
                mergedResult.getTotalCount(), mergedResult.getSuccessCount(), mergedResult.getFailCount(),
                mergedResult.getSuccessContractNumbers(), JSON.toJSONString(mergedResult.getFailures()),
                mergedResult.getElapsedMillis());
        return mergedResult;
    }

    @Override
    public YeCaiContractSyncResultDTO syncYeCaiContracts(YeCaiContractSyncDTO request) {
        long startTime = System.currentTimeMillis();
        YeCaiContractSyncResultDTO result = new YeCaiContractSyncResultDTO();
        List<String> contractNumbers;
        try {
            contractNumbers = resolveYeCaiContractNumbers(request);
        } catch (Exception e) {
            YeCaiContractSyncResultDTO.Item item = buildYeCaiSyncItem(1, "ALL");
            failYeCaiSyncItem(item, e.getMessage());
            result.addItem(item);
            finishYeCaiContractSyncResult(result, startTime);
            return result;
        }
        if (contractNumbers.isEmpty()) {
            YeCaiContractSyncResultDTO.Item item = buildYeCaiSyncItem(1, "ALL");
            failYeCaiSyncItem(item, "合同编码集合和txt文件地址不能同时为空");
            result.addItem(item);
            finishYeCaiContractSyncResult(result, startTime);
            return result;
        }

        int threadCount = Math.min(resolvePositiveInteger(request.getThreadCount(), DEFAULT_MULTI_THREAD_COUNT),
                contractNumbers.size());
        log.info("按智书合同编码同步业财开始，合同数量={}，线程数={}，合同编码={}",
                contractNumbers.size(), threadCount, JSON.toJSONString(contractNumbers));
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        List<Future<YeCaiContractSyncResultDTO.Item>> futures = new ArrayList<>();
        try {
            for (int index = 0; index < contractNumbers.size(); index++) {
                final int itemIndex = index + 1;
                final String contractNumber = contractNumbers.get(index);
                futures.add(executorService.submit(
                        ApiLogTableContext.wrap(() -> syncOneYeCaiContract(itemIndex, contractNumber))));
            }
            for (int index = 0; index < futures.size(); index++) {
                String contractNumber = contractNumbers.get(index);
                try {
                    result.addItem(futures.get(index).get());
                } catch (Exception e) {
                    YeCaiContractSyncResultDTO.Item item = buildYeCaiSyncItem(index + 1, contractNumber);
                    failYeCaiSyncItem(item, "同步业财异常：" + buildExceptionMessage(e));
                    result.addItem(item);
                    log.error("按智书合同编码同步业财任务异常，contractNumber={}，错误={}",
                            contractNumber, e.getMessage(), e);
                }
            }
        } finally {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
                log.warn("按智书合同编码同步业财等待线程池结束被中断，错误={}", e.getMessage(), e);
            }
        }

        finishYeCaiContractSyncResult(result, startTime);
        log.info("按智书合同编码同步业财结束，总数={}，成功数={}，失败数={}，耗时={}ms，明细={}",
                result.getTotalCount(), result.getSuccessCount(), result.getFailCount(),
                result.getElapsedMillis(), JSON.toJSONString(result.getItems()));
        return result;
    }

    @Override
    public HistoryContractSyncResultDTO syncHistoryContracts(Collection<String> contractNumbers) {
        return syncHistoryContracts(contractNumbers, null);
    }

    @Override
    public HistoryContractSyncResultDTO syncHistoryContracts(Collection<String> contractNumbers, String filePath) {
        HistoryContractSyncDTO request = new HistoryContractSyncDTO();
        request.setContractNumbers(contractNumbers);
        request.setFilePath(filePath);
        return syncHistoryContracts(request);
    }

    @Override
    public String exportNotFoundContracts(String contractNumber) {
        return exportNotFoundContracts(Collections.singleton(contractNumber));
    }

    @Override
    public String exportNotFoundContracts(Collection<String> contractNumbers) {
        Set<String> normalizedContractNumbers = normalizeContractNumbers(contractNumbers);
        List<NotFoundContractRecord> notFoundRecords = new ArrayList<>();
        log.info("按合同编码查询智书合同并导出未查询到合同开始，contractNumbers={}", normalizedContractNumbers);
        for (String contractNumber : normalizedContractNumbers) {
            ContractExistsCheckResult checkResult = checkContractExistsByNumber(contractNumber);
            if (!checkResult.isExists()) {
                notFoundRecords.add(new NotFoundContractRecord(contractNumber, checkResult.getReason()));
            }
        }
        Path exportPath = writeNotFoundContractsExcel(notFoundRecords);
        log.info("按合同编码查询智书合同并导出未查询到合同结束，查询数量={}，未查询到数量={}，导出文件={}",
                normalizedContractNumbers.size(), notFoundRecords.size(), exportPath.toAbsolutePath());
        return exportPath.toAbsolutePath().toString();
    }

    @Override
    public ApproveContractToNodeResultDTO approveContractsToNode(ApproveContractToNodeDTO request) {
        long startTime = System.currentTimeMillis();
        ApproveContractToNodeResultDTO result = new ApproveContractToNodeResultDTO();
        ApproveContractToNodeDTO actualRequest = request == null ? new ApproveContractToNodeDTO() : request;
        Set<String> contractNumbers = normalizeContractNumbers(actualRequest.getContractNumbers());
        String targetNodeName = trimToNull(actualRequest.getNodeName());

        log.info("智书合同审核到指定节点开始，contractNumbers={}，targetNodeName={}", contractNumbers, targetNodeName);
        if (contractNumbers.isEmpty()) {
            result.addFailure("ALL", "合同编号集合不能为空");
            finishApproveToNodeResult(result, startTime);
            return result;
        }
        if (targetNodeName == null) {
            for (String contractNumber : contractNumbers) {
                result.addFailure(contractNumber, "目标节点名称不能为空");
            }
            finishApproveToNodeResult(result, startTime);
            return result;
        }

        for (String contractNumber : contractNumbers) {
            try {
                ApproveToNodeItemResult itemResult = approveOneContractToNode(contractNumber, targetNodeName);
                if (itemResult.isSuccess()) {
                    result.addSuccess(contractNumber, itemResult.getContractId(),
                            itemResult.getProcessInstanceId(), itemResult.getCurrentNodeName());
                } else {
                    result.addFailure(contractNumber, itemResult.getReason());
                }
            } catch (Exception e) {
                log.error("智书合同审核到指定节点异常，contractNumber={}，targetNodeName={}，error={}",
                        contractNumber, targetNodeName, e.getMessage(), e);
                result.addFailure(contractNumber, "审核异常：" + e.getMessage());
            }
        }

        finishApproveToNodeResult(result, startTime);
        log.info("智书合同审核到指定节点结束，result={}", JSON.toJSONString(result));
        return result;
    }

    @Override
    public ApproveContractToNodeResultDTO approveContractsToNode(Collection<String> contractNumbers, String nodeName) {
        ApproveContractToNodeDTO request = new ApproveContractToNodeDTO();
        request.setContractNumbers(contractNumbers);
        request.setNodeName(nodeName);
        return approveContractsToNode(request);
    }

    @Override
    public Map<String, ApproveContractToNodeResultDTO> approveContractsToNode(Map<String, List<String>> contractNumbersByNodeName) {
        Map<String, ApproveContractToNodeResultDTO> resultMap = new LinkedHashMap<>();
        if (contractNumbersByNodeName == null || contractNumbersByNodeName.isEmpty()) {
            return resultMap;
        }
        for (Map.Entry<String, List<String>> entry : contractNumbersByNodeName.entrySet()) {
            String nodeName = trimToNull(entry.getKey());
            String resultKey = nodeName == null ? String.valueOf(entry.getKey()) : nodeName;
            List<String> contractNumbers = entry.getValue();
            try {
                resultMap.put(resultKey, approveContractsToNode(contractNumbers, nodeName));
            } catch (Exception e) {
                log.error("智书合同按节点分组审核异常，nodeName={}，contractNumbers={}，error={}",
                        nodeName, contractNumbers, e.getMessage(), e);
                resultMap.put(resultKey, buildApproveToNodeFailureResult(contractNumbers, "审核异常：" + e.getMessage()));
            }
        }
        return resultMap;
    }

    private ApproveContractToNodeResultDTO buildApproveToNodeFailureResult(Collection<String> contractNumbers, String reason) {
        ApproveContractToNodeResultDTO result = new ApproveContractToNodeResultDTO();
        Set<String> normalizedContractNumbers = normalizeContractNumbers(contractNumbers);
        if (normalizedContractNumbers.isEmpty()) {
            result.addFailure("ALL", reason);
        } else {
            for (String contractNumber : normalizedContractNumbers) {
                result.addFailure(contractNumber, reason);
            }
        }
        return result;
    }

    @Override
    public DeleteDraftContractsResultDTO deleteAllDraftContracts() {
        long startTime = System.currentTimeMillis();
        DeleteDraftContractsResultDTO result = new DeleteDraftContractsResultDTO();
        String pageToken = null;
        Set<String> visitedPageTokens = new LinkedHashSet<>();

        log.info("智书草稿合同删除开始");
        while (true) {
            ContractsSearchRequest request = new ContractsSearchRequest();
            request.setPageSize(CONTRACT_SEARCH_PAGE_SIZE);
            request.setPageToken(pageToken);
            ContractsSearchRequest.CombineCondition combineCondition = new ContractsSearchRequest.CombineCondition();
            combineCondition.setContractStatus(0);
            request.setCombineCondition(combineCondition);
            ContractsSearchResponse searchResponse;
            try {
                searchResponse = zhishuContractClient.searchContracts(request);
            } catch (Exception e) {
                log.error("查询智书合同失败，pageToken={}，错误={}", pageToken, e.getMessage(), e);
                result.markFailed("查询智书合同失败：" + e.getMessage());
                break;
            }

            if (searchResponse == null || !searchResponse.isSuccess()) {
                String reason = buildSearchContractsFailureReason(searchResponse);
                log.warn("查询智书合同接口返回失败，pageToken={}，原因={}", pageToken, reason);
                result.markFailed(reason);
                break;
            }

            ContractsSearchResponse.DataInfo data = searchResponse.getData();
            List<ContractQueryResponse> contracts = data == null || data.getItems() == null
                    ? Collections.emptyList() : data.getItems();
            result.addScannedCount(contracts.size());
            List<String> contractNumberList = new ArrayList<>(contracts.size());
            for (ContractQueryResponse contract : contracts) {
                if (isDraftContract(contract)) {
//                    deleteOneDraftContract(contract, result);
                    contractNumberList.add(contract.getContractNumber());
                }
            }
            log.info("需要同步的合同编码为：{}", JSON.toJSONString(contractNumberList));
            String nextPageToken = resolveNextPageToken(data);
            if (nextPageToken == null) {
                break;
            }
            if (visitedPageTokens.contains(nextPageToken)) {
                String reason = "查询智书合同分页标识重复，已停止处理：" + nextPageToken;
                log.warn(reason);
                result.markFailed(reason);
                break;
            }
            visitedPageTokens.add(nextPageToken);
            pageToken = nextPageToken;
        }

        result.setElapsedMillis(System.currentTimeMillis() - startTime);
        result.refreshCounts();
        log.info("智书草稿合同删除结束，结果={}", JSON.toJSONString(result));
        return result;
    }

    private void finishApproveToNodeResult(ApproveContractToNodeResultDTO result, long startTime) {
        result.setElapsedMillis(System.currentTimeMillis() - startTime);
        result.refreshTotalCount();
    }

    private void deleteOneDraftContract(ContractQueryResponse contract, DeleteDraftContractsResultDTO result) {
        String contractId = contract == null || contract.getContractId() == null
                ? null : String.valueOf(contract.getContractId());
        String contractNumber = contract == null ? null : trimToNull(contract.getContractNumber());
        Integer contractStatusCode = contract == null ? null : contract.getContractStatusCode();
        String contractStatusName = contract == null ? null : trimToNull(contract.getContractStatusName());
        if (trimToNull(contractId) == null) {
            result.addFailure(contractId, contractNumber, contractStatusCode, contractStatusName,
                    "合同ID为空，跳过删除草稿合同");
            return;
        }

        try {
            ResultResponse response = zhishuContractClient.deleteDraftContract(contractId);
            log.info("删除智书草稿合同返回，contractNumber={}，contractId={}，response={}",
                    contractNumber, contractId, JSON.toJSONString(response));
            if (response != null && response.getCode() != null && response.getCode() == 0) {
                result.addSuccess(contractId, contractNumber, contractStatusCode, contractStatusName);
            } else {
                result.addFailure(contractId, contractNumber, contractStatusCode, contractStatusName,
                        buildDeleteDraftContractFailureReason(response));
            }
        } catch (Exception e) {
            log.error("删除智书草稿合同失败，contractNumber={}，contractId={}，错误={}",
                    contractNumber, contractId, e.getMessage(), e);
            result.addFailure(contractId, contractNumber, contractStatusCode, contractStatusName,
                    "删除草稿合同失败：" + e.getMessage());
        }
    }

    private String resolveNextPageToken(ContractsSearchResponse.DataInfo data) {
        if (data == null || !Boolean.TRUE.equals(data.getHasMore())) {
            return null;
        }
        return firstNotBlank(data.getNextPageToken(), data.getPageToken());
    }

    private String buildSearchContractsFailureReason(ContractsSearchResponse searchResponse) {
        if (searchResponse == null) {
            return "查询智书合同返回为空";
        }
        return "查询智书合同失败："
                + firstNotBlank(searchResponse.getMsg(), searchResponse.getMessage(), JSON.toJSONString(searchResponse));
    }

    private String buildDeleteDraftContractFailureReason(ResultResponse response) {
        if (response == null) {
            return "删除草稿合同返回为空";
        }
        return "删除草稿合同失败："
                + firstNotBlank(response.getMsg(), JSON.toJSONString(response));
    }

    private ApproveToNodeItemResult approveOneContractToNode(String contractNumber, String targetNodeName) {
        ContractQueryResponse contract = findSingleContractByNumber(contractNumber);
        if (contract == null) {
            return ApproveToNodeItemResult.fail("未查询到合同编号对应的智书合同");
        }

        String contractId = contract.getContractId() == null ? null : String.valueOf(contract.getContractId());
        if (trimToNull(contractId) == null) {
            return ApproveToNodeItemResult.fail("合同ID为空，无法提交或查询审批流程");
        }

        String processInstanceId = trimToNull(contract.getProcessInstanceId());
        if (processInstanceId == null) {
            if (!isDraftContract(contract)) {
                return ApproveToNodeItemResult.fail("合同非草稿且流程实例ID为空，无法自动审核");
            }
            SubmitContractResponse submitResponse = zhishuContractClient.submitContract(contractId);
            log.info("提交智书草稿合同返回，contractNumber={}，contractId={}，response={}",
                    contractNumber, contractId, JSON.toJSONString(submitResponse));
            if (submitResponse == null || !submitResponse.isSuccess()
                    || submitResponse.getData() == null
                    || trimToNull(submitResponse.getData().getProcessInstanceId()) == null) {
                return ApproveToNodeItemResult.fail("提交合同失败：" + JSON.toJSONString(submitResponse));
            }
            processInstanceId = trimToNull(submitResponse.getData().getProcessInstanceId());
        }

        return advanceApprovalToNode(contractNumber, contractId, processInstanceId, targetNodeName);
    }

    private ContractQueryResponse findSingleContractByNumber(String contractNumber) {
        ContractsSearchResponse searchResponse = searchContractsByNumber(contractNumber);
        log.info("按合同编号查询智书合同返回，contractNumber={}，response={}",
                contractNumber, JSON.toJSONString(searchResponse));
        if (searchResponse == null || !searchResponse.isSuccess()
                || searchResponse.getData() == null
                || searchResponse.getData().getItems() == null) {
            return null;
        }

        List<ContractQueryResponse> exactMatches = new ArrayList<>();
        for (ContractQueryResponse item : searchResponse.getData().getItems()) {
            if (item != null && contractNumber.equals(trimToNull(item.getContractNumber()))) {
                exactMatches.add(item);
            }
        }
        if (exactMatches.size() != 1) {
            if (exactMatches.isEmpty()) {
                return null;
            }
            throw new IllegalStateException("合同编号匹配到多条智书合同：" + contractNumber);
        }
        return exactMatches.get(0);
    }

    private ContractExistsCheckResult checkContractExistsByNumber(String contractNumber) {
        try {
            ContractsSearchResponse searchResponse = searchContractsByNumber(contractNumber);
            log.info("按合同编号检查智书合同是否存在返回，contractNumber={}，response={}",
                    contractNumber, JSON.toJSONString(searchResponse));
            if (searchResponse == null) {
                return ContractExistsCheckResult.notFound("查询接口返回为空");
            }
            if (!searchResponse.isSuccess()) {
                return ContractExistsCheckResult.notFound("查询接口返回失败："
                        + firstNotBlank(searchResponse.getMsg(), searchResponse.getMessage(),
                        JSON.toJSONString(searchResponse)));
            }
            if (searchResponse.getData() == null || searchResponse.getData().getItems() == null
                    || searchResponse.getData().getItems().isEmpty()) {
                return ContractExistsCheckResult.notFound("未查询到合同");
            }
            for (ContractQueryResponse item : searchResponse.getData().getItems()) {
                if (item != null && contractNumber.equals(trimToNull(item.getContractNumber()))) {
                    return ContractExistsCheckResult.exists();
                }
            }
            return ContractExistsCheckResult.notFound("未查询到合同");
        } catch (Exception e) {
            log.warn("按合同编号检查智书合同是否存在异常，contractNumber={}，错误={}",
                    contractNumber, e.getMessage(), e);
            return ContractExistsCheckResult.notFound("查询异常：" + e.getMessage());
        }
    }

    private ContractsSearchResponse searchContractsByNumber(String contractNumber) {
        ContractsSearchRequest searchRequest = new ContractsSearchRequest();
        searchRequest.setContractNumber(contractNumber);
        searchRequest.setPageSize(100);
        ContractsSearchRequest.CombineCondition combineCondition = new ContractsSearchRequest.CombineCondition();
        combineCondition.setContractNumber(contractNumber);
        searchRequest.setCombineCondition(combineCondition);
        return zhishuContractClient.searchContracts(searchRequest);
    }

    private Path writeNotFoundContractsExcel(List<NotFoundContractRecord> records) {
        try {
            Path exportDir = Paths.get(CONTRACT_NOT_FOUND_EXPORT_DIR);
            Files.createDirectories(exportDir);
            String timestamp = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date());
            Path exportPath = exportDir.resolve("zhishu_contract_not_found_" + timestamp + ".xlsx");
            try (Workbook workbook = new XSSFWorkbook();
                 OutputStream outputStream = Files.newOutputStream(exportPath)) {
                Sheet sheet = workbook.createSheet("未查询到合同");
                Row header = sheet.createRow(0);
                header.createCell(0).setCellValue("合同编码");
                header.createCell(1).setCellValue("原因");
                header.createCell(2).setCellValue("查询时间");
                String queryTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                for (int index = 0; index < records.size(); index++) {
                    NotFoundContractRecord record = records.get(index);
                    Row row = sheet.createRow(index + 1);
                    row.createCell(0).setCellValue(record.getContractNumber());
                    row.createCell(1).setCellValue(record.getReason());
                    row.createCell(2).setCellValue(queryTime);
                }
                for (int columnIndex = 0; columnIndex < 3; columnIndex++) {
                    sheet.autoSizeColumn(columnIndex);
                }
                workbook.write(outputStream);
            }
            return exportPath;
        } catch (Exception e) {
            throw new RuntimeException("导出未查询到合同Excel失败：" + e.getMessage(), e);
        }
    }

    private boolean isDraftContract(ContractQueryResponse contract) {
        if (contract == null) {
            return false;
        }
        if (contract.getContractStatusCode() != null
                && CONTRACT_DRAFT_STATUS_CODE == contract.getContractStatusCode()) {
            return true;
        }
        String statusName = trimToNull(contract.getContractStatusName());
        return statusName != null && CONTRACT_DRAFT_STATUS_NAME.equalsIgnoreCase(statusName);
    }

    private ApproveToNodeItemResult advanceApprovalToNode(String contractNumber,
                                                          String contractId,
                                                          String processInstanceId,
                                                          String targetNodeName) {
        int approvedSteps = 0;
        while (true) {
            ApprovalQueryResponse approvalQueryResponse = queryApproval(processInstanceId);
            if (approvalQueryResponse == null || approvalQueryResponse.getCode() == null
                    || approvalQueryResponse.getCode() != 0
                    || approvalQueryResponse.getData() == null
                    || approvalQueryResponse.getData().getProcessInstance() == null) {
                return ApproveToNodeItemResult.fail("查询审批实例失败：" + JSON.toJSONString(approvalQueryResponse));
            }

            ApprovalQueryResponse.TaskInstance currentTask = findCurrentTask(
                    approvalQueryResponse.getData().getProcessInstance().getTaskInstanceList());
            if (currentTask == null) {
                return ApproveToNodeItemResult.fail("流程已结束或未找到当前待办节点，未到达目标节点：" + targetNodeName);
            }

            String currentNodeName = getNodeName(currentTask.getNodeName());
            log.info("智书合同审核到指定节点当前任务，contractNumber={}，processInstanceId={}，nodeName={}，approvedSteps={}",
                    contractNumber, processInstanceId, currentNodeName, approvedSteps);
            if (targetNodeName.equals(currentNodeName)) {
                return ApproveToNodeItemResult.success(contractId, processInstanceId, currentNodeName);
            }

            if (approvedSteps >= APPROVE_TO_NODE_MAX_STEPS) {
                return ApproveToNodeItemResult.fail("超过最大自动审批节点数，当前节点：" + currentNodeName);
            }

            String assigneeId = firstAssigneeId(currentTask);
            if (assigneeId == null) {
                return ApproveToNodeItemResult.fail("当前节点审批人为空，节点：" + currentNodeName);
            }

            ApprovalContractRequest approvalRequest = new ApprovalContractRequest();
            approvalRequest.setTaskInstanceId(currentTask.getTaskInstanceId());
            approvalRequest.setAssigneeId(assigneeId);
            approvalRequest.setCommandType(APPROVE_COMMAND_TYPE);
            approvalRequest.setTaskComment(APPROVE_TO_NODE_COMMENT);
            ApprovalResponse approvalResponse = zhishuContractClient.approvalContract(approvalRequest, processInstanceId);
            log.info("智书合同自动审批节点返回，contractNumber={}，processInstanceId={}，nodeName={}，response={}",
                    contractNumber, processInstanceId, currentNodeName, JSON.toJSONString(approvalResponse));
            if (approvalResponse == null || approvalResponse.getCode() == null || approvalResponse.getCode() != 0) {
                return ApproveToNodeItemResult.fail("审批当前节点失败：" + JSON.toJSONString(approvalResponse));
            }
            approvedSteps++;
        }
    }

    private ApprovalQueryResponse queryApproval(String processInstanceId) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("user_id_type", "user_id");
        return zhishuContractClient.getApprovalContract(processInstanceId, params);
    }

    private ApprovalQueryResponse.TaskInstance findCurrentTask(List<ApprovalQueryResponse.TaskInstance> taskInstances) {
        if (taskInstances == null || taskInstances.isEmpty()) {
            return null;
        }
        for (ApprovalQueryResponse.TaskInstance taskInstance : taskInstances) {
            if (taskInstance != null && trimToNull(taskInstance.getEndTime()) == null) {
                return taskInstance;
            }
        }
        return null;
    }

    private String getNodeName(ApprovalQueryResponse.MultiLanguage nodeName) {
        if (nodeName == null) {
            return null;
        }
        return trimToNull(nodeName.getZh());
    }

    private String firstAssigneeId(ApprovalQueryResponse.TaskInstance taskInstance) {
        if (taskInstance == null || taskInstance.getAssigneeIds() == null
                || taskInstance.getAssigneeIds().isEmpty()) {
            return null;
        }
        return trimToNull(taskInstance.getAssigneeIds().get(0));
    }

    @Override
    public HistoryContractSyncResultDTO syncHistoryAntiBriberyContracts(HistoryContractSyncDTO request) {
        long startTime = System.currentTimeMillis();
        HistoryContractSyncDTO actualRequest = request == null ? new HistoryContractSyncDTO() : request;
        Set<String> contractNumberFilter = resolveContractNumberFilter(actualRequest);
        String excelPath = resolveAntiBriberyExcelPath(actualRequest.getResolvedFilePath());
        HistoryContractSyncResultDTO result = new HistoryContractSyncResultDTO();
        SyncContext uploadContext = buildSyncContext(actualRequest);
        Set<String> matchedContractNumbers = new LinkedHashSet<>();

        log.info("智书反商业贿赂协议历史合同同步开始，excelPath={}，过滤合同编码={}",
                excelPath, contractNumberFilter);
        try {
            uploadContext.setCounterPartyCodeLookup(loadCounterPartyCodeLookup());
            List<Map<String, Object>> rows = readAntiBriberySheetRows(excelPath, ANTI_BRIBERY_DATA_SHEET_INDEX);
            log.info("智书反商业贿赂协议历史合同Excel加载完成，rowCount={}", rows.size());
            for (Map<String, Object> row : rows) {
                String contractNumber = resolveAntiBriberyContractNumber(row);
                if (contractNumber == null) {
                    if (contractNumberFilter.isEmpty()) {
                        result.addFailure("UNKNOWN", ANTI_BRIBERY_FLOW_TYPE, "合同编码为空");
                    }
                    continue;
                }
                if (!contractNumberFilter.isEmpty() && !contractNumberFilter.contains(contractNumber)) {
                    continue;
                }
                matchedContractNumbers.add(contractNumber);
                OneContractSyncResult syncResult = syncOneAntiBriberyContract(row, uploadContext);
                if (syncResult.isSuccess()) {
                    result.addSuccess(contractNumber);
                } else {
                    result.addFailure(contractNumber, ANTI_BRIBERY_FLOW_TYPE, syncResult.getReason());
                }
            }
            for (String contractNumber : contractNumberFilter) {
                if (!matchedContractNumbers.contains(contractNumber)) {
                    result.addFailure(contractNumber, ANTI_BRIBERY_FLOW_TYPE, "Excel中未找到合同编码");
                }
            }
        } catch (Exception e) {
            log.error("智书反商业贿赂协议历史合同同步整体异常，excelPath={}，错误={}",
                    excelPath, e.getMessage(), e);
            result.addFailure("ALL", ANTI_BRIBERY_FLOW_TYPE, "同步整体异常：" + e.getMessage());
        }

        long elapsedMillis = System.currentTimeMillis() - startTime;
        result.setElapsedMillis(elapsedMillis);
        result.refreshTotalCount();
        log.info("智书反商业贿赂协议历史合同同步结束，总数={}，成功数={}，失败数={}，成功合同编码={}，失败明细={}，耗时={}ms",
                result.getTotalCount(), result.getSuccessCount(), result.getFailCount(),
                result.getSuccessContractNumbers(), JSON.toJSONString(result.getFailures()), elapsedMillis);
        return result;
    }

    private OneContractSyncResult syncOneAntiBriberyContract(Map<String, Object> row,
                                                             SyncContext uploadContext) {
        long startTime = System.currentTimeMillis();
        String contractNumber = resolveAntiBriberyContractNumber(row);
        try {
            ZhishuCreateContractRequest createRequest = buildAntiBriberyCreateContractRequest(row, uploadContext);
            String validationFailure = validateAntiBriberyCreateRequest(createRequest);
            if (validationFailure != null) {
                return OneContractSyncResult.fail(validationFailure);
            }

            ContractFileIds contractFileIds = uploadAntiBriberyContractFiles(contractNumber, uploadContext);
            if (!contractFileIds.isSuccess()) {
                return OneContractSyncResult.fail(contractFileIds.getFailureReason());
            }
            applyFileFields(contractFileIds, createRequest);

            log.info("智书反商业贿赂协议历史合同创建开始，contract_number={}，request={}",
                    contractNumber, JSON.toJSONString(createRequest));
            ZhishuCreateContractResponse createResponse = zhishuContractClient.createContractV2(createRequest);
            log.info("智书反商业贿赂协议历史合同创建返回，contract_number={}，response={}",
                    contractNumber, JSON.toJSONString(createResponse));
            if (createResponse == null || !createResponse.isSuccess()) {
                return OneContractSyncResult.fail("创建合同失败，返回=" + JSON.toJSONString(createResponse));
            }
            log.info("智书反商业贿赂协议历史合同创建成功，contract_number={}，智书合同ID={}，智书合同编码={}，耗时={}ms",
                    contractNumber, getCreatedContractId(createResponse), getCreatedContractNumber(createResponse),
                    System.currentTimeMillis() - startTime);
            return OneContractSyncResult.success();
        } catch (Exception e) {
            log.error("智书反商业贿赂协议历史合同创建异常，contract_number={}，错误={}",
                    contractNumber, e.getMessage(), e);
            return OneContractSyncResult.fail("创建异常：" + e.getMessage());
        }
    }

    private String resolveAntiBriberyContractNumber(Map<String, Object> row) {
        return trimToNull(toStringValue(getAntiBriberyRowValue(row, ANTI_BRIBERY_HEADER_CONTRACT_NUMBER)));
    }

    private ZhishuCreateContractRequest buildAntiBriberyCreateContractRequest(Map<String, Object> row,
                                                                              SyncContext context) {
        ZhishuCreateContractRequest request = new ZhishuCreateContractRequest();
        request.setContractNumber(resolveAntiBriberyContractNumber(row));
        request.setContractName(trimToNull(toStringValue(
                getAntiBriberyRowValue(row, ANTI_BRIBERY_HEADER_CONTRACT_NAME))));
//        request.setSubmittedTime(toAntiBriberyTimestampMillisString(
//                getAntiBriberyRowValue(row, ANTI_BRIBERY_HEADER_SUBMITTED_TIME)));
        request.setCreateUserId(trimToNull(toStringValue(
                getAntiBriberyRowValue(row, ANTI_BRIBERY_HEADER_CREATE_USER_ID))));
        request.setRemark(trimToNull(toStringValue(
                getAntiBriberyRowValue(row, ANTI_BRIBERY_HEADER_REMARK))));
//        request.setCreateUserId(yeCaiDataConfig.getUserId());
        request.setCounterPartyList(buildAntiBriberyCounterPartyList(
                getAntiBriberyRowValue(row, ANTI_BRIBERY_HEADER_COUNTER_PARTY_ID), context));
        request.setOurPartyList(buildAntiBriberyOurPartyList(
                getAntiBriberyRowValue(row, ANTI_BRIBERY_HEADER_OUR_PARTY_ID)));
        ContractFormCreatResponse contractFormCreatResponse = new ContractFormCreatResponse();
        addContractFormAttribute(contractFormCreatResponse,ZhishuAndYecaiFiledEnum.REQUISITION_DATE.getZhishuFiled(),FormAttributeTypeEnum.DATE.getCode(),toDateString(getAntiBriberyRowValue(row, ANTI_BRIBERY_HEADER_SUBMITTED_TIME)));//签署日期
        addContractFormAttribute(contractFormCreatResponse,ZhishuAndYecaiFiledEnum.PRINT_MODE.getZhishuFiled(),FormAttributeTypeEnum.DROPDOWN_RADIO.getCode(),"cmnyeghfi004e3b71kz8iaz5m");//签署日期
        request.setForm(JSON.toJSONString(contractFormCreatResponse.getForm()));
        applyAntiBriberyDefaultValues(request, context);
        return request;
    }

    private List<ZhishuCreateContractRequest.CounterPartyInfo> buildAntiBriberyCounterPartyList(Object value,
                                                                                                SyncContext context) {
        List<ZhishuCreateContractRequest.CounterPartyInfo> result = new ArrayList<>();
        for (String partyCode : resolveCounterPartyCodes(splitMultiValue(value), context)) {
            ZhishuCreateContractRequest.CounterPartyInfo counterPartyInfo =
                    new ZhishuCreateContractRequest.CounterPartyInfo();
            counterPartyInfo.setCounterPartyCode(partyCode);
            counterPartyInfo.setCounterPartySignInfoResource(buildDisabledSignInfoResource());
            result.add(counterPartyInfo);
        }
        return result;
    }

    private List<ZhishuCreateContractRequest.OurPartyInfo> buildAntiBriberyOurPartyList(Object value) {
        List<ZhishuCreateContractRequest.OurPartyInfo> result = new ArrayList<>();
        for (String partyCode : splitMultiValue(value)) {
            ZhishuCreateContractRequest.OurPartyInfo ourPartyInfo =
                    new ZhishuCreateContractRequest.OurPartyInfo();
            ourPartyInfo.setOurPartyCode(partyCode);
            ourPartyInfo.setOurPartySignInfoResource(buildDisabledSignInfoResource());
            result.add(ourPartyInfo);
        }
        return result;
    }

    private void applyAntiBriberyDefaultValues(ZhishuCreateContractRequest request, SyncContext context) {
        request.setContractCategoryAbbreviation(DEFAULT_CONTRACT_CATEGORY_ABBREVIATION);
        if (trimToNull(request.getCreateUserId()) == null && yeCaiDataConfig != null) {
            request.setCreateUserId(trimToNull(yeCaiDataConfig.getUserId()));
        }
        if (trimToNull(request.getSourceId()) == null) {
            request.setSourceId(request.getContractNumber());
        }
        if (trimToNull(request.getContractStatusCode()) == null) {
//            request.setContractStatusCode("0");
            request.setContractStatusCode(context.getContractStatusCode());
        }
        if (request.getBusinessTypeCode() == null) {
            request.setBusinessTypeCode(0);
        }
        if (request.getPayTypeCode() == null) {
            request.setPayTypeCode(4);
        }
        if (request.getPropertyTypeCode() == null) {
            request.setPropertyTypeCode(1);
        }
        if (request.getFixedValidityCode() == null) {
            request.setFixedValidityCode(0);
        }
        if (request.getAmount() == null) {
            request.setAmount(BigDecimal.ZERO);
        }
        if(request.getSignTypeCode()==null){
            request.setSignTypeCode(2);
        }
        if (trimToNull(request.getCurrencyCode()) == null) {
            request.setCurrencyCode(DEFAULT_CURRENCY_CODE);
        }
    }

    private String validateAntiBriberyCreateRequest(ZhishuCreateContractRequest request) {
        if (trimToNull(request.getContractNumber()) == null) {
            return "合同编码为空";
        }
        if (trimToNull(request.getCreateUserId()) == null) {
            return "create_user_id为空且配置userId为空";
        }
        if (request.getOurPartyList() == null || request.getOurPartyList().isEmpty()) {
            return "我方主体为空";
        }
        if (request.getCounterPartyList() == null || request.getCounterPartyList().isEmpty()) {
            return "对方主体为空";
        }
        return null;
    }

    private List<Map<String, Object>> readAntiBriberySheetRows(String filePath, int sheetIndex) {
        try (InputStream inputStream = Files.newInputStream(Paths.get(filePath));
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            if (sheetIndex < 0 || sheetIndex >= workbook.getNumberOfSheets()) {
                throw new IllegalArgumentException("sheet index out of range: " + sheetIndex);
            }
            Sheet sheet = workbook.getSheetAt(sheetIndex);
            DataFormatter dataFormatter = new DataFormatter();
            FormulaEvaluator formulaEvaluator = workbook.getCreationHelper().createFormulaEvaluator();
            List<HeaderColumn> headers = readAntiBriberyHeaders(sheet, dataFormatter, formulaEvaluator);
            List<Map<String, Object>> rows = new ArrayList<>();
            for (int rowIndex = DATA_START_ROW_INDEX; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }
                Map<String, Object> values = new LinkedHashMap<>();
                boolean hasValue = false;
                for (HeaderColumn header : headers) {
                    Object value = getAntiBriberyCellValue(row.getCell(header.getColumnIndex()),
                            dataFormatter, formulaEvaluator);
                    if (!isBlankValue(value)) {
                        hasValue = true;
                    }
                    putAntiBriberyRowValue(values, header.getHeader(), value);
                }
                if (hasValue) {
                    rows.add(values);
                }
            }
            return rows;
        } catch (Exception e) {
            throw new RuntimeException("read anti bribery excel failed, filePath=" + filePath
                    + ", sheetIndex=" + sheetIndex + ", error=" + e.getMessage(), e);
        }
    }

    private List<HeaderColumn> readAntiBriberyHeaders(Sheet sheet,
                                                      DataFormatter dataFormatter,
                                                      FormulaEvaluator formulaEvaluator) {
        Row headerRow = sheet.getRow(HEADER_ROW_INDEX);
        if (headerRow == null) {
            return Collections.emptyList();
        }
        List<HeaderColumn> headers = new ArrayList<>();
        for (int columnIndex = headerRow.getFirstCellNum(); columnIndex < headerRow.getLastCellNum(); columnIndex++) {
            String header = normalizeRawExcelHeader(dataFormatter.formatCellValue(
                    headerRow.getCell(columnIndex), formulaEvaluator));
            if (header != null) {
                headers.add(new HeaderColumn(columnIndex, header));
            }
        }
        return headers;
    }

    private Object getAntiBriberyCellValue(Cell cell,
                                           DataFormatter dataFormatter,
                                           FormulaEvaluator formulaEvaluator) {
        if (cell == null) {
            return null;
        }
        if (isNumericDateCell(cell)) {
            return cell.getDateCellValue();
        }
        return trimToNull(dataFormatter.formatCellValue(cell, formulaEvaluator));
    }

    private boolean isNumericDateCell(Cell cell) {
        CellType cellType = cell.getCellType();
        if (cellType != CellType.NUMERIC
                && !(cellType == CellType.FORMULA
                && cell.getCachedFormulaResultType() == CellType.NUMERIC)) {
            return false;
        }
        return DateUtil.isCellDateFormatted(cell);
    }

    private void putAntiBriberyRowValue(Map<String, Object> row, String rawHeader, Object value) {
        String header = normalizeRawExcelHeader(rawHeader);
        if (header == null) {
            return;
        }
        row.put(header, value);
        String normalizedHeader = normalizeAntiBriberyHeader(header);
        if (normalizedHeader != null && !row.containsKey(normalizedHeader)) {
            row.put(normalizedHeader, value);
        }
    }

    private Object getAntiBriberyRowValue(Map<String, Object> row, String sourceHeader) {
        String rawHeader = normalizeRawExcelHeader(sourceHeader);
        if (rawHeader == null) {
            return null;
        }
        if (row.containsKey(rawHeader)) {
            return row.get(rawHeader);
        }
        String normalizedHeader = normalizeAntiBriberyHeader(rawHeader);
        if (normalizedHeader != null && row.containsKey(normalizedHeader)) {
            return row.get(normalizedHeader);
        }
        return null;
    }

    private ContractFileIds uploadAntiBriberyContractFiles(String contractNumber, SyncContext context) {
        AntiBriberyContractFiles fallbackFiles = findAntiBriberyContractFiles(contractNumber, context);
        Path fallbackDirectory = resolveFallbackContractDirectory(contractNumber, context);
        if (fallbackFiles.getMainFiles().isEmpty()) {
            return ContractFileIds.fail("合同主文件缺失，主文件目录未找到可用文件："
                    + fallbackFiles.getMainDirectory());
        }
        log.info("智书反商业贿赂协议历史合同使用分类目录文件，contract_number={}，fallbackDir={}，mainCount={}，scanCount={}，attachmentCount={}",
                contractNumber, fallbackDirectory, fallbackFiles.getMainFiles().size(),
                fallbackFiles.getScanFiles().size(), fallbackFiles.getAttachmentFiles().size());
        ContractFileIds result = new ContractFileIds();
        List<File> attachmentFiles = new ArrayList<>();

        String textFileId = uploadOneContractFile(contractNumber, fallbackFiles.getMainFiles().get(0),
                FILE_TYPE_TEXT, context);
        if (textFileId == null) {
            return ContractFileIds.fail("合同主文件上传失败："
                    + fallbackFiles.getMainFiles().get(0).getAbsolutePath());
        }
        result.setTextFileId(textFileId);

        if (fallbackFiles.getMainFiles().size() > 1) {
            log.warn("智书反商业贿赂协议历史合同主文件目录存在多个文件，第一个作为正文，其余作为附件，contract_number={}，mainDir={}，fileCount={}",
                    contractNumber, fallbackFiles.getMainDirectory(), fallbackFiles.getMainFiles().size());
            attachmentFiles.addAll(fallbackFiles.getMainFiles().subList(1, fallbackFiles.getMainFiles().size()));
        }

        if (fallbackFiles.getScanFiles().isEmpty()) {
            log.warn("智书反商业贿赂协议历史合同归档扫描件目录未找到可用文件，将不设置scan_file_id，contract_number={}，scanDir={}",
                    contractNumber, fallbackFiles.getScanDirectory());
        } else {
            String scanFileId = uploadOneContractFile(contractNumber, fallbackFiles.getScanFiles().get(0),
                    FILE_TYPE_SCAN, context);
            if (scanFileId == null) {
                return ContractFileIds.fail("归档扫描件上传失败："
                        + fallbackFiles.getScanFiles().get(0).getAbsolutePath());
            }
            result.setScanFileId(scanFileId);
            if (fallbackFiles.getScanFiles().size() > 1) {
                log.warn("智书反商业贿赂协议历史合同归档扫描件目录存在多个文件，第一个作为扫描件，其余作为附件，contract_number={}，scanDir={}，fileCount={}",
                        contractNumber, fallbackFiles.getScanDirectory(), fallbackFiles.getScanFiles().size());
                attachmentFiles.addAll(fallbackFiles.getScanFiles().subList(1, fallbackFiles.getScanFiles().size()));
            }
        }

        attachmentFiles.addAll(fallbackFiles.getAttachmentFiles());
        Collections.sort(attachmentFiles, (first, second) -> toFallbackSortKey(fallbackDirectory, first)
                .compareTo(toFallbackSortKey(fallbackDirectory, second)));
        for (File attachmentFile : attachmentFiles) {
            String attachmentFileId = uploadOneContractFile(contractNumber, attachmentFile,
                    FILE_TYPE_ATTACHMENT, context);
            if (attachmentFileId == null) {
                return ContractFileIds.fail("其他附件上传失败：" + attachmentFile.getAbsolutePath());
            }
            result.addAttachmentFileId(attachmentFileId);
        }
        return result;
    }

    private String resolveAntiBriberyExcelPath(String filePath) {
        String excelPath = trimToNull(filePath);
        return excelPath == null ? DEFAULT_ANTI_BRIBERY_EXCEL_PATH : excelPath;
    }

    private ContractIndex scanContractIndex(String excelPath) {
        long startTime = System.currentTimeMillis();
        ContractIndex contractIndex = new ContractIndex();
        ExcelUtils.readAllSheetsByRow(excelPath, HEADER_ROW_INDEX, DATA_START_ROW_INDEX, row -> {
            SheetRole sheetRole = SheetRole.fromSheetIndex(row.getSheetIndex());
            if (sheetRole == null) {
                log.warn("智书13Sheet历史合同Excel存在未识别sheet，sheetName={}，sheetIndex={}，已跳过",
                        row.getSheetName(), row.getSheetIndex());
                return true;
            }
            contractIndex.recordSheetRow(row.getSheetIndex(), row.getSheetName());
            String contractNumber = trimToNull(toStringValue(row.getFirstValue(CONTRACT_NUMBER)));
            if (contractNumber == null) {
                log.warn("智书13Sheet历史合同Excel行缺少合同编码，sheetName={}，sheetIndex={}，rowIndex={}",
                        row.getSheetName(), row.getSheetIndex(), row.getRowIndex());
                return true;
            }
            ContractIndexEntry entry = contractIndex.getOrCreate(sheetRole.getFlowType(), contractNumber);
            if (sheetRole == SheetRole.GENERAL_RELATION) {
                for (Object value : row.getValues(FIELD_RELATION_CONTRACTS)) {
                    entry.addRelationContractNumbers(splitMultiValue(value));
                }
            }
            return true;
        });
        log.info("智书13Sheet历史合同Excel索引扫描完成，sheet行数={}，合同索引数={}，耗时={}ms",
                contractIndex.getSheetRowCounts(), contractIndex.getEntriesByGroupKey().size(),
                System.currentTimeMillis() - startTime);
        return contractIndex;
    }

    private Set<String> resolveTargetGroupKeys(Set<String> contractNumberFilter,
                                               ContractIndex contractIndex,
                                               SyncContext context) {
        Set<String> targetGroupKeys = new LinkedHashSet<>();
        if (contractNumberFilter.isEmpty()) {
            targetGroupKeys.addAll(contractIndex.getEntriesByGroupKey().keySet());
            return targetGroupKeys;
        }
        for (String contractNumber : contractNumberFilter) {
            List<ContractIndexEntry> entries = contractIndex.getEntriesByContractNumber(contractNumber);
            if (entries.isEmpty()) {
                context.recordMissingFailure(contractNumber, "Excel中未找到合同编码");
                continue;
            }
            for (ContractIndexEntry entry : entries) {
                targetGroupKeys.add(entry.getGroupKey());
            }
        }
        return targetGroupKeys;
    }

    private Set<String> expandRelationDependencies(Set<String> initialTargetKeys, ContractIndex contractIndex) {
        Set<String> expandedTargetKeys = new LinkedHashSet<>();
        for (String groupKey : initialTargetKeys) {
            expandOneRelationDependency(groupKey, contractIndex, expandedTargetKeys, new LinkedHashSet<>());
        }
        return expandedTargetKeys;
    }

    private void expandOneRelationDependency(String groupKey,
                                             ContractIndex contractIndex,
                                             Set<String> expandedTargetKeys,
                                             Set<String> visitingKeys) {
        if (expandedTargetKeys.contains(groupKey)) {
            return;
        }
        ContractIndexEntry entry = contractIndex.getEntry(groupKey);
        if (entry == null) {
            return;
        }
        expandedTargetKeys.add(groupKey);
        if (!visitingKeys.add(groupKey)) {
            return;
        }
        for (String relationContractNumber : entry.getRelationContractNumbers()) {
            ContractIndexEntry dependency = resolveDependencyEntry(entry, relationContractNumber, contractIndex);
            if (dependency == null) {
                log.warn("智书13Sheet历史合同关联合同未在Excel中找到，contract_number={}，relation_contract={}",
                        entry.getContractNumber(), relationContractNumber);
                continue;
            }
            expandOneRelationDependency(dependency.getGroupKey(), contractIndex, expandedTargetKeys, visitingKeys);
        }
        visitingKeys.remove(groupKey);
    }

    private ContractIndexEntry resolveDependencyEntry(ContractIndexEntry currentEntry,
                                                      String relationContractNumber,
                                                      ContractIndex contractIndex) {
        List<ContractIndexEntry> entries = contractIndex.getEntriesByContractNumber(relationContractNumber);
        if (entries.isEmpty()) {
            return null;
        }
        ContractIndexEntry selected = null;
        for (ContractIndexEntry entry : entries) {
            if (entry.getFlowType() == currentEntry.getFlowType()) {
                selected = entry;
                break;
            }
        }
        if (selected == null) {
            selected = entries.get(0);
        }
        if (entries.size() > 1) {
            log.warn("智书13Sheet历史合同关联合同编码匹配到多个流程，contract_number={}，relation_contract={}，选用流程={}",
                    currentEntry.getContractNumber(), relationContractNumber, selected.getFlowType().getDesc());
        }
        return selected;
    }

    private ContractSyncOrder sortByDependencies(Set<String> targetGroupKeys, ContractIndex contractIndex) {
        ContractSyncOrder syncOrder = new ContractSyncOrder();
        Set<String> visitedKeys = new LinkedHashSet<>();
        Set<String> visitingKeys = new LinkedHashSet<>();
        List<String> visitPath = new ArrayList<>();
        for (String groupKey : targetGroupKeys) {
            visitForSort(groupKey, contractIndex, syncOrder, visitedKeys, visitingKeys, visitPath);
        }
        return syncOrder;
    }

    private boolean visitForSort(String groupKey,
                                 ContractIndex contractIndex,
                                 ContractSyncOrder syncOrder,
                                 Set<String> visitedKeys,
                                 Set<String> visitingKeys,
                                 List<String> visitPath) {
        if (visitedKeys.contains(groupKey)) {
            return !syncOrder.hasFailure(groupKey);
        }
        if (syncOrder.hasFailure(groupKey)) {
            return false;
        }
        ContractIndexEntry entry = contractIndex.getEntry(groupKey);
        if (entry == null) {
            return false;
        }
        if (visitingKeys.contains(groupKey)) {
            int cycleStartIndex = visitPath.indexOf(groupKey);
            for (int index = Math.max(cycleStartIndex, 0); index < visitPath.size(); index++) {
                syncOrder.addFailure(visitPath.get(index), "关联合同存在循环依赖");
            }
            syncOrder.addFailure(groupKey, "关联合同存在循环依赖");
            return false;
        }
        visitingKeys.add(groupKey);
        visitPath.add(groupKey);
        boolean dependenciesSuccess = true;
        for (String relationContractNumber : entry.getRelationContractNumbers()) {
            ContractIndexEntry dependency = resolveDependencyEntry(entry, relationContractNumber, contractIndex);
            if (dependency == null) {
                log.warn("智书13Sheet历史合同关联合同未在Excel中找到，contract_number={}，relation_contract={}",
                        entry.getContractNumber(), relationContractNumber);
                continue;
            }
            if (!visitForSort(dependency.getGroupKey(), contractIndex, syncOrder, visitedKeys, visitingKeys, visitPath)) {
                syncOrder.addFailure(groupKey, "关联合同同步失败：" + relationContractNumber);
                dependenciesSuccess = false;
                break;
            }
        }
        visitingKeys.remove(groupKey);
        visitPath.remove(visitPath.size() - 1);
        visitedKeys.add(groupKey);
        if (dependenciesSuccess && !syncOrder.hasFailure(groupKey)) {
            syncOrder.addOrderedGroupKey(groupKey);
            return true;
        }
        return false;
    }

    private void recordSortFailures(ContractSyncOrder syncOrder,
                                    ContractIndex contractIndex,
                                    SyncContext context) {
        for (Map.Entry<String, String> failure : syncOrder.getFailureReasonsByGroupKey().entrySet()) {
            ContractIndexEntry entry = contractIndex.getEntry(failure.getKey());
            if (entry != null) {
                context.recordFailure(entry, failure.getValue());
            }
        }
    }

    private void syncInBatches(String excelPath,
                               List<String> orderedGroupKeys,
                               ContractIndex contractIndex,
                               SyncContext context) {
        int total = orderedGroupKeys.size();
        Path excelDirectory = resolveExcelDirectory(excelPath);
        for (int startIndex = 0; startIndex < total; startIndex += batchSize) {
            int endIndex = Math.min(startIndex + batchSize, total);
            List<String> batchGroupKeys = orderedGroupKeys.subList(startIndex, endIndex);
            Set<String> batchGroupKeySet = new LinkedHashSet<>(batchGroupKeys);
            long startTime = System.currentTimeMillis();
            Map<String, ContractGroup> contractGroupMap = loadContractGroups(excelPath, batchGroupKeySet, contractIndex);
            log.info("智书13Sheet历史合同批次数据加载完成，batch={}/{}，batchSize={}，loaded={}，耗时={}ms",
                    startIndex / batchSize + 1, (total + batchSize - 1) / batchSize, batchGroupKeys.size(),
                    contractGroupMap.size(), System.currentTimeMillis() - startTime);
            for (String groupKey : batchGroupKeys) {
                if (context.hasResult(groupKey)) {
                    continue;
                }
                ContractIndexEntry entry = contractIndex.getEntry(groupKey);
                ContractGroup contractGroup = contractGroupMap.get(groupKey);
                if (entry == null) {
                    continue;
                }
                if (contractGroup == null) {
                    context.recordFailure(entry, "批次读取未找到合同数据");
                    continue;
                }
                String failedRelationContractNumber = findFailedDependency(entry, contractIndex, context);
                if (failedRelationContractNumber != null) {
                    context.recordFailure(entry, "关联合同同步失败：" + failedRelationContractNumber);
                    continue;
                }
                OneContractSyncResult syncResult = createOneContract(contractGroup, context, excelDirectory);
                if (syncResult.isSuccess()) {
                    context.recordSuccess(contractGroup);
                } else {
                    context.recordFailure(contractGroup, syncResult.getReason());
                }
            }
            contractGroupMap.clear();
        }
    }

    private HistoryContractValidateResultDTO validateHistoryContracts(HistoryContractSyncDTO request) {
        long startTime = System.currentTimeMillis();
        HistoryContractValidateResultDTO result = new HistoryContractValidateResultDTO();
        HistoryContractSyncDTO actualRequest = request == null ? new HistoryContractSyncDTO() : request;
        try {
            Set<String> contractNumberFilter = resolveContractNumberFilter(actualRequest);
            String excelPath = resolveExcelPath(actualRequest.getResolvedFilePath());
            SyncContext context = buildSyncContext(actualRequest);
            context.setCounterPartyCodeLookup(loadCounterPartyCodeLookup());
            ContractIndex contractIndex = scanContractIndex(excelPath);
            Set<String> initialTargetKeys = resolveTargetGroupKeys(contractNumberFilter, contractIndex, context);
            copyHistorySyncFailures(result, context.getResult());
            Set<String> expandedTargetKeys = expandRelationDependencies(initialTargetKeys, contractIndex);
            ContractSyncOrder syncOrder = sortByDependencies(expandedTargetKeys, contractIndex);
            Set<String> failedGroupKeys = recordValidationSortFailures(syncOrder, contractIndex, result);
            log.info("智书13Sheet历史合同校验待处理合同数={}，依赖展开后合同数={}，可校验合同数={}",
                    initialTargetKeys.size(), expandedTargetKeys.size(), syncOrder.getOrderedGroupKeys().size());
            validateInBatches(excelPath, syncOrder.getOrderedGroupKeys(), contractIndex, context, result, failedGroupKeys);
        } catch (Exception e) {
            log.error("智书13Sheet历史合同校验整体异常，错误={}", e.getMessage(), e);
            result.addFailure("ALL", null, "校验整体异常：" + e.getMessage());
        }
        finishHistoryValidateResult(result, startTime);
        return result;
    }

    private void validateInBatches(String excelPath,
                                   List<String> orderedGroupKeys,
                                   ContractIndex contractIndex,
                                   SyncContext context,
                                   HistoryContractValidateResultDTO result,
                                   Set<String> failedGroupKeys) {
        int total = orderedGroupKeys.size();
        Path excelDirectory = resolveExcelDirectory(excelPath);
        for (int startIndex = 0; startIndex < total; startIndex += batchSize) {
            int endIndex = Math.min(startIndex + batchSize, total);
            List<String> batchGroupKeys = orderedGroupKeys.subList(startIndex, endIndex);
            Set<String> batchGroupKeySet = new LinkedHashSet<>(batchGroupKeys);
            long startTime = System.currentTimeMillis();
            Map<String, ContractGroup> contractGroupMap = loadContractGroups(excelPath, batchGroupKeySet, contractIndex);
            log.info("智书13Sheet历史合同校验批次数据加载完成，batch={}/{}，batchSize={}，loaded={}，耗时={}ms",
                    startIndex / batchSize + 1, (total + batchSize - 1) / batchSize, batchGroupKeys.size(),
                    contractGroupMap.size(), System.currentTimeMillis() - startTime);
            for (String groupKey : batchGroupKeys) {
                ContractIndexEntry entry = contractIndex.getEntry(groupKey);
                ContractGroup contractGroup = contractGroupMap.get(groupKey);
                if (entry == null) {
                    continue;
                }
                if (contractGroup == null) {
                    result.addFailure(entry.getContractNumber(), entry.getFlowType().getDesc(), "批次读取未找到合同数据");
                    failedGroupKeys.add(entry.getGroupKey());
                    continue;
                }
                List<String> errors = new ArrayList<>();
                errors.addAll(findFailedDependencyReasons(entry, contractIndex, failedGroupKeys));
                OneContractValidationResult validationResult = validateOneContract(contractGroup, context, excelDirectory);
                errors.addAll(validationResult.getErrors());
                if (errors.isEmpty()) {
                    result.addSuccess(contractGroup.getContractNumber());
                } else {
                    result.addFailure(contractGroup.getContractNumber(), contractGroup.getFlowType().getDesc(), errors);
                    failedGroupKeys.add(entry.getGroupKey());
                }
            }
            contractGroupMap.clear();
        }
    }

    private Set<String> recordValidationSortFailures(ContractSyncOrder syncOrder,
                                                     ContractIndex contractIndex,
                                                     HistoryContractValidateResultDTO result) {
        Set<String> failedGroupKeys = new LinkedHashSet<>();
        for (Map.Entry<String, String> failure : syncOrder.getFailureReasonsByGroupKey().entrySet()) {
            ContractIndexEntry entry = contractIndex.getEntry(failure.getKey());
            if (entry == null) {
                continue;
            }
            result.addFailure(entry.getContractNumber(), entry.getFlowType().getDesc(),
                    toValidationFailureReason(failure.getValue()));
            failedGroupKeys.add(entry.getGroupKey());
        }
        return failedGroupKeys;
    }

    private List<String> findFailedDependencyReasons(ContractIndexEntry entry,
                                                     ContractIndex contractIndex,
                                                     Set<String> failedGroupKeys) {
        List<String> reasons = new ArrayList<>();
        for (String relationContractNumber : entry.getRelationContractNumbers()) {
            ContractIndexEntry dependency = resolveDependencyEntry(entry, relationContractNumber, contractIndex);
            if (dependency != null && failedGroupKeys.contains(dependency.getGroupKey())) {
                reasons.add("关联合同校验失败：" + relationContractNumber);
            }
        }
        return reasons;
    }

    private String toValidationFailureReason(String reason) {
        return reason == null ? null : reason.replace("关联合同同步失败", "关联合同校验失败");
    }

    private Map<String, ContractGroup> loadContractGroups(String excelPath,
                                                          Set<String> groupKeys,
                                                          ContractIndex contractIndex) {
        Map<String, ContractGroup> contractGroupMap = new LinkedHashMap<>();
        ExcelUtils.readAllSheetsByRow(excelPath, HEADER_ROW_INDEX, DATA_START_ROW_INDEX, row -> {
            SheetRole sheetRole = SheetRole.fromSheetIndex(row.getSheetIndex());
            if (sheetRole == null) {
                return true;
            }
            String contractNumber = trimToNull(toStringValue(row.getFirstValue(CONTRACT_NUMBER)));
            if (contractNumber == null) {
                return true;
            }
            String groupKey = buildGroupKey(sheetRole.getFlowType(), contractNumber);
            if (!groupKeys.contains(groupKey)) {
                return true;
            }
            ContractIndexEntry entry = contractIndex.getEntry(groupKey);
            if (entry == null) {
                return true;
            }
            ContractGroup contractGroup = contractGroupMap.get(groupKey);
            if (contractGroup == null) {
                contractGroup = new ContractGroup(entry.getFlowType(), entry.getContractNumber());
                contractGroupMap.put(groupKey, contractGroup);
            }
            contractGroup.addRow(sheetRole, row.toExcelRowData());
            return true;
        });
        return contractGroupMap;
    }


    private String findFailedDependency(ContractIndexEntry entry,
                                        ContractIndex contractIndex,
                                        SyncContext context) {
        for (String relationContractNumber : entry.getRelationContractNumbers()) {
            ContractIndexEntry dependency = resolveDependencyEntry(entry, relationContractNumber, contractIndex);
            if (dependency != null && context.hasFailure(dependency.getGroupKey())) {
                return relationContractNumber;
            }
        }
        return null;
    }

    private OneContractSyncResult createOneContract(ContractGroup contractGroup,
                                                    SyncContext context,
                                                    Path excelDirectory) {
        long startTime = System.currentTimeMillis();
        String contractNumber = contractGroup.getContractNumber();
        try {
            List<CustomFieldValue> customFieldValues = buildCustomFieldValues(contractGroup);
            String contractCategory = trimToNull(toStringValue(getFirstNonBlankValue(contractGroup, CONTRACT_CATEGORY)));
            if (contractCategory == null) {
                return OneContractSyncResult.fail("contractCategory为空");
            }else{
                contractCategory = ContractCategoryMappingEnum.getCodeByName(contractCategory);
            }
            String contractCategoryAbbreviation =
                    resolveContractCategoryAbbreviation(contractGroup, contractCategory, context);
            if (contractCategoryAbbreviation == null) {
                return OneContractSyncResult.fail("contractCategory未匹配到合同类型，contractCategory=" + contractCategory);
            }
            String createUserId = resolveCreateUserId(contractGroup);
            if (createUserId == null) {
                return OneContractSyncResult.fail("create_user_id为空且配置userId为空");
            }
//            ContractFileIds contractFileIds = uploadContractFiles(contractGroup, excelDirectory, context);
            ContractFileIds contractFileIds = uploadAntiBriberyContractFiles(contractGroup.getContractNumber(), context);
            if (!contractFileIds.isSuccess()) {
                return OneContractSyncResult.fail(contractFileIds.getFailureReason());
            }

            ZhishuCreateContractRequest createRequest =
                    buildCreateContractRequest(contractGroup, createUserId, contractCategoryAbbreviation,
                            customFieldValues, contractFileIds, context);
            // TODO 历史合同导入创建前先查询智书是否已有同编码合同，避免重复创建。
//            contractNumber = "H-DF2026040310-test";
//            ContractExistsCheckResult contractExistsCheckResult = checkContractExistsByNumber(contractNumber);
//            if (contractExistsCheckResult.isExists()) {
//                log.info("智书13Sheet历史合同上传文件模式创建合同跳过，智书已存在同编码合同，contract_number={}，flowType={}",
//                        contractNumber, contractGroup.getFlowType().getDesc());
//                return OneContractSyncResult.success();
//                return OneContractSyncResult.fail("合同已存在，跳过");
//            }
            log.info("智书13Sheet历史合同上传文件模式创建合同开始，contract_number={}，flowType={}，contractCategory={}，contractCategoryAbbreviation={}，textFileId={}",
                    contractNumber, contractGroup.getFlowType().getDesc(), contractCategory,
                    contractCategoryAbbreviation, contractFileIds.getTextFileId());
//            createRequest.setContractNumber("H-DF2026040310-test");
            ZhishuCreateContractResponse createResponse = zhishuContractClient.createContractV2(createRequest);
            if (createResponse == null || !createResponse.isSuccess()) {
                return OneContractSyncResult.fail("创建合同失败，返回=" + JSON.toJSONString(createResponse));
            }
            log.info("智书13Sheet历史合同上传文件模式创建合同成功，contract_number={}，flowType={}，智书合同ID={}，智书合同编码={}，耗时={}ms",
                    contractNumber, contractGroup.getFlowType().getDesc(), getCreatedContractId(createResponse),
                    getCreatedContractNumber(createResponse), System.currentTimeMillis() - startTime);
            return OneContractSyncResult.success();
        } catch (Exception e) {
            log.error("智书13Sheet历史合同创建异常，contract_number={}，flowType={}，错误={}",
                    contractNumber, contractGroup.getFlowType().getDesc(), e.getMessage(), e);
            return OneContractSyncResult.fail("创建异常：" + e.getMessage());
        }
    }

    private OneContractValidationResult validateOneContract(ContractGroup contractGroup,
                                                            SyncContext context,
                                                            Path excelDirectory) {
        List<String> errors = new ArrayList<>();
        String contractNumber = contractGroup.getContractNumber();
        List<CustomFieldValue> customFieldValues = Collections.emptyList();
        try {
            customFieldValues = buildCustomFieldValues(contractGroup);
        } catch (Exception e) {
            log.error("智书13Sheet历史合同自定义字段校验异常，contract_number={}，flowType={}，错误={}",
                    contractNumber, contractGroup.getFlowType().getDesc(), e.getMessage(), e);
            errors.add("自定义字段构建异常：" + buildExceptionMessage(e));
        }

        String contractCategory = trimToNull(toStringValue(getFirstNonBlankValue(contractGroup, CONTRACT_CATEGORY)));
        String contractCategoryAbbreviation = null;
        if (contractCategory == null) {
            errors.add("contractCategory为空");
        } else {
            contractCategory = ContractCategoryMappingEnum.getCodeByName(contractCategory);
            try {
                contractCategoryAbbreviation =
                        resolveContractCategoryAbbreviation(contractGroup, contractCategory, context);
                if (contractCategoryAbbreviation == null) {
                    errors.add("contractCategory未匹配到合同类型，contractCategory=" + contractCategory);
                }
            } catch (Exception e) {
                log.error("智书13Sheet历史合同合同类型校验异常，contract_number={}，flowType={}，contractCategory={}，错误={}",
                        contractNumber, contractGroup.getFlowType().getDesc(), contractCategory, e.getMessage(), e);
                errors.add("contractCategory校验异常：" + buildExceptionMessage(e));
            }
        }

        String createUserId = resolveCreateUserId(contractGroup);
        if (createUserId == null) {
            errors.add("create_user_id为空且配置userId为空");
        }

        ContractFileIds contractFileIds = buildValidationAntiBriberyContractFileIds(contractNumber, context, errors);
        ZhishuCreateContractRequest createRequest = null;
        try {
            createRequest = buildCreateContractRequest(contractGroup, createUserId, contractCategoryAbbreviation,
                    customFieldValues, contractFileIds, context);
        } catch (Exception e) {
            log.error("智书13Sheet历史合同创建请求构建校验异常，contract_number={}，flowType={}，错误={}",
                    contractNumber, contractGroup.getFlowType().getDesc(), e.getMessage(), e);
            errors.add("创建请求构建异常：" + buildExceptionMessage(e));
        }
        validateCreateContractRequest(createRequest, errors);
        return OneContractValidationResult.of(errors);
    }

    private ContractFileIds buildValidationAntiBriberyContractFileIds(String contractNumber,
                                                                      SyncContext context,
                                                                      List<String> errors) {
        ContractFileIds result = new ContractFileIds();
        AntiBriberyContractFiles fallbackFiles = findAntiBriberyContractFiles(contractNumber, context);
        Path fallbackDirectory = resolveFallbackContractDirectory(contractNumber, context);
        if (fallbackFiles.getMainFiles().isEmpty()) {
            errors.add("合同主文件缺失，主文件目录未找到可用文件：" + fallbackFiles.getMainDirectory());
            return result;
        }

        log.info("智书反商业贿赂协议历史合同校验分类目录文件，contract_number={}，fallbackDir={}，mainCount={}，scanCount={}，attachmentCount={}",
                contractNumber, fallbackDirectory, fallbackFiles.getMainFiles().size(),
                fallbackFiles.getScanFiles().size(), fallbackFiles.getAttachmentFiles().size());
        result.setTextFileId("VALIDATION_TEXT_FILE_ID");
        if (!fallbackFiles.getScanFiles().isEmpty()) {
            result.setScanFileId("VALIDATION_SCAN_FILE_ID");
        }
        for (int index = 1; index < fallbackFiles.getMainFiles().size(); index++) {
            result.addAttachmentFileId("VALIDATION_MAIN_ATTACHMENT_FILE_ID_" + index);
        }
        for (int index = 1; index < fallbackFiles.getScanFiles().size(); index++) {
            result.addAttachmentFileId("VALIDATION_SCAN_ATTACHMENT_FILE_ID_" + index);
        }
        for (int index = 0; index < fallbackFiles.getAttachmentFiles().size(); index++) {
            result.addAttachmentFileId("VALIDATION_ATTACHMENT_FILE_ID_" + index);
        }
        return result;
    }

    private void validateCreateContractRequest(ZhishuCreateContractRequest request, List<String> errors) {
        if (request == null) {
            return;
        }
        addBlankError(errors, request.getContractNumber(), "contract_number不能为空");
        addBlankError(errors, request.getContractName(), "contract_name不能为空");
        addBlankError(errors, request.getContractCategoryAbbreviation(), "contract_category_abbreviation不能为空");
        addBlankError(errors, request.getCreateUserId(), "create_user_id不能为空");
        addBlankError(errors, request.getContractStatusCode(), "contract_status_code不能为空");
        if (request.getBusinessTypeCode() == null) {
            addErrorIfAbsent(errors, "business_type_code不能为空");
        }
        if (request.getPayTypeCode() == null) {
            addErrorIfAbsent(errors, "pay_type_code不能为空");
        } else if (request.getPayTypeCode() < 1 || request.getPayTypeCode() > 4) {
            addErrorIfAbsent(errors, "pay_type_code不合法：" + request.getPayTypeCode());
        }
        addBlankError(errors, request.getCurrencyCode(), "currency_code不能为空");
        if (isIncomePayType(request.getPayTypeCode())) {
            addBlankError(errors, request.getInCurrencyCode(), "收入类合同in_currency_code不能为空");
        }
        if (isExpensePayType(request.getPayTypeCode())) {
            addBlankError(errors, request.getOutCurrencyCode(), "支出类合同out_currency_code不能为空");
        }
        if ((Integer.valueOf(1).equals(request.getPayTypeCode()) || Integer.valueOf(2).equals(request.getPayTypeCode()))
                && request.getPropertyTypeCode() == null) {
            addErrorIfAbsent(errors, "收入类或支出类合同property_type_code不能为空");
        }
        if (request.getFixedValidityCode() == null) {
            addErrorIfAbsent(errors, "fixed_validity_code不能为空");
        } else if (!Integer.valueOf(1).equals(request.getFixedValidityCode())
                && !Integer.valueOf(2).equals(request.getFixedValidityCode())) {
            addErrorIfAbsent(errors, "fixed_validity_code不合法：" + request.getFixedValidityCode());
        }
        if (Integer.valueOf(1).equals(request.getFixedValidityCode())) {
            validateRequiredDate(errors, request.getStartDate(), "start_date");
            validateRequiredDate(errors, request.getEndDate(), "end_date");
        }
        validatePartyList(errors, request.getOurPartyList(), true);
        validatePartyList(errors, request.getCounterPartyList(), false);
        addBlankError(errors, request.getTextFileId(), "text_file_id不能为空");
        validatePaymentPlans(errors, request.getPaymentPlanList());
        validateCollectionPlans(errors, request.getCollectionPlanList());
    }

    private boolean isIncomePayType(Integer payTypeCode) {
        return Integer.valueOf(1).equals(payTypeCode) || Integer.valueOf(3).equals(payTypeCode);
    }

    private boolean isExpensePayType(Integer payTypeCode) {
        return Integer.valueOf(2).equals(payTypeCode) || Integer.valueOf(3).equals(payTypeCode);
    }

    private void validatePartyList(List<String> errors,
                                   List<? extends Object> partyList,
                                   boolean ourParty) {
        String listName = ourParty ? "our_party_list" : "counter_party_list";
        if (partyList == null || partyList.isEmpty()) {
            addErrorIfAbsent(errors, listName + "不能为空");
            return;
        }
        for (int index = 0; index < partyList.size(); index++) {
            Object item = partyList.get(index);
            String code;
            if (ourParty) {
                code = item instanceof ZhishuCreateContractRequest.OurPartyInfo
                        ? ((ZhishuCreateContractRequest.OurPartyInfo) item).getOurPartyCode() : null;
            } else {
                code = item instanceof ZhishuCreateContractRequest.CounterPartyInfo
                        ? ((ZhishuCreateContractRequest.CounterPartyInfo) item).getCounterPartyCode() : null;
            }
            addBlankError(errors, code, listName + "[" + index + "]编码不能为空");
        }
    }

    private void validatePaymentPlans(List<String> errors,
                                      List<ZhishuCreateContractRequest.PaymentPlanInfo> paymentPlanList) {
        if (paymentPlanList == null || paymentPlanList.isEmpty()) {
            return;
        }
        for (int index = 0; index < paymentPlanList.size(); index++) {
            ZhishuCreateContractRequest.PaymentPlanInfo plan = paymentPlanList.get(index);
            validateRequiredDate(errors, plan.getPaymentDate(), "payment_plan_list[" + index + "].payment_date");
            if (plan.getPaymentAmount() == null) {
                addErrorIfAbsent(errors, "payment_plan_list[" + index + "].payment_amount不能为空");
            }
            addBlankError(errors, plan.getCurrencyCode(), "payment_plan_list[" + index + "].currency_code不能为空");
            if (plan.getPaymentCounterParty() == null
                    || trimToNull(plan.getPaymentCounterParty().getCounterPartyCode()) == null) {
                addErrorIfAbsent(errors, "payment_plan_list[" + index + "].payment_counter_party不能为空");
            }
        }
    }

    private void validateCollectionPlans(List<String> errors,
                                         List<ZhishuCreateContractRequest.CollectionPlanInfo> collectionPlanList) {
        if (collectionPlanList == null || collectionPlanList.isEmpty()) {
            return;
        }
        for (int index = 0; index < collectionPlanList.size(); index++) {
            ZhishuCreateContractRequest.CollectionPlanInfo plan = collectionPlanList.get(index);
            validateRequiredDate(errors, plan.getCollectionDate(), "collection_plan_list[" + index + "].collection_date");
            if (plan.getCollectionAmount() == null) {
                addErrorIfAbsent(errors, "collection_plan_list[" + index + "].collection_amount不能为空");
            }
            addBlankError(errors, plan.getCurrencyCode(), "collection_plan_list[" + index + "].currency_code不能为空");
            if (plan.getCollectionCounterParty() == null
                    || trimToNull(plan.getCollectionCounterParty().getCounterPartyCode()) == null) {
                addErrorIfAbsent(errors, "collection_plan_list[" + index + "].collection_counter_party不能为空");
            }
        }
    }

    private void validateRequiredDate(List<String> errors, String date, String fieldName) {
        if (trimToNull(date) == null) {
            addErrorIfAbsent(errors, fieldName + "不能为空");
            return;
        }
        if (!isValidDate(date)) {
            addErrorIfAbsent(errors, fieldName + "日期格式不合法：" + date);
        }
    }

    private boolean isValidDate(String date) {
        String value = trimToNull(date);
        if (value == null) {
            return false;
        }
        for (String pattern : Arrays.asList("yyyy-MM-dd", "yyyy/M/d", "yyyy/MM/dd")) {
            try {
                SimpleDateFormat format = new SimpleDateFormat(pattern);
                format.setLenient(false);
                format.parse(value);
                return true;
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    private void addBlankError(List<String> errors, String value, String error) {
        if (trimToNull(value) == null) {
            addErrorIfAbsent(errors, error);
        }
    }

    private void addErrorIfAbsent(List<String> errors, String error) {
        if (error != null && !errors.contains(error)) {
            errors.add(error);
        }
    }

    private ZhishuCreateContractRequest buildCreateContractRequest(ContractGroup contractGroup,
                                                                   String createUserId,
                                                                   String contractCategoryAbbreviation,
                                                                   List<CustomFieldValue> customFieldValues,
                                                                   ContractFileIds contractFileIds,
                                                                   SyncContext context) {
        ZhishuCreateContractRequest request = new ZhishuCreateContractRequest();
        applyScalarFields(contractGroup, request);
        request.setCreateUserId(createUserId);
//        request.setCreateUserId(yeCaiDataConfig.getUserId());
        request.setContractCategoryAbbreviation(contractCategoryAbbreviation);
        applyDefaultValues(contractGroup, request, context);
        applyCustomFormFields(request, customFieldValues);
        applyOrderMemberFields(contractGroup, request);
        applyPartyFields(contractGroup, request, context);
        applySignTypeFields(contractGroup, request);
        applyRelationFields(contractGroup, request);
        applyPlanFields(contractGroup, request, context);
        applyFileFields(contractFileIds, request);
        return request;
    }

    private void applyScalarFields(ContractGroup contractGroup, ZhishuCreateContractRequest request) {
        for (String header : contractGroup.getHeaders()) {
            if (shouldSkipScalarField(header)) {
                continue;
            }
            Field field = CREATE_REQUEST_FIELDS.get(header);
            if (field == null) {
                continue;
            }
            ConvertedValue convertedValue = findFirstConvertibleValue(contractGroup, header, field.getType());
            if (convertedValue == null) {
                continue;
            }
            try {
                field.setAccessible(true);
                field.set(request, convertedValue.getValue());
            } catch (IllegalAccessException e) {
                log.warn("智书13Sheet历史合同字段赋值失败，contract_number={}，field={}，value={}，错误={}",
                        contractGroup.getContractNumber(), header, convertedValue.getRawValue(), e.getMessage());
            }
        }
    }

    private void applySignTypeFields(ContractGroup contractGroup, ZhishuCreateContractRequest request) {
        Object signFormValue = getFirstNonBlankValue(contractGroup, FIELD_SIGN_TYPE_CODE_SIGN_FORM);
        Integer signTypeCode = parseSignTypeCode(toStringValue(signFormValue));
        if (signTypeCode != null) {
            request.setSignTypeCode(signTypeCode);
        }

        Object sealPartyValue = getFirstNonBlankValue(contractGroup, FIELD_SIGN_TYPE_CODE_SEAL_PARTY);
        Integer signPartyNo = parseSignPartyNo(toStringValue(sealPartyValue));
        if (signPartyNo == null || request.getCounterPartyList() == null) {
            return;
        }
        for (ZhishuCreateContractRequest.CounterPartyInfo counterPartyInfo : request.getCounterPartyList()) {
            counterPartyInfo.setSignPartyNo(signPartyNo);
        }
    }

    private ConvertedValue findFirstConvertibleValue(ContractGroup contractGroup, String header, Class<?> fieldType) {
        List<LocatedValue> locatedValues = contractGroup.findNonBlankValues(header);
        if (locatedValues.isEmpty()) {
            return null;
        }
        ConvertedValue selected = null;
        for (LocatedValue locatedValue : locatedValues) {
            Object converted = convertValueForField(locatedValue.getValue(), fieldType, header);
            if (converted == null) {
                continue;
            }
            if (selected == null) {
                selected = new ConvertedValue(locatedValue.getValue(), converted, locatedValue);
                continue;
            }
            if (!isSameConvertedValue(selected.getValue(), converted)) {
                log.warn("智书13Sheet历史合同字段存在多个非空值，contract_number={}，field={}，保留位置={}:{}，忽略位置={}:{}",
                        contractGroup.getContractNumber(), header,
                        selected.getSource().getSheetName(), selected.getSource().getRowIndex(),
                        locatedValue.getSheetName(), locatedValue.getRowIndex());
                break;
            }
        }
        if (selected == null) {
            log.warn("智书13Sheet历史合同字段转换失败，contract_number={}，field={}，values={}",
                    contractGroup.getContractNumber(), header, JSON.toJSONString(locatedValues));
        }
        return selected;
    }

    private boolean shouldSkipScalarField(String header) {
        return header == null
                || CONTRACT_CATEGORY.equals(header)
                || header.startsWith("custom_")
                || header.contains(".")
                || header.contains("[]")
                || header.contains("/")
                || header.endsWith("_list");
    }

    private void applyDefaultValues(ContractGroup contractGroup, ZhishuCreateContractRequest request,
                                    SyncContext context) {
        if (trimToNull(request.getContractNumber()) == null) {
            request.setContractNumber(contractGroup.getContractNumber());
        }
        if (trimToNull(request.getSourceId()) == null) {
            request.setSourceId(contractGroup.getContractNumber());
        }
        if (trimToNull(request.getContractName()) == null) {
            request.setContractName("历史合同-" + contractGroup.getContractNumber());
        }
        if (trimToNull(request.getContractCategoryAbbreviation()) == null) {
            request.setContractCategoryAbbreviation(DEFAULT_CONTRACT_CATEGORY_ABBREVIATION);
        }
        if (trimToNull(request.getContractStatusCode()) == null) {
//            request.setContractStatusCode("0");
            request.setContractStatusCode(context.getContractStatusCode());
        }
        if (request.getBusinessTypeCode() == null) {
            request.setBusinessTypeCode(0);
        }
        applyCurrencyValues(contractGroup, request);
        if (request.getAmount() == null) {
            request.setAmount(BigDecimal.ZERO);
        }
    }

    private void applyCurrencyValues(ContractGroup contractGroup, ZhishuCreateContractRequest request) {
        String incomeCurrencyCode = resolveIncomeCurrencyCode(contractGroup);
        String expenseCurrencyCode = resolveExpenseCurrencyCode(contractGroup);
        request.setInCurrencyCode(incomeCurrencyCode);
        request.setOutCurrencyCode(expenseCurrencyCode);
        if (Integer.valueOf(1).equals(request.getPayTypeCode())) {
            request.setCurrencyCode(incomeCurrencyCode);
        } else if (Integer.valueOf(2).equals(request.getPayTypeCode())
                || Integer.valueOf(3).equals(request.getPayTypeCode())) {
            request.setCurrencyCode(expenseCurrencyCode);
        } else {
            request.setCurrencyCode(DEFAULT_CURRENCY_CODE);
        }
    }

    private String resolveIncomeCurrencyCode(ContractGroup contractGroup) {
        return firstNotBlank(trimToNull(toStringValue(getFirstNonBlankValue(contractGroup, FIELD_INCOME_CURRENCY_CODE))),
                DEFAULT_CURRENCY_CODE);
    }

    private String resolveExpenseCurrencyCode(ContractGroup contractGroup) {
        return firstNotBlank(trimToNull(toStringValue(getFirstNonBlankValue(contractGroup, FIELD_EXPENSE_CURRENCY_CODE))),
                DEFAULT_CURRENCY_CODE);
    }

    private List<CustomFieldValue> buildCustomFieldValues(ContractGroup contractGroup) {
        Map<String, CustomFieldValue> customFieldValueMap = new LinkedHashMap<>();
        for (Map.Entry<SheetRole, List<ExcelUtils.ExcelRowData>> entry : contractGroup.getRowsByRole().entrySet()) {
            SheetRole sheetRole = entry.getKey();
            if (sheetRole.isDetailRole()) {
                continue;
            }
            for (ExcelUtils.ExcelRowData row : entry.getValue()) {
                collectTopLevelCustomFields(contractGroup, customFieldValueMap, row);
            }
        }
        addCommonArrayCustomField(contractGroup, customFieldValueMap, SheetRole.GENERAL_ORDER_DETAIL,
                ZhishuAndYecaiFiledEnum.ORDERHT_DOCUMENT_INFO);
        addCommonArrayCustomField(contractGroup, customFieldValueMap, SheetRole.ANCHOR_FEE_DETAIL,
                ZhishuAndYecaiFiledEnum.ANCHOR_FEE_DETAIL);
        return new ArrayList<>(customFieldValueMap.values());
    }

    private void collectTopLevelCustomFields(ContractGroup contractGroup,
                                             Map<String, CustomFieldValue> customFieldValueMap,
                                             ExcelUtils.ExcelRowData row) {
        for (Map.Entry<String, List<ExcelUtils.ExcelCellData>> entry : row.getCellDataByHeader().entrySet()) {
            String header = entry.getKey();
            if (header == null || !header.startsWith("custom_") || isCommonArrayParentField(header)) {
                continue;
            }
            ExcelUtils.ExcelCellData cellData = firstNonBlankCell(entry.getValue());
            if (cellData == null) {
                continue;
            }
            addCustomFieldValue(contractGroup, customFieldValueMap, header, cellData.getValue(),
                    new SourceLocation(row.getSheetName(), row.getRowIndex(), cellData.getColumnIndex()));
        }
    }

    private void addCommonArrayCustomField(ContractGroup contractGroup,
                                           Map<String, CustomFieldValue> customFieldValueMap,
                                           SheetRole sheetRole,
                                           ZhishuAndYecaiFiledEnum parentFieldEnum) {
        List<List<ContractFormCreatResponse.FormAttribute>> rows = buildCommonArrayRows(contractGroup, sheetRole);
        if (rows.isEmpty()) {
            return;
        }
        List<ExcelUtils.ExcelRowData> sourceRows = contractGroup.getRowsByRole(sheetRole);
        SourceLocation source = sourceRows.isEmpty()
                ? new SourceLocation(sheetRole.getDisplayName(), 0, 0)
                : new SourceLocation(sourceRows.get(0).getSheetName(), sourceRows.get(0).getRowIndex(), 1);
        customFieldValueMap.put(parentFieldEnum.getZhishuFiled(),
                new CustomFieldValue(parentFieldEnum, FormAttributeTypeEnum.COMMON_ARRAY, rows, source));
    }

    private List<List<ContractFormCreatResponse.FormAttribute>> buildCommonArrayRows(ContractGroup contractGroup,
                                                                                     SheetRole sheetRole) {
        List<List<ContractFormCreatResponse.FormAttribute>> result = new ArrayList<>();
        for (ExcelUtils.ExcelRowData row : contractGroup.getRowsByRole(sheetRole)) {
            List<ContractFormCreatResponse.FormAttribute> rowAttributes = new ArrayList<>();
            for (Map.Entry<String, List<ExcelUtils.ExcelCellData>> entry : row.getCellDataByHeader().entrySet()) {
                String header = entry.getKey();
                if (header == null || !header.startsWith("custom_") || isCommonArrayParentField(header)) {
                    continue;
                }
                ExcelUtils.ExcelCellData cellData = firstNonBlankCell(entry.getValue());
                if (cellData == null) {
                    continue;
                }
                ZhishuAndYecaiFiledEnum fieldEnum = ZhishuAndYecaiFiledEnum.getByZhishuFiled(header);
                if (fieldEnum == null) {
                    log.warn("智书13Sheet历史合同明细字段未配置枚举，contract_number={}，field={}，sheetName={}，rowIndex={}",
                            contractGroup.getContractNumber(), header, row.getSheetName(), row.getRowIndex());
                    continue;
                }
                rowAttributes.add(buildFormAttribute(fieldEnum, inferAttributeType(header), cellData.getValue()));
            }
            if (!rowAttributes.isEmpty()) {
                result.add(rowAttributes);
            }
        }
        return result;
    }

    private void addCustomFieldValue(ContractGroup contractGroup,
                                     Map<String, CustomFieldValue> customFieldValueMap,
                                     String header,
                                     Object rawValue,
                                     SourceLocation source) {
        if (isBlankValue(rawValue)) {
            return;
        }
        if (customFieldValueMap.containsKey(header)) {
            CustomFieldValue existingValue = customFieldValueMap.get(header);
            log.warn("智书13Sheet历史合同自定义字段重复，contract_number={}，field={}，保留位置={}:{}，忽略位置={}:{}",
                    contractGroup.getContractNumber(), header,
                    existingValue.getSource().getSheetName(), existingValue.getSource().getRowIndex(),
                    source.getSheetName(), source.getRowIndex());
            return;
        }
        ZhishuAndYecaiFiledEnum fieldEnum = ZhishuAndYecaiFiledEnum.getByZhishuFiled(header);
        if (fieldEnum == null) {
            log.warn("智书13Sheet历史合同自定义字段未配置枚举，contract_number={}，field={}，sheetName={}，rowIndex={}",
                    contractGroup.getContractNumber(), header, source.getSheetName(), source.getRowIndex());
            return;
        }
        customFieldValueMap.put(header, new CustomFieldValue(fieldEnum, inferAttributeType(header), rawValue, source));
    }

    private boolean isCommonArrayParentField(String fieldCode) {
        return ZhishuAndYecaiFiledEnum.ORDERHT_DOCUMENT_INFO.getZhishuFiled().equals(fieldCode)
                || ZhishuAndYecaiFiledEnum.ANCHOR_FEE_DETAIL.getZhishuFiled().equals(fieldCode);
    }

    private ExcelUtils.ExcelCellData firstNonBlankCell(List<ExcelUtils.ExcelCellData> cellDataList) {
        if (cellDataList == null) {
            return null;
        }
        for (ExcelUtils.ExcelCellData cellData : cellDataList) {
            if (!isBlankValue(cellData.getValue())) {
                return cellData;
            }
        }
        return null;
    }

    private void applyCustomFormFields(ZhishuCreateContractRequest request, List<CustomFieldValue> customFieldValues) {
        if (customFieldValues.isEmpty()) {
            return;
        }
        List<ContractFormCreatResponse.FormAttribute> formAttributes = new ArrayList<>();
        List<ContractFormCreatResponse.FormAttribute> derivedFormAttributes = new ArrayList<>();
        for (CustomFieldValue customFieldValue : customFieldValues) {
            ContractFormCreatResponse.FormAttribute precedingDocumentAttribute =
                    buildPrecedingDocumentFormAttribute(customFieldValue, derivedFormAttributes);
            if (precedingDocumentAttribute != null) {
                formAttributes.add(precedingDocumentAttribute);
                continue;
            }
            formAttributes.add(buildFormAttribute(customFieldValue.getFieldEnum(),
                    customFieldValue.getAttributeType(), customFieldValue.getRawValue()));
        }
        for (ContractFormCreatResponse.FormAttribute derivedFormAttribute : derivedFormAttributes) {
            putOrReplaceFormAttribute(formAttributes, derivedFormAttribute);
        }
        request.setForm(JSON.toJSONString(formAttributes));
    }

    private void applyOrderMemberFields(ContractGroup contractGroup, ZhishuCreateContractRequest request) {
        JSONArray formAttributes = parseRequestFormAttributes(request);
        if (formAttributes == null) {
            return;
        }
        boolean changed;
        JSONObject orderInfoAttribute = findJsonFormAttribute(formAttributes,
                ZhishuAndYecaiFiledEnum.ORDERHT_DOCUMENT_INFO.getZhishuFiled());
        if (orderInfoAttribute != null) {
            changed = applyOrderMemberFieldsToOrderInfoRows(contractGroup, orderInfoAttribute);
        } else {
            changed = applyOrderMemberFieldsToTopLevel(contractGroup, formAttributes);
        }
        if (changed) {
            request.setForm(JSON.toJSONString(formAttributes));
        }
    }

    private JSONArray parseRequestFormAttributes(ZhishuCreateContractRequest request) {
        String form = request == null ? null : trimToNull(request.getForm());
        if (form == null) {
            return new JSONArray();
        }
        try {
            return JSONArray.parseArray(form);
        } catch (Exception e) {
            log.warn("智书13Sheet历史合同解析form失败，跳过订单人员字段补充，contract_number={}，错误={}",
                    request.getContractNumber(), e.getMessage(), e);
            return null;
        }
    }

    private boolean applyOrderMemberFieldsToOrderInfoRows(ContractGroup contractGroup, JSONObject orderInfoAttribute) {
        JSONArray orderInfoRows = orderInfoAttribute.getJSONArray("attribute_value");
        if (orderInfoRows == null || orderInfoRows.isEmpty()) {
            return false;
        }
        List<Set<String>> rowOrderNumbers = new ArrayList<>();
        Set<String> allOrderNumbers = new LinkedHashSet<>();
        for (int index = 0; index < orderInfoRows.size(); index++) {
            JSONArray rowAttributes = toJsonArray(orderInfoRows.get(index));
            if (rowAttributes == null) {
                rowOrderNumbers.add(Collections.emptySet());
                continue;
            }
            orderInfoRows.set(index, rowAttributes);
            Set<String> orderNumbers = extractOrderNumbersFromOrderInfoRow(rowAttributes);
            rowOrderNumbers.add(orderNumbers);
            allOrderNumbers.addAll(orderNumbers);
        }
        if (allOrderNumbers.isEmpty()) {
            return false;
        }
        Map<String, OrderMemberFieldValues> valuesByOrderNumber =
                loadOrderMemberFieldValues(allOrderNumbers, contractGroup.getContractNumber());
        boolean changed = false;
        for (int index = 0; index < orderInfoRows.size(); index++) {
            JSONArray rowAttributes = toJsonArray(orderInfoRows.get(index));
            if (rowAttributes == null) {
                continue;
            }
            OrderMemberFieldValues rowValues = mergeOrderMemberFieldValues(rowOrderNumbers.get(index),
                    valuesByOrderNumber);
            changed = putOrderMemberFields(rowAttributes, rowValues) || changed;
        }
        return changed;
    }

    private boolean applyOrderMemberFieldsToTopLevel(ContractGroup contractGroup, JSONArray formAttributes) {
        Set<String> orderNumbers = splitMultiValue(getFirstNonBlankValue(contractGroup,
                ZhishuAndYecaiFiledEnum.ORDER_DOCUMENT_NUMBER.getZhishuFiled()));
        if (orderNumbers.isEmpty()) {
            return false;
        }
        Map<String, OrderMemberFieldValues> valuesByOrderNumber =
                loadOrderMemberFieldValues(orderNumbers, contractGroup.getContractNumber());
        return putOrderMemberFields(formAttributes, mergeOrderMemberFieldValues(orderNumbers, valuesByOrderNumber));
    }

    private Set<String> extractOrderNumbersFromOrderInfoRow(JSONArray rowAttributes) {
        JSONObject orderNumberAttribute = findJsonFormAttribute(rowAttributes,
                ZhishuAndYecaiFiledEnum.ORDERHT_DOCUMENT_NUMBER.getZhishuFiled());
        return orderNumberAttribute == null
                ? Collections.emptySet()
                : splitMultiValue(orderNumberAttribute.get("attribute_value"));
    }

    private Map<String, OrderMemberFieldValues> loadOrderMemberFieldValues(Set<String> orderNumbers,
                                                                            String contractNumber) {
        Map<String, OrderInfoResponse> orderInfoMap = new LinkedHashMap<>();
        Set<String> userIds = new LinkedHashSet<>();
        for (String orderNumber : orderNumbers) {
            try {
                OrderInfoResponse orderInfo = queryOrderInfoForMemberFields(orderNumber, contractNumber);
                if (orderInfo == null) {
                    continue;
                }
                orderInfoMap.put(orderNumber, orderInfo);
                collectOrderMemberUserIds(orderInfo, userIds);
            } catch (Exception e) {
                log.warn("智书13Sheet历史合同查询订单人员失败，contract_number={}，orderNumber={}，错误={}",
                        contractNumber, orderNumber, e.getMessage(), e);
            }
        }
        Map<String, FeishuUserInfoResponse.User> activeUserMap = loadActiveFeishuUserMap(userIds, contractNumber);
        Map<String, OrderMemberFieldValues> result = new LinkedHashMap<>();
        for (Map.Entry<String, OrderInfoResponse> entry : orderInfoMap.entrySet()) {
            OrderMemberFieldValues values = buildOrderMemberFieldValues(entry.getValue(), activeUserMap,
                    contractNumber, entry.getKey());
            if (!values.isEmpty()) {
                result.put(entry.getKey(), values);
            }
        }
        return result;
    }

    private OrderInfoResponse queryOrderInfoForMemberFields(String orderNumber, String contractNumber) {
        Map<String, Object> params = buildBasePrecedingDocumentParams(20);
        params.put("dataType", "ORDER");
        String startTime = yeCaiDataConfig == null ? null : trimToNull(yeCaiDataConfig.getStartTime());
        if (startTime != null) {
            params.put("startTime", URLUtil.encode(startTime));
        }
        params.put("prjDimOrderValue", orderNumber);
        MasterDataRes masterDataRes = yuecaiContractClient.getOrderInfo(params);
        if (masterDataRes == null || masterDataRes.getContent() == null || masterDataRes.getContent().isEmpty()) {
            log.info("智书13Sheet历史合同订单人员字段未查询到订单信息，contract_number={}，orderNumber={}",
                    contractNumber, orderNumber);
            return null;
        }
        OrderInfoResponse first = null;
        for (Object content : masterDataRes.getContent()) {
            OrderInfoResponse orderInfo = JSONObject.parseObject(JSON.toJSONString(content), OrderInfoResponse.class);
            if (orderInfo == null) {
                continue;
            }
            if (first == null) {
                first = orderInfo;
            }
            if (orderNumber.equals(trimToNull(orderInfo.getPrjDimOrderValue()))) {
                return orderInfo;
            }
        }
        return first;
    }

    private void collectOrderMemberUserIds(OrderInfoResponse orderInfo, Set<String> userIds) {
        if (orderInfo == null || orderInfo.getMemberList() == null) {
            return;
        }
        for (OrderInfoResponse.Member member : orderInfo.getMemberList()) {
            String userId = member == null ? null : trimToNull(member.getUserId());
            if (userId != null) {
                userIds.add(userId);
            }
        }
    }

    private Map<String, FeishuUserInfoResponse.User> loadActiveFeishuUserMap(Set<String> userIds,
                                                                              String contractNumber) {
        Map<String, FeishuUserInfoResponse.User> userMap = new LinkedHashMap<>();
        if (userIds == null || userIds.isEmpty()) {
            return userMap;
        }
        if (feiShuApiClient == null) {
            log.warn("智书13Sheet历史合同订单人员字段未配置飞书客户端，contract_number={}", contractNumber);
            return userMap;
        }
        try {
            FeishuUserBatchInfoResponse userInfoBatch = feiShuApiClient.getUserInfoBatch(new ArrayList<>(userIds));
            if (userInfoBatch == null || userInfoBatch.getItems() == null) {
                return userMap;
            }
            for (FeishuUserInfoResponse.User user : userInfoBatch.getItems()) {
                String userId = user == null ? null : trimToNull(user.getUserId());
                if (userId == null) {
                    continue;
                }
                if (isFeishuUserResigned(user)) {
                    log.info("智书13Sheet历史合同订单人员字段跳过已离职用户，contract_number={}，userId={}，name={}",
                            contractNumber, user.getUserId(), user.getName());
                    continue;
                }
                userMap.put(userId, user);
            }
        } catch (Exception e) {
            log.warn("智书13Sheet历史合同订单人员字段批量查询飞书用户失败，contract_number={}，错误={}",
                    contractNumber, e.getMessage(), e);
        }
        return userMap;
    }

    private boolean isFeishuUserResigned(FeishuUserInfoResponse.User user) {
        return user != null && user.getStatus() != null && Boolean.TRUE.equals(user.getStatus().getResigned());
    }

    private OrderMemberFieldValues buildOrderMemberFieldValues(OrderInfoResponse orderInfo,
                                                               Map<String, FeishuUserInfoResponse.User> activeUserMap,
                                                               String contractNumber,
                                                               String orderNumber) {
        OrderMemberFieldValues values = new OrderMemberFieldValues();
        if (orderInfo != null) {
            values.addDropdownOption(ZhishuAndYecaiFiledEnum.ORDERHT_COST_CENTER, orderInfo.getCostCenter());
            values.addDropdownRadio(ZhishuAndYecaiFiledEnum.ORDERHT_ORDER_TYPE, orderInfo.getOrderType());
        }
        if (orderInfo == null || orderInfo.getMemberList() == null || orderInfo.getMemberList().isEmpty()) {
            log.info("智书13Sheet历史合同订单人员字段成员为空，contract_number={}，orderNumber={}",
                    contractNumber, orderNumber);
            return values;
        }
        for (OrderInfoResponse.Member member : orderInfo.getMemberList()) {
            String userId = member == null ? null : trimToNull(member.getUserId());
            if (userId == null) {
                log.info("智书13Sheet历史合同订单人员字段成员userId为空，contract_number={}，orderNumber={}",
                        contractNumber, orderNumber);
                continue;
            }
            if (!activeUserMap.containsKey(userId)) {
                log.info("智书13Sheet历史合同订单人员字段未获取到有效飞书用户，contract_number={}，orderNumber={}，userId={}",
                        contractNumber, orderNumber, userId);
                continue;
            }
            ZhishuAndYecaiFiledEnum fieldEnum = getOrderMemberFieldEnum(member.getRoleCode());
            if (fieldEnum != null) {
                values.add(fieldEnum, userId);
            }
        }
        return values;
    }

    private ZhishuAndYecaiFiledEnum getOrderMemberFieldEnum(String roleCode) {
        if ("10".equals(roleCode)) {
            return ZhishuAndYecaiFiledEnum.PROJECT_MANAGER;
        }
        if ("40".equals(roleCode)) {
            return ZhishuAndYecaiFiledEnum.ORDERHT_EXPENSE_GROUP;
        }
        if ("50".equals(roleCode)) {
            return ZhishuAndYecaiFiledEnum.ORDERHT_PROJECT_ACCEPTANCE;
        }
        if ("60".equals(roleCode)) {
            return ZhishuAndYecaiFiledEnum.ORDERHT_PROJECT_BUDGET;
        }
        if ("30".equals(roleCode)) {
            return ZhishuAndYecaiFiledEnum.ORDERHT_PROJECT_SPONOSOR;
        }
        return null;
    }

    private OrderMemberFieldValues mergeOrderMemberFieldValues(Set<String> orderNumbers,
                                                               Map<String, OrderMemberFieldValues> valuesByOrderNumber) {
        OrderMemberFieldValues mergedValues = new OrderMemberFieldValues();
        for (String orderNumber : orderNumbers) {
            OrderMemberFieldValues values = valuesByOrderNumber.get(orderNumber);
            if (values != null) {
                mergedValues.merge(values);
            }
        }
        return mergedValues;
    }

    private boolean putOrderMemberFields(JSONArray formAttributes, OrderMemberFieldValues values) {
        if (formAttributes == null || values == null || values.isEmpty()) {
            return false;
        }
        boolean changed = false;
        for (Map.Entry<ZhishuAndYecaiFiledEnum, LinkedHashSet<String>> entry : values.getUserIdsByField().entrySet()) {
            ContractFormCreatResponse.FormAttribute formAttribute = buildFormAttribute(entry.getKey(),
                    FormAttributeTypeEnum.EMPLOYEE, new ArrayList<>(entry.getValue()));
            formAttribute.setAttributeName(entry.getKey().getName());
            changed = putOrReplaceJsonFormAttribute(formAttributes, formAttribute) || changed;
        }
        for (Map.Entry<ZhishuAndYecaiFiledEnum, String> entry : values.getDropdownRadioValuesByField().entrySet()) {
            ContractFormCreatResponse.FormAttribute formAttribute = buildFormAttribute(entry.getKey(),
                    FormAttributeTypeEnum.DROPDOWN_RADIO, entry.getValue());
            formAttribute.setAttributeName(entry.getKey().getName());
            changed = putOrReplaceJsonFormAttribute(formAttributes, formAttribute) || changed;
        }
        for (Map.Entry<ZhishuAndYecaiFiledEnum, LinkedHashSet<String>> entry
                : values.getDropdownOptionValuesByField().entrySet()) {
            ContractFormCreatResponse.FormAttribute formAttribute =
                    buildDropdownOptionFormAttribute(entry.getKey(), entry.getValue());
            changed = putOrReplaceJsonFormAttribute(formAttributes, formAttribute) || changed;
        }
        return changed;
    }

    private ContractFormCreatResponse.FormAttribute buildDropdownOptionFormAttribute(
            ZhishuAndYecaiFiledEnum fieldEnum,
            Collection<String> optionValues) {
        ContractFormCreatResponse.FormAttribute formAttribute = new ContractFormCreatResponse.FormAttribute();
        formAttribute.setAttributeCode(fieldEnum.getZhishuFiled());
        formAttribute.setAttributeKey(fieldEnum.getZhishuFiled());
        formAttribute.setAttributeName(fieldEnum.getName());
        formAttribute.setAttributeType(FormAttributeTypeEnum.DROPDOWN_OPTION.getCode());
        formAttribute.setAttributeValue(buildDropdownOptionAttributeValue(optionValues));
        return formAttribute;
    }

    private Map<String, Object> buildDropdownOptionAttributeValue(Collection<String> optionValues) {
        Map<String, Object> optionValue = new LinkedHashMap<>();
        optionValue.put("key", new ArrayList<>(optionValues));
        return optionValue;
    }

    private boolean putOrReplaceJsonFormAttribute(JSONArray formAttributes,
                                                  ContractFormCreatResponse.FormAttribute formAttribute) {
        if (formAttributes == null || formAttribute == null || trimToNull(formAttribute.getAttributeCode()) == null) {
            return false;
        }
        JSONObject attribute = JSONObject.parseObject(JSON.toJSONString(formAttribute));
        for (int index = 0; index < formAttributes.size(); index++) {
            JSONObject existingAttribute = formAttributes.getJSONObject(index);
            if (existingAttribute != null
                    && formAttribute.getAttributeCode().equals(existingAttribute.getString("attribute_code"))) {
                formAttributes.set(index, attribute);
                return true;
            }
        }
        formAttributes.add(attribute);
        return true;
    }

    private JSONObject findJsonFormAttribute(JSONArray formAttributes, String attributeCode) {
        if (formAttributes == null || attributeCode == null) {
            return null;
        }
        for (int index = 0; index < formAttributes.size(); index++) {
            JSONObject attribute = formAttributes.getJSONObject(index);
            if (attributeCode.equals(attribute.getString("attribute_code"))) {
                return attribute;
            }
        }
        return null;
    }

    private JSONArray toJsonArray(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof JSONArray) {
            return (JSONArray) value;
        }
        if (value instanceof Collection) {
            return JSONArray.parseArray(JSON.toJSONString(value));
        }
        return null;
    }

    private ContractFormCreatResponse.FormAttribute buildPrecedingDocumentFormAttribute(
            CustomFieldValue customFieldValue,
            List<ContractFormCreatResponse.FormAttribute> derivedFormAttributes) {
        String fieldCode = customFieldValue.getFieldEnum().getZhishuFiled();
        if (!isPrecedingDocumentField(fieldCode)) {
            return null;
        }
        List<PrecedingDocResponse.Receipts> receipts = buildPrecedingDocumentReceipts(fieldCode,
                customFieldValue.getRawValue(), derivedFormAttributes);
        if (receipts.isEmpty()) {
            return null;
        }
        ZhishuAndYecaiFiledEnum fieldEnum = getPrecedingDocumentFieldEnum(fieldCode);
        ContractFormCreatResponse.FormAttribute formAttribute = new ContractFormCreatResponse.FormAttribute();
        formAttribute.setAttributeCode(fieldCode);
        formAttribute.setAttributeKey(fieldCode);
        formAttribute.setModuleName("相关单据");
        formAttribute.setAttributeName(fieldEnum == null ? null : fieldEnum.getName());
        formAttribute.setAttributeType(FormAttributeTypeEnum.FEISHU_APPROVAL.getCode());
        formAttribute.setApprovalType(FormAttributeTypeEnum.THIRD_PARTY_APPROVAL.getCode());
        formAttribute.setAttributeValue(receipts);
        return formAttribute;
    }

    private boolean isPrecedingDocumentField(String fieldCode) {
        return ZhishuAndYecaiFiledEnum.ORDER_DOCUMENT_NUMBER.getZhishuFiled().equals(fieldCode)
                || ZhishuAndYecaiFiledEnum.PROCUREMENT_DOCUMENT_NUMBER.getZhishuFiled().equals(fieldCode)
                || ZhishuAndYecaiFiledEnum.ANCHOR_DOCUMENT_NUMBER.getZhishuFiled().equals(fieldCode);
    }

    private ZhishuAndYecaiFiledEnum getPrecedingDocumentFieldEnum(String fieldCode) {
        if (ZhishuAndYecaiFiledEnum.ORDER_DOCUMENT_NUMBER.getZhishuFiled().equals(fieldCode)) {
            return ZhishuAndYecaiFiledEnum.ORDER_DOCUMENT_NUMBER;
        }
        if (ZhishuAndYecaiFiledEnum.PROCUREMENT_DOCUMENT_NUMBER.getZhishuFiled().equals(fieldCode)) {
            return ZhishuAndYecaiFiledEnum.PROCUREMENT_DOCUMENT_NUMBER;
        }
        if (ZhishuAndYecaiFiledEnum.ANCHOR_DOCUMENT_NUMBER.getZhishuFiled().equals(fieldCode)) {
            return ZhishuAndYecaiFiledEnum.ANCHOR_DOCUMENT_NUMBER;
        }
        return null;
    }

    private List<PrecedingDocResponse.Receipts> buildPrecedingDocumentReceipts(
            String fieldCode,
            Object rawValue,
            List<ContractFormCreatResponse.FormAttribute> derivedFormAttributes) {
        List<PrecedingDocResponse.Receipts> receipts = new ArrayList<>();
        for (String documentNumber : splitMultiValue(rawValue)) {
            if (ZhishuAndYecaiFiledEnum.ORDER_DOCUMENT_NUMBER.getZhishuFiled().equals(fieldCode)) {
                receipts.add(buildOrderInfoReceipt(documentNumber));
            } else if (ZhishuAndYecaiFiledEnum.PROCUREMENT_DOCUMENT_NUMBER.getZhishuFiled().equals(fieldCode)) {
                PrecedingDocResponse.Receipts procurementReceipt = buildProcurementReceiptSafely(documentNumber);
                if (procurementReceipt != null) {
                    receipts.add(procurementReceipt);
                }
            } else if (ZhishuAndYecaiFiledEnum.ANCHOR_DOCUMENT_NUMBER.getZhishuFiled().equals(fieldCode)) {
                receipts.add(buildAnchorCardReceipt(documentNumber, derivedFormAttributes));
            }
        }
        return receipts;
    }

    private PrecedingDocResponse.Receipts buildOrderInfoReceipt(String documentNumber) {
        Map<String, Object> params = buildBasePrecedingDocumentParams(1);
        params.put("dataType", "ORDER");
        String startTime = yeCaiDataConfig == null ? null : trimToNull(yeCaiDataConfig.getStartTime());
        if (startTime != null) {
            params.put("startTime", URLUtil.encode(startTime));
        }
        params.put("prjDimOrderValue", documentNumber);
        MasterDataRes masterDataRes = yuecaiContractClient.getOrderInfo(params);
        OrderInfoResponse data = parseFirstPrecedingDocument(masterDataRes, OrderInfoResponse.class,
                "订单", documentNumber);

        PrecedingDocResponse.Receipts receipts = new PrecedingDocResponse.Receipts();
        receipts.setId(data.getPrjDimOrderValue());
        receipts.setTitle(data.getOrderTitle());
        receipts.setContent(data.getPrjDimOrderValue());
        receipts.setMobileAppLink(buildYuecaiLink(ORDER_INFO_URL));
        receipts.setPcAppLink(buildYuecaiLink(ORDER_INFO_URL));
        return receipts;
    }

    private PrecedingDocResponse.Receipts buildProcurementReceiptSafely(String documentNumber) {
        try {
            return buildProcurementReceipt(documentNumber);
        } catch (Exception e) {
            log.warn("智书13Sheet历史合同采购申请未查询到，跳过前置单据赋值，documentNumber={}，错误={}",
                    documentNumber, e.getMessage(), e);
            return null;
        }
    }

    private PrecedingDocResponse.Receipts buildProcurementReceipt(String documentNumber) {
        MasterDataRes masterDataRes = yuecaiContractClient.getProcurement(
                buildBasePrecedingDocumentParams(1), documentNumber);
        ProcurementResponse data = parseFirstPrecedingDocument(masterDataRes, ProcurementResponse.class,
                "采购申请", documentNumber);

        PrecedingDocResponse.Receipts receipts = new PrecedingDocResponse.Receipts();
        receipts.setId(data.getExpRequisitionNumber());
        receipts.setTitle(data.getAttribute1());
        receipts.setContent(data.getExpRequisitionNumber());
        receipts.setMobileAppLink(buildYuecaiLink(DOCUMENT_LIST_URL));
        receipts.setPcAppLink(buildYuecaiLink(DOCUMENT_LIST_URL));
        return receipts;
    }

    private PrecedingDocResponse.Receipts buildAnchorCardReceipt(
            String documentNumber,
            List<ContractFormCreatResponse.FormAttribute> derivedFormAttributes) {
        MasterDataRes masterDataRes = yuecaiContractClient.getAnchorCard(
                buildBasePrecedingDocumentParams(1), documentNumber, "id");
        AnchorCardResponse data = parseFirstPrecedingDocument(masterDataRes, AnchorCardResponse.class,
                "主播卡片", documentNumber);
        String liveCategory = resolveAnchorCardLiveCategory(data);
        if (liveCategory != null) {
            derivedFormAttributes.add(buildFormAttribute(ZhishuAndYecaiFiledEnum.LIVE_CATEGORY,
                    FormAttributeTypeEnum.DROPDOWN_RADIO, liveCategory));
        }

        PrecedingDocResponse.Receipts receipts = new PrecedingDocResponse.Receipts();
        receipts.setId(data.getHeaderId() == null ? null : String.valueOf(data.getHeaderId()));
        receipts.setTitle(data.getRealName());
        receipts.setContent(data.getId());
        receipts.setMobileAppLink(buildYuecaiLink(ANCHOR_CARD_URL));
        receipts.setPcAppLink(buildYuecaiLink(ANCHOR_CARD_URL));
        return receipts;
    }

    private String resolveAnchorCardLiveCategory(AnchorCardResponse data) {
        if (data == null || data.getLineResultDTOS() == null) {
            return null;
        }
        for (AnchorCardResponse.AnchorCardLineRes lineResultDTO : data.getLineResultDTOS()) {
            if (lineResultDTO == null) {
                continue;
            }
            String liveCategory = trimToNull(lineResultDTO.getLiveCategory());
            if (liveCategory != null) {
                return liveCategory;
            }
        }
        return null;
    }

    private void putOrReplaceFormAttribute(List<ContractFormCreatResponse.FormAttribute> formAttributes,
                                           ContractFormCreatResponse.FormAttribute formAttribute) {
        if (formAttributes == null || formAttribute == null
                || trimToNull(formAttribute.getAttributeCode()) == null) {
            return;
        }
        for (int index = 0; index < formAttributes.size(); index++) {
            ContractFormCreatResponse.FormAttribute existingAttribute = formAttributes.get(index);
            if (existingAttribute != null
                    && formAttribute.getAttributeCode().equals(existingAttribute.getAttributeCode())) {
                formAttributes.set(index, formAttribute);
                return;
            }
        }
        formAttributes.add(formAttribute);
    }

    private Map<String, Object> buildBasePrecedingDocumentParams(int size) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("page", 0);
        params.put("size", size);
        return params;
    }

    private CounterPartyCodeLookup loadCounterPartyCodeLookup() {
        CounterPartyCodeLookup lookup = new CounterPartyCodeLookup();
        try {
            int vendorCount = loadVendorCounterPartyMappings(lookup);
            int customerCount = loadCustomerCounterPartyMappings(lookup);
            log.info("业财交易方编码映射加载完成，供应商数量：{}，客户数量：{}，映射数量：{}",
                    vendorCount, customerCount, lookup.size());
        } catch (Exception e) {
            log.error("业财交易方编码映射加载失败，本次历史合同同步将使用原始对方编码，错误：{}", e.getMessage(), e);
        }
        return lookup;
    }

    private int loadVendorCounterPartyMappings(CounterPartyCodeLookup lookup) {
        int count = 0;
        for (Object content : loadYecaiMasterDataContent(MasterDataTypeEnum.VENDER.getCode())) {
            if (content == null) {
                continue;
            }
            VenderRes venderRes = JSONObject.parseObject(JSON.toJSONString(content), VenderRes.class);
            if (venderRes == null) {
                continue;
            }
            lookup.addVendor(venderRes);
            count++;
        }
        return count;
    }

    private int loadCustomerCounterPartyMappings(CounterPartyCodeLookup lookup) {
        int count = 0;
        for (Object content : loadYecaiMasterDataContent(MasterDataTypeEnum.CUSTOMER.getCode())) {
            if (content == null) {
                continue;
            }
            CustomerRes customerRes = JSONObject.parseObject(JSON.toJSONString(content), CustomerRes.class);
            if (customerRes == null) {
                continue;
            }
            lookup.addCustomer(customerRes);
            count++;
        }
        return count;
    }

    private List<Object> loadYecaiMasterDataContent(String businessType) {
        List<Object> result = new ArrayList<>();
        if (yuecaiContractClient == null) {
            log.warn("业财客户端为空，无法加载主数据，类型：{}", businessType);
            return result;
        }
        int page = 0;
        boolean nextPage = true;
        while (nextPage) {
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("page", page);
            params.put("size", resolveYecaiMasterDataPageSize());
            params.put("dataType", businessType);
            String startTime = yeCaiDataConfig == null ? null : trimToNull(yeCaiDataConfig.getStartTime());
            if (startTime != null) {
                params.put("startTime", URLUtil.encode(startTime));
            }
            MasterDataRes masterDataRes = yuecaiContractClient.getMasterData(params);
            if (masterDataRes == null) {
                break;
            }
            if (masterDataRes.getContent() != null) {
                result.addAll(masterDataRes.getContent());
            }
            int totalPages = masterDataRes.getTotalPages() - 1;
            if (totalPages > page) {
                page++;
            } else {
                nextPage = false;
            }
        }
        log.info("业财主数据加载完成，类型：{}，数量：{}", businessType, result.size());
        return result;
    }

    private int resolveYecaiMasterDataPageSize() {
        if (yeCaiDataConfig != null && yeCaiDataConfig.getPageSize() > 0) {
            return yeCaiDataConfig.getPageSize();
        }
        return 500;
    }

    private <T> T parseFirstPrecedingDocument(MasterDataRes masterDataRes,
                                               Class<T> responseType,
                                              String documentType,
                                              String documentNumber) {
        if (masterDataRes == null || masterDataRes.getContent() == null
                || masterDataRes.getContent().isEmpty()) {
            throw new RuntimeException("前置单据查询为空，类型=" + documentType + "，编码=" + documentNumber);
        }
        Object content = masterDataRes.getContent().get(0);
        if (responseType.isInstance(content)) {
            return responseType.cast(content);
        }
        return JSONObject.parseObject(JSON.toJSONString(content), responseType);
    }

    private String buildYuecaiLink(String path) {
        String baseUrl = yuecaiApiConfig == null ? null : trimToNull(yuecaiApiConfig.getBaseUrl());
        if (baseUrl == null) {
            return path;
        }
        if (baseUrl.endsWith("/") && path != null && path.startsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1) + path;
        }
        return baseUrl + path;
    }

    private ContractFormCreatResponse.FormAttribute buildFormAttribute(ZhishuAndYecaiFiledEnum fieldEnum,
                                                                       FormAttributeTypeEnum attributeType,
                                                                       Object rawValue) {
        ContractFormCreatResponse.FormAttribute formAttribute = new ContractFormCreatResponse.FormAttribute();
        formAttribute.setAttributeCode(fieldEnum.getZhishuFiled());
        formAttribute.setAttributeKey(fieldEnum.getZhishuFiled());
//        formAttribute.setAttributeName(fieldEnum.getName());
        formAttribute.setAttributeType(attributeType.getCode());
        formAttribute.setAttributeValue(buildFormAttributeValue(fieldEnum, attributeType, rawValue));
        return formAttribute;
    }

    private Object buildFormAttributeValue(ZhishuAndYecaiFiledEnum fieldEnum,
                                           FormAttributeTypeEnum attributeType,
                                           Object rawValue) {
        Object mappedOptionValue = buildMappedOptionAttributeValue(fieldEnum, rawValue);
        if (mappedOptionValue != null) {
            return mappedOptionValue;
        }
        return buildFormAttributeValue(attributeType, rawValue);
    }

    private Object buildMappedOptionAttributeValue(ZhishuAndYecaiFiledEnum fieldEnum, Object rawValue) {
        if (fieldEnum == null) {
            return null;
        }
        String fieldCode = fieldEnum.getZhishuFiled();
        if (isTaxRateField(fieldEnum)) {
            String taxRateName = normalizeTaxRateName(rawValue);
            String taxRateKey = getTaxRateOptionKey(fieldEnum, taxRateName);
            if (taxRateKey == null) {
                log.warn("智书13Sheet历史合同税率字段未匹配到选项，field={}，value={}，将保留原值",
                        fieldEnum.getName(), rawValue);
                return null;
            }
            return buildKeyNameOptionValue(taxRateKey, taxRateName + "%");
        }
        if (ZhishuAndYecaiFiledEnum.INVOICE_TYPE.getZhishuFiled().equals(fieldCode)) {
            String invoiceTypeName = trimToNull(toStringValue(rawValue));
            String invoiceTypeCode = InvoiceTypeEnum.getZhishuCodeByDescription(invoiceTypeName);
            if (invoiceTypeCode == null) {
                log.warn("智书13Sheet历史合同发票种类未匹配到选项，value={}，将保留原值", rawValue);
                return null;
            }
            return buildKeyNameOptionValue(invoiceTypeCode, invoiceTypeName);
        }
        if (ZhishuAndYecaiFiledEnum.OUT_TAX_ITEM.getZhishuFiled().equals(fieldCode)
                || ZhishuAndYecaiFiledEnum.IN_TAX_ITEM.getZhishuFiled().equals(fieldCode)) {
            String taxItemName = trimToNull(toStringValue(rawValue));
            String taxItemCode = getTaxItemOptionKey(fieldEnum, taxItemName);
            if (taxItemCode == null) {
                log.warn("智书13Sheet历史合同税目未匹配到选项，field={}，value={}，将保留原值",
                        fieldEnum.getName(), rawValue);
                return null;
            }
            return buildKeyNameOptionValue(taxItemCode, taxItemName);
        }
        if (ZhishuAndYecaiFiledEnum.BANK_CHARGE_PAYER.getZhishuFiled().equals(fieldCode)) {
            String bankChargePayerName = trimToNull(toStringValue(rawValue));
            String bankChargePayerCode = BankChargePayerEnum.getZhishuCodeByDescription(bankChargePayerName);
            if (bankChargePayerCode == null) {
                log.warn("智书13Sheet历史合同银行手续费承担方未匹配到选项，value={}，将保留原值", rawValue);
                return null;
            }
            return buildKeyNameOptionValue(bankChargePayerCode, bankChargePayerName);
        }
        if (ZhishuAndYecaiFiledEnum.PRINT_MODE.getZhishuFiled().equals(fieldCode)) {
            String printModeName = trimToNull(toStringValue(rawValue));
            String printModeCode = PrintModeEnum.getCodeByName(printModeName);
            if (printModeCode == null) {
                log.warn("智书13Sheet历史合同打印模式未匹配到选项，value={}，将保留原值", rawValue);
                return null;
            }
            return buildKeyNameOptionValue(printModeCode, printModeName);
        }
        if (ZhishuAndYecaiFiledEnum.PLATFORM.getZhishuFiled().equals(fieldCode)) {
            String platformName = trimToNull(toStringValue(rawValue));
            String platformCode = PlatformEnum.getCodeByName(platformName);
            if (platformCode == null) {
                return null;
            }
            return buildKeyNameOptionValue(platformCode, platformName);
        }
        if (ZhishuAndYecaiFiledEnum.ACCEPTANCE_REQUIRED.getZhishuFiled().equals(fieldCode)) {
            String acceptanceName = normalizeAcceptanceRequiredName(rawValue);
            if (acceptanceName == null) {
                return null;
            }
            String acceptanceCode = "是".equals(acceptanceName)
                    ? ACCEPTANCE_REQUIRED_YES_CODE
                    : ACCEPTANCE_REQUIRED_NO_CODE;
            return buildKeyNameOptionValue(acceptanceCode, acceptanceName);
        }
        return null;
    }

    private String getTaxItemOptionKey(ZhishuAndYecaiFiledEnum fieldEnum, String taxItemName) {
        if (fieldEnum == ZhishuAndYecaiFiledEnum.OUT_TAX_ITEM) {
            return TaxItemEnum.getExpenseCodeByName(taxItemName);
        }
        if (fieldEnum == ZhishuAndYecaiFiledEnum.IN_TAX_ITEM) {
            return TaxItemEnum.getIncomeCodeByName(taxItemName);
        }
        return null;
    }

    private boolean isTaxRateField(ZhishuAndYecaiFiledEnum fieldEnum) {
        return fieldEnum == ZhishuAndYecaiFiledEnum.OUT_INCOME_TAX_RATE
                || fieldEnum == ZhishuAndYecaiFiledEnum.IN_INCOME_TAX_RATE;
    }

    private String normalizeTaxRateName(Object rawValue) {
        BigDecimal taxRate = toBigDecimal(rawValue);
        if (taxRate == null) {
            return null;
        }
        return taxRate.stripTrailingZeros().toPlainString();
    }

    private String getTaxRateOptionKey(ZhishuAndYecaiFiledEnum fieldEnum, String taxRateName) {
        if (fieldEnum == ZhishuAndYecaiFiledEnum.IN_INCOME_TAX_RATE) {
            return getIncomeTaxRateOptionKey(taxRateName);
        }
        if (fieldEnum == ZhishuAndYecaiFiledEnum.OUT_INCOME_TAX_RATE) {
            return getExpenseTaxRateOptionKey(taxRateName);
        }
        return null;
    }

    private String getExpenseTaxRateOptionKey(String taxRateName) {
        if ("0".equals(taxRateName)) {
            return "cmq9h8ss3002q597dhm30vgoj";
        }
        if ("1".equals(taxRateName)) {
            return "cmpdgo28e000v3b71qneibmb7";
        }
        if ("3".equals(taxRateName)) {
            return "cmpdgo28e000w3b71g31t8o0b";
        }
        if ("6".equals(taxRateName)) {
            return "cmq9gbq4f001h597dy173e7w0";
        }
        if ("9".equals(taxRateName)) {
            return "cmq9h8ydp002r597dqg3sv6pw";
        }
        if ("13".equals(taxRateName)) {
            return "cmq9gbrt3001j597dq3bnhfab";
        }
        return null;
    }

    private String getIncomeTaxRateOptionKey(String taxRateName) {
        if ("0".equals(taxRateName)) {
            return "cmq7gxg0l000j3b71ja2kpksh";
        }
        if ("1".equals(taxRateName)) {
            return "cmq7gxg0l000k3b71vyk57dlb";
        }
        if ("6".equals(taxRateName)) {
            return "cmqktvqgw000h35715izucadi";
        }
        if ("9".equals(taxRateName)) {
            return "cmqktwer1000i3571kd5mmcn7";
        }
        if ("13".equals(taxRateName)) {
            return "cmqktwiir000j3571mq3y376o";
        }
        return null;
    }

    private Map<String, Object> buildKeyNameOptionValue(String key, String name) {
        Map<String, Object> optionValue = new LinkedHashMap<>();
        optionValue.put("key", key);
        optionValue.put("name", name);
        return optionValue;
    }

    private String normalizeAcceptanceRequiredName(Object value) {
        String text = trimToNull(toStringValue(value));
        if (text == null) {
            return null;
        }
        if (ACCEPTANCE_REQUIRED_YES_CODE.equals(text) || "Y".equalsIgnoreCase(text)
                || "YES".equalsIgnoreCase(text) || "是".equals(text) || "需要".equals(text)) {
            return "是";
        }
        if (ACCEPTANCE_REQUIRED_NO_CODE.equals(text) || "N".equalsIgnoreCase(text)
                || "NO".equalsIgnoreCase(text) || "否".equals(text) || "不需要".equals(text)) {
            return "否";
        }
        return null;
    }

    private void applyPartyFields(ContractGroup contractGroup, ZhishuCreateContractRequest request, SyncContext context) {
        List<ZhishuCreateContractRequest.OurPartyInfo> ourPartyList = buildOurPartyList(contractGroup);
        if (!ourPartyList.isEmpty()) {
            request.setOurPartyList(ourPartyList);
        }
        List<ZhishuCreateContractRequest.CounterPartyInfo> counterPartyList = buildCounterPartyList(contractGroup, context);
        if (!counterPartyList.isEmpty()) {
            request.setCounterPartyList(counterPartyList);
        }
    }

    private List<ZhishuCreateContractRequest.OurPartyInfo> buildOurPartyList(ContractGroup contractGroup) {
        List<ZhishuCreateContractRequest.OurPartyInfo> result = new ArrayList<>();
        Set<String> partyCodes = collectSplitValues(contractGroup, contractGroup.getFlowType().getOurPartyRole(),
                FIELD_OUR_PARTY_CODE);
        for (String partyCode : partyCodes) {
            ZhishuCreateContractRequest.OurPartyInfo ourPartyInfo = new ZhishuCreateContractRequest.OurPartyInfo();
            ourPartyInfo.setOurPartyCode(partyCode);
            ourPartyInfo.setOurPartySignInfoResource(buildDisabledSignInfoResource());
            result.add(ourPartyInfo);
        }
        return result;
    }

    private List<ZhishuCreateContractRequest.CounterPartyInfo> buildCounterPartyList(ContractGroup contractGroup,
                                                                                     SyncContext context) {
        List<ZhishuCreateContractRequest.CounterPartyInfo> result = new ArrayList<>();
        Set<String> partyCodes = collectSplitValues(contractGroup, contractGroup.getFlowType().getCounterPartyRole(),
                FIELD_COUNTER_PARTY_CODE);
        for (String partyCode : resolveCounterPartyCodes(partyCodes, context)) {
            ZhishuCreateContractRequest.CounterPartyInfo counterPartyInfo =
                    new ZhishuCreateContractRequest.CounterPartyInfo();
            counterPartyInfo.setCounterPartyCode(partyCode);
            counterPartyInfo.setCounterPartySignInfoResource(buildDisabledSignInfoResource());
            result.add(counterPartyInfo);
        }
        return result;
    }

    private ZhishuCreateContractRequest.SignInfoResource buildDisabledSignInfoResource() {
        ZhishuCreateContractRequest.SignInfoResource signInfoResource =
                new ZhishuCreateContractRequest.SignInfoResource();
        signInfoResource.setEnable(false);
        return signInfoResource;
    }

    private void applyRelationFields(ContractGroup contractGroup, ZhishuCreateContractRequest request) {
        Set<String> relationContracts = contractGroup.getRelationContractNumbers();
        String frameworkContractNumber = trimToNull(toStringValue(getFirstNonBlankValue(contractGroup,
                FIELD_FRAMEWORK_CONTRACT_NUMBER)));
        if (relationContracts.isEmpty() && frameworkContractNumber == null) {
            return;
        }
        ZhishuCreateContractRequest.RelationInfo relationInfo = new ZhishuCreateContractRequest.RelationInfo();
        if (!relationContracts.isEmpty()) {
            relationInfo.setRelationContracts(new ArrayList<>(relationContracts));
        }
//        relationInfo.setFrameworkContractNumber(frameworkContractNumber);
        request.setRelation(relationInfo);
        String relationNumber = toStringValue(getFirstNonBlankValue(contractGroup, FIELD_RELATION_CONTRACTS));
        if(relationNumber!=null&&!relationNumber.isEmpty()){
            List<ZhishuCreateContractRequest.RelationList> relationLists = new ArrayList<>();
            ZhishuCreateContractRequest.RelationList relationList = new ZhishuCreateContractRequest.RelationList();
            relationList.setRelationName("关联合同");
            relationList.setContractNumbers(Arrays.asList(relationNumber));
            relationLists.add(relationList);
            request.setRelationList(relationLists);
        }
    }

    private void applyPlanFields(ContractGroup contractGroup, ZhishuCreateContractRequest request, SyncContext context) {
        List<ZhishuCreateContractRequest.PaymentPlanInfo> paymentPlanList = buildPaymentPlanList(contractGroup, context);
        if (!paymentPlanList.isEmpty()) {
            request.setPaymentPlanList(paymentPlanList);
        }
        List<ZhishuCreateContractRequest.CollectionPlanInfo> collectionPlanList =
                buildCollectionPlanList(contractGroup, context);
        if (!collectionPlanList.isEmpty()) {
            request.setCollectionPlanList(collectionPlanList);
        }
    }

    private List<ZhishuCreateContractRequest.PaymentPlanInfo> buildPaymentPlanList(ContractGroup contractGroup,
                                                                                   SyncContext context) {
        List<ZhishuCreateContractRequest.PaymentPlanInfo> paymentPlanList = new ArrayList<>();
        for (ExcelUtils.ExcelRowData row : contractGroup.getRowsByRole(SheetRole.GENERAL_PAYMENT_PLAN)) {
            ZhishuCreateContractRequest.PaymentPlanInfo paymentPlan = new ZhishuCreateContractRequest.PaymentPlanInfo();
            paymentPlan.setPaymentDate(toDateString(row.getFirstValue(FIELD_PAYMENT_DATE)));
            paymentPlan.setPrepaid(toBoolean(row.getFirstValue(FIELD_PAYMENT_PREPAID)));
            paymentPlan.setPaymentAmount(toBigDecimal(row.getFirstValue(FIELD_PAYMENT_AMOUNT)));
            paymentPlan.setPaymentDesc(trimToNull(toStringValue(row.getFirstValue(FIELD_PAYMENT_DESC))));
            paymentPlan.setCurrencyCode(resolveExpenseCurrencyCode(contractGroup));
            List<ZhishuCreateContractRequest.CounterPartyRef> paymentCounterPartyRefs =
                    buildCounterPartyRefs(row.getFirstValue(FIELD_PAYMENT_COUNTER_PARTY), context);
            if (!paymentCounterPartyRefs.isEmpty()) {
                paymentPlan.setPaymentCounterParty(paymentCounterPartyRefs.get(0));
            }

            String paymentCustomAttributes = buildPaymentCustomAttributes(row.getFirstValue(FIELD_PAYMENT_TYPE));
            if (paymentCustomAttributes != null) {
                paymentPlan.setPaymentCustomAttributes(paymentCustomAttributes);
            }
            if (!isPaymentPlanEmpty(paymentPlan)) {
                paymentPlanList.add(paymentPlan);
            }
        }
        return paymentPlanList;
    }

    private String buildPaymentCustomAttributes(Object paymentTypeValue) {
        String paymentTypeName = trimToNull(toStringValue(paymentTypeValue));
        if (paymentTypeName == null) {
            return null;
        }
        String paymentTypeKey = "押金/保证金".equals(paymentTypeName)
                ? "cmoi963cu006e3b713s1y5f7i"
                : "cmoi963cu006f3b715s4wknfc";

        ContractFormCreatResponse.FormAttribute paymentTypeAttribute =
                new ContractFormCreatResponse.FormAttribute();
        paymentTypeAttribute.setModuleName("履约计划");
        paymentTypeAttribute.setAttributeName("付款性质");
        paymentTypeAttribute.setAttributeCode(ZhishuAndYecaiFiledEnum.PAYMENT_NODE_TYPE.getZhishuFiled());
        paymentTypeAttribute.setAttributeKey(ZhishuAndYecaiFiledEnum.PAYMENT_NODE_TYPE.getZhishuFiled());
        paymentTypeAttribute.setAttributeType(FormAttributeTypeEnum.DROPDOWN_RADIO.getCode());
        paymentTypeAttribute.setAttributeValue(buildKeyNameOptionValue(paymentTypeKey, paymentTypeName));
        return JSON.toJSONString(Collections.singletonList(paymentTypeAttribute));
    }

    private List<ZhishuCreateContractRequest.CollectionPlanInfo> buildCollectionPlanList(ContractGroup contractGroup,
                                                                                         SyncContext context) {
        List<ZhishuCreateContractRequest.CollectionPlanInfo> collectionPlanList = new ArrayList<>();
        for (ExcelUtils.ExcelRowData row : contractGroup.getRowsByRole(SheetRole.GENERAL_COLLECTION_PLAN)) {
            ZhishuCreateContractRequest.CollectionPlanInfo collectionPlan =
                    new ZhishuCreateContractRequest.CollectionPlanInfo();
            collectionPlan.setCollectionDate(toDateString(row.getFirstValue(FIELD_COLLECTION_DATE)));
            collectionPlan.setCollectionAmount(toBigDecimal(row.getFirstValue(FIELD_COLLECTION_AMOUNT)));
            collectionPlan.setCollectionDesc(trimToNull(toStringValue(row.getFirstValue(FIELD_COLLECTION_DESC))));
            collectionPlan.setCurrencyCode(resolveIncomeCurrencyCode(contractGroup));
            List<ZhishuCreateContractRequest.CounterPartyRef> collectionCounterPartyRefs =
                    buildCounterPartyRefs(row.getFirstValue(FIELD_COLLECTION_COUNTER_PARTY), context);
            if (!collectionCounterPartyRefs.isEmpty()) {
                collectionPlan.setCollectionCounterParty(collectionCounterPartyRefs.get(0));
            }
            if (!isCollectionPlanEmpty(collectionPlan)) {
                collectionPlanList.add(collectionPlan);
            }
        }
        return collectionPlanList;
    }

    private List<ZhishuCreateContractRequest.CounterPartyRef> buildCounterPartyRefs(Object value, SyncContext context) {
        Set<String> partyCodes = splitMultiValue(value);
        if (partyCodes.isEmpty()) {
            return Collections.emptyList();
        }
        List<ZhishuCreateContractRequest.CounterPartyRef> refs = new ArrayList<>();
        for (String partyCode : resolveCounterPartyCodes(partyCodes, context)) {
            ZhishuCreateContractRequest.CounterPartyRef ref = new ZhishuCreateContractRequest.CounterPartyRef();
            ref.setCounterPartyCode(partyCode);
            refs.add(ref);
        }
        return refs;
    }

    private Set<String> resolveCounterPartyCodes(Set<String> partyCodes, SyncContext context) {
        Set<String> result = new LinkedHashSet<>();
        if (partyCodes == null || partyCodes.isEmpty()) {
            return result;
        }
        CounterPartyCodeLookup lookup = context == null ? null : context.getCounterPartyCodeLookup();
        for (String partyCode : partyCodes) {
            String resolvedCode = lookup == null ? trimToNull(partyCode) : lookup.resolve(partyCode);
            if (resolvedCode != null) {
                result.add(resolvedCode);
            }
        }
        removeCounterPartyCodePartsCoveredByCombinedCode(result);
        return result;
    }

    private void removeCounterPartyCodePartsCoveredByCombinedCode(Set<String> partyCodes) {
        if (partyCodes == null || partyCodes.size() <= 1) {
            return;
        }
        Set<String> coveredParts = new LinkedHashSet<>();
        for (String partyCode : partyCodes) {
            if (partyCode == null || !partyCode.contains(";")) {
                continue;
            }
            for (String part : partyCode.split(";")) {
                String item = trimToNull(part);
                if (item != null) {
                    coveredParts.add(item);
                }
            }
        }
        if (!coveredParts.isEmpty()) {
            partyCodes.removeAll(coveredParts);
        }
    }

    private boolean isPaymentPlanEmpty(ZhishuCreateContractRequest.PaymentPlanInfo paymentPlan) {
        return trimToNull(paymentPlan.getPaymentDate()) == null
                && paymentPlan.getPrepaid() == null
                && paymentPlan.getPaymentAmount() == null
                && trimToNull(paymentPlan.getPaymentDesc()) == null
                && (paymentPlan.getPaymentCustomAttributes() == null
                || paymentPlan.getPaymentCustomAttributes().isEmpty())
                && (paymentPlan.getPaymentCounterParty() == null);
    }

    private boolean isCollectionPlanEmpty(ZhishuCreateContractRequest.CollectionPlanInfo collectionPlan) {
        return trimToNull(collectionPlan.getCollectionDate()) == null
                && collectionPlan.getCollectionAmount() == null
                && trimToNull(collectionPlan.getCollectionDesc()) == null
                && (collectionPlan.getCollectionCounterParty() == null);
    }

    private String resolveContractCategoryAbbreviation(ContractGroup contractGroup,
                                                       String contractCategory,
                                                       SyncContext context) {
        String cached = context.getContractCategoryAbbreviation(contractCategory);
        if (cached != null) {
            return cached;
        }
        List<ContractCategoryNode> matchedNodes = new ArrayList<>();
        for (ContractCategoryNode node : getContractCategoryNodes(context)) {
            if (node.matches(contractCategory)) {
                matchedNodes.add(node);
            }
        }
        if (matchedNodes.isEmpty()) {
            log.warn("智书13Sheet历史合同合同类型未匹配，contract_number={}，flowType={}，contractCategory={}",
                    contractGroup.getContractNumber(), contractGroup.getFlowType().getDesc(), contractCategory);
            return null;
        }
        if (matchedNodes.size() > 1) {
            log.warn("智书13Sheet历史合同合同类型匹配到多个，默认取第一个，contract_number={}，contractCategory={}，matchCount={}",
                    contractGroup.getContractNumber(), contractCategory, matchedNodes.size());
        }
        ContractCategoryNode selected = matchedNodes.get(0);
        String abbreviation = trimToNull(selected.getAbbreviation());
        if (abbreviation == null) {
            log.warn("智书13Sheet历史合同合同类型缩写为空，contract_number={}，contractCategory={}，selected={}",
                    contractGroup.getContractNumber(), contractCategory, JSON.toJSONString(selected));
            return null;
        }
        context.putContractCategoryAbbreviation(contractCategory, abbreviation);
        log.info("智书13Sheet历史合同合同类型解析完成，contract_number={}，contractCategory={}，abbreviation={}，name={}，number={}",
                contractGroup.getContractNumber(), contractCategory, abbreviation, selected.getName(), selected.getNumber());
        return abbreviation;
    }

    private List<ContractCategoryNode> getContractCategoryNodes(SyncContext context) {
        List<ContractCategoryNode> cachedNodes = context.getContractCategoryNodes();
        if (cachedNodes != null) {
            return cachedNodes;
        }
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("lang", "zh-CN");
        log.info("智书13Sheet历史合同查询合同类型目录开始");
        QueryContractCategoryResponse response = zhishuContractClient.queryContractCategorys(params);
        if (response == null || !response.isSuccess()) {
            throw new RuntimeException("查询合同类型目录失败，返回=" + JSON.toJSONString(response));
        }
        List<ContractCategoryNode> nodes = new ArrayList<>();
        QueryContractCategoryResponse.DataInfo data = response.getData();
        List<QueryContractCategoryResponse.CategoryResource> resources =
                data == null ? null : data.getCategoryResources();
        appendContractCategoryNodes(resources, nodes);
        context.setContractCategoryNodes(nodes);
        log.info("智书13Sheet历史合同查询合同类型目录完成，categoryCount={}", nodes.size());
        return nodes;
    }

    private void appendContractCategoryNodes(List<QueryContractCategoryResponse.CategoryResource> resources,
                                             List<ContractCategoryNode> nodes) {
        if (resources == null || resources.isEmpty()) {
            return;
        }
        for (QueryContractCategoryResponse.CategoryResource resource : resources) {
            if (resource == null) {
                continue;
            }
            nodes.add(new ContractCategoryNode(resource.getName(), resource.getNumber(),
                    resource.getAbbreviation(), resource.getId()));
            appendContractCategoryNodes(resource.getChildren(), nodes);
        }
    }

    private ContractFileIds uploadContractFiles(ContractGroup contractGroup,
                                                Path excelDirectory,
                                                SyncContext context) {
        ContractFileIds result = new ContractFileIds();
        List<String> textFilePaths = collectSplitValueList(contractGroup, FIELD_CONTRACT_TEXT);
        if (textFilePaths.isEmpty()) {
            if (!uploadFallbackContractFiles(contractGroup, result, context, "合同文本文件为空")) {
                return ContractFileIds.fail("合同文本文件为空，兜底目录未找到可用文件："
                        + resolveFallbackContractDirectory(contractGroup, context));
            }
        } else {
            File textFile = tryResolveLocalContractFile(textFilePaths.get(0), excelDirectory);
            if (textFile == null) {
                if (!uploadFallbackContractFiles(contractGroup, result, context,
                        "合同文本文件无效：" + textFilePaths.get(0))) {
                    return ContractFileIds.fail("合同文本文件无效：" + textFilePaths.get(0)
                            + "，兜底目录未找到可用文件：" + resolveFallbackContractDirectory(contractGroup, context));
                }
            } else {
                String textFileId = uploadOneContractFile(contractGroup, textFile, FILE_TYPE_TEXT, context);
                if (textFileId == null) {
                    return ContractFileIds.fail("合同文本文件上传失败：" + textFilePaths.get(0));
                }
                result.setTextFileId(textFileId);

                for (int index = 1; index < textFilePaths.size(); index++) {
                    String attachmentFileId = uploadOneContractFile(contractGroup, textFilePaths.get(index),
                            FILE_TYPE_ATTACHMENT, excelDirectory, context);
                    if (attachmentFileId == null) {
                        return ContractFileIds.fail("合同文本额外文件上传失败：" + textFilePaths.get(index));
                    }
                    result.addAttachmentFileId(attachmentFileId);
                }
            }
        }
        for (String causePath : collectSplitValueList(contractGroup, FIELD_CONTRACT_CAUSES)) {
            String causeFileId = uploadOneContractFile(contractGroup, causePath, FILE_TYPE_CAUSE,
                    excelDirectory, context);
            if (causeFileId == null) {
                return ContractFileIds.fail("合同附件上传失败：" + causePath);
            }
            result.addContractCauseFileId(causeFileId);
        }
        for (String attachmentPath : collectSplitValueList(contractGroup, FIELD_CONTRACT_ATTACHMENTS)) {
            String attachmentFileId = uploadOneContractFile(contractGroup, attachmentPath, FILE_TYPE_ATTACHMENT,
                    excelDirectory, context);
            if (attachmentFileId == null) {
                return ContractFileIds.fail("其他附件上传失败：" + attachmentPath);
            }
            result.addAttachmentFileId(attachmentFileId);
        }
        return result;
    }

    private String uploadOneContractFile(ContractGroup contractGroup,
                                         String filePath,
                                         String fileType,
                                         Path excelDirectory,
                                         SyncContext context) {
        File file = resolveLocalContractFile(filePath, excelDirectory);
        return uploadOneContractFile(contractGroup, file, fileType, context);
    }

    private String uploadOneContractFile(ContractGroup contractGroup,
                                         File file,
                                         String fileType,
                                         SyncContext context) {
        return uploadOneContractFile(contractGroup.getContractNumber(), file, fileType, context);
    }

    private String uploadOneContractFile(String contractNumber,
                                         File file,
                                         String fileType,
                                         SyncContext context) {
        String cacheKey = buildUploadCacheKey(file, fileType);
        String cachedFileId = context.getUploadedFileId(cacheKey);
        if (cachedFileId != null) {
            log.info("智书13Sheet历史合同文件上传命中缓存，contract_number={}，fileType={}，filePath={}，fileId={}",
                    contractNumber, fileType, file.getAbsolutePath(), cachedFileId);
            return cachedFileId;
        }
        log.info("智书13Sheet历史合同上传文件开始，contract_number={}，fileType={}，filePath={}，fileSize={}",
                contractNumber, fileType, file.getAbsolutePath(), file.length());
        UploadContractFileResponse uploadResponse =
                zhishuContractClient.uploadContractFile(file, fileType, NEED_CONVERT_TO_PDF);
        if (uploadResponse == null || !uploadResponse.isSuccess() || uploadResponse.getData() == null
                || trimToNull(uploadResponse.getData().getFileId()) == null) {
            throw new RuntimeException("上传合同相关文件失败，filePath=" + file.getAbsolutePath()
                    + "，返回=" + JSON.toJSONString(uploadResponse));
        }
        String fileId = trimToNull(uploadResponse.getData().getFileId());
        context.putUploadedFileId(cacheKey, fileId);
        log.info("智书13Sheet历史合同上传文件完成，contract_number={}，fileType={}，filePath={}，fileId={}",
                contractNumber, fileType, file.getAbsolutePath(), fileId);
        return fileId;
    }

    private boolean uploadFallbackContractFiles(ContractGroup contractGroup,
                                                ContractFileIds result,
                                                SyncContext context,
                                                String reason) {
        List<File> fallbackFiles = findFallbackContractFiles(contractGroup, context);
        if (fallbackFiles.isEmpty()) {
            log.warn("智书13Sheet历史合同兜底目录未找到可用文件，contract_number={}，reason={}，fallbackDir={}",
                    contractGroup.getContractNumber(), reason, resolveFallbackContractDirectory(contractGroup, context));
            return false;
        }
        log.info("智书13Sheet历史合同使用兜底目录文件，contract_number={}，reason={}，fallbackDir={}，fileCount={}",
                contractGroup.getContractNumber(), reason, resolveFallbackContractDirectory(contractGroup, context),
                fallbackFiles.size());
        String textFileId = uploadOneContractFile(contractGroup, fallbackFiles.get(0), FILE_TYPE_TEXT, context);
        if (textFileId == null) {
            return false;
        }
        result.setTextFileId(textFileId);
        for (int index = 1; index < fallbackFiles.size(); index++) {
            String attachmentFileId = uploadOneContractFile(contractGroup, fallbackFiles.get(index),
                    FILE_TYPE_ATTACHMENT, context);
            if (attachmentFileId == null) {
                return false;
            }
            result.addAttachmentFileId(attachmentFileId);
        }
        return true;
    }

    private AntiBriberyContractFiles findAntiBriberyContractFiles(String contractNumber, SyncContext context) {
        Path fallbackDirectory = resolveFallbackContractDirectory(contractNumber, context);
        Path mainDirectory = fallbackDirectory.resolve(ANTI_BRIBERY_MAIN_FILE_FOLDER);
        Path scanDirectory = fallbackDirectory.resolve(ANTI_BRIBERY_SCAN_FILE_FOLDER);
        List<File> mainFiles = findNonEmptyFilesInDirectory(mainDirectory);
        List<File> scanFiles = findNonEmptyFilesInDirectory(scanDirectory);
        List<File> attachmentFiles = new ArrayList<>();

        File directory = fallbackDirectory.toFile();
        File[] children = directory.listFiles();
        if (children != null) {
            for (File child : children) {
                if (!child.isDirectory()) {
                    continue;
                }
                String folderName = child.getName();
                if (ANTI_BRIBERY_MAIN_FILE_FOLDER.equals(folderName)
                        || ANTI_BRIBERY_SCAN_FILE_FOLDER.equals(folderName)) {
                    continue;
                }
                attachmentFiles.addAll(findNonEmptyFilesInDirectory(child.toPath()));
            }
        }
        Collections.sort(attachmentFiles, (first, second) -> toFallbackSortKey(fallbackDirectory, first)
                .compareTo(toFallbackSortKey(fallbackDirectory, second)));
        return new AntiBriberyContractFiles(fallbackDirectory, mainDirectory, scanDirectory,
                mainFiles, scanFiles, attachmentFiles);
    }

    private File tryResolveLocalContractFile(String filePath, Path excelDirectory) {
        try {
            return resolveLocalContractFile(filePath, excelDirectory);
        } catch (RuntimeException e) {
            log.warn("智书13Sheet历史合同本地文件解析失败，filePath={}，excelDirectory={}，错误={}",
                    filePath, excelDirectory, e.getMessage());
            return null;
        }
    }

    private File resolveLocalContractFile(String filePath, Path excelDirectory) {
        String cleanPath = trimToNull(filePath);
        if (cleanPath == null) {
            throw new RuntimeException("文件路径为空");
        }
        cleanPath = stripWrappingQuotes(cleanPath);
        Path path = Paths.get(cleanPath);
        if (!path.isAbsolute()) {
            path = excelDirectory.resolve(path);
        }
        File file = path.normalize().toFile();
        if (!file.exists()) {
            throw new RuntimeException("文件不存在：" + file.getAbsolutePath());
        }
        if (!file.isFile()) {
            throw new RuntimeException("不是有效文件：" + file.getAbsolutePath());
        }
        if (file.length() <= 0) {
            throw new RuntimeException("文件为空：" + file.getAbsolutePath());
        }
        return file;
    }

    private List<File> findFallbackContractFiles(ContractGroup contractGroup, SyncContext context) {
        return findFallbackContractFiles(contractGroup.getContractNumber(), context);
    }

    private List<File> findFallbackContractFiles(String contractNumber, SyncContext context) {
        Path fallbackDirectory = resolveFallbackContractDirectory(contractNumber, context);
        return findNonEmptyFilesInDirectory(fallbackDirectory);
    }

    private List<File> findNonEmptyFilesInDirectory(Path directory) {
        File directoryFile = directory.toFile();
        if (!directoryFile.exists() || !directoryFile.isDirectory()) {
            return Collections.emptyList();
        }
        List<File> files = new ArrayList<>();
        collectFallbackContractFiles(directoryFile, files);
        Collections.sort(files, (first, second) -> toFallbackSortKey(directory, first)
                .compareTo(toFallbackSortKey(directory, second)));
        return files;
    }

    private void collectFallbackContractFiles(File current, List<File> files) {
        File[] children = current.listFiles();
        if (children == null || children.length == 0) {
            return;
        }
        for (File child : children) {
            if (child.isDirectory()) {
                collectFallbackContractFiles(child, files);
                continue;
            }
            if (child.isFile() && child.length() > 0) {
                files.add(child);
            }
        }
    }

    private String toFallbackSortKey(Path fallbackDirectory, File file) {
        Path filePath = file.toPath().toAbsolutePath().normalize();
        try {
            return fallbackDirectory.relativize(filePath).toString();
        } catch (IllegalArgumentException e) {
            return filePath.toString();
        }
    }

    private Path resolveFallbackContractDirectory(ContractGroup contractGroup, SyncContext context) {
        return resolveFallbackContractDirectory(contractGroup.getContractNumber(), context);
    }

    private Path resolveFallbackContractDirectory(String contractNumber, SyncContext context) {
        Path fallbackRoot = context == null ? contractFileFallbackRoot : context.getContractFileFallbackRoot();
        return fallbackRoot.resolve(contractNumber).toAbsolutePath().normalize();
    }

    private String stripWrappingQuotes(String value) {
        String result = value;
        while (result.length() >= 2
                && ((result.startsWith("\"") && result.endsWith("\""))
                || (result.startsWith("'") && result.endsWith("'")))) {
            result = result.substring(1, result.length() - 1).trim();
        }
        return result;
    }

    private String buildUploadCacheKey(File file, String fileType) {
        return file.getAbsolutePath() + "|" + fileType + "|" + NEED_CONVERT_TO_PDF;
    }

    private void applyFileFields(ContractFileIds contractFileIds, ZhishuCreateContractRequest request) {
        request.setTextFileId(contractFileIds.getTextFileId());
        if (trimToNull(contractFileIds.getScanFileId()) != null) {
            request.setScanFileId(contractFileIds.getScanFileId());
        }
        if (!contractFileIds.getContractCauseFileIds().isEmpty()) {
            request.setContractCauseFileIdList(new ArrayList<>(contractFileIds.getContractCauseFileIds()));
        }
        if (!contractFileIds.getAttachmentFileIds().isEmpty()) {
            request.setAttachmentFileIdList(new ArrayList<>(contractFileIds.getAttachmentFileIds()));
        }
    }

    private Set<String> collectSplitValues(ContractGroup contractGroup, SheetRole sheetRole, String header) {
        Set<String> values = new LinkedHashSet<>();
        for (ExcelUtils.ExcelRowData row : contractGroup.getRowsByRole(sheetRole)) {
            for (Object value : row.getValues(header)) {
                values.addAll(splitMultiValue(value));
            }
        }
        return values;
    }

    private Set<String> collectSplitValues(ContractGroup contractGroup, String header) {
        Set<String> values = new LinkedHashSet<>();
        for (ExcelUtils.ExcelRowData row : contractGroup.getRows()) {
            for (Object value : row.getValues(header)) {
                values.addAll(splitMultiValue(value));
            }
        }
        return values;
    }

    private List<String> collectSplitValueList(ContractGroup contractGroup, String header) {
        List<String> values = new ArrayList<>();
        Set<String> deduplicatedValues = new LinkedHashSet<>();
        for (ExcelUtils.ExcelRowData row : contractGroup.getRows()) {
            for (Object value : row.getValues(header)) {
                for (String item : splitMultiValueToList(value)) {
                    if (deduplicatedValues.add(item)) {
                        values.add(item);
                    }
                }
            }
        }
        return values;
    }

    private List<String> splitMultiValueToList(Object value) {
        List<String> result = new ArrayList<>();
        String text = trimToNull(toStringValue(value));
        if (text == null) {
            return result;
        }
        String[] parts = text.split("[,，;；、\\r\\n]+");
        for (String part : parts) {
            String item = trimToNull(part);
            if (item != null) {
                result.add(item);
            }
        }
        return result;
    }

    private Set<String> splitMultiValue(Object value) {
        Set<String> result = new LinkedHashSet<>();
        String text = trimToNull(toStringValue(value));
        if (text == null) {
            return result;
        }
        String[] parts = text.split("[,，;；、\\r\\n]+");
        for (String part : parts) {
            String item = trimToNull(part);
            if (item != null) {
                result.add(item);
            }
        }
        return result;
    }

    private Object getFirstNonBlankValue(ContractGroup contractGroup, String header) {
        List<LocatedValue> values = contractGroup.findNonBlankValues(header);
        if (values.isEmpty()) {
            return null;
        }
        if (values.size() > 1) {
            LocatedValue first = values.get(0);
            for (int index = 1; index < values.size(); index++) {
                LocatedValue ignored = values.get(index);
                if (!isSameRawValue(first.getValue(), ignored.getValue())) {
                    log.warn("智书13Sheet历史合同字段存在多个非空值，contract_number={}，field={}，保留位置={}:{}，忽略位置={}:{}",
                            contractGroup.getContractNumber(), header, first.getSheetName(), first.getRowIndex(),
                            ignored.getSheetName(), ignored.getRowIndex());
                    break;
                }
            }
        }
        return values.get(0).getValue();
    }

    private String resolveExcelPath(String filePath) {
        String excelPath = trimToNull(filePath);
        return excelPath == null ? historyContractExcelPath : excelPath;
    }

    private Path resolveExcelDirectory(String excelPath) {
        Path excelFilePath = Paths.get(excelPath).toAbsolutePath().normalize();
        Path parent = excelFilePath.getParent();
        return parent == null ? Paths.get("").toAbsolutePath().normalize() : parent;
    }

    private String resolveCreateUserId(ContractGroup contractGroup) {
        Object excelValue = getFirstNonBlankValue(contractGroup, "create_user_id");
        String createUserId = trimToNull(toStringValue(excelValue));
        if (createUserId != null) {
            return createUserId;
        }
//        return yeCaiDataConfig == null ? null : trimToNull(yeCaiDataConfig.getUserId());
        return "149744414";
    }

    private Object convertValueForField(Object rawValue, Class<?> fieldType, String fieldName) {
        if (fieldType == String.class) {
            return trimToNull(toStringValue(rawValue));
        }
        if (fieldType == BigDecimal.class) {
            return toBigDecimal(rawValue);
        }
        if (fieldType == Integer.class) {
            return toInteger(rawValue, fieldName);
        }
        if (fieldType == Boolean.class) {
            return toBoolean(rawValue);
        }
        return null;
    }

    private Object buildFormAttributeValue(FormAttributeTypeEnum attributeType, Object rawValue) {
        if (attributeType == FormAttributeTypeEnum.COMMON_ARRAY || attributeType == FormAttributeTypeEnum.ARRAY) {
            return rawValue;
        }
        if (attributeType == FormAttributeTypeEnum.AMOUNT) {
            ContractFormCreatResponse.AmountValue amountValue = new ContractFormCreatResponse.AmountValue();
            amountValue.setAmount(toBigDecimal(rawValue));
            amountValue.setCurrency(DEFAULT_CURRENCY_CODE);
            amountValue.setCurrencyName("CNY-人民币元");
            return amountValue;
        }
        if (attributeType == FormAttributeTypeEnum.EMPLOYEE) {
            return buildEmployeeAttributeValueList(rawValue);
        }
        if (attributeType == FormAttributeTypeEnum.RADIO || attributeType == FormAttributeTypeEnum.DROPDOWN_RADIO
                || attributeType == FormAttributeTypeEnum.DROPDOWN_OPTION) {
            Map<String, Object> optionValue = new LinkedHashMap<>();
            optionValue.put("key", toStringValue(rawValue));
            optionValue.put("name", toStringValue(rawValue));
            return optionValue;
        }
        if (attributeType == FormAttributeTypeEnum.FEISHU_APPROVAL) {
            ContractFormCreatResponse.ApprovalValue approvalValue = new ContractFormCreatResponse.ApprovalValue();
            approvalValue.setContent(toStringValue(rawValue));
            approvalValue.setTitle(toStringValue(rawValue));
            return Collections.singletonList(approvalValue);
        }
        if (attributeType == FormAttributeTypeEnum.NUMBER) {
            BigDecimal decimal = toBigDecimal(rawValue);
            return decimal == null ? toStringValue(rawValue) : decimal.stripTrailingZeros().toPlainString();
        }
        if (attributeType == FormAttributeTypeEnum.DATE) {
            return toDateString(rawValue);
        }
        if (attributeType == FormAttributeTypeEnum.DATE_RANGE) {
            return buildHistoryDateRangeAttributeValue(rawValue);
        }
        return toStringValue(rawValue);
    }

    private FormAttributeTypeEnum inferAttributeType(String fieldCode) {
        if (fieldCode.startsWith("custom_1201_")) {
            return FormAttributeTypeEnum.COMMON_ARRAY;
        }
        if (fieldCode.startsWith("custom_1012_")) {
            return FormAttributeTypeEnum.AMOUNT;
        }
        if (fieldCode.startsWith("custom_1001_")) {
            return FormAttributeTypeEnum.EMPLOYEE;
        }
        if (fieldCode.startsWith("custom_15_") || fieldCode.startsWith("custom_16_")) {
            return FormAttributeTypeEnum.DROPDOWN_RADIO;
        }
        if (fieldCode.startsWith("custom_13_")) {
            return FormAttributeTypeEnum.RADIO;
        }
        if (fieldCode.startsWith("custom_10_")) {
            return FormAttributeTypeEnum.DATE;
        }
        if (fieldCode.startsWith("custom_12_")) {
            return FormAttributeTypeEnum.DATE_RANGE;
        }
        if (fieldCode.startsWith("custom_5_")) {
            return FormAttributeTypeEnum.NUMBER;
        }
        if (fieldCode.startsWith("custom_1024_")) {
            return FormAttributeTypeEnum.FEISHU_APPROVAL;
        }
        return FormAttributeTypeEnum.SINGLELINE_TEXT;
    }

    private ContractFormCreatResponse.FormAttribute buildContractFormAttribute(String attributeCode,
                                                                               String attributeType,
                                                                               Object attributeValue) {
        return buildContractFormAttribute(attributeCode, attributeCode, null, attributeType, null, attributeValue);
    }

    private ContractFormCreatResponse.FormAttribute buildContractFormAttribute(String attributeCode,
                                                                               String attributeKey,
                                                                               String moduleName,
                                                                               String attributeType,
                                                                               String approvalType,
                                                                               Object attributeValue) {
        ContractFormCreatResponse.FormAttribute formAttribute = new ContractFormCreatResponse.FormAttribute();
        formAttribute.setAttributeCode(attributeCode);
        formAttribute.setAttributeKey(attributeKey != null ? attributeKey : attributeCode);
        formAttribute.setModuleName(moduleName);
        formAttribute.setAttributeType(attributeType);
        formAttribute.setApprovalType(approvalType);
        formAttribute.setAttributeValue(buildContractFormAttributeValue(attributeType, attributeValue));
        return formAttribute;
    }

    private void addContractFormAttribute(ContractFormCreatResponse contractFormCreatResponse,
                                          String attributeCode,
                                          String attributeType,
                                          Object attributeValue) {
        if (contractFormCreatResponse == null || attributeCode == null || attributeCode.trim().isEmpty()
                || attributeValue == null) {
            return;
        }
        if (contractFormCreatResponse.getForm() == null) {
            contractFormCreatResponse.setForm(new ArrayList<>());
        }
        contractFormCreatResponse.getForm()
                .add(buildContractFormAttribute(attributeCode, attributeType, attributeValue));
    }

    private Object buildContractFormAttributeValue(String attributeType, Object attributeValue) {
        if (attributeValue == null || attributeType == null) {
            return attributeValue;
        }
        if (attributeType.equals(FormAttributeTypeEnum.SINGLELINE_TEXT.getCode())
                || attributeType.equals(FormAttributeTypeEnum.MULTILINE_TEXT.getCode())
                || attributeType.equals(FormAttributeTypeEnum.STRING.getCode())
                || attributeType.equals(FormAttributeTypeEnum.DATE.getCode())
                || attributeType.equals(FormAttributeTypeEnum.NUMBER.getCode())
                || attributeType.equals(FormAttributeTypeEnum.CALCULATION.getCode())) {
            return attributeValue.toString();
        }
        if (attributeType.equals(FormAttributeTypeEnum.AMOUNT.getCode())) {
            return buildAmountAttributeValue(attributeValue);
        }
        if (attributeType.equals(FormAttributeTypeEnum.DATE_RANGE.getCode())) {
            return buildDateRangeAttributeValue(attributeValue);
        }
        if (attributeType.equals(FormAttributeTypeEnum.RADIO.getCode())
                || attributeType.equals(FormAttributeTypeEnum.DROPDOWN_RADIO.getCode())
                || attributeType.equals(FormAttributeTypeEnum.TREE_RADIO.getCode())) {
            return buildKeyNameAttributeValue(attributeValue);
        }
        if (attributeType.equals(FormAttributeTypeEnum.CHECKBOX.getCode())
                || attributeType.equals(FormAttributeTypeEnum.DROPDOWN_OPTION.getCode())
                || attributeType.equals(FormAttributeTypeEnum.TREE_OPTION.getCode())) {
            return buildKeyNameAttributeValueList(attributeValue);
        }
        if (attributeType.equals(FormAttributeTypeEnum.EMPLOYEE.getCode())) {
            return buildEmployeeAttributeValueList(attributeValue);
        }
        if (attributeType.equals(FormAttributeTypeEnum.DEPARTMENT.getCode())) {
            return buildDepartmentAttributeValueList(attributeValue);
        }
        if (attributeType.equals(FormAttributeTypeEnum.HYPERLINK.getCode())) {
            return buildHyperlinkAttributeValue(attributeValue);
        }
        if (attributeType.equals(FormAttributeTypeEnum.COUNTRY_OR_REGION.getCode())) {
            return buildCountryOrRegionAttributeValue(attributeValue);
        }
        if (attributeType.equals(FormAttributeTypeEnum.ADDRESS.getCode())) {
            return buildAddressAttributeValue(attributeValue);
        }
        if (attributeType.equals(FormAttributeTypeEnum.FILE.getCode())) {
            return buildFileAttributeValueList(attributeValue);
        }
        if (attributeType.equals(FormAttributeTypeEnum.FEISHU_APPROVAL.getCode())
                || attributeType.equals(FormAttributeTypeEnum.ARRAY.getCode())
                || attributeType.equals(FormAttributeTypeEnum.COMMON_ARRAY.getCode())) {
            return Arrays.asList(attributeValue);
        }
        return attributeValue;
    }

    private ContractFormCreatResponse.AmountValue buildAmountAttributeValue(Object value) {
        if (value instanceof ContractFormCreatResponse.AmountValue) {
            return (ContractFormCreatResponse.AmountValue) value;
        }
        ContractFormCreatResponse.AmountValue amountValue = new ContractFormCreatResponse.AmountValue();
        Object amount = getMapOrBeanValue(value, "amount");
        Object currency = getMapOrBeanValue(value, "currency");
        Object currencyName = getMapOrBeanValue(value, "currency_name");
        if (amount == null) {
            amount = value;
        }
        if (amount != null && !String.valueOf(amount).trim().isEmpty()) {
            amountValue.setAmount(new BigDecimal(String.valueOf(amount)));
        }
        amountValue.setCurrency(currency == null ? "CNY" : String.valueOf(currency));
        amountValue.setCurrencyName(currencyName == null ? null : String.valueOf(currencyName));
        return amountValue;
    }

    private Map<String, Object> buildDateRangeAttributeValue(Object value) {
        Map<String, Object> dateRange = new LinkedHashMap<>();
        if (value instanceof List) {
            List<?> values = (List<?>) value;
            dateRange.put("start_date", values.size() > 0 ? values.get(0) : null);
            dateRange.put("end_date", values.size() > 1 ? values.get(1) : null);
            return dateRange;
        }
        Object startDate = getMapOrBeanValue(value, "start_date");
        Object endDate = getMapOrBeanValue(value, "end_date");
        if (startDate == null) {
            startDate = getMapOrBeanValue(value, "startDate");
        }
        if (endDate == null) {
            endDate = getMapOrBeanValue(value, "endDate");
        }
        dateRange.put("start_date", DateUtils.convertDateToString((Date) startDate, "yyyy-MM-dd"));
        dateRange.put("end_date", DateUtils.convertDateToString((Date) endDate, "yyyy-MM-dd"));
        return dateRange;
    }

    private Object buildKeyNameAttributeValue(Object value) {
        if (value instanceof Map || value instanceof ContractFormCreatResponse.TreeNodeValue
                || value instanceof ContractFormCreatResponse.OptionValue) {
            return value;
        }
        Map<String, Object> optionValue = new LinkedHashMap<>();
        optionValue.put("name", "");
        optionValue.put("key", value);
        return optionValue;
    }

    private Object buildKeyNameAttributeValueList(Object value) {
        if (value instanceof Map || value instanceof ContractFormCreatResponse.TreeNodeValue
                || value instanceof ContractFormCreatResponse.OptionValue) {
            return value;
        }
        Map<String, Object> optionValue = new LinkedHashMap<>();
        optionValue.put("key", Arrays.asList(value));
        return optionValue;
    }

    private List<ContractFormCreatResponse.EmployeeValue> buildEmployeeAttributeValueList(Object value) {
        List<ContractFormCreatResponse.EmployeeValue> values = new ArrayList<>();
        if (value instanceof CharSequence) {
            for (String userId : splitMultiValue(value)) {
                ContractFormCreatResponse.EmployeeValue employeeValue = new ContractFormCreatResponse.EmployeeValue();
                employeeValue.setUserId(userId);
                employeeValue.setUserIdType("lark_user_id");
                values.add(employeeValue);
            }
            return values;
        }
        for (Object item : toObjectList(value)) {
            ContractFormCreatResponse.EmployeeValue employeeValue = new ContractFormCreatResponse.EmployeeValue();
            Object userId = getMapOrBeanValue(item, "user_id");
            Object userIdType = getMapOrBeanValue(item, "user_id_type");
            if (userId == null) {
                userId = item;
            }
            String userIdText = userId == null ? null : trimToNull(String.valueOf(userId));
            if (userIdText == null) {
                continue;
            }
            employeeValue.setUserId(userIdText);
            employeeValue.setUserIdType(userIdType == null ? "lark_user_id" : String.valueOf(userIdType));
            values.add(employeeValue);
        }
        return values;
    }

    private List<ContractFormCreatResponse.DepartmentValue> buildDepartmentAttributeValueList(Object value) {
        List<ContractFormCreatResponse.DepartmentValue> values = new ArrayList<>();
        for (Object item : toObjectList(value)) {
            ContractFormCreatResponse.DepartmentValue departmentValue =
                    new ContractFormCreatResponse.DepartmentValue();
            Object departmentId = getMapOrBeanValue(item, "department_id");
            Object departmentIdType = getMapOrBeanValue(item, "department_id_type");
            Object openDepartmentId = getMapOrBeanValue(item, "open_department_id");
            Object larkDepartmentId = getMapOrBeanValue(item, "lark_department_id");
            if (departmentId == null) {
                departmentId = item;
            }
            departmentValue.setDepartmentId(departmentId == null ? null : String.valueOf(departmentId));
            departmentValue.setDepartmentIdType(
                    departmentIdType == null ? "department_id" : String.valueOf(departmentIdType));
            departmentValue.setOpenDepartmentId(openDepartmentId == null ? null : String.valueOf(openDepartmentId));
            departmentValue.setLarkDepartmentId(larkDepartmentId == null ? null : String.valueOf(larkDepartmentId));
            values.add(departmentValue);
        }
        return values;
    }

    private ContractFormCreatResponse.HyperlinkValue buildHyperlinkAttributeValue(Object value) {
        if (value instanceof ContractFormCreatResponse.HyperlinkValue) {
            return (ContractFormCreatResponse.HyperlinkValue) value;
        }
        ContractFormCreatResponse.HyperlinkValue hyperlinkValue = new ContractFormCreatResponse.HyperlinkValue();
        Object title = getMapOrBeanValue(value, "title");
        Object url = getMapOrBeanValue(value, "url");
        hyperlinkValue.setTitle(title == null ? null : String.valueOf(title));
        hyperlinkValue.setUrl(url == null ? String.valueOf(value) : String.valueOf(url));
        return hyperlinkValue;
    }

    private ContractFormCreatResponse.CountryOrRegionValue buildCountryOrRegionAttributeValue(Object value) {
        if (value instanceof ContractFormCreatResponse.CountryOrRegionValue) {
            return (ContractFormCreatResponse.CountryOrRegionValue) value;
        }
        ContractFormCreatResponse.CountryOrRegionValue countryOrRegionValue =
                new ContractFormCreatResponse.CountryOrRegionValue();
        Object countryCode = getMapOrBeanValue(value, "country_code");
        if (countryCode == null) {
            countryCode = value;
        }
        countryOrRegionValue.setCountryCode(countryCode == null ? null : String.valueOf(countryCode));
        return countryOrRegionValue;
    }

    private ContractFormCreatResponse.AddressValue buildAddressAttributeValue(Object value) {
        if (value instanceof ContractFormCreatResponse.AddressValue) {
            return (ContractFormCreatResponse.AddressValue) value;
        }
        ContractFormCreatResponse.AddressValue addressValue = new ContractFormCreatResponse.AddressValue();
        Object countryCode = getMapOrBeanValue(value, "country_code");
        Object regionCode = getMapOrBeanValue(value, "region_code");
        Object cityCode = getMapOrBeanValue(value, "city_code");
        Object address = getMapOrBeanValue(value, "address");
        addressValue.setCountryCode(countryCode == null ? null : String.valueOf(countryCode));
        addressValue.setRegionCode(regionCode == null ? null : String.valueOf(regionCode));
        addressValue.setCityCode(cityCode == null ? null : String.valueOf(cityCode));
        addressValue.setAddress(address == null ? null : String.valueOf(address));
        return addressValue;
    }

    private List<ContractFormCreatResponse.FileValue> buildFileAttributeValueList(Object value) {
        List<ContractFormCreatResponse.FileValue> values = new ArrayList<>();
        for (Object item : toObjectList(value)) {
            ContractFormCreatResponse.FileValue fileValue = new ContractFormCreatResponse.FileValue();
            Object fileId = getMapOrBeanValue(item, "file_id");
            Object fileName = getMapOrBeanValue(item, "file_name");
            Object fileSize = getMapOrBeanValue(item, "file_size");
            Object mime = getMapOrBeanValue(item, "mime");
            if (fileId == null) {
                fileId = item;
            }
            fileValue.setFileId(fileId == null ? null : String.valueOf(fileId));
            fileValue.setFileName(fileName == null ? null : String.valueOf(fileName));
            fileValue.setFileSize(fileSize == null ? null : String.valueOf(fileSize));
            fileValue.setMime(mime == null ? null : String.valueOf(mime));
            values.add(fileValue);
        }
        return values;
    }

    private List<Object> toObjectList(Object value) {
        if (value == null) {
            return Collections.emptyList();
        }
        if (value instanceof Collection) {
            return new ArrayList<>((Collection<?>) value);
        }
        return Collections.singletonList(value);
    }

    private Object getMapOrBeanValue(Object source, String fieldName) {
        if (source == null || fieldName == null) {
            return null;
        }
        if (source instanceof Map) {
            Object value = ((Map<?, ?>) source).get(fieldName);
            if (value == null) {
                value = ((Map<?, ?>) source).get(snakeToCamel(fieldName));
            }
            return value;
        }
        if (source instanceof JSONObject) {
            Object value = ((JSONObject) source).get(fieldName);
            return value == null ? ((JSONObject) source).get(snakeToCamel(fieldName)) : value;
        }
        return getBeanFieldValue(source, snakeToCamel(fieldName));
    }

    private Object getBeanFieldValue(Object bean, String fieldName) {
        if (bean == null || fieldName == null || fieldName.isEmpty()) {
            return null;
        }
        try {
            String methodName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            return bean.getClass().getMethod(methodName).invoke(bean);
        } catch (Exception e) {
            return null;
        }
    }

    private String snakeToCamel(String value) {
        StringBuilder builder = new StringBuilder();
        boolean upperNext = false;
        for (char c : value.toCharArray()) {
            if (c == '_') {
                upperNext = true;
            } else if (upperNext) {
                builder.append(Character.toUpperCase(c));
                upperNext = false;
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }

    private Map<String, Object> buildHistoryDateRangeAttributeValue(Object value) {
        List<String> dates = extractDateRange(value);
        Map<String, Object> dateRange = new LinkedHashMap<>();
        dateRange.put("start_date", dates.size() > 0 ? dates.get(0) : null);
        dateRange.put("end_date", dates.size() > 1 ? dates.get(1) : null);
        return dateRange;
    }

    private List<String> extractDateRange(Object value) {
        List<String> dates = new ArrayList<>();
        if (value == null) {
            return dates;
        }
        if (value instanceof Date) {
            dates.add(toDateString(value));
            return dates;
        }
        String text = trimToNull(toStringValue(value));
        if (text == null) {
            return dates;
        }
        Matcher matcher = DATE_PATTERN.matcher(text);
        while (matcher.find()) {
            dates.add(normalizeDateText(matcher.group()));
            if (dates.size() >= 2) {
                break;
            }
        }
        if (dates.isEmpty()) {
            dates.add(text);
        }
        return dates;
    }

    private String normalizeDateText(String value) {
        if (value == null) {
            return null;
        }
        String[] parts = value.replace('/', '-').split("-");
        if (parts.length != 3) {
            return value;
        }
        return parts[0] + "-" + leftPad2(parts[1]) + "-" + leftPad2(parts[2]);
    }

    private String normalizeAntiBriberyHeader(String header) {
        if (header == null) {
            return null;
        }
        String value = header.replace("\uFEFF", "").trim();
        int index = value.indexOf('(');
        if (index < 0) {
            index = value.indexOf('（');
        }
        if (index >= 0) {
            value = value.substring(0, index).trim();
        }
        return trimToNull(value);
    }

    private String normalizeRawExcelHeader(String header) {
        if (header == null) {
            return null;
        }
        return trimToNull(header.replace("\uFEFF", ""));
    }

    private String toAntiBriberyTimestampMillisString(Object value) {
        Date date = toAntiBriberyDate(value);
        if (date != null) {
            return String.valueOf(date.getTime());
        }
        return trimToNull(toStringValue(value));
    }

    private Date toAntiBriberyDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Date) {
            return (Date) value;
        }
        if (value instanceof Number) {
            return DateUtil.getJavaDate(((Number) value).doubleValue());
        }
        String text = trimToNull(toStringValue(value));
        if (text == null) {
            return null;
        }
        Matcher matcher = DATE_PATTERN.matcher(text);
        if (matcher.find()) {
            return parseNormalizedDate(normalizeDateText(matcher.group()));
        }
        BigDecimal decimal = toBigDecimal(text);
        if (decimal != null && decimal.compareTo(BigDecimal.valueOf(20000)) > 0) {
            return DateUtil.getJavaDate(decimal.doubleValue());
        }
        return null;
    }

    private Date parseNormalizedDate(String value) {
        String text = trimToNull(value);
        if (text == null) {
            return null;
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        dateFormat.setLenient(false);
        try {
            return dateFormat.parse(text);
        } catch (java.text.ParseException e) {
            return null;
        }
    }

    private String leftPad2(String value) {
        return value != null && value.length() == 1 ? "0" + value : value;
    }

    private String toDateString(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Date) {
            return new SimpleDateFormat("yyyy-MM-dd").format((Date) value);
        }
        return trimToNull(toStringValue(value));
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return new BigDecimal(String.valueOf(value));
        }
        String text = trimToNull(toStringValue(value));
        if (text == null) {
            return null;
        }
        text = text.replace(",", "").replace("，", "").replace("%", "");
        if (text.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer toInteger(Object value, String fieldName) {
        BigDecimal decimal = toBigDecimal(value);
        if (decimal != null) {
            return decimal.intValue();
        }
        String text = trimToNull(toStringValue(value));
        if (text == null) {
            return null;
        }
        if ("pay_type_code".equals(fieldName)) {
            return parsePayTypeCode(text);
        }
        if ("property_type_code".equals(fieldName)) {
            return parsePropertyTypeCode(text);
        }
        if ("fixed_validity_code".equals(fieldName)) {
            return parseFixedValidityCode(text);
        }
        if ("sign_type_code".equals(fieldName)) {
            return parseSignTypeCode(text);
        }
        if ("seal_number".equals(fieldName)) {
            return parseSealNumber(text);
        }
        return null;
    }

    private Integer parsePayTypeCode(String text) {
        if (text.contains("既收又支") || text.contains("收支")) {
            return 3;
        }
        if (text.contains("收入")) {
            return 1;
        }
        if (text.contains("支出")) {
            return 2;
        }
        if (text.contains("无金额")) {
            return 4;
        }
        return null;
    }

    private Integer parsePropertyTypeCode(String text) {
        if (text.contains("不固定总价")) {
            return 1;
        }
        if (text.contains("固定总价")) {
            return 0;
        }
        if (text.contains("无金额")) {
            return 2;
        }
        if (text.contains("其他")) {
            return 3;
        }
        return null;
    }

    private Integer parseFixedValidityCode(String text) {
        if (text.contains("无固定期限") || text.contains("不固定")) {
            return 2;
        }
        if (text.contains("固定期限") || text.contains("固定")) {
            return 1;
        }
        return null;
    }

    private Integer parseSignTypeCode(String text) {
        text = trimToNull(text);
        if (text == null) {
            return null;
        }
        BigDecimal decimal = toBigDecimal(text);
        if (decimal != null) {
            return decimal.intValue();
        }
        if (text.contains("纸质")) {
            return 2;
        }
        if (text.contains("电子") || text.contains("线上")) {
            return 2;
        }
        return null;
    }

    private Integer parseSignPartyNo(String text) {
        text = trimToNull(text);
        if (text == null) {
            return null;
        }
        BigDecimal decimal = toBigDecimal(text);
        if (decimal != null) {
            return decimal.intValue();
        }
        if (text.contains("不限制") || text.contains("不限")) {
            return 0;
        }
        if (text.contains("我方")) {
            return 1;
        }
        if (text.contains("对方")) {
            return 2;
        }
        return null;
    }

    private Integer parseSealNumber(String text) {
        if (text.contains("两")) {
            return 2;
        }
        Matcher matcher = DIGIT_PATTERN.matcher(text);
        if (matcher.find()) {
            return Integer.valueOf(matcher.group());
        }
        if (text.contains("一")) {
            return 1;
        }
        if (text.contains("三")) {
            return 3;
        }
        if (text.contains("四")) {
            return 4;
        }
        return null;
    }

    private Boolean toBoolean(Object value) {
        String text = trimToNull(toStringValue(value));
        if (text == null) {
            return null;
        }
        if ("1".equals(text) || "true".equalsIgnoreCase(text) || "是".equals(text)) {
            return true;
        }
        if ("2".equals(text) || "false".equalsIgnoreCase(text) || "否".equals(text)) {
            return false;
        }
        return null;
    }

    private String toStringValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Date) {
            return new SimpleDateFormat("yyyy-MM-dd").format((Date) value);
        }
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).stripTrailingZeros().toPlainString();
        }
        return String.valueOf(value);
    }

    private boolean isBlankValue(Object value) {
        return trimToNull(toStringValue(value)) == null;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimValue = value.trim();
        return trimValue.isEmpty() ? null : trimValue;
    }

    private String firstNotBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String trimValue = trimToNull(value);
            if (trimValue != null) {
                return trimValue;
            }
        }
        return null;
    }

    private String firstValue(Set<String> values) {
        for (String value : values) {
            return value;
        }
        return null;
    }

    private Set<String> normalizeContractNumbers(Collection<String> contractNumbers) {
        if (contractNumbers == null || contractNumbers.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> result = new LinkedHashSet<>();
        for (String contractNumber : contractNumbers) {
            String value = trimToNull(contractNumber);
            if (value != null) {
                result.add(value);
            }
        }
        return result;
    }

    private Set<String> resolveContractNumberFilter(HistoryContractSyncDTO request) {
        Set<String> contractNumbers = normalizeContractNumbers(request == null ? null : request.getContractNumbers());
        if (!contractNumbers.isEmpty()) {
            if (trimToNull(request.getContractNumberFilePath()) != null) {
                log.info("历史合同同步同时传入合同编码集合和txt文件地址，优先使用合同编码集合，txt文件地址={}",
                        request.getContractNumberFilePath());
            }
            return contractNumbers;
        }
        String contractNumberFilePath = request == null ? null : trimToNull(request.getContractNumberFilePath());
        if (contractNumberFilePath == null) {
            return Collections.emptySet();
        }
        return normalizeContractNumbers(readContractNumbersFromTxt(contractNumberFilePath));
    }

    private List<String> resolveYeCaiContractNumbers(YeCaiContractSyncDTO request) {
        List<String> contractNumbers = normalizeContractNumberList(request == null ? null : request.getContractNumbers());
        if (!contractNumbers.isEmpty()) {
            if (trimToNull(request.getContractNumberFilePath()) != null) {
                log.info("按智书合同编码同步业财同时传入合同编码集合和txt文件地址，优先使用合同编码集合，txt文件地址={}",
                        request.getContractNumberFilePath());
            }
            return contractNumbers;
        }
        String contractNumberFilePath = request == null ? null : trimToNull(request.getContractNumberFilePath());
        if (contractNumberFilePath == null) {
            return Collections.emptyList();
        }
        return normalizeContractNumberList(readContractNumbersFromTxt(contractNumberFilePath));
    }

    private List<String> normalizeContractNumberList(Collection<String> contractNumbers) {
        if (contractNumbers == null || contractNumbers.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (String contractNumber : contractNumbers) {
            String value = trimToNull(contractNumber == null ? null : contractNumber.replace("\uFEFF", ""));
            if (value != null) {
                result.add(value);
            }
        }
        return result;
    }

    private YeCaiContractSyncResultDTO.Item syncOneYeCaiContract(int index, String contractNumber) {
        YeCaiContractSyncResultDTO.Item item = buildYeCaiSyncItem(index, contractNumber);
        try {
            ContractsSearchRequest request = new ContractsSearchRequest();
            request.setContractNumber(contractNumber);
            ContractsSearchResponse response = zhishuContractClient.searchContracts(request);
            ContractQueryResponse contract = getFirstContract(response);
            if (contract == null) {
                failYeCaiSyncItem(item, "智书合同查询无结果");
                log.error("按智书合同编码同步业财未查询到智书合同，contractNumber={}", contractNumber);
                return item;
            }
            Long contractId = contract.getContractId();
            if (contractId == null) {
                failYeCaiSyncItem(item, "智书合同查询结果缺少合同主键");
                log.error("按智书合同编码同步业财查询结果缺少合同主键，contractNumber={}", contractNumber);
                return item;
            }
            item.setZhishuContractId(String.valueOf(contractId));
            ContractSyncDTO dto = new ContractSyncDTO();
            dto.setContractId(String.valueOf(contractId));
            contractService.syncContractFromZhishu(dto);
            successYeCaiSyncItem(item);
            log.info("按智书合同编码同步业财成功，contractNumber={}，contractId={}", contractNumber, contractId);
        } catch (Exception e) {
            String errorMessage = buildExceptionMessage(e);
            failYeCaiSyncItem(item, errorMessage);
            log.error("按智书合同编码同步业财异常，contractNumber={}，错误={}", contractNumber, errorMessage, e);
        } finally {
            item.setEndTime(LocalDateTime.now());
        }
        return item;
    }

    private ContractQueryResponse getFirstContract(ContractsSearchResponse response) {
        if (response == null || response.getData() == null
                || response.getData().getItems() == null
                || response.getData().getItems().isEmpty()) {
            return null;
        }
        return response.getData().getItems().get(0);
    }

    private YeCaiContractSyncResultDTO.Item buildYeCaiSyncItem(int index, String contractNumber) {
        YeCaiContractSyncResultDTO.Item item = new YeCaiContractSyncResultDTO.Item();
        item.setIndex(index);
        item.setContractNumber(contractNumber);
        item.setStartTime(LocalDateTime.now());
        return item;
    }

    private void successYeCaiSyncItem(YeCaiContractSyncResultDTO.Item item) {
        item.setResult(YECAI_SYNC_SUCCESS);
        item.setErrorMessage("");
    }

    private void failYeCaiSyncItem(YeCaiContractSyncResultDTO.Item item, String errorMessage) {
        item.setResult(YECAI_SYNC_FAIL);
        item.setErrorMessage(errorMessage);
        if (item.getEndTime() == null) {
            item.setEndTime(LocalDateTime.now());
        }
    }

    private void finishYeCaiContractSyncResult(YeCaiContractSyncResultDTO result, long startTime) {
        result.setElapsedMillis(System.currentTimeMillis() - startTime);
        result.refreshCount();
    }

    private String buildExceptionMessage(Exception exception) {
        if (exception == null) {
            return "";
        }
        String message = exception.getMessage();
        return message == null || message.trim().isEmpty()
                ? exception.getClass().getSimpleName() : message;
    }

    private List<String> readContractNumbersFromTxt(String contractNumberFilePath) {
        try {
            List<String> lines = Files.readAllLines(Paths.get(contractNumberFilePath), StandardCharsets.UTF_8);
            List<String> contractNumbers = new ArrayList<>();
            for (String line : lines) {
                String contractNumber = trimToNull(line == null ? null : line.replace("\uFEFF", ""));
                if (contractNumber != null) {
                    contractNumbers.add(contractNumber);
                }
            }
            log.info("读取历史合同同步合同编码txt完成，filePath={}，合同数={}", contractNumberFilePath, contractNumbers.size());
            return contractNumbers;
        } catch (Exception e) {
            throw new RuntimeException("读取合同编码txt失败：" + contractNumberFilePath + "，错误：" + e.getMessage(), e);
        }
    }

    private SyncContext buildSyncContext(HistoryContractSyncDTO request) {
        SyncContext context = new SyncContext();
        context.setContractFileFallbackRoot(resolveContractFileFallbackRoot(request));
        context.setContractStatusCode(resolveContractStatusCode(request));
        return context;
    }

    private Path resolveContractFileFallbackRoot(HistoryContractSyncDTO request) {
        String root = request == null ? null : trimToNull(request.getContractFileFallbackRoot());
        return Paths.get(root == null ? DEFAULT_CONTRACT_FILE_FALLBACK_ROOT : root);
    }

    private String resolveContractStatusCode(HistoryContractSyncDTO request) {
        String contractStatusCode = request == null ? null : trimToNull(request.getContractStatusCode());
        return contractStatusCode == null ? DEFAULT_HISTORY_CONTRACT_STATUS_CODE : contractStatusCode;
    }

    private int resolvePositiveInteger(Integer value, int defaultValue) {
        return value == null || value <= 0 ? defaultValue : value;
    }

    private List<List<String>> splitContractNumberBatches(List<String> contractNumbers, int batchSize) {
        if (contractNumbers == null || contractNumbers.isEmpty()) {
            return Collections.emptyList();
        }
        List<List<String>> batches = new ArrayList<>();
        for (int start = 0; start < contractNumbers.size(); start += batchSize) {
            int end = Math.min(start + batchSize, contractNumbers.size());
            batches.add(new ArrayList<>(contractNumbers.subList(start, end)));
        }
        return batches;
    }

    private HistoryContractSyncDTO copyHistorySyncRequestForBatch(HistoryContractSyncDTO source,
                                                                  List<String> contractNumbers) {
        HistoryContractSyncDTO target = new HistoryContractSyncDTO();
        target.setContractNumbers(contractNumbers);
        target.setFilePath(source.getFilePath());
        target.setExcelPath(source.getExcelPath());
        target.setContractFileFallbackRoot(source.getContractFileFallbackRoot());
        target.setContractStatusCode(source.getContractStatusCode());
        return target;
    }

    private void mergeHistorySyncResult(HistoryContractSyncResultDTO mergedResult,
                                        HistoryContractSyncResultDTO batchResult) {
        if (batchResult == null) {
            return;
        }
        if (batchResult.getSuccessContractNumbers() != null) {
            for (String contractNumber : batchResult.getSuccessContractNumbers()) {
                mergedResult.addSuccess(contractNumber);
            }
        }
        if (batchResult.getFailures() != null) {
            for (HistoryContractSyncResultDTO.Failure failure : batchResult.getFailures()) {
                if (failure == null) {
                    continue;
                }
                mergedResult.addFailure(failure.getContractNumber(), failure.getFlowType(), failure.getReason());
            }
        }
    }

    private void copyHistorySyncFailures(HistoryContractValidateResultDTO target,
                                         HistoryContractSyncResultDTO source) {
        if (source == null || source.getFailures() == null) {
            return;
        }
        for (HistoryContractSyncResultDTO.Failure failure : source.getFailures()) {
            if (failure == null) {
                continue;
            }
            target.addFailure(failure.getContractNumber(), failure.getFlowType(), failure.getReason());
        }
    }

    private void mergeHistoryValidateResult(HistoryContractValidateResultDTO mergedResult,
                                            HistoryContractValidateResultDTO batchResult) {
        if (batchResult == null) {
            return;
        }
        if (batchResult.getSuccessContractNumbers() != null) {
            for (String contractNumber : batchResult.getSuccessContractNumbers()) {
                mergedResult.addSuccess(contractNumber);
            }
        }
        if (batchResult.getFailures() != null) {
            for (HistoryContractValidateResultDTO.Failure failure : batchResult.getFailures()) {
                if (failure == null) {
                    continue;
                }
                mergedResult.addFailure(failure.getContractNumber(), failure.getFlowType(), failure.getErrors());
            }
        }
    }

    private void finishHistoryMultiThreadResult(HistoryContractSyncResultDTO result, long startTime) {
        result.setElapsedMillis(System.currentTimeMillis() - startTime);
        result.refreshTotalCount();
    }

    private void finishHistoryValidateResult(HistoryContractValidateResultDTO result, long startTime) {
        result.setElapsedMillis(System.currentTimeMillis() - startTime);
        result.refreshTotalCount();
    }

    private boolean isSameConvertedValue(Object first, Object second) {
        if (first == null || second == null) {
            return first == second;
        }
        if (first instanceof BigDecimal && second instanceof BigDecimal) {
            return ((BigDecimal) first).compareTo((BigDecimal) second) == 0;
        }
        return String.valueOf(first).equals(String.valueOf(second));
    }

    private boolean isSameRawValue(Object first, Object second) {
        return isSameConvertedValue(toStringValue(first), toStringValue(second));
    }

    private String getCreatedContractId(ZhishuCreateContractResponse response) {
        if (response == null || response.getData() == null || response.getData().getContract() == null) {
            return null;
        }
        return response.getData().getContract().getContractId();
    }

    private String getCreatedContractNumber(ZhishuCreateContractResponse response) {
        if (response == null || response.getData() == null || response.getData().getContract() == null) {
            return null;
        }
        return response.getData().getContract().getContractNumber();
    }

    private static String buildGroupKey(FlowType flowType, String contractNumber) {
        return flowType.name() + ":" + contractNumber;
    }

    private static Map<String, Field> buildCreateRequestFields() {
        Map<String, Field> fields = new LinkedHashMap<>();
        Class<?> current = ZhishuCreateContractRequest.class;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                fields.put(field.getName(), field);
                JSONField jsonField = field.getAnnotation(JSONField.class);
                if (jsonField != null && jsonField.name() != null && !jsonField.name().trim().isEmpty()) {
                    fields.put(jsonField.name(), field);
                }
            }
            current = current.getSuperclass();
        }
        return Collections.unmodifiableMap(fields);
    }

    private enum FlowType {
        GENERAL("一般流程"),
        ANCHOR("主播流程");

        private final String desc;

        FlowType(String desc) {
            this.desc = desc;
        }

        private String getDesc() {
            return desc;
        }

        private SheetRole getOurPartyRole() {
            return this == GENERAL ? SheetRole.GENERAL_OUR_PARTY : SheetRole.ANCHOR_OUR_PARTY;
        }

        private SheetRole getCounterPartyRole() {
            return this == GENERAL ? SheetRole.GENERAL_COUNTER_PARTY : SheetRole.ANCHOR_COUNTER_PARTY;
        }
    }

    private enum SheetRole {
        GENERAL_MAIN(0, FlowType.GENERAL, "一般流程主表", false),
        GENERAL_RELATION(1, FlowType.GENERAL, "关联合同", false),
        GENERAL_ORDER_INFO(2, FlowType.GENERAL, "相关订单-订单信息", false),
        GENERAL_PURCHASE_REQUEST(3, FlowType.GENERAL, "采购申请", false),
        GENERAL_ORDER_DETAIL(4, FlowType.GENERAL, "订单信息明细", true),
        GENERAL_COUNTER_PARTY(5, FlowType.GENERAL, "对方信息", false),
        GENERAL_OUR_PARTY(6, FlowType.GENERAL, "我方主体列表", false),
        GENERAL_PAYMENT_PLAN(7, FlowType.GENERAL, "付款计划", false),
        GENERAL_COLLECTION_PLAN(8, FlowType.GENERAL, "收款计划", false),
        ANCHOR_MAIN(9, FlowType.ANCHOR, "主播流程主表", false),
        ANCHOR_COUNTER_PARTY(10, FlowType.ANCHOR, "主播流程_对方信息", false),
        ANCHOR_OUR_PARTY(11, FlowType.ANCHOR, "主播流程_我方信息", false),
        ANCHOR_FEE_DETAIL(12, FlowType.ANCHOR, "主播流程_费用明细", true);

        private final int sheetIndex;
        private final FlowType flowType;
        private final String displayName;
        private final boolean detailRole;

        SheetRole(int sheetIndex, FlowType flowType, String displayName, boolean detailRole) {
            this.sheetIndex = sheetIndex;
            this.flowType = flowType;
            this.displayName = displayName;
            this.detailRole = detailRole;
        }

        private static SheetRole fromSheetIndex(int sheetIndex) {
            for (SheetRole sheetRole : values()) {
                if (sheetRole.sheetIndex == sheetIndex) {
                    return sheetRole;
                }
            }
            return null;
        }

        private FlowType getFlowType() {
            return flowType;
        }

        private String getDisplayName() {
            return displayName;
        }

        private boolean isDetailRole() {
            return detailRole;
        }
    }

    private static class ContractIndex {
        private final Map<String, ContractIndexEntry> entriesByGroupKey = new LinkedHashMap<>();
        private final Map<String, List<ContractIndexEntry>> entriesByContractNumber = new LinkedHashMap<>();
        private final Map<String, Integer> sheetRowCounts = new LinkedHashMap<>();
        private long nextOrder;

        private ContractIndexEntry getOrCreate(FlowType flowType, String contractNumber) {
            String groupKey = buildGroupKey(flowType, contractNumber);
            ContractIndexEntry entry = entriesByGroupKey.get(groupKey);
            if (entry != null) {
                return entry;
            }
            entry = new ContractIndexEntry(flowType, contractNumber, groupKey, nextOrder++);
            entriesByGroupKey.put(groupKey, entry);
            List<ContractIndexEntry> entries = entriesByContractNumber.get(contractNumber);
            if (entries == null) {
                entries = new ArrayList<>();
                entriesByContractNumber.put(contractNumber, entries);
            }
            entries.add(entry);
            return entry;
        }

        private void recordSheetRow(int sheetIndex, String sheetName) {
            String key = sheetIndex + ":" + sheetName;
            Integer count = sheetRowCounts.get(key);
            sheetRowCounts.put(key, count == null ? 1 : count + 1);
        }

        private ContractIndexEntry getEntry(String groupKey) {
            return entriesByGroupKey.get(groupKey);
        }

        private Map<String, ContractIndexEntry> getEntriesByGroupKey() {
            return entriesByGroupKey;
        }

        private List<ContractIndexEntry> getEntriesByContractNumber(String contractNumber) {
            List<ContractIndexEntry> entries = entriesByContractNumber.get(contractNumber);
            return entries == null ? Collections.emptyList() : entries;
        }

        private Map<String, Integer> getSheetRowCounts() {
            return sheetRowCounts;
        }
    }

    private static class ContractIndexEntry {
        private final FlowType flowType;
        private final String contractNumber;
        private final String groupKey;
        @SuppressWarnings("unused")
        private final long firstOrder;
        private final Set<String> relationContractNumbers = new LinkedHashSet<>();

        private ContractIndexEntry(FlowType flowType, String contractNumber, String groupKey, long firstOrder) {
            this.flowType = flowType;
            this.contractNumber = contractNumber;
            this.groupKey = groupKey;
            this.firstOrder = firstOrder;
        }

        private FlowType getFlowType() {
            return flowType;
        }

        private String getContractNumber() {
            return contractNumber;
        }

        private String getGroupKey() {
            return groupKey;
        }

        private Set<String> getRelationContractNumbers() {
            return relationContractNumbers;
        }

        private void addRelationContractNumbers(Set<String> relationContractNumbers) {
            this.relationContractNumbers.addAll(relationContractNumbers);
        }
    }

    private static class ContractSyncOrder {
        private final List<String> orderedGroupKeys = new ArrayList<>();
        private final Set<String> orderedGroupKeySet = new LinkedHashSet<>();
        private final Map<String, String> failureReasonsByGroupKey = new LinkedHashMap<>();

        private void addOrderedGroupKey(String groupKey) {
            if (orderedGroupKeySet.add(groupKey)) {
                orderedGroupKeys.add(groupKey);
            }
        }

        private void addFailure(String groupKey, String reason) {
            if (!failureReasonsByGroupKey.containsKey(groupKey)) {
                failureReasonsByGroupKey.put(groupKey, reason);
            }
        }

        private boolean hasFailure(String groupKey) {
            return failureReasonsByGroupKey.containsKey(groupKey);
        }

        private List<String> getOrderedGroupKeys() {
            return orderedGroupKeys;
        }

        private Map<String, String> getFailureReasonsByGroupKey() {
            return failureReasonsByGroupKey;
        }
    }

    private static class CounterPartyCodeLookup {
        private final Map<String, String> codeMapping = new LinkedHashMap<>();

        private void addVendor(VenderRes venderRes) {
            if (venderRes == null) {
                return;
            }
            String venderCode = trimStatic(venderRes.getVenderCode());
            if (venderCode == null) {
                return;
            }
//            if("V-C-CN-SP-TPS-0012".equals(venderCode)){
//                System.out.println(venderRes.getCustomerCode());
//            }
            String customerCode = trimStatic(venderRes.getCustomerCode());
            String targetCode = customerCode == null ? venderCode : venderCode + ";" + customerCode;
            putMapping(venderCode, targetCode, "供应商");
        }

        private void addCustomer(CustomerRes customerRes) {
            if (customerRes == null) {
                return;
            }
            String customerCode = trimStatic(customerRes.getCustomerCode());
            if (customerCode == null) {
                return;
            }
            String venderCode = trimStatic(customerRes.getVenderCode());
            String targetCode = venderCode == null ? customerCode : venderCode + ";" + customerCode;
            putMapping(customerCode, targetCode, "客户");
        }

        private String resolve(String sourceCode) {
            String normalizedCode = trimStatic(sourceCode);
            if (normalizedCode == null) {
                return null;
            }
            String targetCode = codeMapping.get(normalizedCode);
            return targetCode == null ? normalizedCode : targetCode;
        }

        private int size() {
            return codeMapping.size();
        }

        private void putMapping(String sourceCode, String targetCode, String dataType) {
            String normalizedSourceCode = trimStatic(sourceCode);
            String normalizedTargetCode = trimStatic(targetCode);
            if (normalizedSourceCode == null || normalizedTargetCode == null) {
                return;
            }
            String oldTargetCode = codeMapping.get(normalizedSourceCode);
            if (oldTargetCode == null) {
                codeMapping.put(normalizedSourceCode, normalizedTargetCode);
                return;
            }
            if (!oldTargetCode.equals(normalizedTargetCode)) {
                log.warn("业财交易方编码存在多条不同映射，编码：{}，保留映射：{}，忽略映射：{}，来源类型：{}",
                        normalizedSourceCode, oldTargetCode, normalizedTargetCode, dataType);
            }
        }

        private static String trimStatic(String value) {
            if (value == null) {
                return null;
            }
            String result = value.trim();
            return result.isEmpty() ? null : result;
        }
    }

    private static class SyncContext {
        private final HistoryContractSyncResultDTO result = new HistoryContractSyncResultDTO();
        private final Set<String> targetKeys = new LinkedHashSet<>();
        private final Set<String> visitingKeys = new LinkedHashSet<>();
        private final Set<String> successKeys = new LinkedHashSet<>();
        private final Map<String, String> failureReasonsByKey = new LinkedHashMap<>();
        private final Map<String, String> uploadedFileIdsByKey = new LinkedHashMap<>();
        private final Map<String, String> contractCategoryAbbreviationsByValue = new LinkedHashMap<>();
        private CounterPartyCodeLookup counterPartyCodeLookup = new CounterPartyCodeLookup();
        private List<ContractCategoryNode> contractCategoryNodes;
        private Map<String, ContractGroup> contractGroupMap = Collections.emptyMap();
        private Map<String, List<ContractGroup>> groupsByContractNumber = Collections.emptyMap();
        private Path contractFileFallbackRoot = Paths.get(DEFAULT_CONTRACT_FILE_FALLBACK_ROOT);
        private String contractStatusCode = DEFAULT_HISTORY_CONTRACT_STATUS_CODE;

        private HistoryContractSyncResultDTO getResult() {
            return result;
        }

        private Set<String> getTargetKeys() {
            return targetKeys;
        }

        private void setContractGroupMap(Map<String, ContractGroup> contractGroupMap) {
            this.contractGroupMap = contractGroupMap;
        }

        private void setGroupsByContractNumber(Map<String, List<ContractGroup>> groupsByContractNumber) {
            this.groupsByContractNumber = groupsByContractNumber;
        }

        private Map<String, List<ContractGroup>> getGroupsByContractNumber() {
            return groupsByContractNumber;
        }

        private CounterPartyCodeLookup getCounterPartyCodeLookup() {
            return counterPartyCodeLookup;
        }

        private void setCounterPartyCodeLookup(CounterPartyCodeLookup counterPartyCodeLookup) {
            this.counterPartyCodeLookup = counterPartyCodeLookup == null
                    ? new CounterPartyCodeLookup() : counterPartyCodeLookup;
        }

        private Path getContractFileFallbackRoot() {
            return contractFileFallbackRoot;
        }

        private void setContractFileFallbackRoot(Path contractFileFallbackRoot) {
            this.contractFileFallbackRoot = contractFileFallbackRoot == null
                    ? Paths.get(DEFAULT_CONTRACT_FILE_FALLBACK_ROOT) : contractFileFallbackRoot;
        }

        private String getContractStatusCode() {
            return contractStatusCode;
        }

        private void setContractStatusCode(String contractStatusCode) {
            String value = contractStatusCode == null ? null : contractStatusCode.trim();
            this.contractStatusCode = value == null || value.isEmpty()
                    ? DEFAULT_HISTORY_CONTRACT_STATUS_CODE : value;
        }

        private void addTarget(ContractGroup contractGroup) {
            targetKeys.add(contractGroup.getGroupKey());
        }

        private List<ContractGroup> getTargetGroupsSnapshot() {
            List<ContractGroup> groups = new ArrayList<>();
            for (String targetKey : targetKeys) {
                ContractGroup contractGroup = contractGroupMap.get(targetKey);
                if (contractGroup != null) {
                    groups.add(contractGroup);
                }
            }
            return groups;
        }

        private boolean hasResult(String groupKey) {
            return successKeys.contains(groupKey) || failureReasonsByKey.containsKey(groupKey);
        }

        private boolean hasFailure(String groupKey) {
            return failureReasonsByKey.containsKey(groupKey);
        }

        private String getUploadedFileId(String cacheKey) {
            return uploadedFileIdsByKey.get(cacheKey);
        }

        private void putUploadedFileId(String cacheKey, String fileId) {
            uploadedFileIdsByKey.put(cacheKey, fileId);
        }

        private String getContractCategoryAbbreviation(String contractCategory) {
            return contractCategoryAbbreviationsByValue.get(contractCategory);
        }

        private void putContractCategoryAbbreviation(String contractCategory, String abbreviation) {
            contractCategoryAbbreviationsByValue.put(contractCategory, abbreviation);
        }

        private List<ContractCategoryNode> getContractCategoryNodes() {
            return contractCategoryNodes;
        }

        private void setContractCategoryNodes(List<ContractCategoryNode> contractCategoryNodes) {
            this.contractCategoryNodes = contractCategoryNodes;
        }

        private boolean isVisiting(String groupKey) {
            return visitingKeys.contains(groupKey);
        }

        private void markVisiting(String groupKey) {
            visitingKeys.add(groupKey);
        }

        private void clearVisiting(String groupKey) {
            visitingKeys.remove(groupKey);
        }

        private void recordSuccess(ContractGroup contractGroup) {
            if (hasResult(contractGroup.getGroupKey())) {
                return;
            }
            successKeys.add(contractGroup.getGroupKey());
            result.addSuccess(contractGroup.getContractNumber());
        }

        private void recordFailure(ContractGroup contractGroup, String reason) {
            if (hasResult(contractGroup.getGroupKey())) {
                return;
            }
            failureReasonsByKey.put(contractGroup.getGroupKey(), reason);
            result.addFailure(contractGroup.getContractNumber(), contractGroup.getFlowType().getDesc(), reason);
        }

        private void recordFailure(ContractIndexEntry entry, String reason) {
            if (hasResult(entry.getGroupKey())) {
                return;
            }
            failureReasonsByKey.put(entry.getGroupKey(), reason);
            result.addFailure(entry.getContractNumber(), entry.getFlowType().getDesc(), reason);
        }

        private void recordMissingFailure(String contractNumber, String reason) {
            String failureKey = "MISSING:" + contractNumber;
            if (failureReasonsByKey.containsKey(failureKey)) {
                return;
            }
            failureReasonsByKey.put(failureKey, reason);
            result.addFailure(contractNumber, null, reason);
        }
    }

    private static class ContractCategoryNode {
        private final String name;
        private final String number;
        private final String abbreviation;
        private final String id;

        private ContractCategoryNode(String name, String number, String abbreviation, String id) {
            this.name = trimStatic(name);
            this.number = trimStatic(number);
            this.abbreviation = trimStatic(abbreviation);
            this.id = trimStatic(id);
        }

        private boolean matches(String value) {
            String target = trimStatic(value);
            if (target == null) {
                return false;
            }
            return target.equals(name) || target.equals(number) || target.equals(abbreviation);
        }

        private String getName() {
            return name;
        }

        private String getNumber() {
            return number;
        }

        private String getAbbreviation() {
            return abbreviation;
        }

        @SuppressWarnings("unused")
        private String getId() {
            return id;
        }

        private static String trimStatic(String value) {
            if (value == null) {
                return null;
            }
            String result = value.trim();
            return result.isEmpty() ? null : result;
        }
    }

    private static class HeaderColumn {
        private final int columnIndex;
        private final String header;

        private HeaderColumn(int columnIndex, String header) {
            this.columnIndex = columnIndex;
            this.header = header;
        }

        private int getColumnIndex() {
            return columnIndex;
        }

        private String getHeader() {
            return header;
        }
    }

    private static class ContractFileIds {
        private boolean success = true;
        private String failureReason;
        private String textFileId;
        private String scanFileId;
        private final List<String> contractCauseFileIds = new ArrayList<>();
        private final List<String> attachmentFileIds = new ArrayList<>();

        private static ContractFileIds fail(String failureReason) {
            ContractFileIds result = new ContractFileIds();
            result.success = false;
            result.failureReason = failureReason;
            return result;
        }

        private boolean isSuccess() {
            return success;
        }

        private String getFailureReason() {
            return failureReason;
        }

        private String getTextFileId() {
            return textFileId;
        }

        private void setTextFileId(String textFileId) {
            this.textFileId = textFileId;
        }

        private String getScanFileId() {
            return scanFileId;
        }

        private void setScanFileId(String scanFileId) {
            this.scanFileId = scanFileId;
        }

        private List<String> getContractCauseFileIds() {
            return contractCauseFileIds;
        }

        private void addContractCauseFileId(String fileId) {
            contractCauseFileIds.add(fileId);
        }

        private List<String> getAttachmentFileIds() {
            return attachmentFileIds;
        }

        private void addAttachmentFileId(String fileId) {
            attachmentFileIds.add(fileId);
        }
    }

    private static class AntiBriberyContractFiles {
        private final Path fallbackDirectory;
        private final Path mainDirectory;
        private final Path scanDirectory;
        private final List<File> mainFiles;
        private final List<File> scanFiles;
        private final List<File> attachmentFiles;

        private AntiBriberyContractFiles(Path fallbackDirectory,
                                         Path mainDirectory,
                                         Path scanDirectory,
                                         List<File> mainFiles,
                                         List<File> scanFiles,
                                         List<File> attachmentFiles) {
            this.fallbackDirectory = fallbackDirectory;
            this.mainDirectory = mainDirectory;
            this.scanDirectory = scanDirectory;
            this.mainFiles = mainFiles;
            this.scanFiles = scanFiles;
            this.attachmentFiles = attachmentFiles;
        }

        private Path getFallbackDirectory() {
            return fallbackDirectory;
        }

        private Path getMainDirectory() {
            return mainDirectory;
        }

        private Path getScanDirectory() {
            return scanDirectory;
        }

        private List<File> getMainFiles() {
            return mainFiles;
        }

        private List<File> getScanFiles() {
            return scanFiles;
        }

        private List<File> getAttachmentFiles() {
            return attachmentFiles;
        }
    }

    private static class ContractGroup {
        private final FlowType flowType;
        private final String contractNumber;
        private final List<ExcelUtils.ExcelRowData> rows = new ArrayList<>();
        private final Map<SheetRole, List<ExcelUtils.ExcelRowData>> rowsByRole = new LinkedHashMap<>();

        private ContractGroup(FlowType flowType, String contractNumber) {
            this.flowType = flowType;
            this.contractNumber = contractNumber;
        }

        private void addRow(SheetRole sheetRole, ExcelUtils.ExcelRowData row) {
            rows.add(row);
            List<ExcelUtils.ExcelRowData> roleRows = rowsByRole.get(sheetRole);
            if (roleRows == null) {
                roleRows = new ArrayList<>();
                rowsByRole.put(sheetRole, roleRows);
            }
            roleRows.add(row);
        }

        private String getGroupKey() {
            return buildGroupKey(flowType, contractNumber);
        }

        private FlowType getFlowType() {
            return flowType;
        }

        private String getContractNumber() {
            return contractNumber;
        }

        private List<ExcelUtils.ExcelRowData> getRows() {
            return rows;
        }

        private Map<SheetRole, List<ExcelUtils.ExcelRowData>> getRowsByRole() {
            return rowsByRole;
        }

        private List<ExcelUtils.ExcelRowData> getRowsByRole(SheetRole sheetRole) {
            List<ExcelUtils.ExcelRowData> roleRows = rowsByRole.get(sheetRole);
            return roleRows == null ? Collections.emptyList() : roleRows;
        }

        private Set<String> getHeaders() {
            Set<String> headers = new LinkedHashSet<>();
            for (ExcelUtils.ExcelRowData row : rows) {
                headers.addAll(row.getCellDataByHeader().keySet());
            }
            return headers;
        }

        private Set<String> getRelationContractNumbers() {
            Set<String> relationContractNumbers = new LinkedHashSet<>();
            for (ExcelUtils.ExcelRowData row : getRowsByRole(SheetRole.GENERAL_RELATION)) {
                for (Object value : row.getValues(FIELD_RELATION_CONTRACTS)) {
                    String text = value == null ? null : String.valueOf(value).trim();
                    if (text != null && !text.isEmpty()) {
                        String[] parts = text.split("[,，;；、\\r\\n]+");
                        for (String part : parts) {
                            String item = part == null ? null : part.trim();
                            if (item != null && !item.isEmpty()) {
                                relationContractNumbers.add(item);
                            }
                        }
                    }
                }
            }
            return relationContractNumbers;
        }

        private List<LocatedValue> findNonBlankValues(String header) {
            List<LocatedValue> values = new ArrayList<>();
            for (ExcelUtils.ExcelRowData row : rows) {
                List<ExcelUtils.ExcelCellData> cellDataList = row.getCellDataByHeader().get(header);
                if (cellDataList == null) {
                    continue;
                }
                for (ExcelUtils.ExcelCellData cellData : cellDataList) {
                    if (cellData.getValue() != null && !String.valueOf(cellData.getValue()).trim().isEmpty()) {
                        values.add(new LocatedValue(row.getSheetName(), row.getRowIndex(),
                                cellData.getColumnIndex(), header, cellData.getValue()));
                    }
                }
            }
            return values;
        }
    }

    private static class LocatedValue {
        private final String sheetName;
        private final int rowIndex;
        private final int columnIndex;
        private final String header;
        private final Object value;

        private LocatedValue(String sheetName, int rowIndex, int columnIndex, String header, Object value) {
            this.sheetName = sheetName;
            this.rowIndex = rowIndex;
            this.columnIndex = columnIndex;
            this.header = header;
            this.value = value;
        }

        private String getSheetName() {
            return sheetName;
        }

        private int getRowIndex() {
            return rowIndex;
        }

        @SuppressWarnings("unused")
        private int getColumnIndex() {
            return columnIndex;
        }

        @SuppressWarnings("unused")
        private String getHeader() {
            return header;
        }

        private Object getValue() {
            return value;
        }
    }

    private static class ConvertedValue {
        private final Object rawValue;
        private final Object value;
        private final LocatedValue source;

        private ConvertedValue(Object rawValue, Object value, LocatedValue source) {
            this.rawValue = rawValue;
            this.value = value;
            this.source = source;
        }

        private Object getRawValue() {
            return rawValue;
        }

        private Object getValue() {
            return value;
        }

        private LocatedValue getSource() {
            return source;
        }
    }

    private static class SourceLocation {
        private final String sheetName;
        private final int rowIndex;
        private final int columnIndex;

        private SourceLocation(String sheetName, int rowIndex, int columnIndex) {
            this.sheetName = sheetName;
            this.rowIndex = rowIndex;
            this.columnIndex = columnIndex;
        }

        private String getSheetName() {
            return sheetName;
        }

        private int getRowIndex() {
            return rowIndex;
        }

        @SuppressWarnings("unused")
        private int getColumnIndex() {
            return columnIndex;
        }
    }

    private static class CustomFieldValue {
        private final ZhishuAndYecaiFiledEnum fieldEnum;
        private final FormAttributeTypeEnum attributeType;
        private final Object rawValue;
        private final SourceLocation source;

        private CustomFieldValue(ZhishuAndYecaiFiledEnum fieldEnum, FormAttributeTypeEnum attributeType,
                                 Object rawValue, SourceLocation source) {
            this.fieldEnum = fieldEnum;
            this.attributeType = attributeType;
            this.rawValue = rawValue;
            this.source = source;
        }

        private ZhishuAndYecaiFiledEnum getFieldEnum() {
            return fieldEnum;
        }

        private FormAttributeTypeEnum getAttributeType() {
            return attributeType;
        }

        private Object getRawValue() {
            return rawValue;
        }

        private SourceLocation getSource() {
            return source;
        }
    }

    private static class OrderMemberFieldValues {
        private final Map<ZhishuAndYecaiFiledEnum, LinkedHashSet<String>> userIdsByField = new LinkedHashMap<>();
        private final Map<ZhishuAndYecaiFiledEnum, String> dropdownRadioValuesByField = new LinkedHashMap<>();
        private final Map<ZhishuAndYecaiFiledEnum, LinkedHashSet<String>> dropdownOptionValuesByField =
                new LinkedHashMap<>();

        private void add(ZhishuAndYecaiFiledEnum fieldEnum, String userId) {
            if (fieldEnum == null || userId == null || userId.trim().isEmpty()) {
                return;
            }
            LinkedHashSet<String> userIds = userIdsByField.get(fieldEnum);
            if (userIds == null) {
                userIds = new LinkedHashSet<>();
                userIdsByField.put(fieldEnum, userIds);
            }
            userIds.add(userId.trim());
        }

        private void addDropdownRadio(ZhishuAndYecaiFiledEnum fieldEnum, String optionValue) {
            if (fieldEnum == null || optionValue == null || optionValue.trim().isEmpty()) {
                return;
            }
            if (!dropdownRadioValuesByField.containsKey(fieldEnum)) {
                dropdownRadioValuesByField.put(fieldEnum, optionValue.trim());
            }
        }

        private void addDropdownOption(ZhishuAndYecaiFiledEnum fieldEnum, String optionValue) {
            if (fieldEnum == null || optionValue == null || optionValue.trim().isEmpty()) {
                return;
            }
            LinkedHashSet<String> optionValues = dropdownOptionValuesByField.get(fieldEnum);
            if (optionValues == null) {
                optionValues = new LinkedHashSet<>();
                dropdownOptionValuesByField.put(fieldEnum, optionValues);
            }
            for (String item : optionValue.split("[,，;；\\r\\n]+")) {
                String value = item == null ? null : item.trim();
                if (value != null && !value.isEmpty()) {
                    optionValues.add(value);
                }
            }
        }

        private void merge(OrderMemberFieldValues other) {
            if (other == null || other.isEmpty()) {
                return;
            }
            for (Map.Entry<ZhishuAndYecaiFiledEnum, LinkedHashSet<String>> entry : other.userIdsByField.entrySet()) {
                for (String userId : entry.getValue()) {
                    add(entry.getKey(), userId);
                }
            }
            for (Map.Entry<ZhishuAndYecaiFiledEnum, String> entry : other.dropdownRadioValuesByField.entrySet()) {
                addDropdownRadio(entry.getKey(), entry.getValue());
            }
            for (Map.Entry<ZhishuAndYecaiFiledEnum, LinkedHashSet<String>> entry
                    : other.dropdownOptionValuesByField.entrySet()) {
                for (String optionValue : entry.getValue()) {
                    addDropdownOption(entry.getKey(), optionValue);
                }
            }
        }

        private boolean isEmpty() {
            return userIdsByField.isEmpty()
                    && dropdownRadioValuesByField.isEmpty()
                    && dropdownOptionValuesByField.isEmpty();
        }

        private Map<ZhishuAndYecaiFiledEnum, LinkedHashSet<String>> getUserIdsByField() {
            return userIdsByField;
        }

        private Map<ZhishuAndYecaiFiledEnum, String> getDropdownRadioValuesByField() {
            return dropdownRadioValuesByField;
        }

        private Map<ZhishuAndYecaiFiledEnum, LinkedHashSet<String>> getDropdownOptionValuesByField() {
            return dropdownOptionValuesByField;
        }
    }

    private static class OneContractSyncResult {
        private final boolean success;
        private final String reason;

        private OneContractSyncResult(boolean success, String reason) {
            this.success = success;
            this.reason = reason;
        }

        private static OneContractSyncResult success() {
            return new OneContractSyncResult(true, null);
        }

        private static OneContractSyncResult fail(String reason) {
            return new OneContractSyncResult(false, reason);
        }

        private boolean isSuccess() {
            return success;
        }

        private String getReason() {
            return reason;
        }
    }

    private static class OneContractValidationResult {
        private final List<String> errors;

        private OneContractValidationResult(List<String> errors) {
            this.errors = errors == null ? Collections.emptyList() : new ArrayList<>(errors);
        }

        private static OneContractValidationResult of(List<String> errors) {
            return new OneContractValidationResult(errors);
        }

        private List<String> getErrors() {
            return errors;
        }
    }

    private static class ContractExistsCheckResult {
        private final boolean exists;
        private final String reason;

        private ContractExistsCheckResult(boolean exists, String reason) {
            this.exists = exists;
            this.reason = reason;
        }

        private static ContractExistsCheckResult exists() {
            return new ContractExistsCheckResult(true, null);
        }

        private static ContractExistsCheckResult notFound(String reason) {
            return new ContractExistsCheckResult(false, reason);
        }

        private boolean isExists() {
            return exists;
        }

        private String getReason() {
            return reason;
        }
    }

    private static class NotFoundContractRecord {
        private final String contractNumber;
        private final String reason;

        private NotFoundContractRecord(String contractNumber, String reason) {
            this.contractNumber = contractNumber;
            this.reason = reason;
        }

        private String getContractNumber() {
            return contractNumber;
        }

        private String getReason() {
            return reason;
        }
    }

    private static class ApproveToNodeItemResult {
        private final boolean success;
        private final String reason;
        private final String contractId;
        private final String processInstanceId;
        private final String currentNodeName;

        private ApproveToNodeItemResult(boolean success,
                                        String reason,
                                        String contractId,
                                        String processInstanceId,
                                        String currentNodeName) {
            this.success = success;
            this.reason = reason;
            this.contractId = contractId;
            this.processInstanceId = processInstanceId;
            this.currentNodeName = currentNodeName;
        }

        private static ApproveToNodeItemResult success(String contractId,
                                                       String processInstanceId,
                                                       String currentNodeName) {
            return new ApproveToNodeItemResult(true, null, contractId, processInstanceId, currentNodeName);
        }

        private static ApproveToNodeItemResult fail(String reason) {
            return new ApproveToNodeItemResult(false, reason, null, null, null);
        }

        private boolean isSuccess() {
            return success;
        }

        private String getReason() {
            return reason;
        }

        private String getContractId() {
            return contractId;
        }

        private String getProcessInstanceId() {
            return processInstanceId;
        }

        private String getCurrentNodeName() {
            return currentNodeName;
        }
    }
}
