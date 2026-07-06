package com.hero.middleware.utils;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 主播流程审批节点读取工具，用于从历史合同 Excel 中按泛微状态分组已有异常原因的合同编码。
 */
public final class AnchorFlowApproveNodeReader {

    private static final String MAIN_SHEET_NAME = "反商业贿赂协议";
    private static final String REASON_HEADER_NAME = "reason";
    private static final int HEADER_ROW_INDEX = 0;
    private static final int DATA_START_ROW_INDEX = 1;
    private static final int CONTRACT_NUMBER_COLUMN_INDEX = 0;
    private static final int STATUS_COLUMN_INDEX = 2;

    private AnchorFlowApproveNodeReader() {
    }

    /**
     * 读取主播流程主表，按 C 列状态分组 A 列合同编码，并仅保留 reason 非空的行。
     */
    public static Map<String, List<String>> readContractNumbersByStatus(Path excelPath) {
        checkFileExists(excelPath);
        try (InputStream inputStream = Files.newInputStream(excelPath);
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet mainSheet = getRequiredSheet(workbook, MAIN_SHEET_NAME);
            DataFormatter formatter = new DataFormatter();
            int reasonColumnIndex = findReasonColumnIndex(mainSheet, formatter);
            return readContractGroups(mainSheet, reasonColumnIndex, formatter);
        } catch (Exception e) {
            throw new RuntimeException("读取主播流程审批节点分组失败：" + excelPath, e);
        }
    }

    /**
     * 校验 Excel 文件路径是否存在。
     */
    private static void checkFileExists(Path excelPath) {
        if (excelPath == null) {
            throw new IllegalArgumentException("Excel 文件路径不能为空");
        }
        if (!Files.exists(excelPath)) {
            throw new IllegalArgumentException("Excel 文件不存在：" + excelPath);
        }
        if (!Files.isRegularFile(excelPath)) {
            throw new IllegalArgumentException("Excel 路径不是文件：" + excelPath);
        }
    }

    /**
     * 获取必需工作表，缺失时抛出明确错误。
     */
    private static Sheet getRequiredSheet(Workbook workbook, String sheetName) {
        Sheet sheet = workbook.getSheet(sheetName);
        if (sheet == null) {
            throw new IllegalStateException("未找到工作表：" + sheetName);
        }
        return sheet;
    }

    /**
     * 查找 reason 列，未找到时返回 -1 表示全部行都不满足 reason 非空条件。
     */
    private static int findReasonColumnIndex(Sheet mainSheet, DataFormatter formatter) {
        Row headerRow = mainSheet.getRow(HEADER_ROW_INDEX);
        if (headerRow == null) {
            throw new IllegalStateException("主播流程主表缺少表头行");
        }
        short lastCellNum = headerRow.getLastCellNum();
        int headerCellCount = lastCellNum < 0 ? 0 : lastCellNum;
        for (int cellIndex = 0; cellIndex < headerCellCount; cellIndex++) {
            String header = trimToNull(readCellText(headerRow.getCell(cellIndex), formatter));
            if (REASON_HEADER_NAME.equalsIgnoreCase(header)) {
                return cellIndex;
            }
        }
        return -1;
    }

    /**
     * 读取合同编码分组数据。
     */
    private static Map<String, List<String>> readContractGroups(Sheet mainSheet, int reasonColumnIndex,
                                                                DataFormatter formatter) {
        Map<String, List<String>> contractNumbersByStatus = new LinkedHashMap<>();
        for (int rowIndex = DATA_START_ROW_INDEX; rowIndex <= mainSheet.getLastRowNum(); rowIndex++) {
            Row row = mainSheet.getRow(rowIndex);
            // TODO 获取信息方式，是否有异常信息
            if (row == null || hasReason(row, reasonColumnIndex, formatter)) {
                continue;
            }
            String contractNumber = trimToNull(readCellText(row.getCell(CONTRACT_NUMBER_COLUMN_INDEX), formatter));
            String status = trimToNull(readCellText(row.getCell(STATUS_COLUMN_INDEX), formatter));
            if (contractNumber == null || status == null) {
                continue;
            }

            // 按 C 列状态维护分组，保留 Excel 中合同编码的原始顺序。
            List<String> contractNumbers = contractNumbersByStatus.get(status);
            if (contractNumbers == null) {
                contractNumbers = new ArrayList<>();
                contractNumbersByStatus.put(status, contractNumbers);
            }
            contractNumbers.add(contractNumber);
        }
        return contractNumbersByStatus;
    }

    /**
     * 判断当前行 reason 列是否已有内容，缺少 reason 列时默认不提取。
     */
    private static boolean hasReason(Row row, int reasonColumnIndex, DataFormatter formatter) {
        if (reasonColumnIndex < 0) {
            return false;
        }
        return trimToNull(readCellText(row.getCell(reasonColumnIndex), formatter)) != null;
    }

    /**
     * 读取单元格展示文本，用于合同编码、状态和 reason 判断。
     */
    private static String readCellText(Cell cell, DataFormatter formatter) {
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
    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimValue = value.trim();
        return trimValue.isEmpty() ? null : trimValue;
    }
}
