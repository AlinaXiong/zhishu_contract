package com.hero.middleware.controller;

import com.hero.middleware.annotation.SkipApiLogTable;
import com.hero.middleware.common.Result;
import com.hero.middleware.dto.ApproveContractToNodeDTO;
import com.hero.middleware.dto.ApproveContractToNodeResultDTO;
import com.hero.middleware.dto.DeleteDraftContractsResultDTO;
import com.hero.middleware.dto.HistoryContractSyncDTO;
import com.hero.middleware.dto.HistoryContractSyncResultDTO;
import com.hero.middleware.dto.HistoryContractValidateResultDTO;
import com.hero.middleware.dto.YeCaiContractSyncDTO;
import com.hero.middleware.dto.YeCaiContractSyncResultDTO;
import com.hero.middleware.service.ZhiShuSynService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Api(tags = "智书历史合同同步")
@Slf4j
@RestController
@RequestMapping("/api/contract/syn")
@SkipApiLogTable
public class ContractSynController {

    private static final long MAX_EXTRACTED_CONTRACT_FILE_BYTES = 5L * 1024 * 1024 * 1024;

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

    @ApiOperation("多线程同步历史合同到智书（上传文件）")
    @PostMapping(value = "/history/multi-thread", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<HistoryContractSyncResultDTO> syncHistoryContractsMultiThreadByFile(
            @RequestPart("excelFile") MultipartFile excelFile,
            @RequestPart("contractFile") MultipartFile contractFile,
            @RequestParam("contractNumbers") List<String> contractNumbers,
            @RequestParam(value = "contractStatusCode", required = false) String contractStatusCode,
            @RequestParam(value = "threadCount", required = false) Integer threadCount,
            @RequestParam(value = "batchSize", required = false) Integer batchSize) {
        if (contractNumbers == null || contractNumbers.isEmpty()) {
            return Result.error(400, "合同编码集合不能为空");
        }
        if (excelFile == null || excelFile.isEmpty()) {
            return Result.error(400, "导入模板Excel文件不能为空");
        }
        if (contractFile == null || contractFile.isEmpty()) {
            return Result.error(400, "合同附件压缩包不能为空");
        }
        if (!isZipFile(contractFile)) {
            return Result.error(400, "contractFile必须为zip压缩包");
        }

        Path temporaryDirectory = null;
        try {
            temporaryDirectory = Files.createTempDirectory("history-contract-upload-");
            Path excelFilePath = saveUpload(temporaryDirectory, excelFile, "history-contract.xlsx");
            Path contractFilesDirectory = extractContractFiles(temporaryDirectory, contractFile);
            Path contractFileFallbackRoot = resolveContractFileFallbackRoot(contractFilesDirectory, contractNumbers);

            HistoryContractSyncDTO request = new HistoryContractSyncDTO();
            request.setContractNumbers(normalizeMultipartContractNumbers(contractNumbers));
            request.setFilePath(excelFilePath.toString());
            request.setContractFileFallbackRoot(contractFileFallbackRoot.toString());
            request.setContractStatusCode(contractStatusCode);
            request.setThreadCount(threadCount);
            request.setBatchSize(batchSize);

            String validateMessage = validateMultiThreadHistorySyncRequest(request);
            if (validateMessage != null) {
                log.warn("智书历史合同上传同步请求参数错误：{}", validateMessage);
                return Result.error(400, validateMessage);
            }

            log.info("接收智书历史合同多线程上传同步请求，合同编码数={}，contractFileName={}，excelFileName={}，threadCount={}，batchSize={}",
                    request.getContractNumbers().size(), contractFile.getOriginalFilename(), excelFile.getOriginalFilename(),
                    threadCount, batchSize);
            HistoryContractSyncResultDTO result = zhiShuSynService.syncHistoryContractsMultiThread(request);
            log.info("智书历史合同多线程上传同步请求处理完成，结果：{}", result);
            return Result.success(result);
        } catch (IOException e) {
            log.error("保存智书历史合同上传文件失败，错误={}", e.getMessage(), e);
            return Result.error(400, "保存上传文件失败：" + e.getMessage());
        } finally {
            deleteDirectory(temporaryDirectory);
        }
    }

    @ApiOperation("多线程校验历史合同到智书")
    @PostMapping("/history/multi-thread/validate")
    public Result<HistoryContractValidateResultDTO> validateHistoryContractsMultiThread(
            @RequestBody(required = false) HistoryContractSyncDTO request) {
        log.info("接收智书历史合同多线程校验请求，请求参数：{}", request);
        String validateMessage = validateMultiThreadHistorySyncRequest(request);
        if (validateMessage != null) {
            log.warn("智书历史合同多线程校验请求参数错误：{}", validateMessage);
            return Result.error(400, validateMessage);
        }
        HistoryContractValidateResultDTO result = zhiShuSynService.validateHistoryContractsMultiThread(request);
        log.info("智书历史合同多线程校验请求处理完成，结果：{}", result);
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

    private Path saveUpload(Path directory, MultipartFile file, String fallbackFileName) throws IOException {
        String fileName = trimToNull(file.getOriginalFilename());
        if (fileName == null) {
            fileName = fallbackFileName;
        } else {
            try {
                fileName = Paths.get(fileName).getFileName().toString();
            } catch (RuntimeException e) {
                fileName = fallbackFileName;
            }
        }
        Path target = directory.resolve(fileName).normalize();
        if (!target.getParent().equals(directory)) {
            throw new IOException("非法上传文件名");
        }
        file.transferTo(target.toFile());
        return target;
    }

    private boolean isZipFile(MultipartFile file) {
        String originalFilename = trimToNull(file.getOriginalFilename());
        return originalFilename != null && originalFilename.toLowerCase(Locale.ROOT).endsWith(".zip");
    }

    private Path extractContractFiles(Path temporaryDirectory, MultipartFile contractFile) throws IOException {
        Path contractFilesDirectory = Files.createDirectories(temporaryDirectory.resolve("contract-files"));
        int extractedFileCount = 0;
        long extractedFileBytes = 0;
        byte[] buffer = new byte[8192];
        try (InputStream inputStream = contractFile.getInputStream();
             ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                if (zipEntry.isDirectory()) {
                    continue;
                }
                Path relativePath = resolveRelativePath(zipEntry.getName());
                Path target = contractFilesDirectory.resolve(relativePath).normalize();
                if (!target.startsWith(contractFilesDirectory)) {
                    throw new IOException("非法合同附件压缩路径");
                }
                Files.createDirectories(target.getParent());
                try (OutputStream outputStream = Files.newOutputStream(target)) {
                    int bytesRead;
                    while ((bytesRead = zipInputStream.read(buffer)) != -1) {
                        extractedFileBytes += bytesRead;
                        if (extractedFileBytes > MAX_EXTRACTED_CONTRACT_FILE_BYTES) {
                            throw new IOException("合同附件解压后总大小不能超过5GB");
                        }
                        outputStream.write(buffer, 0, bytesRead);
                    }
                }
                extractedFileCount++;
            }
        }
        if (extractedFileCount == 0) {
            throw new IOException("合同附件文件夹中没有有效文件");
        }
        return contractFilesDirectory;
    }

    private Path resolveRelativePath(String sourcePath) throws IOException {
        String fileName = trimToNull(sourcePath);
        if (fileName == null) {
            throw new IOException("合同附件缺少文件名");
        }
        String normalizedFileName = fileName.replace('\\', '/');
        if (normalizedFileName.regionMatches(true, 0, "C:/fakepath/", 0, "C:/fakepath/".length())) {
            normalizedFileName = normalizedFileName.substring("C:/fakepath/".length());
        }
        while (normalizedFileName.startsWith("/")) {
            normalizedFileName = normalizedFileName.substring(1);
        }
        try {
            Path relativePath = Paths.get(normalizedFileName).normalize();
            if (relativePath.isAbsolute() || relativePath.getNameCount() == 0 || relativePath.startsWith("..")) {
                throw new IOException("非法合同附件文件路径：" + fileName);
            }
            return relativePath;
        } catch (RuntimeException e) {
            throw new IOException("非法合同附件文件路径：" + fileName, e);
        }
    }

    private List<String> normalizeMultipartContractNumbers(List<String> contractNumbers) {
        List<String> result = new ArrayList<>();
        for (String contractNumber : contractNumbers) {
            String normalizedContractNumber = trimToNull(contractNumber);
            if (normalizedContractNumber != null) {
                result.add(normalizedContractNumber);
            }
        }
        return result;
    }

    private Path resolveContractFileFallbackRoot(Path contractFilesDirectory, List<String> contractNumbers)
            throws IOException {
        if (containsContractDirectory(contractFilesDirectory, contractNumbers)) {
            return contractFilesDirectory;
        }
        try (Stream<Path> paths = Files.list(contractFilesDirectory)) {
            List<Path> candidateRoots = new ArrayList<>();
            paths.filter(Files::isDirectory)
                    .filter(path -> containsContractDirectory(path, contractNumbers))
                    .forEach(candidateRoots::add);
            if (candidateRoots.size() == 1) {
                return candidateRoots.get(0);
            }
        }
        throw new IOException("合同附件文件夹未包含合同编码目录");
    }

    private boolean containsContractDirectory(Path root, List<String> contractNumbers) {
        for (String contractNumber : contractNumbers) {
            String normalizedContractNumber = trimToNull(contractNumber);
            if (normalizedContractNumber != null && Files.isDirectory(root.resolve(normalizedContractNumber))) {
                return true;
            }
        }
        return false;
    }

    private void deleteDirectory(Path directory) {
        if (directory == null) {
            return;
        }
        try (Stream<Path> paths = Files.walk(directory)) {
            paths
                    .sorted((left, right) -> right.compareTo(left))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            log.warn("删除临时上传文件失败，path={}，错误={}", path, e.getMessage());
                        }
                    });
        } catch (IOException e) {
            log.warn("清理临时上传目录失败，path={}，错误={}", directory, e.getMessage());
        }
    }
}
