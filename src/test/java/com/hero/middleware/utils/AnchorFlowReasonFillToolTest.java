package com.hero.middleware.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 主播流程异常原因回填工具，用于按合同编码将异常 JSON 的 reason 写回主表。
 */
class AnchorFlowReasonFillToolTest {

    private static final String ENABLED_PROPERTY = "anchor.reason.enabled";
    private static final Path SOURCE_FILE = Paths.get("C:\\Users\\AAA\\Downloads\\签署反商业贿赂协议6.25终版.xlsx");
    private static final Path ERROR_INFO_FILE = Paths.get("D:\\Project Word\\公司项目文件\\英雄电竞\\反商业-异常数据信息.txt");
    private static final String MAIN_SHEET_NAME = "反商业贿赂协议";
    private static final String CONTRACT_NUMBER_FIELD = "contractNumber";
    private static final String REASON_FIELD = "reason";
    private static final int HEADER_ROW_INDEX = 0;
    private static final int DATA_START_ROW_INDEX = 1;
    private static final int CONTRACT_COLUMN_INDEX = 0;
    private static final int EXPECTED_CONTRACT_COUNT = 20;
    private static final int EXPECTED_MATCHED_COUNT = 20;
    private static final int DEFAULT_REASON_COLUMN_WIDTH = 100 * 256;
    private static final DateTimeFormatter BACKUP_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * 执行异常原因回填，并在写入前后校验关键数据。
     */
    @Test
    void fillAnchorFlowReason() throws Exception {
        Assumptions.assumeTrue(Boolean.getBoolean(ENABLED_PROPERTY),
                "默认跳过 Excel 写入工具，如需执行请增加 -D" + ENABLED_PROPERTY + "=true");

        checkFileExists(SOURCE_FILE);
        checkFileExists(ERROR_INFO_FILE);
        Map<String, String> reasonByContractNumber = readReasonByContractNumber();
        assertEquals(EXPECTED_CONTRACT_COUNT, reasonByContractNumber.size(), "JSON 有效合同号数量不符合预期");

        Path backupPath = backupSourceFile();
        DataFormatter formatter = new DataFormatter();
        FillResult fillResult;
        int reasonColumnIndex;

        try (Workbook workbook = openWorkbook(SOURCE_FILE)) {
            Sheet mainSheet = getRequiredSheet(workbook, MAIN_SHEET_NAME);
            reasonColumnIndex = findOrCreateReasonColumn(mainSheet, formatter);

            fillResult = fillReason(mainSheet, reasonColumnIndex, reasonByContractNumber, formatter);
            assertFillResult(fillResult);
            assertReasonHeader(mainSheet, reasonColumnIndex, formatter);
            assertReasonValues(mainSheet, reasonColumnIndex, reasonByContractNumber, formatter);

            writeWorkbook(workbook, SOURCE_FILE);
        }

        verifySavedWorkbook(reasonByContractNumber, formatter);
        printSummary(backupPath, reasonByContractNumber.size(), fillResult.getMatchedContractNumbers().size(),
                reasonColumnIndex);
    }

    /**
     * 校验文件是否存在，避免路径错误时继续执行写入。
     */
    private void checkFileExists(Path filePath) {
        assertTrue(Files.exists(filePath), "文件不存在：" + filePath);
        assertTrue(Files.isRegularFile(filePath), "路径不是文件：" + filePath);
    }

    /**
     * 读取异常 JSON，并按合同编码聚合 reason。
     */
    private Map<String, String> readReasonByContractNumber() throws Exception {
        String jsonText = new String(Files.readAllBytes(ERROR_INFO_FILE), StandardCharsets.UTF_8);
        JSONArray jsonArray = JSON.parseArray(jsonText);
        Map<String, String> reasonByContractNumber = new LinkedHashMap<>();

        // 同一合同号出现多次时合并 reason，避免后续记录覆盖前面的异常信息。
        for (int index = 0; index < jsonArray.size(); index++) {
            JSONObject item = jsonArray.getJSONObject(index);
            String contractNumber = trimToNull(item.getString(CONTRACT_NUMBER_FIELD));
            if (contractNumber == null) {
                continue;
            }
            String reason = item.getString(REASON_FIELD);
            appendReason(reasonByContractNumber, contractNumber, reason == null ? "" : reason);
        }
        return reasonByContractNumber;
    }

