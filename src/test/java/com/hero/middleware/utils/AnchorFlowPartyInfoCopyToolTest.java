package com.hero.middleware.utils;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.OutputStream;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 主播流程合同字段辅助复制工具，用于按主表合同编码同步对方信息和我方信息。
 */
class AnchorFlowPartyInfoCopyToolTest {

    private static final String ENABLED_PROPERTY = "anchor.copy.enabled";
    private static final Path SOURCE_FILE = Paths.get("C:\\Users\\AAA\\Downloads\\智书合同字段_一般流程_主播流程.xlsx");
    private static final Path LOOKUP_FILE = Paths.get("C:\\Users\\AAA\\Downloads\\智书合同字段_主播流程_20260629_终版 (2).xlsx");
    private static final String MAIN_SHEET_NAME = "主播流程主表";
    private static final String LOOKUP_COUNTER_PARTY_SHEET_NAME = "对方信息";
    private static final String LOOKUP_OUR_PARTY_SHEET_NAME = "我方信息";
    private static final String TARGET_COUNTER_PARTY_SHEET_NAME = "主播流程_对方信息";
    private static final String TARGET_OUR_PARTY_SHEET_NAME = "主播流程_我方信息";
    private static final String START_CONTRACT_CODE = "VMSB-B-202504006";
    private static final String START_OUR_PARTY_CODE = "006000";
    private static final int CONTRACT_COLUMN_INDEX = 0;
    private static final int PARTY_CODE_COLUMN_INDEX = 1;
    private static final int DATA_START_ROW_INDEX = 1;
    private static final int EXPECTED_CONTRACT_COUNT = 423;
    private static final int EXPECTED_COUNTER_PARTY_ROWS = 480;
    private static final int EXPECTED_OUR_PARTY_ROWS = 424;
    private static final DateTimeFormatter BACKUP_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * 执行主播流程对方信息和我方信息复制，并在写入前后校验关键数据。
     */
    @Test
    void copyAnchorFlowPartyInfo() throws Exception {
        Assumptions.assumeTrue(Boolean.getBoolean(ENABLED_PROPERTY),
                "默认跳过 Excel 写入工具，如需执行请增加 -D" + ENABLED_PROPERTY + "=true");

        checkFileExists(SOURCE_FILE);
        checkFileExists(LOOKUP_FILE);
        Path backupPath = backupSourceFile();
        DataFormatter formatter = new DataFormatter();
        CopyResult counterPartyResult;
        CopyResult ourPartyResult;
        List<String> contractCodes;

        try (Workbook sourceWorkbook = openWorkbook(SOURCE_FILE);
             Workbook lookupWorkbook = openWorkbook(LOOKUP_FILE)) {
            Sheet mainSheet = getRequiredSheet(sourceWorkbook, MAIN_SHEET_NAME);
            Sheet lookupCounterPartySheet = getRequiredSheet(lookupWorkbook, LOOKUP_COUNTER_PARTY_SHEET_NAME);
            Sheet lookupOurPartySheet = getRequiredSheet(lookupWorkbook, LOOKUP_OUR_PARTY_SHEET_NAME);
            Sheet targetCounterPartySheet = getRequiredSheet(sourceWorkbook, TARGET_COUNTER_PARTY_SHEET_NAME);
            Sheet targetOurPartySheet = getRequiredSheet(sourceWorkbook, TARGET_OUR_PARTY_SHEET_NAME);

            contractCodes = readContractCodes(mainSheet, formatter);
            assertEquals(EXPECTED_CONTRACT_COUNT, contractCodes.size(), "主表合同编码数量不符合预期");

            counterPartyResult = copyMatchedRows(lookupCounterPartySheet, targetCounterPartySheet,
                    contractCodes, formatter);
            ourPartyResult = copyMatchedRows(lookupOurPartySheet, targetOurPartySheet, contractCodes, formatter);

            assertCopyResult(counterPartyResult, EXPECTED_COUNTER_PARTY_ROWS, "对方信息");
            assertCopyResult(ourPartyResult, EXPECTED_OUR_PARTY_ROWS, "我方信息");
            assertStartOurPartyCode(targetOurPartySheet, formatter);

            writeWorkbook(sourceWorkbook, SOURCE_FILE);
        }

        verifySavedWorkbook(formatter);
        printSummary(backupPath, contractCodes.size(), counterPartyResult.getWrittenRows(),
                ourPartyResult.getWrittenRows());
    }

