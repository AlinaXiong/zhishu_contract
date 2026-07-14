package com.hero.middleware.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hero.middleware.client.feishu.FeiShuApiClient;
import com.hero.middleware.client.feishu.response.FeishuUserBatchInfoResponse;
import com.hero.middleware.client.feishu.response.FeishuUserInfoResponse;
import com.hero.middleware.client.yuecai.YuecaiContractClient;
import com.hero.middleware.client.yuecai.response.AnchorCardResponse;
import com.hero.middleware.client.yuecai.response.MasterDataRes;
import com.hero.middleware.client.yuecai.response.OrderInfoResponse;
import com.hero.middleware.client.yuecai.response.ProcurementResponse;
import com.hero.middleware.client.zhishu.ZhishuContractClient;
import com.hero.middleware.client.zhishu.request.ContractsSearchRequest;
import com.hero.middleware.client.zhishu.request.ContractsSearchRequest;
import com.hero.middleware.client.zhishu.request.CreateTemplateInstanceRequest;
import com.hero.middleware.client.zhishu.request.ZhishuCreateContractRequest;
import com.hero.middleware.client.zhishu.response.*;
import com.hero.middleware.config.YeCaiDataConfig;
import com.hero.middleware.config.YuecaiApiConfig;
import com.hero.middleware.dto.*;
import com.hero.middleware.dto.ContractSyncDTO;
import com.hero.middleware.enums.ContractStatusEnum;
import com.hero.middleware.enums.ZhishuAndYecaiFiledEnum;
import com.hero.middleware.service.ContractService;
import com.hero.middleware.service.ZhiShuSynService;
import com.hero.middleware.utils.AnchorFlowApproveNodeReader;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import com.hero.middleware.utils.AnchorFlowApproveNodeReader;
import lombok.SneakyThrows;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 智书同步服务测试类，覆盖历史合同同步和审批节点推进相关逻辑。
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
@SpringBootTest
class ZhiShuSynServiceImplTest {

    private static final String APPROVE_ENABLED_PROPERTY = "zhishu.approve.enabled";
    private static final String YECAI_SYNC_SUCCESS = "成功";
    private static final String YECAI_SYNC_FAIL = "失败";
    private static final String YECAI_SYNC_THREAD_COUNT_PROPERTY = "yecai.sync.thread-count";
    private static final int DEFAULT_YECAI_SYNC_THREAD_COUNT = 5;
    private static final DateTimeFormatter YECAI_SYNC_FILE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter YECAI_SYNC_CELL_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Path ANCHOR_FLOW_EXCEL_PATH =
            Paths.get("C:\\Users\\AAA\\Downloads\\签署反商业贿赂协议6.25终版.xlsx");
    private static final Map<String, String> APPROVE_NODE_NAME_MAPPING = buildApproveNodeNameMapping();

    /**
     * 构建泛微状态到智书节点名称的映射关系。
     */
    private static Map<String, String> buildApproveNodeNameMapping() {
        Map<String, String> mapping = new LinkedHashMap<>();
        mapping.put("1", "申请人确认签约性质");
        mapping.put("2", "用印节点");
        mapping.put("3", "上传电子版");
        return mapping;
    }

    @TempDir
    Path tempDir;

    @Autowired
    private ZhishuContractClient zhishuContractClient;

    @Mock
    private YeCaiDataConfig yeCaiDataConfig;

    @Mock
    private YuecaiContractClient yuecaiContractClient;

    @Mock
    private YuecaiApiConfig yuecaiApiConfig;

    @Mock
    private FeiShuApiClient feiShuApiClient;

    @Autowired
    private ZhiShuSynService zhiShuSynService;


    @Autowired
    private ContractService contractService;

    private List<String> parseTxtFile(String filePath) throws Exception {
        if (filePath == null || filePath.trim().length() == 0) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        List<String> lines = Files.readAllLines(new File(filePath.trim()).toPath(), StandardCharsets.UTF_8);
        for (String line : lines) {
            if (line == null) {
                continue;
            }
            String value = line.trim();
            if (value.length() > 0) {
                result.add(value);
            }
        }
        return result;
    }

    @Test
    public void clearContract(){
        DeleteDraftContractsResultDTO deleteDraftContractsResultDTO = zhiShuSynService.deleteAllDraftContracts();
        log.info(JSONObject.toJSONString(deleteDraftContractsResultDTO));
    }

    @Test
    public void getAllContractStatus() throws Exception {
        List<String> list = parseTxtFile("E:\\lidongliang\\需求文档\\英雄电竞\\泛微合同历史数据同步\\导入合同数据\\正式导入列表\\全量数据表格\\一般合同.txt");
        if (list.isEmpty()) {
            log.info("获取智书合同状态结束，合同编码集合为空");
            return;
        }
        int threadCount = Math.min(10, list.size());
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        List<Future<ContractStatusRecord>> futures = new ArrayList<>();
        for (String contractNumber : list) {
            futures.add(executorService.submit(new Callable<ContractStatusRecord>() {
                @Override
                public ContractStatusRecord call() {
                    return queryContractStatus(contractNumber);
                }
            }));
        }
        List<ContractStatusRecord> records = new ArrayList<>();
        try {
            for (Future<ContractStatusRecord> future : futures) {
                records.add(future.get());
            }
        } finally {
            shutdownExecutor(executorService);
        }
        Path outputPath = writeContractStatusExcel(records);
        log.info("获取智书合同状态完成，合同数量：{}，线程数：{}，Excel路径：{}",
                records.size(), threadCount, outputPath.toAbsolutePath());
    }

    @Test
    public void getAllContract(){
        ContractsSearchRequest request = new ContractsSearchRequest();
        request.setPageSize(10);
        request.setPageToken(null);
//        request.setContractNumber(contractNumber);
        ContractsSearchRequest.CombineCondition combineCondition = new ContractsSearchRequest.CombineCondition();
        combineCondition.setCreateTimeStart("2026-06-01 00:00:00");
//        combineCondition.setContractNumber(contractNumber);
        request.setCombineCondition(combineCondition);
        ContractsSearchResponse contractsSearchResponse = zhishuContractClient.searchContracts(request);
    }

    private ContractStatusRecord queryContractStatus(String contractNumber) {
        ContractStatusRecord record = new ContractStatusRecord();
        record.setContractNumber(contractNumber);
        try {
            ContractsSearchRequest request = new ContractsSearchRequest();
            request.setPageSize(10);
            request.setPageToken(null);
            request.setContractNumber(contractNumber);
            ContractsSearchRequest.CombineCondition combineCondition = new ContractsSearchRequest.CombineCondition();
            combineCondition.setContractNumber(contractNumber);
            request.setCombineCondition(combineCondition);
            ContractsSearchResponse contractsSearchResponse = zhishuContractClient.searchContracts(request);
            ContractQueryResponse contract = getExactContract(contractNumber, contractsSearchResponse);
            if (contract == null) {
                record.setStatusName("未查询到合同");
                return record;
            }
            Integer contractStatusCode = contract.getContractStatusCode();
            record.setStatusCode(contractStatusCode);
            record.setStatusName(ContractStatusEnum.getNameByCode(contractStatusCode));
        } catch (Exception e) {
            record.setStatusName("查询异常：" + e.getMessage());
            log.error("获取智书合同状态异常，合同编码：{}", contractNumber, e);
        }
        return record;
    }

    private ContractQueryResponse getExactContract(String contractNumber, ContractsSearchResponse contractsSearchResponse) {
        if (contractsSearchResponse == null || contractsSearchResponse.getData() == null
                || contractsSearchResponse.getData().getItems() == null
                || contractsSearchResponse.getData().getItems().isEmpty()) {
            return null;
        }
        for (ContractQueryResponse item : contractsSearchResponse.getData().getItems()) {
            if (item != null && contractNumber != null && contractNumber.equals(item.getContractNumber())) {
                return item;
            }
        }
        return null;
    }