    /**
     * 合并同一合同编码的异常原因。
     */
    private void appendReason(Map<String, String> reasonByContractNumber, String contractNumber, String reason) {
        String oldReason = reasonByContractNumber.get(contractNumber);
        if (oldReason == null || oldReason.isEmpty()) {
            reasonByContractNumber.put(contractNumber, reason);
            return;
        }
        if (reason == null || reason.isEmpty() || oldReason.contains(reason)) {
            return;
        }
        reasonByContractNumber.put(contractNumber, oldReason + "\n" + reason);
    }

    /**
     * 打开 Excel 工作簿并返回可读写对象。
     */
    private Workbook openWorkbook(Path filePath) throws Exception {
        try (InputStream inputStream = Files.newInputStream(filePath)) {
            return WorkbookFactory.create(inputStream);
        }
    }

    /**
     * 将修改后的工作簿写回指定文件。
     */
    private void writeWorkbook(Workbook workbook, Path filePath) throws Exception {
        try (OutputStream outputStream = Files.newOutputStream(filePath)) {
            workbook.write(outputStream);
        }
    }

    /**
     * 在写入前备份源文件，方便人工回滚。
     */
    private Path backupSourceFile() throws Exception {
        String fileName = SOURCE_FILE.getFileName().toString();
        int suffixIndex = fileName.lastIndexOf('.');
        String name = suffixIndex < 0 ? fileName : fileName.substring(0, suffixIndex);
        String suffix = suffixIndex < 0 ? "" : fileName.substring(suffixIndex);
        String backupFileName = name + "_reason_backup_" + LocalDateTime.now().format(BACKUP_TIME_FORMAT) + suffix;
        Path backupPath = ensureUniqueBackupPath(SOURCE_FILE.resolveSibling(backupFileName));
        Files.copy(SOURCE_FILE, backupPath, StandardCopyOption.COPY_ATTRIBUTES);
        return backupPath;
    }

    /**
     * 生成不覆盖已有文件的备份路径。
     */
    private Path ensureUniqueBackupPath(Path backupPath) {
        if (!Files.exists(backupPath)) {
            return backupPath;
        }
        String fileName = backupPath.getFileName().toString();
        int suffixIndex = fileName.lastIndexOf('.');
        String name = suffixIndex < 0 ? fileName : fileName.substring(0, suffixIndex);
        String suffix = suffixIndex < 0 ? "" : fileName.substring(suffixIndex);
        int index = 1;
        Path candidate = backupPath;
        while (Files.exists(candidate)) {
            candidate = backupPath.resolveSibling(name + "_" + index + suffix);
            index++;
        }
        return candidate;
    }

    /**
     * 获取必需工作表，缺失时抛出明确错误。
     */
    private Sheet getRequiredSheet(Workbook workbook, String sheetName) {
        Sheet sheet = workbook.getSheet(sheetName);
        if (sheet == null) {
            throw new IllegalStateException("未找到工作表：" + sheetName);
        }
        return sheet;
    }

    /**
     * 查找或创建 reason 表头列，保证工具重复执行时不会反复新增列。
     */
    private int findOrCreateReasonColumn(Sheet mainSheet, DataFormatter formatter) {
        Row headerRow = mainSheet.getRow(HEADER_ROW_INDEX);
        if (headerRow == null) {
            headerRow = mainSheet.createRow(HEADER_ROW_INDEX);
        }
        short lastCellNum = headerRow.getLastCellNum();
        int headerCellCount = lastCellNum < 0 ? 0 : lastCellNum;
        for (int cellIndex = 0; cellIndex < headerCellCount; cellIndex++) {
            String header = trimToNull(readCellText(headerRow.getCell(cellIndex), formatter));
            if (REASON_FIELD.equalsIgnoreCase(header)) {
                return cellIndex;
            }
        }

        // 当前表头不存在 reason 时，在最后一列之后追加新列。
        int reasonColumnIndex = headerCellCount;
        Cell reasonHeaderCell = headerRow.createCell(reasonColumnIndex, CellType.STRING);
        reasonHeaderCell.setCellValue(REASON_FIELD);
        copyHeaderStyle(headerRow, reasonHeaderCell, reasonColumnIndex);
        mainSheet.setColumnWidth(reasonColumnIndex, DEFAULT_REASON_COLUMN_WIDTH);
        return reasonColumnIndex;
    }

