package com.hero.middleware.utils;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ExcelUtilsTest {

    @TempDir
    Path tempDir;

    @Test
    void readAllSheetsKeepsSheetRowAndDuplicateHeaderValues() throws Exception {
        Path excelPath = tempDir.resolve("history-contract.xlsx");
        writeWorkbook(excelPath);

        List<ExcelUtils.ExcelSheetData> sheets = ExcelUtils.readAllSheets(excelPath.toString(), 0, 1);

        assertEquals(2, sheets.size());
        ExcelUtils.ExcelSheetData mainSheet = sheets.get(0);
        assertEquals("字段模板", mainSheet.getSheetName());
        assertEquals("contract_number", mainSheet.getHeaders().get(0));
        assertEquals("in_amount", mainSheet.getHeaders().get(1));
        assertEquals("in_amount", mainSheet.getHeaders().get(2));
        ExcelUtils.ExcelRowData row = mainSheet.getRows().get(0);
        assertEquals(2, row.getRowIndex());
        assertEquals("C-001", row.getFirstValue("contract_number"));
        assertEquals(new BigDecimal("20.5"), row.getFirstValue("in_amount"));
        assertNull(row.getValues("in_amount").get(0));
        assertEquals(new BigDecimal("20.5"), row.getValues("in_amount").get(1));
        assertEquals("我方主体列表", sheets.get(1).getSheetName());
    }

    @Test
    void readAllSheetsByRowStreamsSheetRowAndDuplicateHeaderValues() throws Exception {
        Path excelPath = tempDir.resolve("history-contract-stream.xlsx");
        writeWorkbook(excelPath);
        List<ExcelUtils.ExcelStreamRowData> rows = new ArrayList<>();

        ExcelUtils.readAllSheetsByRow(excelPath.toString(), 0, 1, row -> {
            rows.add(row);
            return true;
        });

        assertEquals(2, rows.size());
        ExcelUtils.ExcelStreamRowData mainRow = rows.get(0);
        assertEquals("字段模板", mainRow.getSheetName());
        assertEquals(0, mainRow.getSheetIndex());
        assertEquals(2, mainRow.getRowIndex());
        assertEquals("C-001", mainRow.getFirstValue("contract_number"));
        assertEquals(new BigDecimal("20.5"), mainRow.getFirstValue("in_amount"));
        assertNull(mainRow.getValues("in_amount").get(0));
        assertEquals(new BigDecimal("20.5"), mainRow.getValues("in_amount").get(1));
        assertEquals("我方主体列表", rows.get(1).getSheetName());
        assertEquals(1, rows.get(1).getSheetIndex());
    }

    private void writeWorkbook(Path excelPath) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet mainSheet = workbook.createSheet("字段模板");
            Row header = mainSheet.createRow(0);
            header.createCell(0).setCellValue("contract_number（合同编码）");
            header.createCell(1).setCellValue("in_amount（预估收入金额）");
            header.createCell(2).setCellValue("in_amount（收入总额）");
            Row data = mainSheet.createRow(1);
            data.createCell(0).setCellValue("C-001");
            data.createCell(2).setCellValue(20.5D);

            Sheet partySheet = workbook.createSheet("我方主体列表");
            Row partyHeader = partySheet.createRow(0);
            partyHeader.createCell(0).setCellValue("contract_number（合同编码）");
            partyHeader.createCell(1).setCellValue("our_party_code（我方主体编码）");
            Row partyData = partySheet.createRow(1);
            partyData.createCell(0).setCellValue("C-001");
            partyData.createCell(1).setCellValue("L001");

            try (OutputStream outputStream = Files.newOutputStream(excelPath)) {
                workbook.write(outputStream);
            }
        }
    }
}