    /**
     * 校验文件是否存在，避免路径错误时继续执行写入。
     */
    private void checkFileExists(Path filePath) {
        assertTrue(Files.exists(filePath), "文件不存在：" + filePath);
        assertTrue(Files.isRegularFile(filePath), "路径不是文件：" + filePath);
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
        String backupFileName = name + "_backup_" + LocalDateTime.now().format(BACKUP_TIME_FORMAT) + suffix;
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
     * 从主播流程主表读取起始合同编码所在行到表尾的合同编码。
     */
    private List<String> readContractCodes(Sheet mainSheet, DataFormatter formatter) {
        List<String> contractCodes = new ArrayList<>();
        boolean started = false;
        for (int rowIndex = DATA_START_ROW_INDEX; rowIndex <= mainSheet.getLastRowNum(); rowIndex++) {
            Row row = mainSheet.getRow(rowIndex);
            String contractCode = trimToNull(readCellText(row == null ? null : row.getCell(CONTRACT_COLUMN_INDEX),
                    formatter));
            if (!started && START_CONTRACT_CODE.equals(contractCode)) {
                started = true;
            }
            if (started && contractCode != null) {
                contractCodes.add(contractCode);
            }
        }
        if (!started) {
            throw new IllegalStateException("未在 " + MAIN_SHEET_NAME + " 的 A 列找到起始合同编码："
                    + START_CONTRACT_CODE);
        }
        return contractCodes;
    }

    /**
     * 按主表合同编码顺序复制匹配行到目标工作表。
     */
    private CopyResult copyMatchedRows(Sheet lookupSheet, Sheet targetSheet, List<String> contractCodes,
                                       DataFormatter formatter) {
        Map<String, List<Row>> lookupRows = collectLookupRows(lookupSheet, contractCodes, formatter);
        clearTargetDataRows(targetSheet);
        Workbook targetWorkbook = targetSheet.getWorkbook();
        Map<String, CellStyle> styleCache = new LinkedHashMap<>();
        int targetRowIndex = DATA_START_ROW_INDEX;
        int writtenRows = 0;
        List<String> missingContractCodes = new ArrayList<>();

        // 按主表顺序写入匹配行，同一合同多行时保留查找表中的原始顺序。
        for (String contractCode : contractCodes) {
            List<Row> rows = lookupRows.get(contractCode);
            if (rows == null || rows.isEmpty()) {
                missingContractCodes.add(contractCode);
                continue;
            }
            for (Row sourceRow : rows) {
                Row targetRow = targetSheet.createRow(targetRowIndex);
                copyRow(sourceRow, targetRow, targetWorkbook, styleCache, formatter);
                targetRowIndex++;
                writtenRows++;
            }
        }
        return new CopyResult(writtenRows, missingContractCodes);
    }

    /**
     * 收集查找表中合同编码匹配主表范围的行。
     */
    private Map<String, List<Row>> collectLookupRows(Sheet lookupSheet, List<String> contractCodes,
                                                     DataFormatter formatter) {
        Set<String> contractCodeSet = new LinkedHashSet<>(contractCodes);
        Map<String, List<Row>> lookupRows = new LinkedHashMap<>();
        for (int rowIndex = DATA_START_ROW_INDEX; rowIndex <= lookupSheet.getLastRowNum(); rowIndex++) {
            Row row = lookupSheet.getRow(rowIndex);
            String contractCode = trimToNull(readCellText(row == null ? null : row.getCell(CONTRACT_COLUMN_INDEX),
                    formatter));
            if (!contractCodeSet.contains(contractCode)) {
                continue;
            }
            if (!lookupRows.containsKey(contractCode)) {
                lookupRows.put(contractCode, new ArrayList<Row>());
            }
            lookupRows.get(contractCode).add(row);
        }
        return lookupRows;
    }

    /**
     * 清空目标工作表第 2 行及之后的数据行。
     */
    private void clearTargetDataRows(Sheet targetSheet) {
        for (int rowIndex = targetSheet.getLastRowNum(); rowIndex >= DATA_START_ROW_INDEX; rowIndex--) {
            Row row = targetSheet.getRow(rowIndex);
            if (row != null) {
                targetSheet.removeRow(row);
            }
        }
    }

    /**
     * 复制整行数据，B 列固定写为文本以保留前导 0。
     */
    private void copyRow(Row sourceRow, Row targetRow, Workbook targetWorkbook, Map<String, CellStyle> styleCache,
                         DataFormatter formatter) {
        targetRow.setHeight(sourceRow.getHeight());
        targetRow.setZeroHeight(sourceRow.getZeroHeight());
        if (sourceRow.getRowStyle() != null) {
            targetRow.setRowStyle(cloneStyle(targetWorkbook, sourceRow.getRowStyle(), false, styleCache));
        }
        int lastCellIndex = Math.max(sourceRow.getLastCellNum(), PARTY_CODE_COLUMN_INDEX + 1);
        for (int cellIndex = 0; cellIndex < lastCellIndex; cellIndex++) {
            Cell sourceCell = sourceRow.getCell(cellIndex);
            Cell targetCell = targetRow.createCell(cellIndex);
            boolean forceText = cellIndex == PARTY_CODE_COLUMN_INDEX;
            copyCell(sourceCell, targetCell, targetWorkbook, styleCache, formatter, forceText);
        }
    }

    /**
     * 复制单元格内容和基础样式，必要时强制文本格式。
     */
    private void copyCell(Cell sourceCell, Cell targetCell, Workbook targetWorkbook,
                          Map<String, CellStyle> styleCache, DataFormatter formatter, boolean forceText) {
        if (sourceCell == null) {
            if (forceText) {
                targetCell.setCellStyle(buildTextStyle(targetWorkbook, styleCache));
            }
            return;
        }
        if (sourceCell.getCellStyle() != null) {
            targetCell.setCellStyle(cloneStyle(targetWorkbook, sourceCell.getCellStyle(), forceText, styleCache));
        } else if (forceText) {
            targetCell.setCellStyle(buildTextStyle(targetWorkbook, styleCache));
        }
        if (forceText) {
            targetCell.setCellValue(readCellText(sourceCell, formatter));
            return;
        }
        copyCellValue(sourceCell, targetCell, formatter);
    }

    /**
     * 按源单元格类型复制值。
     */
    private void copyCellValue(Cell sourceCell, Cell targetCell, DataFormatter formatter) {
        CellType cellType = sourceCell.getCellType();
        if (cellType == CellType.FORMULA) {
            cellType = sourceCell.getCachedFormulaResultType();
        }
        switch (cellType) {
            case STRING:
                targetCell.setCellValue(sourceCell.getStringCellValue());
                break;
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(sourceCell)) {
                    targetCell.setCellValue(sourceCell.getDateCellValue());
                } else {
                    targetCell.setCellValue(sourceCell.getNumericCellValue());
                }
                break;
            case BOOLEAN:
                targetCell.setCellValue(sourceCell.getBooleanCellValue());
                break;
            case ERROR:
                targetCell.setCellErrorValue(sourceCell.getErrorCellValue());
                break;
            case BLANK:
                targetCell.setBlank();
                break;
            default:
                targetCell.setCellValue(readCellText(sourceCell, formatter));
                break;
        }
    }