    /**
     * 为新增的 reason 表头复制左侧表头样式。
     */
    private void copyHeaderStyle(Row headerRow, Cell reasonHeaderCell, int reasonColumnIndex) {
        if (reasonColumnIndex <= 0) {
            return;
        }
        Cell previousHeaderCell = headerRow.getCell(reasonColumnIndex - 1);
        if (previousHeaderCell == null || previousHeaderCell.getCellStyle() == null) {
            return;
        }
        CellStyle headerStyle = reasonHeaderCell.getSheet().getWorkbook().createCellStyle();
        headerStyle.cloneStyleFrom(previousHeaderCell.getCellStyle());
        reasonHeaderCell.setCellStyle(headerStyle);
    }

    /**
     * 根据合同编码将 reason 写入主表对应行。
     */
    private FillResult fillReason(Sheet mainSheet, int reasonColumnIndex,
                                  Map<String, String> reasonByContractNumber, DataFormatter formatter) {
        Set<String> matchedContractNumbers = new LinkedHashSet<>();
        Map<String, CellStyle> styleCache = new LinkedHashMap<>();

        // 仅更新 JSON 中存在合同编码的行，不清空其它单元格内容。
        for (int rowIndex = DATA_START_ROW_INDEX; rowIndex <= mainSheet.getLastRowNum(); rowIndex++) {
            Row row = mainSheet.getRow(rowIndex);
            String contractNumber = trimToNull(readCellText(row == null ? null : row.getCell(CONTRACT_COLUMN_INDEX),
                    formatter));
            if (!reasonByContractNumber.containsKey(contractNumber)) {
                continue;
            }
            if (row == null) {
                row = mainSheet.createRow(rowIndex);
            }
            Cell reasonCell = row.getCell(reasonColumnIndex);
            if (reasonCell == null) {
                reasonCell = row.createCell(reasonColumnIndex, CellType.STRING);
            }
            reasonCell.setCellStyle(buildReasonCellStyle(row, reasonColumnIndex, styleCache));
            reasonCell.setCellValue(reasonByContractNumber.get(contractNumber));
            matchedContractNumbers.add(contractNumber);
        }

        List<String> missingContractNumbers = new ArrayList<>();
        for (String contractNumber : reasonByContractNumber.keySet()) {
            if (!matchedContractNumbers.contains(contractNumber)) {
                missingContractNumbers.add(contractNumber);
            }
        }
        return new FillResult(matchedContractNumbers, missingContractNumbers);
    }

    /**
     * 构建 reason 数据单元格样式，设置为文本并自动换行。
     */
    private CellStyle buildReasonCellStyle(Row row, int reasonColumnIndex, Map<String, CellStyle> styleCache) {
        Cell baseCell = reasonColumnIndex > 0 ? row.getCell(reasonColumnIndex - 1) : null;
        CellStyle baseStyle = baseCell == null ? null : baseCell.getCellStyle();
        String cacheKey = baseStyle == null ? "REASON_TEXT" : "REASON_TEXT:" + baseStyle.getIndex();
        CellStyle cachedStyle = styleCache.get(cacheKey);
        if (cachedStyle != null) {
            return cachedStyle;
        }
        Workbook workbook = row.getSheet().getWorkbook();
        CellStyle reasonStyle = workbook.createCellStyle();
        if (baseStyle != null) {
            reasonStyle.cloneStyleFrom(baseStyle);
        }
        DataFormat dataFormat = workbook.createDataFormat();
        reasonStyle.setDataFormat(dataFormat.getFormat("@"));
        reasonStyle.setWrapText(true);
        styleCache.put(cacheKey, reasonStyle);
        return reasonStyle;
    }

    /**
     * 校验回填结果的匹配数量和缺失合同编码。
     */
    private void assertFillResult(FillResult fillResult) {
        assertEquals(EXPECTED_MATCHED_COUNT, fillResult.getMatchedContractNumbers().size(), "Excel 匹配数量不符合预期");
        assertTrue(fillResult.getMissingContractNumbers().isEmpty(),
                "存在未匹配合同编码：" + fillResult.getMissingContractNumbers());
    }

    /**
     * 校验 reason 表头是否存在。
     */
    private void assertReasonHeader(Sheet mainSheet, int reasonColumnIndex, DataFormatter formatter) {
        Row headerRow = mainSheet.getRow(HEADER_ROW_INDEX);
        assertNotNull(headerRow, "主表表头行不存在");
        assertEquals(REASON_FIELD, readCellText(headerRow.getCell(reasonColumnIndex), formatter), "reason 表头不符合预期");
    }