    private Path writeContractStatusExcel(List<ContractStatusRecord> records) throws Exception {
        Path outputDir = Paths.get("file");
        Files.createDirectories(outputDir);
        Path outputPath = outputDir.resolve("zhishu_contract_status_"
                + YECAI_SYNC_FILE_TIME_FORMATTER.format(LocalDateTime.now()) + ".xlsx");
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("合同状态");
            writeHeader(sheet.createRow(0), "合同编码", "状态编码", "状态名称");
            int rowIndex = 1;
            for (ContractStatusRecord record : records) {
                Row row = sheet.createRow(rowIndex++);
                createCell(row, 0, record.getContractNumber());
                createCell(row, 1, record.getStatusCode());
                createCell(row, 2, record.getStatusName());
            }
            for (int i = 0; i <= 2; i++) {
                sheet.autoSizeColumn(i);
            }
            try (OutputStream outputStream = Files.newOutputStream(outputPath)) {
                workbook.write(outputStream);
            }
        }
        return outputPath;
    }

    private static class ContractStatusRecord {

        private String contractNumber;

        private Integer statusCode;

        private String statusName;

        String getContractNumber() {
            return contractNumber;
        }

        void setContractNumber(String contractNumber) {
            this.contractNumber = contractNumber;
        }

        Integer getStatusCode() {
            return statusCode;
        }

        void setStatusCode(Integer statusCode) {
            this.statusCode = statusCode;
        }

        String getStatusName() {
            return statusName;
        }

        void setStatusName(String statusName) {
            this.statusName = statusName;
        }
    }

    @Test
    public void extractTodayHistorySyncSuccessContractNumbersToTxt() throws Exception {
        String today = "2026-06-30";
        List<Path> logFiles = findTodayHistorySyncLogFiles(today);
        LinkedHashSet<String> successContractNumbers = new LinkedHashSet<>();
        int matchedLineCount = 0;
        for (Path logFile : logFiles) {
            try (java.io.BufferedReader reader = Files.newBufferedReader(logFile, StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.contains("智书13Sheet历史合同同步结束，总数=")) {
                        continue;
                    }
                    matchedLineCount++;
                    extractSuccessContractNumbers(line, successContractNumbers);
                }
            }
        }

        Path outputDir = Paths.get("file");
        Files.createDirectories(outputDir);
        Path outputPath = outputDir.resolve("zhishu_history_success_contracts_"
                + YECAI_SYNC_FILE_TIME_FORMATTER.format(LocalDateTime.now()) + ".txt");
        Files.write(outputPath, successContractNumbers, StandardCharsets.UTF_8);
        log.info("提取智书13Sheet历史合同同步成功编码完成，扫描日志文件数量:{}，命中日志行数量:{}，成功合同编码数量:{}，输出文件:{}",
                logFiles.size(), matchedLineCount, successContractNumbers.size(), outputPath.toAbsolutePath());
    }

    private List<Path> findTodayHistorySyncLogFiles(String today) throws Exception {
        List<Path> logFiles = new ArrayList<>();
        for (String logDirName : Arrays.asList("log", "logs")) {
            Path logDir = Paths.get(logDirName);
            if (!Files.isDirectory(logDir)) {
                continue;
            }
            try (java.nio.file.DirectoryStream<Path> stream = Files.newDirectoryStream(logDir)) {
                for (Path logFile : stream) {
                    if (!Files.isRegularFile(logFile)) {
                        continue;
                    }
                    String fileName = logFile.getFileName().toString();
                    if ("hero-middleware.log".equals(fileName) || fileName.contains(today)) {
                        logFiles.add(logFile);
                    }
                }
            }
        }
        logFiles.sort(Comparator.comparingLong(this::getLastModifiedMillis)
                .thenComparing(path -> path.toAbsolutePath().toString()));
        return logFiles;
    }

    private long getLastModifiedMillis(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (Exception e) {
            return Long.MAX_VALUE;
        }
    }

    private void extractSuccessContractNumbers(String line, Set<String> successContractNumbers) {
        String startText = "成功合同编码=[";
        int startIndex = line.indexOf(startText);
        if (startIndex < 0) {
            return;
        }
        int contentStartIndex = startIndex + startText.length();
        int contentEndIndex = line.indexOf("]", contentStartIndex);
        if (contentEndIndex < 0) {
            return;
        }
        String content = line.substring(contentStartIndex, contentEndIndex);
        if (content.trim().length() == 0) {
            return;
        }
        for (String contractNumber : content.split(",")) {
            String value = contractNumber.trim();
            if (value.length() > 0) {
                successContractNumbers.add(value);
            }
        }
    }

    @Test
    public void extractTodayHistorySyncFailureDetailsToExcel() throws Exception {
        String today = "2026-07-01";
        List<Path> logFiles = findTodayHistorySyncLogFiles(today);
        LinkedHashSet<String> successContractNumbers = new LinkedHashSet<>();
        LinkedHashMap<String, HistoryFailureLogRecord> failureRecordMap = new LinkedHashMap<>();
        int matchedLineCount = 0;
        int extractedFailureCount = 0;
        int overwrittenFailureCount = 0;
        for (Path logFile : logFiles) {
            try (java.io.BufferedReader reader = Files.newBufferedReader(logFile, StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.contains("智书13Sheet历史合同同步结束，总数=")) {
                        continue;
                    }
                    matchedLineCount++;
                    extractSuccessContractNumbers(line, successContractNumbers);
                    String logTime = extractLogTime(line);
                    List<HistoryContractSyncResultDTO.Failure> failures = extractFailureDetails(line);
                    for (HistoryContractSyncResultDTO.Failure failure : failures) {
                        if (failure == null || failure.getContractNumber() == null
                                || failure.getContractNumber().trim().length() == 0) {
                            continue;
                        }
                        String contractNumber = failure.getContractNumber().trim();
                        failure.setContractNumber(contractNumber);
                        if (failureRecordMap.containsKey(contractNumber)) {
                            overwrittenFailureCount++;
                        }
                        failureRecordMap.put(contractNumber, new HistoryFailureLogRecord(failure, logTime));
                        extractedFailureCount++;
                    }
                }
            }
        }

        int beforeFilterCount = failureRecordMap.size();
        int successFilteredCount = 0;
        for (String successContractNumber : successContractNumbers) {
            if (failureRecordMap.remove(successContractNumber) != null) {
                successFilteredCount++;
            }
        }
        Path outputPath = writeHistorySyncFailureDetailsExcel(failureRecordMap.values());
        log.info("提取智书13Sheet历史合同同步失败明细完成，扫描日志文件数量:{}，命中日志行数量:{}，提取失败明细数量:{}，覆盖失败明细数量:{}，去重后失败明细数量:{}，成功日志筛除数量:{}，最终导出数量:{}，Excel路径:{}",
                logFiles.size(), matchedLineCount, extractedFailureCount, overwrittenFailureCount, beforeFilterCount,
                successFilteredCount, failureRecordMap.size(), outputPath.toAbsolutePath());
    }

    private String extractLogTime(String line) {
        if (line != null && line.length() >= 23) {
            String logTime = line.substring(0, 23);
            if (logTime.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}")) {
                return logTime;
            }
        }
        return "";
    }

    private List<HistoryContractSyncResultDTO.Failure> extractFailureDetails(String line) {
        String startText = "失败明细=";
        int startIndex = line.indexOf(startText);
        if (startIndex < 0) {
            return Collections.emptyList();
        }
        int contentStartIndex = startIndex + startText.length();
        int contentEndIndex = line.indexOf("，耗时=", contentStartIndex);
        String failureJson;
        if (contentEndIndex >= 0) {
            failureJson = line.substring(contentStartIndex, contentEndIndex);
        } else {
            int lastArrayEndIndex = line.lastIndexOf("]");
            if (lastArrayEndIndex < contentStartIndex) {
                return Collections.emptyList();
            }
            failureJson = line.substring(contentStartIndex, lastArrayEndIndex + 1);
        }
        if (failureJson.trim().length() == 0 || "[]".equals(failureJson.trim())) {
            return Collections.emptyList();
        }
        try {
            return JSON.parseArray(failureJson, HistoryContractSyncResultDTO.Failure.class);
        } catch (Exception e) {
            log.warn("解析智书13Sheet历史合同同步失败明细失败，失败明细:{}", failureJson, e);
            return Collections.emptyList();
        }
    }

    private Path writeHistorySyncFailureDetailsExcel(Collection<HistoryFailureLogRecord> records) throws Exception {
        Path outputDir = Paths.get("file");
        Files.createDirectories(outputDir);
        Path outputPath = outputDir.resolve("zhishu_history_failure_details_"
                + YECAI_SYNC_FILE_TIME_FORMATTER.format(LocalDateTime.now()) + ".xlsx");
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("失败明细");
            writeHeader(sheet.createRow(0), "合同编码", "流程类型", "失败原因", "日志时间");
            int rowIndex = 1;
            for (HistoryFailureLogRecord record : records) {
                Row row = sheet.createRow(rowIndex++);
                createCell(row, 0, record.getFailure().getContractNumber());
                createCell(row, 1, record.getFailure().getFlowType());
                createCell(row, 2, record.getFailure().getReason());
                createCell(row, 3, record.getLogTime());
            }
            for (int i = 0; i <= 3; i++) {
                sheet.autoSizeColumn(i);
            }
            try (OutputStream outputStream = Files.newOutputStream(outputPath)) {
                workbook.write(outputStream);
            }
        }
        return outputPath;
    }

    private static class HistoryFailureLogRecord {

        private final HistoryContractSyncResultDTO.Failure failure;

        private final String logTime;

        HistoryFailureLogRecord(HistoryContractSyncResultDTO.Failure failure, String logTime) {
            this.failure = failure;
            this.logTime = logTime;
        }

        HistoryContractSyncResultDTO.Failure getFailure() {
            return failure;
        }

        String getLogTime() {
            return logTime;
        }
    }

    @SneakyThrows
    @Test
    public void fnYeCai(){
        // 测试 1
//        List<String> list = parseTxtFile("D:\\Project Word\\公司项目文件\\英雄电竞\\测试业财.txt");
//        // 贿赂 86
//        List<String> list = parseTxtFile("D:\\Project Word\\公司项目文件\\英雄电竞\\贿赂协议.txt");
//        // 主播 1555
//        List<String> list = parseTxtFile("D:\\Project Word\\公司项目文件\\英雄电竞\\主播.txt");
//        List<String> list = parseTxtFile("C:\\Users\\AAA\\Downloads\\待推送业财合同编号.txt");
//        // 一般 4480
//        List<String> list = parseTxtFile("D:\\Project Word\\公司项目文件\\英雄电竞\\一般合同.txt");
        List<String> list= Arrays.asList("H-DF2025120568".split(","));
        List<YeCaiSyncRecord> records = syncYeCaiContracts(list);
        Path reportPath = writeYeCaiSyncReport(records);
        log.info("同步业财结果统计完成，总数:{}，成功:{}，失败:{}，Excel路径:{}",
                records.size(), countYeCaiSyncRecords(records, YECAI_SYNC_SUCCESS),
                countYeCaiSyncRecords(records, YECAI_SYNC_FAIL), reportPath.toAbsolutePath());
    }

    @Test
    public void fnYeCaiSyn() throws Exception {
        // 测试 1
//        List<String> list = parseTxtFile("D:\\Project Word\\公司项目文件\\英雄电竞\\测试业财.txt");
//        // 贿赂 86
//        List<String> list = parseTxtFile("D:\\Project Word\\公司项目文件\\英雄电竞\\贿赂协议.txt");
//        // 主播 1555
//        List<String> list = parseTxtFile("D:\\Project Word\\公司项目文件\\英雄电竞\\主播.txt");
//        List<String> list = parseTxtFile("C:\\Users\\AAA\\Downloads\\待推送业财合同编号_Excel中不存在_20260701083239(2).txt");
//        // 一般 4480
//        List<String> list = parseTxtFile("D:\\Project Word\\公司项目文件\\英雄电竞\\一般合同.txt");
        List<String> list = parseTxtFile("E:\\lidongliang\\需求文档\\英雄电竞\\泛微合同历史数据同步\\向业财同步\\0706.txt");
//        List<String> list=new ArrayList<>();
//        list.add("H-DF2025070237");
//        list.add("HH-P-202307001-S1");
//        list.add("H-DF2026060336");
        List<YeCaiSyncRecord> records = syncYeCaiContracts(list);
        Path reportPath = writeYeCaiSyncReport(records);
        log.info("同步业财结果统计完成，总数:{}，成功:{}，失败:{}，Excel路径:{}",
                records.size(), countYeCaiSyncRecords(records, YECAI_SYNC_SUCCESS),
                countYeCaiSyncRecords(records, YECAI_SYNC_FAIL), reportPath.toAbsolutePath());
    }
    private List<YeCaiSyncRecord> syncYeCaiContracts(List<String> contractNumbers) throws Exception {
        if (contractNumbers == null || contractNumbers.isEmpty()) {
            return Collections.emptyList();
        }
        int threadCount = resolveYeCaiSyncThreadCount(contractNumbers.size());
        log.info("同步业财开始，多线程数量:{}，合同数量:{}", threadCount, contractNumbers.size());
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        List<Future<YeCaiSyncRecord>> futures = new ArrayList<>();
        int index = 1;
        for (String contractNumber : contractNumbers) {
            final int currentIndex = index++;
            final String currentContractNumber = contractNumber;
            futures.add(executorService.submit(new Callable<YeCaiSyncRecord>() {
                @Override
                public YeCaiSyncRecord call() {
                    return syncOneYeCaiContract(currentIndex, currentContractNumber);
                }
            }));
        }
        List<YeCaiSyncRecord> records = new ArrayList<>();
        try {
            for (Future<YeCaiSyncRecord> future : futures) {
                records.add(future.get());
            }
        } finally {
            shutdownExecutor(executorService);
        }
        Collections.sort(records, new Comparator<YeCaiSyncRecord>() {
            @Override
            public int compare(YeCaiSyncRecord o1, YeCaiSyncRecord o2) {
                return Integer.compare(o1.getIndex(), o2.getIndex());
            }
        });
        return records;
    }

    private YeCaiSyncRecord syncOneYeCaiContract(int index, String e) {
        YeCaiSyncRecord record = new YeCaiSyncRecord(index, e);
        try {
            ContractsSearchRequest request = new ContractsSearchRequest();
            request.setContractNumber(e);
            ContractsSearchResponse response = zhishuContractClient.searchContracts(request);
            ContractQueryResponse contract = getFirstContract(response);
            if (contract == null) {
                record.fail("智书合同查询无结果");
                log.error("同步业财>>>>>>>查询智书合同合同编码:{}，未查询到合同", e);
                return record;
            }
            Long id = contract.getContractId();
            if (id == null) {
                record.fail("智书合同查询结果缺少合同主键");
                log.error("同步业财>>>>>>>查询智书合同合同编码:{}，合同主键为空", e);
                return record;
            }
            record.setZhishuContractId(String.valueOf(id));
            log.info("同步业财>>>>>>>查询智书合同合同编码:{}，合同主键:{}", e, id);
            ContractSyncDTO dto = new ContractSyncDTO();
            dto.setContractId(String.valueOf(id));
            contractService.syncContractFromZhishu(dto);
            record.success();
        } catch (Exception ee) {
            String errorMessage = buildErrorMessage(ee);
            record.fail(errorMessage);
            log.error("同步业财异常捕获>>>>>>>查询智书合同合同编码:{}，错误: {}", e, errorMessage, ee);
        } finally {
            record.finish();
        }
        return record;
    }

    private int resolveYeCaiSyncThreadCount(int totalCount) {
        int configuredThreadCount = Integer.getInteger(
                YECAI_SYNC_THREAD_COUNT_PROPERTY, DEFAULT_YECAI_SYNC_THREAD_COUNT);
        if (configuredThreadCount <= 0) {
            configuredThreadCount = DEFAULT_YECAI_SYNC_THREAD_COUNT;
        }
        return Math.max(1, Math.min(configuredThreadCount, totalCount));
    }

    private void shutdownExecutor(ExecutorService executorService) {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private ContractQueryResponse getFirstContract(ContractsSearchResponse response) {
        if (response == null || response.getData() == null
                || response.getData().getItems() == null
                || response.getData().getItems().isEmpty()) {
            return null;
        }
        return response.getData().getItems().get(0);
    }

    private Path writeYeCaiSyncReport(List<YeCaiSyncRecord> records) throws Exception {
        Path reportDir = Paths.get("target", "yecai-sync-report");
        Files.createDirectories(reportDir);
        Path reportPath = reportDir.resolve("fnYeCai-sync-result-"
                + YECAI_SYNC_FILE_TIME_FORMATTER.format(LocalDateTime.now()) + ".xlsx");
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            writeYeCaiSummarySheet(workbook, records);
            writeYeCaiDetailSheet(workbook, records);
            try (OutputStream outputStream = Files.newOutputStream(reportPath)) {
                workbook.write(outputStream);
            }
        }
        return reportPath;
    }

    private void writeYeCaiSummarySheet(XSSFWorkbook workbook, List<YeCaiSyncRecord> records) {
        Sheet sheet = workbook.createSheet("汇总");
        int successCount = countYeCaiSyncRecords(records, YECAI_SYNC_SUCCESS);
        int failCount = countYeCaiSyncRecords(records, YECAI_SYNC_FAIL);
        int rowIndex = 0;
        writeKeyValueRow(sheet, rowIndex++, "总数", records.size());
        writeKeyValueRow(sheet, rowIndex++, "成功数", successCount);
        writeKeyValueRow(sheet, rowIndex++, "失败数", failCount);
        writeKeyValueRow(sheet, rowIndex++, "生成时间", formatDateTime(LocalDateTime.now()));
        rowIndex++;
        rowIndex = writeContractNumbersSection(sheet, rowIndex, "成功合同编码", records, YECAI_SYNC_SUCCESS);
        rowIndex++;
        writeContractNumbersSection(sheet, rowIndex, "失败合同编码", records, YECAI_SYNC_FAIL);
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private int writeContractNumbersSection(Sheet sheet, int rowIndex, String title,
                                            List<YeCaiSyncRecord> records, String result) {
        Row titleRow = sheet.createRow(rowIndex++);
        createCell(titleRow, 0, title);
        createCell(titleRow, 1, "合同编码");
        for (YeCaiSyncRecord record : records) {
            if (result.equals(record.getResult())) {
                Row row = sheet.createRow(rowIndex++);
                createCell(row, 1, record.getContractNumber());
            }
        }
        return rowIndex;
    }

    private void writeYeCaiDetailSheet(XSSFWorkbook workbook, List<YeCaiSyncRecord> records) {
        Sheet sheet = workbook.createSheet("明细");
        writeHeader(sheet.createRow(0), "序号", "合同编码", "智书合同ID", "同步结果", "错误信息", "开始时间", "结束时间");
        int rowIndex = 1;
        for (YeCaiSyncRecord record : records) {
            Row row = sheet.createRow(rowIndex++);
            createCell(row, 0, record.getIndex());
            createCell(row, 1, record.getContractNumber());
            createCell(row, 2, record.getZhishuContractId());
            createCell(row, 3, record.getResult());
            createCell(row, 4, record.getErrorMessage());
            createCell(row, 5, formatDateTime(record.getStartTime()));
            createCell(row, 6, formatDateTime(record.getEndTime()));
        }
        for (int i = 0; i <= 6; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void writeHeader(Row row, String... headers) {
        for (int i = 0; i < headers.length; i++) {
            createCell(row, i, headers[i]);
        }
    }

    private void writeKeyValueRow(Sheet sheet, int rowIndex, String key, Object value) {
        Row row = sheet.createRow(rowIndex);
        createCell(row, 0, key);
        createCell(row, 1, value);
    }

    private void createCell(Row row, int columnIndex, Object value) {
        Cell cell = row.createCell(columnIndex);
        if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
            return;
        }
        cell.setCellValue(value == null ? "" : String.valueOf(value));
    }

    private int countYeCaiSyncRecords(List<YeCaiSyncRecord> records, String result) {
        int count = 0;
        for (YeCaiSyncRecord record : records) {
            if (result.equals(record.getResult())) {
                count++;
            }
        }
        return count;
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "" : YECAI_SYNC_CELL_TIME_FORMATTER.format(dateTime);
    }

    private String buildErrorMessage(Exception exception) {
        if (exception == null) {
            return "";
        }
        String message = exception.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return exception.getClass().getSimpleName();
        }
        return message;
    }

    private static class YeCaiSyncRecord {

        private final int index;
        private final String contractNumber;
        private final LocalDateTime startTime;
        private String zhishuContractId;
        private String result;
        private String errorMessage;
        private LocalDateTime endTime;

        YeCaiSyncRecord(int index, String contractNumber) {
            this.index = index;
            this.contractNumber = contractNumber;
            this.startTime = LocalDateTime.now();
        }

        void success() {
            this.result = YECAI_SYNC_SUCCESS;
            this.errorMessage = "";
        }

        void fail(String errorMessage) {
            this.result = YECAI_SYNC_FAIL;
            this.errorMessage = errorMessage;
        }

        void finish() {
            this.endTime = LocalDateTime.now();
        }

        int getIndex() {
            return index;
        }

        String getContractNumber() {
            return contractNumber;
        }

        String getZhishuContractId() {
            return zhishuContractId;
        }

        void setZhishuContractId(String zhishuContractId) {
            this.zhishuContractId = zhishuContractId;
        }

        String getResult() {
            return result;
        }

        String getErrorMessage() {
            return errorMessage;
        }

        LocalDateTime getStartTime() {
            return startTime;
        }

        LocalDateTime getEndTime() {
            return endTime;
        }
    }

    @SneakyThrows
    @Test
    public void fnYeCai2(){
        // 测试 1
//        List<String> list = parseTxtFile("D:\\Project Word\\公司项目文件\\英雄电竞\\测试业财.txt");
//        // 贿赂 785
        List<String> list = parseTxtFile("E:\\lidongliang\\需求文档\\英雄电竞\\泛微合同历史数据同步\\向业财同步\\0702.txt");
//        // 主播 1555
//        List<String> list = parseTxtFile("D:\\Project Word\\公司项目文件\\英雄电竞\\主播.txt");
//        // 一般 4480
//        List<String> list = parseTxtFile("D:\\Project Word\\公司项目文件\\英雄电竞\\一般合同.txt");
        list.forEach(e -> {
            ContractsSearchRequest request = new ContractsSearchRequest();
            request.setContractNumber(e);
            ContractsSearchResponse response = zhishuContractClient.searchContracts(request);
            try {
                if (response.getData().getItems().get(0) == null) {
                    log.error("同步业财>>>>>>>查询智书合同合同编码:{}", e);
                } else {
                    ContractQueryResponse response1 = response.getData().getItems().get(0);
                    Long id = response1.getContractId();
                    log.info("同步业财>>>>>>>查询智书合同合同主键:{}", id);
                    // TODO 同步业财逻辑
                    ContractSyncDTO dto = new ContractSyncDTO();
                    dto.setContractId(id + "");
                    contractService.syncContractFromZhishu(dto);
                }
            } catch (Exception ee) {
                log.error("同步业财异常捕获>>>>>>>查询智书合同合同编码:{}", e);
            }
        });
    }
    /**
     * 从主播流程主表按泛微状态分组合同编码，并批量推进到对应智书节点。
     */
    @Test
    public void testZhiShuApproveContractsToNode() throws Exception {
//        Assumptions.assumeTrue(Boolean.getBoolean(APPROVE_ENABLED_PROPERTY),
//                "默认跳过真实审批推进，如需执行请增加 -D" + APPROVE_ENABLED_PROPERTY + "=true");

        Map<String, List<String>> contractNumbersByStatus = new HashMap<>();
//                AnchorFlowApproveNodeReader.readContractNumbersByStatus(ANCHOR_FLOW_EXCEL_PATH);
        contractNumbersByStatus=new HashMap<>();
//        contractNumbersByStatus.put("法务确认", parseTxtFile("E:\\lidongliang\\需求文档\\英雄电竞\\泛微合同历史数据同步\\导入合同数据\\正式导入列表\\20260702执行合同编码\\审批执行\\法务确认.txt"));
//        contractNumbersByStatus.put("上传电子版", parseTxtFile("E:\\lidongliang\\需求文档\\英雄电竞\\泛微合同历史数据同步\\导入合同数据\\正式导入列表\\20260702执行合同编码\\审批执行\\上传电子档.txt"));
//        contractNumbersByStatus.put("申请人确认签约性质", parseTxtFile("E:\\lidongliang\\需求文档\\英雄电竞\\泛微合同历史数据同步\\导入合同数据\\正式导入列表\\20260702执行合同编码\\审批执行\\申请人确认签约性质.txt"));
        contractNumbersByStatus.put("用印节点", parseTxtFile("E:\\lidongliang\\需求文档\\英雄电竞\\泛微合同历史数据同步\\导入合同数据\\正式导入列表\\20260702执行合同编码\\审批执行\\用印.txt"));
//        contractNumbersByStatus.put("申请人确认签署类型", Arrays.asList("XJ-F2-202502006".split(";")));
//        contractNumbersByStatus.put("法务确认", Arrays.asList("H-KS2023020001".split(";")));

        // 按 C 列状态分组调用审批推进，未配置映射的状态使用原状态名作为节点名。
        JSONArray jsonArray=new JSONArray();
        for (Map.Entry<String, List<String>> entry : contractNumbersByStatus.entrySet()) {
            String status = entry.getKey();
            List<String> contractNumbers = entry.getValue();
            String nodeName = APPROVE_NODE_NAME_MAPPING.getOrDefault(status, status);
            System.out.println("泛微状态：" + status + "，智书节点：" + nodeName + "，合同数量："
                    + contractNumbers.size());
            String message = JSONObject.toJSONString(
                    zhiShuSynService.approveContractsToNode(contractNumbers, nodeName));
            System.out.println(message);
            jsonArray.add(message);
        }
        jsonArray.forEach(e-> System.out.println(e));
    }

    @Test
    public void testZhiShuSynService() throws Exception {
//        QueryContractCategoryResponse queryContractCategoryResponse = zhishuContractClient.queryContractCategorys(new HashMap<>());
//        System.out.println(JSONObject.toJSONString(queryContractCategoryResponse));
        List<String> list = new ArrayList<>();
//        list.add("H-OF2026060011");//一般流程
//        list.add("H-OF2026030026");//一般流程
//        list.add("H-OF2026050051");//一般流程
//        list.add("H-OF2026050048");//一般流程
        list.add("H-OS2026060018");//一般流程
//        list.add("XF-P-202507001");//主播卡片
//        List<String> list = parseTxtFile("E:\\lidongliang\\需求文档\\英雄电竞\\泛微合同历史数据同步\\导入合同数据\\正式导入列表\\执行合同编码.txt");
//        System.out.println(list.size());
        HistoryContractSyncResultDTO historyContractSyncResultDTO = zhiShuSynService.syncHistoryContracts(list, "E:\\lidongliang\\需求文档\\英雄电竞\\泛微合同历史数据同步\\导入合同数据\\正式导入列表\\智书合同字段_一般流程_执行文件_币种.xlsx");
        System.out.println(JSONObject.toJSONString(historyContractSyncResultDTO));
    }

    @Test
    public void testZhiShuSynServiceMultiThread() throws Exception {
//        String contractNumberFilePath = "E:\\lidongliang\\需求文档\\英雄电竞\\泛微合同历史数据同步\\导入合同数据\\正式导入列表\\执行合同编码.txt";
        String contractNumberFilePath = "E:\\lidongliang\\需求文档\\英雄电竞\\泛微合同历史数据同步\\导入合同数据\\正式导入列表\\20260706\\执行合同编码.txt";
//        String excelFilePath = "E:\\lidongliang\\需求文档\\英雄电竞\\泛微合同历史数据同步\\导入合同数据\\正式导入列表\\智书合同字段_一般流程_执行文件_币种.xlsx";
        String excelFilePath = "E:\\lidongliang\\需求文档\\英雄电竞\\泛微合同历史数据同步\\导入合同数据\\正式导入列表\\20260706\\缺失39个-匹配新订单编号-V2.xlsx";
//        String excelFilePath = "E:\\lidongliang\\需求文档\\英雄电竞\\泛微合同历史数据同步\\导入合同数据\\正式导入列表\\智书合同字段_一般流程_主播流程-20260629.xlsx";
        int threadCount = 5;
        int batchSize = 10;

        List<String> contractNumbers = parseTxtFile(contractNumberFilePath);
//        List<String> contractNumbers = new ArrayList<>();
//        contractNumbers.add("H-KF2023040006");
//        contractNumbers.add("H-OF2025090083");
//        contractNumbers.add("HH-S2-202606018");
//        contractNumbers.add("HH-S2-202606014");
//        contractNumbers.add("HH-F1-202601003");
//        contractNumbers.add("H-OF2026050039");
//        contractNumbers.add("H-KF2023120030");
//        contractNumbers.add("H-OF2026050048");
//        contractNumbers.add("H-OF2026050051");
        System.out.println("待同步合同数量：" + contractNumbers.size());
        if (contractNumbers.isEmpty()) {
            return;
        }

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        List<Future<HistoryContractSyncResultDTO>> futureList = new ArrayList<>();
        try {
            for (int start = 0; start < contractNumbers.size(); start += batchSize) {
                int end = Math.min(start + batchSize, contractNumbers.size());
                List<String> batchContractNumbers = new ArrayList<>(contractNumbers.subList(start, end));
                int batchNo = futureList.size() + 1;
                futureList.add(executorService.submit(() -> {
                    System.out.println("开始同步第" + batchNo + "批，合同数量：" + batchContractNumbers.size()
                            + "，合同编码：" + JSONObject.toJSONString(batchContractNumbers));
                    HistoryContractSyncResultDTO result =
                            zhiShuSynService.syncHistoryContracts(batchContractNumbers, excelFilePath);
                    System.out.println("第" + batchNo + "批同步完成：" + JSONObject.toJSONString(result));
                    return result;
                }));
            }

            int successCount = 0;
            int failCount = 0;
            List<String> successContractNumbers = new ArrayList<>();
            List<HistoryContractSyncResultDTO.Failure> failures = new ArrayList<>();
            for (Future<HistoryContractSyncResultDTO> future : futureList) {
                HistoryContractSyncResultDTO result = future.get();
                if (result == null) {
                    continue;
                }
                successCount += result.getSuccessCount() == null ? 0 : result.getSuccessCount();
                failCount += result.getFailCount() == null ? 0 : result.getFailCount();
                if (result.getSuccessContractNumbers() != null) {
                    successContractNumbers.addAll(result.getSuccessContractNumbers());
                }
                if (result.getFailures() != null) {
                    failures.addAll(result.getFailures());
                }
            }

            JSONObject summary = new JSONObject();
            summary.put("totalCount", contractNumbers.size());
            summary.put("batchCount", futureList.size());
            summary.put("threadCount", threadCount);
            summary.put("batchSize", batchSize);
            summary.put("successCount", successCount);
            summary.put("failCount", failCount);
            summary.put("successContractNumbers", successContractNumbers);
            summary.put("failures", failures);
            System.out.println("多线程同步汇总：" + summary.toJSONString());
        } finally {
            executorService.shutdown();
            if (!executorService.awaitTermination(10, TimeUnit.MINUTES)) {
                executorService.shutdownNow();
            }
        }
    }

    @Test
    public void testExportNotFoundContractsMultiThread() throws Exception {
//        String contractNumberFilePath = "E:\\lidongliang\\需求文档\\英雄电竞\\泛微合同历史数据同步\\导入合同数据\\正式导入列表\\执行合同编码.txt";
        String contractNumberFilePath = "E:\\lidongliang\\需求文档\\英雄电竞\\泛微合同历史数据同步\\导入合同数据\\正式导入列表\\执行合同编码 - 副本.txt";
        int threadCount = 10;
        int batchSize = 100;

//        List<String> contractNumbers = parseTxtFile(contractNumberFilePath);
        List<String> contractNumbers = new ArrayList<>();
        contractNumbers.add("H-DF2025070175");
        System.out.println("待查询合同数量：" + contractNumbers.size());
        if (contractNumbers.isEmpty()) {
            return;
        }

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        List<Future<String>> futureList = new ArrayList<>();
        try {
            for (int start = 0; start < contractNumbers.size(); start += batchSize) {
                int end = Math.min(start + batchSize, contractNumbers.size());
                List<String> batchContractNumbers = new ArrayList<>(contractNumbers.subList(start, end));
                int batchNo = futureList.size() + 1;
                futureList.add(executorService.submit(() -> {
                    System.out.println("开始查询第" + batchNo + "批，合同数量：" + batchContractNumbers.size()
                            + "，合同编码：" + JSONObject.toJSONString(batchContractNumbers));
                    String exportPath = zhiShuSynService.exportNotFoundContracts(batchContractNumbers);
                    System.out.println("第" + batchNo + "批查询完成，未查询到合同导出文件：" + exportPath);
                    return exportPath;
                }));
            }

            List<String> exportPaths = new ArrayList<>();
            for (Future<String> future : futureList) {
                exportPaths.add(future.get());
            }

            JSONObject summary = new JSONObject();
            summary.put("totalCount", contractNumbers.size());
            summary.put("batchCount", futureList.size());
            summary.put("threadCount", threadCount);
            summary.put("batchSize", batchSize);
            summary.put("exportPaths", exportPaths);
            System.out.println("多线程查询未同步合同汇总：" + summary.toJSONString());
        } finally {
            executorService.shutdown();
            if (!executorService.awaitTermination(10, TimeUnit.MINUTES)) {
                executorService.shutdownNow();
            }
        }
    }

    @Test
    public void testZhiShuSynAntiBribery() throws Exception {
        HistoryContractSyncDTO request = new HistoryContractSyncDTO();
//        List<String> contractNumbers = parseTxtFile("C:\\Users\\Administrator\\Desktop\\1.txt");
        List<String> contractNumbers = new ArrayList<>();
        contractNumbers.add("H-P2024090033");
        request.setContractNumbers(contractNumbers);
        zhiShuSynService.syncHistoryAntiBriberyContracts(request);
    }

    @Test
    void syncHistoryContractsUploadsFilesAndSyncsRelationBeforeCurrentContract() throws Exception {
        Path excelPath = writeThirteenSheetWorkbook();
        ZhiShuSynServiceImpl service = buildService(excelPath, 1);
        mockSuccessClient();

        HistoryContractSyncResultDTO result = service.syncHistoryContracts(Collections.singleton("G-CHILD"));

        assertEquals(Integer.valueOf(2), result.getSuccessCount());
        assertEquals("G-PARENT", result.getSuccessContractNumbers().get(0));
        assertEquals("G-CHILD", result.getSuccessContractNumbers().get(1));

        ArgumentCaptor<ZhishuCreateContractRequest> contractCaptor =
                ArgumentCaptor.forClass(ZhishuCreateContractRequest.class);
        verify(zhishuContractClient, times(2)).createContractV2(contractCaptor.capture());
        verify(zhishuContractClient, never()).createTemplateInstance(any(CreateTemplateInstanceRequest.class));
        verify(zhishuContractClient, never()).getTemplateList(any());

        List<ZhishuCreateContractRequest> requests = contractCaptor.getAllValues();
        assertEquals("G-PARENT", requests.get(0).getContractNumber());
        assertEquals("G-CHILD", requests.get(1).getContractNumber());
        assertNull(requests.get(0).getTemplateInstanceId());
        assertNull(requests.get(1).getTemplateInstanceId());

        ZhishuCreateContractRequest childRequest = requests.get(1);
        assertEquals("CAT-G", childRequest.getContractCategoryAbbreviation());
        assertEquals("file-general-child-main.txt-text", childRequest.getTextFileId());
        assertTrue(childRequest.getAttachmentFileIdList().contains("file-general-child-extra.txt-attachment"));
        assertEquals(Integer.valueOf(1), childRequest.getPayTypeCode());
        assertEquals(Integer.valueOf(0), childRequest.getPropertyTypeCode());
        assertEquals(Integer.valueOf(1), childRequest.getFixedValidityCode());
        assertEquals(Integer.valueOf(1), childRequest.getSignTypeCode());
        assertEquals(Integer.valueOf(2), childRequest.getSealNumber());
        assertEquals("L001", childRequest.getOurPartyList().get(0).getOurPartyCode());
        assertEquals("V001", childRequest.getCounterPartyList().get(0).getCounterPartyCode());
        assertEquals(1, childRequest.getPaymentPlanList().size());
        assertEquals("2026-05-01", childRequest.getPaymentPlanList().get(0).getPaymentDate());
        assertTrue(childRequest.getPaymentPlanList().get(0).getPrepaid());
        assertEquals(new BigDecimal("30"), childRequest.getPaymentPlanList().get(0).getPaymentAmount());
        assertEquals("first payment", childRequest.getPaymentPlanList().get(0).getPaymentDesc());
        assertEquals("V001", childRequest.getPaymentPlanList().get(0).getPaymentCounterParty().getCounterPartyCode());
        assertEquals(1, childRequest.getCollectionPlanList().size());
        assertEquals("2026-05-15", childRequest.getCollectionPlanList().get(0).getCollectionDate());
        assertEquals(new BigDecimal("20"), childRequest.getCollectionPlanList().get(0).getCollectionAmount());
        assertEquals("first collection", childRequest.getCollectionPlanList().get(0).getCollectionDesc());
        assertEquals("V001",
                childRequest.getCollectionPlanList().get(0).getCollectionCounterParty().getCounterPartyCode());
        assertEquals("G-PARENT", childRequest.getRelation().getRelationContracts().get(0));

        JSONArray form = JSON.parseArray(childRequest.getForm());
        JSONObject orderInfo = findAttribute(form, ZhishuAndYecaiFiledEnum.ORDERHT_DOCUMENT_INFO.getZhishuFiled());
        assertNotNull(orderInfo);
        assertEquals("common_array", orderInfo.getString("attribute_type"));
        JSONArray orderRows = orderInfo.getJSONArray("attribute_value");
        assertEquals(1, orderRows.size());
        JSONObject orderNumber = findAttribute(orderRows.getJSONArray(0),
                ZhishuAndYecaiFiledEnum.ORDERHT_DOCUMENT_NUMBER.getZhishuFiled());
        assertNotNull(orderNumber);
        assertEquals("ORDER-001", orderNumber.getString("attribute_value"));
        assertNull(findAttribute(form, ZhishuAndYecaiFiledEnum.ORDERHT_DOCUMENT_NUMBER.getZhishuFiled()));
        assertPrecedingDocumentAttribute(form, ZhishuAndYecaiFiledEnum.ORDER_DOCUMENT_NUMBER.getZhishuFiled(),
                "ORDER-DOC-001", "Order Doc One", "ORDER-DOC-001",
                "https://yuecai.example/project/order-query/list");
        assertPrecedingDocumentAttribute(form, ZhishuAndYecaiFiledEnum.PROCUREMENT_DOCUMENT_NUMBER.getZhishuFiled(),
                "PROC-DOC-001", "Procurement Project", "PROC-DOC-001",
                "https://yuecai.example/exp/requisition/list");

        ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
        ArgumentCaptor<String> fileTypeCaptor = ArgumentCaptor.forClass(String.class);
        verify(zhishuContractClient, times(3)).uploadContractFile(fileCaptor.capture(),
                fileTypeCaptor.capture(), anyBoolean());
        assertTrue(fileCaptor.getAllValues().stream()
                .allMatch(file -> file.getAbsolutePath().startsWith(tempDir.toString())));
        assertEquals(Arrays.asList("text", "text", "attachment"), fileTypeCaptor.getAllValues());
    }

    @Test
    void syncHistoryContractsSkipsCreateWhenContractAlreadyExists() throws Exception {
        Path excelPath = writeThirteenSheetWorkbook();
        ZhiShuSynServiceImpl service = buildService(excelPath, 1);
        mockCategoryAndUploadSuccess();
        mockPrecedingDocumentSuccess();
        when(yeCaiDataConfig.getUserId()).thenReturn("user-001");
        when(zhishuContractClient.searchContracts(any(ContractsSearchRequest.class)))
                .thenReturn(buildSearchResponse("A-001"));

        HistoryContractSyncResultDTO result = service.syncHistoryContracts(Collections.singleton("A-001"));

        assertEquals(Integer.valueOf(1), result.getSuccessCount());
        assertEquals(Integer.valueOf(0), result.getFailCount());
        assertEquals("A-001", result.getSuccessContractNumbers().get(0));
        verify(zhishuContractClient, never()).createContractV2(any(ZhishuCreateContractRequest.class));
    }

    @Test
    void syncHistoryContractsMultiThreadDoesNotLoadCounterPartyMapping() throws Exception {
        Path excelPath = writeThirteenSheetWorkbookWithGeneralCounterParty("C001");
        ZhiShuSynServiceImpl service = buildService(excelPath, 1);
        ZhishuContractClient mockZhishuContractClient = mock(ZhishuContractClient.class);
        ReflectionTestUtils.setField(service, "zhishuContractClient", mockZhishuContractClient);

        HistoryContractSyncDTO request = new HistoryContractSyncDTO();
        request.setContractNumbers(Arrays.asList("G-CHILD", "A-001"));
        request.setFilePath(excelPath.toString());
        request.setContractFileFallbackRoot(fallbackContractRoot().toString());
        request.setThreadCount(2);
        request.setBatchSize(1);

        HistoryContractSyncResultDTO result = service.syncHistoryContractsMultiThread(request);

        assertEquals(Integer.valueOf(3), result.getTotalCount());
        verify(yuecaiContractClient, never()).getMasterData(any());
    }

    @Test
    void syncHistoryContractsBuildsAnchorFeeDetailAndUploadsCauseAndAttachmentFiles() throws Exception {
        Path excelPath = writeThirteenSheetWorkbook();
        ZhiShuSynServiceImpl service = buildService(excelPath);
        mockSuccessClient();

        HistoryContractSyncResultDTO result = service.syncHistoryContracts(Collections.singleton("A-001"));

        assertEquals(Integer.valueOf(1), result.getSuccessCount());
        ArgumentCaptor<ZhishuCreateContractRequest> contractCaptor =
                ArgumentCaptor.forClass(ZhishuCreateContractRequest.class);
        verify(zhishuContractClient).createContractV2(contractCaptor.capture());
        verify(zhishuContractClient, never()).createTemplateInstance(any(CreateTemplateInstanceRequest.class));

        ZhishuCreateContractRequest request = contractCaptor.getValue();
        assertEquals("A-001", request.getContractNumber());
        assertEquals("CAT-A", request.getContractCategoryAbbreviation());
        assertNull(request.getTemplateInstanceId());
        assertEquals("file-anchor-main.txt-text", request.getTextFileId());
        assertEquals(Collections.singletonList("file-anchor-cause.txt-cause"), request.getContractCauseFileIdList());
        assertEquals(Collections.singletonList("file-anchor-attachment.txt-attachment"), request.getAttachmentFileIdList());
        assertEquals("AL001", request.getOurPartyList().get(0).getOurPartyCode());
        assertEquals("AV001", request.getCounterPartyList().get(0).getCounterPartyCode());
        assertEquals(Integer.valueOf(2), request.getSignTypeCode());
        assertEquals(Integer.valueOf(2), request.getCounterPartyList().get(0).getSignPartyNo());
        assertEquals(1, request.getPaymentPlanList().size());
        ZhishuCreateContractRequest.PaymentPlanInfo paymentPlan = request.getPaymentPlanList().get(0);
        assertEquals("2026-06-01", paymentPlan.getPaymentDate());
        assertEquals(Boolean.TRUE, paymentPlan.getPrepaid());
        assertEquals(new BigDecimal("1200"), paymentPlan.getPaymentAmount());
        assertEquals("anchor payment", paymentPlan.getPaymentDesc());
        assertEquals("CNY", paymentPlan.getCurrencyCode());
        assertEquals("AV001", paymentPlan.getPaymentCounterParty().getCounterPartyCode());
        assertPaymentCustomAttributes(paymentPlan.getPaymentCustomAttributes(),
                "cmoi963cu006e3b713s1y5f7i", "押金/保证金");
        assertEquals(1, request.getCollectionPlanList().size());
        ZhishuCreateContractRequest.CollectionPlanInfo collectionPlan = request.getCollectionPlanList().get(0);
        assertEquals("2026-06-15", collectionPlan.getCollectionDate());
        assertEquals(new BigDecimal("800"), collectionPlan.getCollectionAmount());
        assertEquals("anchor collection", collectionPlan.getCollectionDesc());
        assertEquals("CNY", collectionPlan.getCurrencyCode());
        assertEquals("AV001", collectionPlan.getCollectionCounterParty().getCounterPartyCode());

        JSONArray form = JSON.parseArray(request.getForm());
        assertNotNull(findAttribute(form, ZhishuAndYecaiFiledEnum.OTHER_DISTRIBUTABLE_INCOME.getZhishuFiled()));
        assertNotNull(findAttribute(form, ZhishuAndYecaiFiledEnum.GUARANTEED_FEE.getZhishuFiled()));
        assertOptionAttribute(form, ZhishuAndYecaiFiledEnum.ACCEPTANCE_REQUIRED.getZhishuFiled(),
                "cmp0y2rse004e3b716tihbjhf", "是");
        assertOptionAttribute(form, ZhishuAndYecaiFiledEnum.LIVE_CATEGORY.getZhishuFiled(),
                "娱乐", "娱乐");

        JSONObject feeDetail = findAttribute(form, ZhishuAndYecaiFiledEnum.ANCHOR_FEE_DETAIL.getZhishuFiled());
        assertNotNull(feeDetail);
        assertEquals("common_array", feeDetail.getString("attribute_type"));
        JSONArray feeRows = feeDetail.getJSONArray("attribute_value");
        assertEquals(1, feeRows.size());
        JSONObject feeItem = findAttribute(feeRows.getJSONArray(0),
                ZhishuAndYecaiFiledEnum.ANCHOR_FEE_ITEM.getZhishuFiled());
        assertNotNull(feeItem);
        assertEquals("Gift", feeItem.getJSONObject("attribute_value").getString("name"));
        assertNull(findAttribute(form, ZhishuAndYecaiFiledEnum.ANCHOR_FEE_ITEM.getZhishuFiled()));
        assertPrecedingDocumentAttribute(form, ZhishuAndYecaiFiledEnum.ANCHOR_DOCUMENT_NUMBER.getZhishuFiled(),
                "9001", "Anchor Real Name", "ANCHOR-CARD-ID-001",
                "https://yuecai.example/hfbs/anchor-doc/document");
    }

    @Test
    void syncHistoryContractsFailsWhenPrecedingDocumentDetailIsMissing() throws Exception {
        Path excelPath = writeThirteenSheetWorkbook();
        ZhiShuSynServiceImpl service = buildService(excelPath);
        mockCategoryAndUploadSuccess();
        when(yeCaiDataConfig.getUserId()).thenReturn("user-001");
        when(yuecaiContractClient.getAnchorCard(any(), anyString(), anyString()))
                .thenReturn(buildEmptyMasterDataResponse());

        HistoryContractSyncResultDTO result = service.syncHistoryContracts(Collections.singleton("A-001"));

        assertEquals(Integer.valueOf(0), result.getSuccessCount());
        assertEquals(Integer.valueOf(1), result.getFailCount());
        assertTrue(result.getFailures().get(0).getReason().contains("前置单据查询为空"));
        verify(zhishuContractClient, never()).createContractV2(any(ZhishuCreateContractRequest.class));
    }

    @Test
    void syncHistoryContractsSkipsCurrentWhenRelationFails() throws Exception {
        Path excelPath = writeThirteenSheetWorkbook();
        ZhiShuSynServiceImpl service = buildService(excelPath);
        mockCategoryAndUploadSuccess();
        when(yeCaiDataConfig.getUserId()).thenReturn("user-001");
        when(zhishuContractClient.createContractV2(any(ZhishuCreateContractRequest.class)))
                .thenReturn(buildContractResponse(1, "parent failed", null, null));

        HistoryContractSyncResultDTO result = service.syncHistoryContracts(Collections.singleton("G-CHILD"));

        assertEquals(Integer.valueOf(0), result.getSuccessCount());
        assertEquals(Integer.valueOf(2), result.getFailCount());
        verify(zhishuContractClient, times(1)).createContractV2(any(ZhishuCreateContractRequest.class));
        verify(zhishuContractClient, never()).createTemplateInstance(any(CreateTemplateInstanceRequest.class));
        assertTrue(result.getFailures().stream()
                .anyMatch(failure -> "G-CHILD".equals(failure.getContractNumber())
                        && failure.getReason().contains("关联合同同步失败")));
    }

    @Test
    void syncHistoryContractsWithEmptyFilterSyncsAllIndexedContracts() throws Exception {
        Path excelPath = writeThirteenSheetWorkbook();
        ZhiShuSynServiceImpl service = buildService(excelPath, 2);
        mockSuccessClient();

        HistoryContractSyncResultDTO result = service.syncHistoryContracts(Collections.emptyList());

        assertEquals(Integer.valueOf(3), result.getSuccessCount());
        assertEquals("G-PARENT", result.getSuccessContractNumbers().get(0));
        assertEquals("G-CHILD", result.getSuccessContractNumbers().get(1));
        assertEquals("A-001", result.getSuccessContractNumbers().get(2));
        verify(zhishuContractClient, times(3)).createContractV2(any(ZhishuCreateContractRequest.class));
        verify(zhishuContractClient, times(6)).uploadContractFile(any(File.class), anyString(), anyBoolean());
    }

    @Test
    void syncHistoryContractsFailsCycleWithoutCreatingContract() throws Exception {
        Path excelPath = writeThirteenSheetWorkbook(true);
        ZhiShuSynServiceImpl service = buildService(excelPath, 1);

        HistoryContractSyncResultDTO result = service.syncHistoryContracts(Collections.singleton("G-CHILD"));

        assertEquals(Integer.valueOf(0), result.getSuccessCount());
        assertTrue(result.getFailCount() >= 2);
        verify(zhishuContractClient, never()).createContractV2(any(ZhishuCreateContractRequest.class));
        verify(zhishuContractClient, never()).uploadContractFile(any(File.class), anyString(), anyBoolean());
        assertTrue(result.getFailures().stream()
                .anyMatch(failure -> "G-CHILD".equals(failure.getContractNumber())
                        && failure.getReason().contains("循环依赖")));
    }

    @Test
    void syncHistoryContractsWithFilterSkipsUnrelatedRowsFromLargeWorkbook() throws Exception {
        Path excelPath = writeThirteenSheetWorkbook(false, 50);
        ZhiShuSynServiceImpl service = buildService(excelPath, 1);
        mockSuccessClient();

        HistoryContractSyncResultDTO result = service.syncHistoryContracts(Collections.singleton("G-CHILD"));

        assertEquals(Integer.valueOf(2), result.getSuccessCount());
        verify(zhishuContractClient, times(2)).createContractV2(any(ZhishuCreateContractRequest.class));
        verify(zhishuContractClient, times(3)).uploadContractFile(any(File.class), anyString(), anyBoolean());
    }

    @Test
    void syncHistoryContractsFailsWhenContractTextFileIsMissing() throws Exception {
        Path excelPath = writeThirteenSheetWorkbook(false, 0, false);
        ZhiShuSynServiceImpl service = buildService(excelPath);
        when(yeCaiDataConfig.getUserId()).thenReturn("user-001");
        when(zhishuContractClient.queryContractCategorys(any())).thenReturn(buildCategoryResponse());

        HistoryContractSyncResultDTO result = service.syncHistoryContracts(Collections.singleton("A-001"));

        assertEquals(Integer.valueOf(0), result.getSuccessCount());
        assertEquals(Integer.valueOf(1), result.getFailCount());
        verify(zhishuContractClient, never()).createContractV2(any(ZhishuCreateContractRequest.class));
        verify(zhishuContractClient, never()).uploadContractFile(any(File.class), anyString(), anyBoolean());
    }

    @Test
    void syncHistoryContractsUsesFallbackTextFileWhenContractTextFileIsMissing() throws Exception {
        Path excelPath = writeThirteenSheetWorkbook(false, 0, false);
        createFallbackContractFile("A-001", "main-contract.txt", "fallback-main");
        ZhiShuSynServiceImpl service = buildService(excelPath);
        mockSuccessClient();

        HistoryContractSyncResultDTO result = service.syncHistoryContracts(Collections.singleton("A-001"));

        assertEquals(Integer.valueOf(1), result.getSuccessCount());
        ArgumentCaptor<ZhishuCreateContractRequest> contractCaptor =
                ArgumentCaptor.forClass(ZhishuCreateContractRequest.class);
        verify(zhishuContractClient).createContractV2(contractCaptor.capture());
        ZhishuCreateContractRequest request = contractCaptor.getValue();
        assertEquals("file-main-contract.txt-text", request.getTextFileId());
        assertEquals(Collections.singletonList("file-anchor-cause.txt-cause"), request.getContractCauseFileIdList());
        assertEquals(Collections.singletonList("file-anchor-attachment.txt-attachment"),
                request.getAttachmentFileIdList());
        verify(zhishuContractClient, times(3)).uploadContractFile(any(File.class), anyString(), anyBoolean());
    }

    @Test
    void syncHistoryContractsUsesFirstFallbackFileAsTextAndRestAsAttachments() throws Exception {
        Path excelPath = writeThirteenSheetWorkbook(false, 0, false);
        createFallbackContractFile("A-001", "b-attachment.txt", "fallback-attachment");
        createFallbackContractFile("A-001", "a-main.txt", "fallback-main");
        createFallbackContractFile("A-001", "nested/c-attachment.txt", "fallback-nested-attachment");
        ZhiShuSynServiceImpl service = buildService(excelPath);
        mockSuccessClient();

        HistoryContractSyncResultDTO result = service.syncHistoryContracts(Collections.singleton("A-001"));

        assertEquals(Integer.valueOf(1), result.getSuccessCount());
        ArgumentCaptor<ZhishuCreateContractRequest> contractCaptor =
                ArgumentCaptor.forClass(ZhishuCreateContractRequest.class);
        verify(zhishuContractClient).createContractV2(contractCaptor.capture());
        ZhishuCreateContractRequest request = contractCaptor.getValue();
        assertEquals("file-a-main.txt-text", request.getTextFileId());
        assertTrue(request.getAttachmentFileIdList().contains("file-b-attachment.txt-attachment"));
        assertTrue(request.getAttachmentFileIdList().contains("file-c-attachment.txt-attachment"));
        assertTrue(request.getAttachmentFileIdList().contains("file-anchor-attachment.txt-attachment"));
        verify(zhishuContractClient, times(5)).uploadContractFile(any(File.class), anyString(), anyBoolean());
    }

    @Test
    void syncHistoryContractsUsesFallbackWhenExcelTextPathIsInvalid() throws Exception {
        Path excelPath = writeThirteenSheetWorkbookWithAnchorTextPath("files/missing-anchor-main.txt");
        createFallbackContractFile("A-001", "main-contract.txt", "fallback-main");
        ZhiShuSynServiceImpl service = buildService(excelPath);
        mockSuccessClient();

        HistoryContractSyncResultDTO result = service.syncHistoryContracts(Collections.singleton("A-001"));

        assertEquals(Integer.valueOf(1), result.getSuccessCount());
        ArgumentCaptor<ZhishuCreateContractRequest> contractCaptor =
                ArgumentCaptor.forClass(ZhishuCreateContractRequest.class);
        verify(zhishuContractClient).createContractV2(contractCaptor.capture());
        assertEquals("file-main-contract.txt-text", contractCaptor.getValue().getTextFileId());
    }

    @Test
    void syncHistoryContractsKeepsExcelTextFileWhenValidAndDoesNotUseFallback() throws Exception {
        Path excelPath = writeThirteenSheetWorkbook();
        createFallbackContractFile("A-001", "a-main.txt", "fallback-main");
        ZhiShuSynServiceImpl service = buildService(excelPath);
        mockSuccessClient();

        HistoryContractSyncResultDTO result = service.syncHistoryContracts(Collections.singleton("A-001"));

        assertEquals(Integer.valueOf(1), result.getSuccessCount());
        ArgumentCaptor<ZhishuCreateContractRequest> contractCaptor =
                ArgumentCaptor.forClass(ZhishuCreateContractRequest.class);
        verify(zhishuContractClient).createContractV2(contractCaptor.capture());
        assertEquals("file-anchor-main.txt-text", contractCaptor.getValue().getTextFileId());
        ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
        verify(zhishuContractClient, times(3)).uploadContractFile(fileCaptor.capture(), anyString(), anyBoolean());
        assertTrue(fileCaptor.getAllValues().stream()
                .noneMatch(file -> file.getAbsolutePath().contains(fallbackContractRoot().toString())));
    }

    @Test
    void syncHistoryContractsFailsWhenFallbackDirectoryHasOnlyEmptyFiles() throws Exception {
        Path excelPath = writeThirteenSheetWorkbook(false, 0, false);
        createFallbackContractFile("A-001", "empty.txt", "");
        ZhiShuSynServiceImpl service = buildService(excelPath);
        when(yeCaiDataConfig.getUserId()).thenReturn("user-001");
        when(zhishuContractClient.queryContractCategorys(any())).thenReturn(buildCategoryResponse());

        HistoryContractSyncResultDTO result = service.syncHistoryContracts(Collections.singleton("A-001"));

        assertEquals(Integer.valueOf(0), result.getSuccessCount());
        assertEquals(Integer.valueOf(1), result.getFailCount());
        verify(zhishuContractClient, never()).createContractV2(any(ZhishuCreateContractRequest.class));
        verify(zhishuContractClient, never()).uploadContractFile(any(File.class), anyString(), anyBoolean());
    }

    @Test
    void syncHistoryAntiBriberyContractsBuildsRequestUploadsFallbackFilesAndCreatesContract() throws Exception {
        Path excelPath = writeAntiBriberyDataWorkbook("AB-001", "", true);
        createFallbackContractFile("AB-001", "主文件/a-main.txt", "main");
        createFallbackContractFile("AB-001", "归档扫描件/scan.pdf", "scan");
        createFallbackContractFile("AB-001", "其他附件/b-attachment.txt", "attachment");
        createFallbackContractFile("AB-001", "其他附件/nested/c-attachment.txt", "nested-attachment");
        ZhiShuSynServiceImpl service = buildAntiBriberyService(excelPath);
        mockAntiBriberySuccessClient();

        HistoryContractSyncDTO request = new HistoryContractSyncDTO();
        request.setFilePath(excelPath.toString());
        HistoryContractSyncResultDTO result = service.syncHistoryAntiBriberyContracts(request);

        assertEquals(Integer.valueOf(1), result.getSuccessCount());
        ArgumentCaptor<ZhishuCreateContractRequest> contractCaptor =
                ArgumentCaptor.forClass(ZhishuCreateContractRequest.class);
        verify(zhishuContractClient).createContractV2(contractCaptor.capture());
        ZhishuCreateContractRequest createRequest = contractCaptor.getValue();
        assertEquals("AB-001", createRequest.getContractNumber());
        assertEquals("Anti Bribery AB-001", createRequest.getContractName());
        assertEquals("DEFAULT", createRequest.getContractCategoryAbbreviation());
        assertEquals("user-001", createRequest.getCreateUserId());
        assertEquals("AB-001", createRequest.getSourceId());
        assertEquals("0", createRequest.getContractStatusCode());
        assertEquals(Integer.valueOf(2), createRequest.getPayTypeCode());
        assertEquals(Integer.valueOf(0), createRequest.getFixedValidityCode());
        assertEquals(Integer.valueOf(0), createRequest.getBusinessTypeCode());
        assertEquals(Integer.valueOf(2), createRequest.getPropertyTypeCode());
        assertEquals(BigDecimal.ZERO, createRequest.getAmount());
        assertEquals("CNY", createRequest.getCurrencyCode());
        assertEquals(String.valueOf(DateUtil.getJavaDate(45566D).getTime()),
                createRequest.getSubmittedTime());
        assertEquals("L001", createRequest.getOurPartyList().get(0).getOurPartyCode());
        assertEquals(Boolean.FALSE, createRequest.getOurPartyList().get(0)
                .getOurPartySignInfoResource().getEnable());
        assertEquals("V001", createRequest.getCounterPartyList().get(0).getCounterPartyCode());
        assertEquals(Boolean.FALSE, createRequest.getCounterPartyList().get(0)
                .getCounterPartySignInfoResource().getEnable());
        assertEquals("file-a-main.txt-text", createRequest.getTextFileId());
        assertEquals("file-scan.pdf-scan", createRequest.getScanFileId());
        assertEquals(Arrays.asList("file-b-attachment.txt-attachment", "file-c-attachment.txt-attachment"),
                createRequest.getAttachmentFileIdList());
        verify(zhishuContractClient, times(4)).uploadContractFile(any(File.class), anyString(), anyBoolean());
    }

    @Test
    void syncHistoryAntiBriberyContractsAllowsMissingScanFolder() throws Exception {
        Path excelPath = writeAntiBriberyDataWorkbook("AB-001", "");
        createFallbackContractFile("AB-001", "主文件/main.txt", "main");
        ZhiShuSynServiceImpl service = buildAntiBriberyService(excelPath);
        mockAntiBriberySuccessClient();

        HistoryContractSyncDTO request = new HistoryContractSyncDTO();
        request.setFilePath(excelPath.toString());
        HistoryContractSyncResultDTO result = service.syncHistoryAntiBriberyContracts(request);

        assertEquals(Integer.valueOf(1), result.getSuccessCount());
        ArgumentCaptor<ZhishuCreateContractRequest> contractCaptor =
                ArgumentCaptor.forClass(ZhishuCreateContractRequest.class);
        verify(zhishuContractClient).createContractV2(contractCaptor.capture());
        assertEquals("file-main.txt-text", contractCaptor.getValue().getTextFileId());
        assertNull(contractCaptor.getValue().getScanFileId());
        verify(zhishuContractClient).uploadContractFile(any(File.class), anyString(), anyBoolean());
    }

    @Test
    void syncHistoryAntiBriberyContractsMovesExtraMainAndScanFilesToAttachments() throws Exception {
        Path excelPath = writeAntiBriberyDataWorkbook("AB-001", "");
        createFallbackContractFile("AB-001", "主文件/a-main.txt", "main");
        createFallbackContractFile("AB-001", "主文件/b-main-extra.txt", "main-extra");
        createFallbackContractFile("AB-001", "归档扫描件/a-scan.pdf", "scan");
        createFallbackContractFile("AB-001", "归档扫描件/b-scan-extra.pdf", "scan-extra");
        createFallbackContractFile("AB-001", "其他附件/c-attachment.txt", "attachment");
        ZhiShuSynServiceImpl service = buildAntiBriberyService(excelPath);
        mockAntiBriberySuccessClient();

        HistoryContractSyncDTO request = new HistoryContractSyncDTO();
        request.setFilePath(excelPath.toString());
        HistoryContractSyncResultDTO result = service.syncHistoryAntiBriberyContracts(request);

        assertEquals(Integer.valueOf(1), result.getSuccessCount());
        ArgumentCaptor<ZhishuCreateContractRequest> contractCaptor =
                ArgumentCaptor.forClass(ZhishuCreateContractRequest.class);
        verify(zhishuContractClient).createContractV2(contractCaptor.capture());
        ZhishuCreateContractRequest createRequest = contractCaptor.getValue();
        assertEquals("file-a-main.txt-text", createRequest.getTextFileId());
        assertEquals("file-a-scan.pdf-scan", createRequest.getScanFileId());
        assertTrue(createRequest.getAttachmentFileIdList().contains("file-b-main-extra.txt-attachment"));
        assertTrue(createRequest.getAttachmentFileIdList().contains("file-b-scan-extra.pdf-attachment"));
        assertTrue(createRequest.getAttachmentFileIdList().contains("file-c-attachment.txt-attachment"));
        verify(zhishuContractClient, times(5)).uploadContractFile(any(File.class), anyString(), anyBoolean());
    }

    @Test
    void syncHistoryAntiBriberyContractsFiltersAndRecordsMissingContracts() throws Exception {
        Path excelPath = writeAntiBriberyDataWorkbook("AB-001", "applicant-user");
        ZhiShuSynServiceImpl service = buildAntiBriberyService(excelPath);

        HistoryContractSyncDTO request = new HistoryContractSyncDTO();
        request.setFilePath(excelPath.toString());
        request.setContractNumbers(Collections.singleton("AB-002"));
        HistoryContractSyncResultDTO result = service.syncHistoryAntiBriberyContracts(request);

        assertEquals(Integer.valueOf(0), result.getSuccessCount());
        assertEquals(Integer.valueOf(1), result.getFailCount());
        assertEquals("AB-002", result.getFailures().get(0).getContractNumber());
        verify(zhishuContractClient, never()).createContractV2(any(ZhishuCreateContractRequest.class));
        verify(zhishuContractClient, never()).uploadContractFile(any(File.class), anyString(), anyBoolean());
    }

    @Test
    void syncHistoryAntiBriberyContractsFailsWhenFallbackFilesMissing() throws Exception {
        Path excelPath = writeAntiBriberyDataWorkbook("AB-001", "");
        ZhiShuSynServiceImpl service = buildAntiBriberyService(excelPath);
        when(yeCaiDataConfig.getUserId()).thenReturn("user-001");

        HistoryContractSyncDTO request = new HistoryContractSyncDTO();
        request.setFilePath(excelPath.toString());
        HistoryContractSyncResultDTO result = service.syncHistoryAntiBriberyContracts(request);

        assertEquals(Integer.valueOf(0), result.getSuccessCount());
        assertEquals(Integer.valueOf(1), result.getFailCount());
        verify(zhishuContractClient, never()).createContractV2(any(ZhishuCreateContractRequest.class));
        verify(zhishuContractClient, never()).uploadContractFile(any(File.class), anyString(), anyBoolean());
    }

    @Test
    void syncHistoryAntiBriberyContractsRecordsCreateApiFailure() throws Exception {
        Path excelPath = writeAntiBriberyDataWorkbook("AB-001", "");
        createFallbackContractFile("AB-001", "主文件/main.txt", "main");
        ZhiShuSynServiceImpl service = buildAntiBriberyService(excelPath);
        when(yeCaiDataConfig.getUserId()).thenReturn("user-001");
        when(zhishuContractClient.uploadContractFile(any(File.class), anyString(), anyBoolean()))
                .thenAnswer(invocation -> {
                    File file = invocation.getArgument(0);
                    String fileType = invocation.getArgument(1);
                    return buildUploadFileResponse(0, "file-" + file.getName() + "-" + fileType);
                });
        when(zhishuContractClient.createContractV2(any(ZhishuCreateContractRequest.class)))
                .thenReturn(buildContractResponse(1, "create failed", null, null));

        HistoryContractSyncDTO request = new HistoryContractSyncDTO();
        request.setFilePath(excelPath.toString());
        HistoryContractSyncResultDTO result = service.syncHistoryAntiBriberyContracts(request);

        assertEquals(Integer.valueOf(0), result.getSuccessCount());
        assertEquals(Integer.valueOf(1), result.getFailCount());
        verify(zhishuContractClient).uploadContractFile(any(File.class), anyString(), anyBoolean());
        verify(zhishuContractClient).createContractV2(any(ZhishuCreateContractRequest.class));
    }

    @Test
    void syncHistoryContractsFailsWhenUploadFailsBeforeCreatingContract() throws Exception {
        Path excelPath = writeThirteenSheetWorkbook();
        ZhiShuSynServiceImpl service = buildService(excelPath);
        when(yeCaiDataConfig.getUserId()).thenReturn("user-001");
        when(zhishuContractClient.queryContractCategorys(any())).thenReturn(buildCategoryResponse());
        when(zhishuContractClient.uploadContractFile(any(File.class), anyString(), anyBoolean()))
                .thenReturn(buildUploadFileResponse(1, null));

        HistoryContractSyncResultDTO result = service.syncHistoryContracts(Collections.singleton("A-001"));

        assertEquals(Integer.valueOf(0), result.getSuccessCount());
        assertEquals(Integer.valueOf(1), result.getFailCount());
        verify(zhishuContractClient, never()).createContractV2(any(ZhishuCreateContractRequest.class));
    }

    private void mockSuccessClient() {
        mockCategoryAndUploadSuccess();
        mockPrecedingDocumentSuccess();
        when(yeCaiDataConfig.getUserId()).thenReturn("user-001");
        when(zhishuContractClient.createContractV2(any(ZhishuCreateContractRequest.class)))
                .thenAnswer(invocation -> {
                    ZhishuCreateContractRequest request = invocation.getArgument(0);
                    return buildContractResponse(0, "success", "id-" + request.getContractNumber(),
                            "ZS-" + request.getContractNumber());
                });
    }

    private void mockPrecedingDocumentSuccess() {
        lenient().when(yuecaiApiConfig.getBaseUrl()).thenReturn("https://yuecai.example");
        lenient().when(yuecaiContractClient.getOrderInfo(any()))
                .thenReturn(buildMasterDataResponse(buildOrderInfoResponse()));
        lenient().when(yuecaiContractClient.getProcurement(any(), anyString()))
                .thenReturn(buildMasterDataResponse(buildProcurementResponse()));
        lenient().when(yuecaiContractClient.getAnchorCard(any(), anyString(), anyString()))
                .thenReturn(buildMasterDataResponse(buildAnchorCardResponse()));
    }

    private void mockOrderMemberQuery(String orderNumber, OrderInfoResponse.Member... members) {
        when(yuecaiContractClient.getOrderInfo(any())).thenReturn(
                buildMasterDataResponse(buildOrderInfoResponse(orderNumber, Arrays.asList(members))));
    }

    private void mockFeishuUsers(FeishuUserInfoResponse.User... users) {
        FeishuUserBatchInfoResponse response = new FeishuUserBatchInfoResponse();
        response.setItems(Arrays.asList(users));
        when(feiShuApiClient.getUserInfoBatch(any())).thenReturn(response);
    }

    private void mockAntiBriberySuccessClient() {
        when(yeCaiDataConfig.getUserId()).thenReturn("user-001");
        when(zhishuContractClient.uploadContractFile(any(File.class), anyString(), anyBoolean()))
                .thenAnswer(invocation -> {
                    File file = invocation.getArgument(0);
                    String fileType = invocation.getArgument(1);
                    return buildUploadFileResponse(0, "file-" + file.getName() + "-" + fileType);
                });
        when(zhishuContractClient.createContractV2(any(ZhishuCreateContractRequest.class)))
                .thenAnswer(invocation -> {
                    ZhishuCreateContractRequest request = invocation.getArgument(0);
                    return buildContractResponse(0, "success", "id-" + request.getContractNumber(),
                            "ZS-" + request.getContractNumber());
                });
    }

    private void mockCategoryAndUploadSuccess() {
        when(zhishuContractClient.queryContractCategorys(any())).thenReturn(buildCategoryResponse());
        when(zhishuContractClient.uploadContractFile(any(File.class), anyString(), anyBoolean()))
                .thenAnswer(invocation -> {
                    File file = invocation.getArgument(0);
                    String fileType = invocation.getArgument(1);
                    return buildUploadFileResponse(0, "file-" + file.getName() + "-" + fileType);
                });
    }

    private ZhiShuSynServiceImpl buildService(Path excelPath) {
        return buildService(excelPath, 200);
    }

    private ZhiShuSynServiceImpl buildService(Path excelPath, int batchSize) {
        ZhiShuSynServiceImpl service = new ZhiShuSynServiceImpl(excelPath.toString(), batchSize);
        ReflectionTestUtils.setField(service, "zhishuContractClient", zhishuContractClient);
        ReflectionTestUtils.setField(service, "yeCaiDataConfig", yeCaiDataConfig);
        ReflectionTestUtils.setField(service, "yuecaiContractClient", yuecaiContractClient);
        ReflectionTestUtils.setField(service, "yuecaiApiConfig", yuecaiApiConfig);
        ReflectionTestUtils.setField(service, "contractFileFallbackRoot", fallbackContractRoot());
        return service;
    }

    private ZhiShuSynServiceImpl buildAntiBriberyService(Path excelPath) {
        return buildService(excelPath);
    }

    private Path writeThirteenSheetWorkbook() throws Exception {
        return writeThirteenSheetWorkbook(false);
    }

    private Path writeThirteenSheetWorkbook(boolean cycleRelation) throws Exception {
        return writeThirteenSheetWorkbook(cycleRelation, 0);
    }

    private Path writeThirteenSheetWorkbook(boolean cycleRelation, int extraGeneralContracts) throws Exception {
        return writeThirteenSheetWorkbook(cycleRelation, extraGeneralContracts, true);
    }

    private Path writeThirteenSheetWorkbook(boolean cycleRelation,
                                            int extraGeneralContracts,
                                            boolean includeAnchorTextFile) throws Exception {
        return writeThirteenSheetWorkbook(cycleRelation, extraGeneralContracts,
                includeAnchorTextFile ? "files/anchor-main.txt" : null);
    }

    private Path writeThirteenSheetWorkbookWithAnchorTextPath(String anchorTextFilePath) throws Exception {
        return writeThirteenSheetWorkbook(false, 0, anchorTextFilePath);
    }

    private Path writeThirteenSheetWorkbookWithGeneralCounterParty(String counterPartyCode) throws Exception {
        return writeThirteenSheetWorkbook(false, 0, "files/anchor-main.txt", counterPartyCode);
    }

    private Path writeThirteenSheetWorkbook(boolean cycleRelation,
                                            int extraGeneralContracts,
                                            String anchorTextFilePath) throws Exception {
        return writeThirteenSheetWorkbook(cycleRelation, extraGeneralContracts, anchorTextFilePath, "V001");
    }

    private Path writeThirteenSheetWorkbook(boolean cycleRelation,
                                            int extraGeneralContracts,
                                            String anchorTextFilePath,
                                            String generalCounterPartyCode) throws Exception {
        createLocalFile("files/general-parent-main.txt", "parent");
        createLocalFile("files/general-child-main.txt", "child");
        createLocalFile("files/general-child-extra.txt", "child-extra");
        createLocalFile("files/anchor-cause.txt", "cause");
        createLocalFile("files/anchor-attachment.txt", "attachment");
        if ("files/anchor-main.txt".equals(anchorTextFilePath)) {
            createLocalFile("files/anchor-main.txt", "anchor");
        }
        for (int index = 0; index < extraGeneralContracts; index++) {
            createLocalFile("files/general-extra-" + index + ".txt", "extra-" + index);
        }

        Path excelPath = tempDir.resolve("history-contract-13-sheet.xlsx");
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            writeGeneralMainSheet(workbook, extraGeneralContracts);
            writeRelationSheet(workbook, cycleRelation);
            writeGeneralOrderInfoSheet(workbook);
            writePurchaseRequestSheet(workbook);
            writeOrderDetailSheet(workbook);
            writeGeneralCounterPartySheet(workbook, generalCounterPartyCode);
            writeGeneralOurPartySheet(workbook);
            writePaymentPlanSheet(workbook, generalCounterPartyCode);
            writeCollectionPlanSheet(workbook, generalCounterPartyCode);
            writeAnchorMainSheet(workbook, anchorTextFilePath);
            writeAnchorCounterPartySheet(workbook);
            writeAnchorOurPartySheet(workbook);
            writeAnchorFeeDetailSheet(workbook);
            writeAnchorPaymentPlanSheet(workbook);
            writeAnchorCollectionPlanSheet(workbook);
            try (OutputStream outputStream = Files.newOutputStream(excelPath)) {
                workbook.write(outputStream);
            }
        }
        return excelPath;
    }

    private Path writeAntiBriberyDataWorkbook(String contractNumber, String createUserId) throws Exception {
        return writeAntiBriberyDataWorkbook(contractNumber, createUserId, false);
    }

    private Path writeAntiBriberyDataWorkbook(String contractNumber,
                                              String createUserId,
                                              boolean stringSubmittedTimeWithDateStyle) throws Exception {
        Path excelPath = tempDir.resolve("anti-bribery-data-" + contractNumber + ".xlsx");
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            writeEmptySheet(workbook, "ignored-1");
            writeEmptySheet(workbook, "ignored-2");
            writeEmptySheet(workbook, "ignored-3");
            Sheet sheet = workbook.createSheet("反商业贿赂协议");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("contract_number（合同编码）");
            header.createCell(1).setCellValue("contract_name（合同名称）");
            header.createCell(2).setCellValue("合同状态");
            header.createCell(3).setCellValue("收支类型");
            header.createCell(4).setCellValue("fixed_validity_code（合同期限类型）");
            header.createCell(5).setCellValue("签署日期");
            header.createCell(6).setCellValue("合同申请人（名字）");
            header.createCell(7).setCellValue("合同申请人（user_id)");
            header.createCell(8).setCellValue("对方信息id");
            header.createCell(9).setCellValue("我方信息id");

            Row data = sheet.createRow(1);
            data.createCell(0).setCellValue(contractNumber);
            data.createCell(1).setCellValue("Anti Bribery " + contractNumber);
            data.createCell(2).setCellValue("9");
            data.createCell(3).setCellValue("9");
            data.createCell(4).setCellValue("9");
            Cell submittedTime = data.createCell(5);
            if (stringSubmittedTimeWithDateStyle) {
                CellStyle dateStyle = workbook.createCellStyle();
                dateStyle.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("m/d/yy"));
                submittedTime.setCellStyle(dateStyle);
                submittedTime.setCellValue("45566");
            } else {
                submittedTime.setCellValue(45566D);
            }
            data.createCell(6).setCellValue("applicant-name");
            data.createCell(7).setCellValue(createUserId == null ? "" : createUserId);
            data.createCell(8).setCellValue("V001");
            data.createCell(9).setCellValue("L001");

            try (OutputStream outputStream = Files.newOutputStream(excelPath)) {
                workbook.write(outputStream);
            }
        }
        return excelPath;
    }

    private void writeGeneralMainSheet(XSSFWorkbook workbook, int extraGeneralContracts) {
        Sheet sheet = workbook.createSheet("general-main");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("contract_number(contract code)");
        header.createCell(1).setCellValue("contract_name(contract name)");
        header.createCell(2).setCellValue("contractCategory(contract category)");
        header.createCell(3).setCellValue("amount(amount)");
        header.createCell(4).setCellValue("pay_type_code(pay type)");
        header.createCell(5).setCellValue("property_type_code(property type)");
        header.createCell(6).setCellValue("fixed_validity_code(fixed validity)");
        header.createCell(7).setCellValue("sign_type_code(sign type)");
        header.createCell(8).setCellValue("seal_number(seal number)");
        header.createCell(9).setCellValue("custom_1001_948719050bfe402ab083c98e52fa71b2(owner)");
        header.createCell(10).setCellValue("custom_1012_cec7052f613b465980f23f7004e2f82c(purchase amount)");
        header.createCell(11).setCellValue("contract_files.contract_text(contract text)");

        Row child = sheet.createRow(1);
        child.createCell(0).setCellValue("G-CHILD");
        child.createCell(1).setCellValue("General Child");
        child.createCell(2).setCellValue("TPL-G");
        child.createCell(3).setCellValue(100.5D);
        child.createCell(4).setCellValue(1D);
        child.createCell(5).setCellValue(0D);
        child.createCell(6).setCellValue(1D);
        child.createCell(7).setCellValue(1D);
        child.createCell(8).setCellValue(2D);
        child.createCell(9).setCellValue("employee-001");
        child.createCell(10).setCellValue(88.8D);
        child.createCell(11).setCellValue("files/general-child-main.txt;files/general-child-extra.txt");

        Row parent = sheet.createRow(2);
        parent.createCell(0).setCellValue("G-PARENT");
        parent.createCell(1).setCellValue("General Parent");
        parent.createCell(2).setCellValue("TPL-G");
        parent.createCell(4).setCellValue(4D);
        parent.createCell(11).setCellValue("files/general-parent-main.txt");

        for (int index = 0; index < extraGeneralContracts; index++) {
            Row extra = sheet.createRow(3 + index);
            extra.createCell(0).setCellValue("G-EXTRA-" + index);
            extra.createCell(1).setCellValue("General Extra " + index);
            extra.createCell(2).setCellValue("TPL-G");
            extra.createCell(4).setCellValue(4D);
            extra.createCell(11).setCellValue("files/general-extra-" + index + ".txt");
        }
    }

    private void writeRelationSheet(XSSFWorkbook workbook, boolean cycleRelation) {
        Sheet sheet = workbook.createSheet("relation");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("contract_number(contract code)");
        header.createCell(1).setCellValue("relation.relation_contracts(relation contracts)");
        Row data = sheet.createRow(1);
        data.createCell(0).setCellValue("G-CHILD");
        data.createCell(1).setCellValue("G-PARENT");
        if (cycleRelation) {
            Row parentRelation = sheet.createRow(2);
            parentRelation.createCell(0).setCellValue("G-PARENT");
            parentRelation.createCell(1).setCellValue("G-CHILD");
        }
    }

    private void writeGeneralOrderInfoSheet(XSSFWorkbook workbook) {
        Sheet sheet = workbook.createSheet("general-order-info");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("contract_number(contract code)");
        header.createCell(1).setCellValue(
                "custom_1024_90a78c8120994f95b2dbfedd297c7d81(order document)");

        Row data = sheet.createRow(1);
        data.createCell(0).setCellValue("G-CHILD");
        data.createCell(1).setCellValue("ORDER-DOC-001");
    }

    private void writePurchaseRequestSheet(XSSFWorkbook workbook) {
        Sheet sheet = workbook.createSheet("purchase-request");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("contract_number(contract code)");
        header.createCell(1).setCellValue(
                "custom_1024_7db9a8ee2b3d4a3f9d9835dd9fee69df(procurement document)");

        Row data = sheet.createRow(1);
        data.createCell(0).setCellValue("G-CHILD");
        data.createCell(1).setCellValue("PROC-DOC-001");
    }

    private void writeOrderDetailSheet(XSSFWorkbook workbook) {
        Sheet sheet = workbook.createSheet("order-detail");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("contract_number(contract code)");
        header.createCell(1).setCellValue("custom_1_5549b19faea641eeac924deada603c11(order name)");
        header.createCell(2).setCellValue("custom_1_7f977c0d30064dd199434f706470c669(order number)");
        header.createCell(3).setCellValue("custom_15_622e96ab047c4f689d287a27066f7bcb(order type)");
        Row data = sheet.createRow(1);
        data.createCell(0).setCellValue("G-CHILD");
        data.createCell(1).setCellValue("Order One");
        data.createCell(2).setCellValue("ORDER-001");
        data.createCell(3).setCellValue("Project Order");
    }

    private void writeGeneralCounterPartySheet(XSSFWorkbook workbook) {
        writeGeneralCounterPartySheet(workbook, "V001");
    }

    private void writeGeneralCounterPartySheet(XSSFWorkbook workbook, String counterPartyCode) {
        Sheet sheet = workbook.createSheet("general-counter-party");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("contract_number(contract code)");
        header.createCell(1).setCellValue("counter_party_code(counter party code)");
        Row data = sheet.createRow(1);
        data.createCell(0).setCellValue("G-CHILD");
        data.createCell(1).setCellValue(counterPartyCode);
    }

    private void writeGeneralOurPartySheet(XSSFWorkbook workbook) {
        Sheet sheet = workbook.createSheet("general-our-party");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("contract_number(contract code)");
        header.createCell(1).setCellValue("our_party_code(our party code)");
        Row data = sheet.createRow(1);
        data.createCell(0).setCellValue("G-CHILD");
        data.createCell(1).setCellValue("L001");
    }

    private void writePaymentPlanSheet(XSSFWorkbook workbook) {
        writePaymentPlanSheet(workbook, "V001");
    }

    private void writePaymentPlanSheet(XSSFWorkbook workbook, String counterPartyCode) {
        Sheet sheet = workbook.createSheet("payment-plan");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("contract_number(contract code)");
        header.createCell(1).setCellValue("payment_plan_list[].payment_amount(payment amount)");
        header.createCell(2).setCellValue("payment_plan_list[].payment_date(payment date)");
        header.createCell(3).setCellValue("payment_plan_list[].payment_desc(payment desc)");
        header.createCell(4).setCellValue("payment_plan_list[].prepaid(prepaid)");
        header.createCell(5).setCellValue("payment_plan_list[].payment_counter_party[].counter_party_code(payment counter party)");
        Row data = sheet.createRow(1);
        data.createCell(0).setCellValue("G-CHILD");
        data.createCell(1).setCellValue(30D);
        data.createCell(2).setCellValue("2026-05-01");
        data.createCell(3).setCellValue("first payment");
        data.createCell(4).setCellValue(true);
        data.createCell(5).setCellValue(counterPartyCode);
    }

    private void writeCollectionPlanSheet(XSSFWorkbook workbook, String counterPartyCode) {
        Sheet sheet = workbook.createSheet("collection-plan");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("contract_number(contract code)");
        header.createCell(1).setCellValue("collection_plan_list[].collection_amount(collection amount)");
        header.createCell(2).setCellValue("collection_plan_list[].collection_date(collection date)");
        header.createCell(3).setCellValue("collection_plan_list[].collection_desc(collection desc)");
        header.createCell(4).setCellValue(
                "collection_plan_list[].collection_counter_party[].counter_party_code(collection counter party)");
        Row data = sheet.createRow(1);
        data.createCell(0).setCellValue("G-CHILD");
        data.createCell(1).setCellValue(20D);
        data.createCell(2).setCellValue("2026-05-15");
        data.createCell(3).setCellValue("first collection");
        data.createCell(4).setCellValue(counterPartyCode);
    }

    private void writeAnchorMainSheet(XSSFWorkbook workbook, String anchorTextFilePath) {
        Sheet sheet = workbook.createSheet("anchor-main");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("contract_number(contract code)");
        header.createCell(1).setCellValue("contract_name(contract name)");
        header.createCell(2).setCellValue("contractCategory(contract category)");
        header.createCell(3).setCellValue("pay_type_code(pay type)");
        header.createCell(4).setCellValue("property_type_code(property type)");
        header.createCell(5).setCellValue("fixed_validity_code(fixed validity)");
        header.createCell(6).setCellValue("custom_5_def7270057bd4913a1fd087b4b1f128e(other income)");
        header.createCell(7).setCellValue("custom_1012_65df3bda1aae46a1822c4d4531be5e25(guaranteed fee)");
        header.createCell(8).setCellValue("contract_files.contract_text(contract text)");
        header.createCell(9).setCellValue("contract_files.contract_causes(contract causes)");
        header.createCell(10).setCellValue("contract_files.contract_attachments(contract attachments)");
        header.createCell(11).setCellValue(
                "custom_1024_61820798c0f348658d8daa64f8b2aef9(anchor document)");
        header.createCell(12).setCellValue(
                "custom_15_99b283c1e1374c02aaeacead8b336cd7(anchor platform)");
        header.createCell(13).setCellValue(
                "custom_13_c9805a6fe9f245ebbfeea13407277306(acceptance required)");
        header.createCell(14).setCellValue("sign_type_code（先盖章方）");
        header.createCell(15).setCellValue("sign_type_code（签约形式）");

        Row data = sheet.createRow(1);
        data.createCell(0).setCellValue("A-001");
        data.createCell(1).setCellValue("Anchor Contract");
        data.createCell(2).setCellValue("TPL-A");
        data.createCell(3).setCellValue(2D);
        data.createCell(4).setCellValue(0D);
        data.createCell(5).setCellValue(1D);
        data.createCell(6).setCellValue(0.2D);
        data.createCell(7).setCellValue(5000D);
        if (anchorTextFilePath != null) {
            data.createCell(8).setCellValue(anchorTextFilePath);
        }
        data.createCell(9).setCellValue("files/anchor-cause.txt");
        data.createCell(10).setCellValue("files/anchor-attachment.txt");
        data.createCell(11).setCellValue("ANCHOR-DOC-001");
        data.createCell(12).setCellValue("抖音");
        data.createCell(13).setCellValue("是");
        data.createCell(14).setCellValue("对方先盖章");
        data.createCell(15).setCellValue("电子签约");
    }

    private void writeAnchorCounterPartySheet(XSSFWorkbook workbook) {
        Sheet sheet = workbook.createSheet("anchor-counter-party");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("contract_number(contract code)");
        header.createCell(1).setCellValue("counter_party_code(counter party code)");
        Row data = sheet.createRow(1);
        data.createCell(0).setCellValue("A-001");
        data.createCell(1).setCellValue("AV001");
    }

    private void writeAnchorOurPartySheet(XSSFWorkbook workbook) {
        Sheet sheet = workbook.createSheet("anchor-our-party");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("contract_number(contract code)");
        header.createCell(1).setCellValue("our_party_code(our party code)");
        Row data = sheet.createRow(1);
        data.createCell(0).setCellValue("A-001");
        data.createCell(1).setCellValue("AL001");
    }

    private void writeAnchorFeeDetailSheet(XSSFWorkbook workbook) {
        Sheet sheet = workbook.createSheet("anchor-fee-detail");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("contract_number(contract code)");
        header.createCell(1).setCellValue("custom_15_02f24c2051cb4cc5a5d7a9bc7230f3ff(fee item)");
        header.createCell(2).setCellValue("custom_1012_fba348aede86465b90d669eda6e956a7(income monthly average)");
        header.createCell(3).setCellValue("custom_1012_869047e5ebc3425d912890ccd9455a6e(cost monthly average)");
        header.createCell(4).setCellValue("custom_1012_47053c50bd7742e79f39e3f37e718720(gross profit monthly average)");
        Row data = sheet.createRow(1);
        data.createCell(0).setCellValue("A-001");
        data.createCell(1).setCellValue("Gift");
        data.createCell(2).setCellValue(1000D);
        data.createCell(3).setCellValue(400D);
        data.createCell(4).setCellValue(600D);
    }

    private void writeAnchorPaymentPlanSheet(XSSFWorkbook workbook) {
        Sheet sheet = workbook.createSheet("主播流程_付款计划");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("contract_number（合同编码）");
        header.createCell(1).setCellValue("payment_plan_list（付款计划）");
        header.createCell(2).setCellValue("payment_plan_list[].payment_date（付款时间）");
        header.createCell(3).setCellValue("payment_plan_list[].prepaid（是否预付）");
        header.createCell(4).setCellValue("payment_plan_list[].payment_amount（付款金额）");
        header.createCell(5).setCellValue("payment_plan_list[].payment_desc（付款说明）");
        header.createCell(6).setCellValue("payment_plan_list[].payment_custom_attributes/custom_付款性质（付款性质）");
        header.createCell(7).setCellValue(
                "payment_plan_list[].payment_counter_party[].counter_party_code（付款对象）");
        header.createCell(8).setCellValue("付款计划行id(付款记录传的id)");
        Row data = sheet.createRow(1);
        data.createCell(0).setCellValue("A-001");
        data.createCell(2).setCellValue("2026-06-01");
        data.createCell(3).setCellValue(true);
        data.createCell(4).setCellValue(1200D);
        data.createCell(5).setCellValue("anchor payment");
        data.createCell(6).setCellValue("押金/保证金");
        data.createCell(7).setCellValue("AV001");
        data.createCell(8).setCellValue("anchor-payment-row-1");
    }

    private void writeAnchorCollectionPlanSheet(XSSFWorkbook workbook) {
        Sheet sheet = workbook.createSheet("主播流程_收款计划");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("contract_number（合同编码）");
        header.createCell(1).setCellValue("collection_plan_list（收款计划）");
        header.createCell(2).setCellValue("collection_plan_list[].collection_date（收款时间）");
        header.createCell(3).setCellValue("collection_plan_list[].collection_amount（收款金额）");
        header.createCell(4).setCellValue("collection_plan_list[].collection_desc（收款说明）");
        header.createCell(5).setCellValue(
                "collection_plan_list[].collection_counter_party[].counter_party_code（收款对象）");
        header.createCell(6).setCellValue("收款计划行id(收款记录传的id)");
        Row data = sheet.createRow(1);
        data.createCell(0).setCellValue("A-001");
        data.createCell(2).setCellValue("2026-06-15");
        data.createCell(3).setCellValue(800D);
        data.createCell(4).setCellValue("anchor collection");
        data.createCell(5).setCellValue("AV001");
        data.createCell(6).setCellValue("anchor-collection-row-1");
    }

    private void writeEmptySheet(XSSFWorkbook workbook, String sheetName) {
        Sheet sheet = workbook.createSheet(sheetName);
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("contract_number(contract code)");
    }

    private void createLocalFile(String relativePath, String content) throws Exception {
        Path file = tempDir.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
    }

    private void createFallbackContractFile(String contractNumber, String relativePath, String content)
            throws Exception {
        Path file = fallbackContractRoot().resolve(contractNumber).resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
    }

    private Path fallbackContractRoot() {
        return tempDir.resolve("fallback-contract-files");
    }

    private OrderInfoResponse buildOrderInfoResponse() {
        return buildOrderInfoResponse("ORDER-DOC-001", Collections.emptyList());
    }

    private OrderInfoResponse buildOrderInfoResponse(String orderNumber, List<OrderInfoResponse.Member> members) {
        OrderInfoResponse response = new OrderInfoResponse();
        response.setPrjDimOrderValue(orderNumber);
        response.setOrderTitle("Order Doc One");
        response.setMemberList(members);
        return response;
    }

    private OrderInfoResponse.Member orderMember(String roleCode, String userId) {
        OrderInfoResponse.Member member = new OrderInfoResponse.Member();
        member.setRoleCode(roleCode);
        member.setUserId(userId);
        return member;
    }

    private FeishuUserInfoResponse.User feishuUser(String userId, boolean resigned) {
        FeishuUserInfoResponse.User user = new FeishuUserInfoResponse.User();
        user.setUserId(userId);
        user.setName(userId + "-name");
        FeishuUserInfoResponse.Status status = new FeishuUserInfoResponse.Status();
        status.setResigned(resigned);
        user.setStatus(status);
        return user;
    }

    private ProcurementResponse buildProcurementResponse() {
        ProcurementResponse response = new ProcurementResponse();
        response.setExpRequisitionNumber("PROC-DOC-001");
        response.setAttribute1("Procurement Project");
        return response;
    }

    private AnchorCardResponse buildAnchorCardResponse() {
        AnchorCardResponse response = new AnchorCardResponse();
        response.setHeaderId(9001L);
        response.setRealName("Anchor Real Name");
        response.setId("ANCHOR-CARD-ID-001");
        AnchorCardResponse.AnchorCardLineRes lineRes = new AnchorCardResponse.AnchorCardLineRes();
        lineRes.setLiveCategory("娱乐");
        response.setLineResultDTOS(Collections.singletonList(lineRes));
        return response;
    }

    private MasterDataRes buildMasterDataResponse(Object content) {
        MasterDataRes response = new MasterDataRes();
        response.setContent(Collections.singletonList(content));
        response.setSize(1);
        response.setTotalElements(1);
        response.setNumberOfElements(1);
        return response;
    }

    private MasterDataRes buildMasterDataListResponse(List<?> content) {
        MasterDataRes response = new MasterDataRes();
        response.setContent(new ArrayList<Object>(content));
        response.setSize(content.size());
        response.setTotalElements(content.size());
        response.setNumberOfElements(content.size());
        response.setTotalPages(1);
        return response;
    }

    private MasterDataRes buildEmptyMasterDataResponse() {
        MasterDataRes response = new MasterDataRes();
        response.setContent(Collections.emptyList());
        response.setSize(0);
        response.setTotalElements(0);
        response.setNumberOfElements(0);
        response.setEmpty(true);
        return response;
    }

    private void assertPrecedingDocumentAttribute(JSONArray form,
                                                  String attributeCode,
                                                  String expectedId,
                                                  String expectedTitle,
                                                  String expectedContent,
                                                  String expectedLink) {
        JSONObject attribute = findAttribute(form, attributeCode);
        assertNotNull(attribute);
        assertEquals("feishu_approval", attribute.getString("attribute_type"));
        assertEquals("third_party_approval", attribute.getString("approval_type"));
        assertEquals("相关单据", attribute.getString("module_name"));
        JSONArray receipts = attribute.getJSONArray("attribute_value");
        assertNotNull(receipts);
        assertEquals(1, receipts.size());
        JSONObject receipt = receipts.getJSONObject(0);
        assertEquals(expectedId, receipt.getString("id"));
        assertEquals(expectedTitle, receipt.getString("title"));
        assertEquals(expectedContent, receipt.getString("content"));
        assertEquals(expectedLink, receipt.getString("mobile_app_link"));
        assertEquals(expectedLink, receipt.getString("pc_app_link"));
    }

    private void assertPaymentCustomAttributes(String paymentCustomAttributes,
                                               String expectedKey,
                                               String expectedName) {
        JSONArray attributes = JSON.parseArray(paymentCustomAttributes);
        assertNotNull(attributes);
        assertEquals(1, attributes.size());
        JSONObject attribute = attributes.getJSONObject(0);
        assertEquals(ZhishuAndYecaiFiledEnum.PAYMENT_NODE_TYPE.getZhishuFiled(),
                attribute.getString("attribute_code"));
        JSONObject attributeValue = attribute.getJSONObject("attribute_value");
        assertNotNull(attributeValue);
        assertEquals(expectedKey, attributeValue.getString("key"));
        assertEquals(expectedName, attributeValue.getString("name"));
    }

    private void assertOptionAttribute(JSONArray form,
                                       String attributeCode,
                                       String expectedKey,
                                       String expectedName) {
        JSONObject attribute = findAttribute(form, attributeCode);
        assertNotNull(attribute);
        JSONObject attributeValue = attribute.getJSONObject("attribute_value");
        assertNotNull(attributeValue);
        assertEquals(expectedKey, attributeValue.getString("key"));
        assertEquals(expectedName, attributeValue.getString("name"));
    }

    private void assertEmployeeAttribute(JSONArray form, String attributeCode, String... expectedUserIds) {
        JSONObject attribute = findAttribute(form, attributeCode);
        assertNotNull(attribute);
        assertEquals("employee", attribute.getString("attribute_type"));
        JSONArray attributeValue = attribute.getJSONArray("attribute_value");
        assertNotNull(attributeValue);
        assertEquals(expectedUserIds.length, attributeValue.size());
        for (int index = 0; index < expectedUserIds.length; index++) {
            JSONObject employee = attributeValue.getJSONObject(index);
            assertEquals(expectedUserIds[index], employee.getString("user_id"));
            assertEquals("lark_user_id", employee.getString("user_id_type"));
        }
    }

    private JSONObject findAttribute(JSONArray form, String attributeCode) {
        if (form == null) {
            return null;
        }
        for (int index = 0; index < form.size(); index++) {
            JSONObject attribute = form.getJSONObject(index);
            if (attributeCode.equals(attribute.getString("attribute_code"))) {
                return attribute;
            }
        }
        return null;
    }

    private QueryContractCategoryResponse buildCategoryResponse() {
        QueryContractCategoryResponse response = new QueryContractCategoryResponse();
        response.setCode(0);
        response.setMsg("success");
        QueryContractCategoryResponse.DataInfo data = new QueryContractCategoryResponse.DataInfo();

        List<QueryContractCategoryResponse.CategoryResource> resources = new ArrayList<>();
        resources.add(category("General Contract", "TPL-G", "CAT-G"));
        resources.add(category("General Contract Duplicate", "TPL-G", "CAT-G-SECOND"));
        QueryContractCategoryResponse.CategoryResource parent = category("Parent", "PARENT", "PARENT");
        parent.setChildren(Collections.singletonList(category("Anchor Contract", "TPL-A", "CAT-A")));
        resources.add(parent);

        data.setCategoryResources(resources);
        response.setData(data);
        return response;
    }

    private QueryContractCategoryResponse.CategoryResource category(String name, String number, String abbreviation) {
        QueryContractCategoryResponse.CategoryResource resource = new QueryContractCategoryResponse.CategoryResource();
        resource.setName(name);
        resource.setNumber(number);
        resource.setAbbreviation(abbreviation);
        resource.setId("id-" + abbreviation);
        return resource;
    }

    private UploadContractFileResponse buildUploadFileResponse(Integer code, String fileId) {
        UploadContractFileResponse response = new UploadContractFileResponse();
        response.setCode(code);
        response.setMsg(code != null && code == 0 ? "success" : "failed");
        if (fileId != null) {
            UploadContractFileResponse.DataInfo data = new UploadContractFileResponse.DataInfo();
            data.setFileId(fileId);
            response.setData(data);
        }
        return response;
    }

    private ZhishuCreateContractResponse buildContractResponse(Integer code, String msg,
                                                               String contractId, String contractNumber) {
        ZhishuCreateContractResponse response = new ZhishuCreateContractResponse();
        response.setCode(code);
        response.setMsg(msg);
        if (code != null && code == 0) {
            ZhishuCreateContractResponse.ContractData data = new ZhishuCreateContractResponse.ContractData();
            ZhishuCreateContractResponse.ContractInfo contract = new ZhishuCreateContractResponse.ContractInfo();
            contract.setContractId(contractId);
            contract.setContractNumber(contractNumber);
            data.setContract(contract);
            response.setData(data);
        }
        return response;
    }

    private ContractsSearchResponse buildSearchResponse(String... contractNumbers) {
        ContractsSearchResponse response = new ContractsSearchResponse();
        response.setCode(0);
        response.setMsg("success");
        ContractsSearchResponse.DataInfo dataInfo = new ContractsSearchResponse.DataInfo();
        List<ContractQueryResponse> items = new ArrayList<>();
        if (contractNumbers != null) {
            for (String contractNumber : contractNumbers) {
                ContractQueryResponse item = new ContractQueryResponse();
                item.setContractId(100000L + items.size());
                item.setContractNumber(contractNumber);
                items.add(item);
            }
        }
        dataInfo.setItems(items);
        response.setData(dataInfo);
        return response;
    }
}