    /**
     * 克隆样式，并在需要时覆盖为文本格式。
     */
    private CellStyle cloneStyle(Workbook targetWorkbook, CellStyle sourceStyle, boolean forceText,
                                 Map<String, CellStyle> styleCache) {
        String cacheKey = sourceStyle.getIndex() + ":" + forceText;
        CellStyle cachedStyle = styleCache.get(cacheKey);
        if (cachedStyle != null) {
            return cachedStyle;
        }
        CellStyle targetStyle = targetWorkbook.createCellStyle();
        targetStyle.cloneStyleFrom(sourceStyle);
        if (forceText) {
            DataFormat dataFormat = targetWorkbook.createDataFormat();
            targetStyle.setDataFormat(dataFormat.getFormat("@"));
        }
        styleCache.put(cacheKey, targetStyle);
        return targetStyle;
    }

    /**
     * 构建仅包含文本格式的单元格样式。
     */
    private CellStyle buildTextStyle(Workbook targetWorkbook, Map<String, CellStyle> styleCache) {
        String cacheKey = "TEXT_ONLY";
        CellStyle cachedStyle = styleCache.get(cacheKey);
        if (cachedStyle != null) {
            return cachedStyle;
        }
        CellStyle targetStyle = targetWorkbook.createCellStyle();
        targetStyle.setDataFormat(targetWorkbook.createDataFormat().getFormat("@"));
        styleCache.put(cacheKey, targetStyle);
        return targetStyle;
    }