    /**
     * 校验每个匹配行写入的 reason 是否与 JSON 一致。
     */
    private int assertReasonValues(Sheet mainSheet, int reasonColumnIndex,
                                   Map<String, String> reasonByContractNumber, DataFormatter formatter) {
        Set<String> matchedContractNumbers = new LinkedHashSet<>();
        for (int rowIndex = DATA_START_ROW_INDEX; rowIndex <= mainSheet.getLastRowNum(); rowIndex++) {
            Row row = mainSheet.getRow(rowIndex);
            String contractNumber = trimToNull(readCellText(row == null ? null : row.getCell(CONTRACT_COLUMN_INDEX),
                    formatter));
            if (!reasonByContractNumber.containsKey(contractNumber)) {
                continue;
            }
            Cell reasonCell = row.getCell(reasonColumnIndex);
            assertNotNull(reasonCell, "合同编码 " + contractNumber + " 的 reason 单元格不存在");
            assertEquals(reasonByContractNumber.get(contractNumber), readCellText(reasonCell, formatter),
                    "合同编码 " + contractNumber + " 的 reason 不符合预期");
            matchedContractNumbers.add(contractNumber);
        }
        assertEquals(reasonByContractNumber.size(), matchedContractNumbers.size(), "保存前 reason 匹配数量不符合预期");
        return matchedContractNumbers.size();
    }

    /**
     * 写入后重新打开文件，校验保存后的 reason 数据。
     */
    private void verifySavedWorkbook(Map<String, String> reasonByContractNumber, DataFormatter formatter)
            throws Exception {
        try (Workbook workbook = openWorkbook(SOURCE_FILE)) {
            Sheet mainSheet = getRequiredSheet(workbook, MAIN_SHEET_NAME);
            int reasonColumnIndex = findReasonColumn(mainSheet, formatter);
            assertReasonHeader(mainSheet, reasonColumnIndex, formatter);
            assertEquals(EXPECTED_MATCHED_COUNT,
                    assertReasonValues(mainSheet, reasonColumnIndex, reasonByContractNumber, formatter),
                    "保存后 reason 匹配数量不符合预期");
        }
    }

    /**
     * 查找已存在的 reason 表头列。
     */
    private int findReasonColumn(Sheet mainSheet, DataFormatter formatter) {
        Row headerRow = mainSheet.getRow(HEADER_ROW_INDEX);
        if (headerRow == null) {
            throw new IllegalStateException("主表表头行不存在");
        }
        short lastCellNum = headerRow.getLastCellNum();
        int headerCellCount = lastCellNum < 0 ? 0 : lastCellNum;
        for (int cellIndex = 0; cellIndex < headerCellCount; cellIndex++) {
            String header = trimToNull(readCellText(headerRow.getCell(cellIndex), formatter));
            if (REASON_FIELD.equalsIgnoreCase(header)) {
                return cellIndex;
            }
        }
        throw new IllegalStateException("未找到 reason 表头列");
    }

    /**
     * 读取单元格展示文本，用于合同编码匹配和 reason 校验。
     */
    private String readCellText(Cell cell, DataFormatter formatter) {
        if (cell == null) {
            return "";
        }
        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue();
        }
        return formatter.formatCellValue(cell);
    }

    /**
     * 将空白字符串统一转为空值。
     */
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimValue = value.trim();
        return trimValue.isEmpty() ? null : trimValue;
    }

    /**
     * 打印工具执行摘要，方便人工核对。
     */
    private void printSummary(Path backupPath, int jsonContractCount, int matchedCount, int reasonColumnIndex) {
        System.out.println("主播流程异常原因回填完成");
        System.out.println("JSON 有效合同号数量：" + jsonContractCount);
        System.out.println("Excel 匹配数量：" + matchedCount);
        System.out.println("reason 写入列序号：" + (reasonColumnIndex + 1));
        System.out.println("备份文件路径：" + backupPath);
    }

    /**
     * 回填结果摘要，记录匹配和未匹配的合同编码。
     */
    private static class FillResult {

        private final Set<String> matchedContractNumbers;
        private final List<String> missingContractNumbers;

        /**
         * 创建回填结果摘要。
         */
        private FillResult(Set<String> matchedContractNumbers, List<String> missingContractNumbers) {
            this.matchedContractNumbers = matchedContractNumbers;
            this.missingContractNumbers = missingContractNumbers;
        }

        /**
         * 获取匹配到的合同编码。
         */
        private Set<String> getMatchedContractNumbers() {
            return matchedContractNumbers;
        }

        /**
         * 获取未匹配到的合同编码。
         */
        private List<String> getMissingContractNumbers() {
            return missingContractNumbers;
        }
    }
}