    /**
     * 校验复制结果的行数和缺失合同编码。
     */
    private void assertCopyResult(CopyResult result, int expectedRows, String sheetDescription) {
        assertEquals(expectedRows, result.getWrittenRows(), sheetDescription + "写入行数不符合预期");
        assertTrue(result.getMissingContractCodes().isEmpty(),
                sheetDescription + "存在未匹配合同编码：" + result.getMissingContractCodes());
    }

    /**
     * 校验起始合同在我方信息中的 B 列保持 006000 文本。
     */
    private void assertStartOurPartyCode(Sheet targetOurPartySheet, DataFormatter formatter) {
        Cell partyCodeCell = findPartyCodeCell(targetOurPartySheet, START_CONTRACT_CODE, formatter);
        assertEquals(CellType.STRING, partyCodeCell.getCellType(), "我方信息起始合同 B 列应为文本类型");
        assertEquals(START_OUR_PARTY_CODE, readCellText(partyCodeCell, formatter), "我方信息起始合同 B 列值不符合预期");
    }

    /**
     * 写入后重新打开文件，校验保存后的目标数据。
     */
    private void verifySavedWorkbook(DataFormatter formatter) throws Exception {
        try (Workbook workbook = openWorkbook(SOURCE_FILE)) {
            Sheet targetCounterPartySheet = getRequiredSheet(workbook, TARGET_COUNTER_PARTY_SHEET_NAME);
            Sheet targetOurPartySheet = getRequiredSheet(workbook, TARGET_OUR_PARTY_SHEET_NAME);
            assertEquals(EXPECTED_COUNTER_PARTY_ROWS, countDataRows(targetCounterPartySheet, formatter),
                    "保存后对方信息行数不符合预期");
            assertEquals(EXPECTED_OUR_PARTY_ROWS, countDataRows(targetOurPartySheet, formatter),
                    "保存后我方信息行数不符合预期");
            assertStartOurPartyCode(targetOurPartySheet, formatter);
        }
    }

    /**
     * 统计目标工作表中 A 列非空的数据行数量。
     */
    private int countDataRows(Sheet targetSheet, DataFormatter formatter) {
        int count = 0;
        for (int rowIndex = DATA_START_ROW_INDEX; rowIndex <= targetSheet.getLastRowNum(); rowIndex++) {
            Row row = targetSheet.getRow(rowIndex);
            String contractCode = trimToNull(readCellText(row == null ? null : row.getCell(CONTRACT_COLUMN_INDEX),
                    formatter));
            if (contractCode != null) {
                count++;
            }
        }
        return count;
    }

    /**
     * 按合同编码查找 B 列主体编码单元格。
     */
    private Cell findPartyCodeCell(Sheet sheet, String contractCode, DataFormatter formatter) {
        for (int rowIndex = DATA_START_ROW_INDEX; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            String currentContractCode = trimToNull(readCellText(row == null ? null : row.getCell(CONTRACT_COLUMN_INDEX),
                    formatter));
            if (contractCode.equals(currentContractCode)) {
                Cell partyCodeCell = row.getCell(PARTY_CODE_COLUMN_INDEX);
                if (partyCodeCell == null) {
                    throw new IllegalStateException("合同编码 " + contractCode + " 的 B 列为空单元格");
                }
                return partyCodeCell;
            }
        }
        throw new IllegalStateException("未在目标工作表找到合同编码：" + contractCode);
    }

    /**
     * 读取单元格展示文本，用于合同编码和主体编码匹配。
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
    private void printSummary(Path backupPath, int contractCount, int counterPartyRows, int ourPartyRows) {
        System.out.println("主播流程对方信息/我方信息复制完成");
        System.out.println("主表合同编码数量：" + contractCount);
        System.out.println("对方信息写入行数：" + counterPartyRows);
        System.out.println("我方信息写入行数：" + ourPartyRows);
        System.out.println("备份文件路径：" + backupPath);
    }

    /**
     * 复制结果摘要，记录写入行数和未匹配合同编码。
     */
    private static class CopyResult {

        private final int writtenRows;
        private final List<String> missingContractCodes;

        /**
         * 创建复制结果摘要。
         */
        private CopyResult(int writtenRows, List<String> missingContractCodes) {
            this.writtenRows = writtenRows;
            this.missingContractCodes = missingContractCodes;
        }

        /**
         * 获取实际写入行数。
         */
        private int getWrittenRows() {
            return writtenRows;
        }

        /**
         * 获取未匹配到的合同编码。
         */
        private List<String> getMissingContractCodes() {
            return missingContractCodes;
        }
    }
}
