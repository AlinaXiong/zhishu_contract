package com.hero.middleware.utils;

import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ooxml.util.SAXHelper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ExcelUtils {

    private static final String HEADER_SIGN_TYPE_CODE = "sign_type_code";
    private static final String HEADER_SIGN_TYPE_CODE_SEAL_PARTY = "sign_type_code#seal_party";
    private static final String HEADER_SIGN_TYPE_CODE_SIGN_FORM = "sign_type_code#sign_form";

    private ExcelUtils() {
    }

    public interface ExcelRowHandler {
        boolean handle(Map<String, Object> rowData);
    }

    public interface ExcelStreamRowHandler {
        boolean handle(ExcelStreamRowData rowData);
    }

    public static List<ExcelSheetData> readAllSheets(String filePath, int headerRowIndex, int startRowIndex) {
        return readAllSheets(filePath, headerRowIndex, startRowIndex, null);
    }

    public static List<ExcelSheetData> readAllSheets(String filePath, int headerRowIndex, int startRowIndex,
                                                     Integer maxRowsPerSheet) {
        try (InputStream inputStream = Files.newInputStream(Paths.get(filePath));
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            DataFormatter formatter = new DataFormatter();
            List<ExcelSheetData> sheets = new ArrayList<>();
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                sheets.add(readSheetData(sheet, sheetIndex, headerRowIndex, startRowIndex, maxRowsPerSheet,
                        evaluator, formatter));
            }
            return sheets;
        } catch (Exception e) {
            throw new RuntimeException("Parse excel failed: " + filePath, e);
        }
    }

    public static void readAllSheetsByRow(String filePath, int headerRowIndex, int startRowIndex,
                                          ExcelStreamRowHandler rowHandler) {
        readAllSheetsByRow(filePath, headerRowIndex, startRowIndex, null, rowHandler);
    }

    public static void readAllSheetsByRow(String filePath, int headerRowIndex, int startRowIndex,
                                          Integer maxRowsPerSheet, ExcelStreamRowHandler rowHandler) {
        if (filePath != null && filePath.toLowerCase().endsWith(".xlsx")) {
            readXlsxAllSheetsByRow(filePath, headerRowIndex, startRowIndex, maxRowsPerSheet, rowHandler);
        } else {
            readAllSheetsByRowWithWorkbook(filePath, headerRowIndex, startRowIndex, maxRowsPerSheet, rowHandler);
        }
    }

    public static List<Map<String, Object>> readExcel(String filePath, int sheetIndex, int headerRowIndex,
                                                      int startRowIndex) {
        return readExcel(filePath, sheetIndex, headerRowIndex, startRowIndex, null);
    }

    public static List<Map<String, Object>> readExcel(String filePath, int sheetIndex, int headerRowIndex,
                                                      int startRowIndex, Integer maxRows) {
        try (InputStream inputStream = Files.newInputStream(Paths.get(filePath));
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(sheetIndex);
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            DataFormatter formatter = new DataFormatter();
            Row headerRow = sheet.getRow(headerRowIndex);
            if (headerRow == null) {
                return new ArrayList<>();
            }
            List<String> headers = readHeaders(headerRow, formatter);
            List<Map<String, Object>> result = new ArrayList<>();
            int readRows = 0;
            for (int rowIndex = startRowIndex; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                if (maxRows != null && readRows >= maxRows) {
                    break;
                }
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }
                Map<String, Object> rowData = buildRowData(row, headers, evaluator);
                if (!rowData.isEmpty()) {
                    result.add(rowData);
                    readRows++;
                }
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Parse excel failed: " + filePath, e);
        }
    }

    public static <T> List<T> readExcel(String filePath, int sheetIndex, int headerRowIndex,
                                        int startRowIndex, Integer maxRows, Class<T> clazz) {
        List<Map<String, Object>> rows = readExcel(filePath, sheetIndex, headerRowIndex, startRowIndex, maxRows);
        List<T> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            result.add(mapToBean(row, clazz));
        }
        return result;
    }

    public static void readExcelByRow(String filePath, int sheetIndex, int headerRowIndex,
                                      int startRowIndex, Integer maxRows, ExcelRowHandler rowHandler) {
        if (filePath != null && filePath.toLowerCase().endsWith(".xlsx")) {
            readXlsxByRow(filePath, sheetIndex, headerRowIndex, startRowIndex, maxRows, rowHandler);
        } else {
            readExcelByRowWithWorkbook(filePath, sheetIndex, headerRowIndex, startRowIndex, maxRows, rowHandler);
        }
    }

    public static List<List<Object>> readExcelRows(String filePath, int sheetIndex, int startRowIndex,
                                                   Integer maxRows) {
        try (InputStream inputStream = Files.newInputStream(Paths.get(filePath));
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(sheetIndex);
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            List<List<Object>> result = new ArrayList<>();
            int readRows = 0;
            for (int rowIndex = startRowIndex; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                if (maxRows != null && readRows >= maxRows) {
                    break;
                }
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }
                List<Object> rowData = new ArrayList<>();
                boolean hasValue = false;
                short lastCellNum = row.getLastCellNum();
                for (int cellIndex = 0; cellIndex < lastCellNum; cellIndex++) {
                    Object value = getCellValue(row.getCell(cellIndex), evaluator);
                    if (!isBlankValue(value)) {
                        hasValue = true;
                    }
                    rowData.add(value);
                }
                if (hasValue) {
                    result.add(rowData);
                    readRows++;
                }
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Parse excel failed: " + filePath, e);
        }
    }

    public static <T> T mapToBean(Map<String, Object> row, Class<T> clazz) {
        try {
            T target = clazz.newInstance();
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                Field field = findField(clazz, entry.getKey());
                if (field == null) {
                    continue;
                }
                field.setAccessible(true);
                field.set(target, entry.getValue());
            }
            return target;
        } catch (Exception e) {
            throw new RuntimeException("Excel row convert failed: " + clazz.getName(), e);
        }
    }

    private static ExcelSheetData readSheetData(Sheet sheet, int sheetIndex, int headerRowIndex, int startRowIndex,
                                                Integer maxRows, FormulaEvaluator evaluator,
                                                DataFormatter formatter) {
        Row headerRow = sheet.getRow(headerRowIndex);
        if (headerRow == null) {
            return new ExcelSheetData(sheet.getSheetName(), sheetIndex, Collections.emptyList(), Collections.emptyList());
        }
        List<String> headers = readHeaders(headerRow, formatter);
        List<ExcelRowData> rows = new ArrayList<>();
        int readRows = 0;
        for (int rowIndex = startRowIndex; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            if (maxRows != null && readRows >= maxRows) {
                break;
            }
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            ExcelRowData rowData = buildExcelRowData(sheet.getSheetName(), rowIndex + 1, row, headers, evaluator);
            if (!rowData.isEmpty()) {
                rows.add(rowData);
                readRows++;
            }
        }
        return new ExcelSheetData(sheet.getSheetName(), sheetIndex, headers, rows);
    }

    private static void readXlsxByRow(String filePath, int sheetIndex, int headerRowIndex,
                                      int startRowIndex, Integer maxRows, ExcelRowHandler rowHandler) {
        try (OPCPackage opcPackage = OPCPackage.open(filePath)) {
            ReadOnlySharedStringsTable sharedStringsTable = new ReadOnlySharedStringsTable(opcPackage);
            XSSFReader reader = new XSSFReader(opcPackage);
            StylesTable stylesTable = reader.getStylesTable();
            Iterator<InputStream> sheetIterator = reader.getSheetsData();
            int currentSheetIndex = 0;
            while (sheetIterator.hasNext()) {
                try (InputStream sheetInputStream = sheetIterator.next()) {
                    if (currentSheetIndex == sheetIndex) {
                        parseXlsxSheet(sheetInputStream, sharedStringsTable, stylesTable, null, currentSheetIndex,
                                headerRowIndex, startRowIndex, maxRows, rowHandler, null);
                        return;
                    }
                }
                currentSheetIndex++;
            }
            throw new IllegalArgumentException("Sheet index not found: " + sheetIndex);
        } catch (StopReadingException ignored) {
            // Used only to stop SAX parsing early after the caller has enough rows.
        } catch (Exception e) {
            throw new RuntimeException("Parse xlsx failed: " + filePath, e);
        }
    }

    private static void readXlsxAllSheetsByRow(String filePath, int headerRowIndex, int startRowIndex,
                                               Integer maxRowsPerSheet, ExcelStreamRowHandler rowHandler) {
        try (OPCPackage opcPackage = OPCPackage.open(filePath)) {
            ReadOnlySharedStringsTable sharedStringsTable = new ReadOnlySharedStringsTable(opcPackage);
            XSSFReader reader = new XSSFReader(opcPackage);
            StylesTable stylesTable = reader.getStylesTable();
            XSSFReader.SheetIterator sheetIterator = (XSSFReader.SheetIterator) reader.getSheetsData();
            int currentSheetIndex = 0;
            while (sheetIterator.hasNext()) {
                try (InputStream sheetInputStream = sheetIterator.next()) {
                    try {
                        parseXlsxSheet(sheetInputStream, sharedStringsTable, stylesTable, sheetIterator.getSheetName(),
                                currentSheetIndex, headerRowIndex, startRowIndex, maxRowsPerSheet, null, rowHandler);
                    } catch (StopReadingException e) {
                        if (e.isStopAll()) {
                            throw e;
                        }
                    }
                }
                currentSheetIndex++;
            }
        } catch (StopReadingException ignored) {
            // Used only to stop SAX parsing early when the caller asks to stop all sheets.
        } catch (Exception e) {
            throw new RuntimeException("Parse xlsx failed: " + filePath, e);
        }
    }

    private static void parseXlsxSheet(InputStream sheetInputStream,
                                       ReadOnlySharedStringsTable sharedStringsTable,
                                       StylesTable stylesTable,
                                       String sheetName,
                                       int sheetIndex,
                                       int headerRowIndex,
                                       int startRowIndex,
                                       Integer maxRows,
                                       ExcelRowHandler rowHandler,
                                       ExcelStreamRowHandler streamRowHandler) throws Exception {
        XMLReader parser = SAXHelper.newXMLReader();
        parser.setContentHandler(new XlsxSheetHandler(sharedStringsTable, stylesTable, sheetName, sheetIndex,
                headerRowIndex, startRowIndex, maxRows, rowHandler, streamRowHandler));
        parser.parse(new InputSource(sheetInputStream));
    }

    private static void readAllSheetsByRowWithWorkbook(String filePath, int headerRowIndex, int startRowIndex,
                                                       Integer maxRowsPerSheet,
                                                       ExcelStreamRowHandler rowHandler) {
        try (InputStream inputStream = Files.newInputStream(Paths.get(filePath));
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            DataFormatter formatter = new DataFormatter();
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                Row headerRow = sheet.getRow(headerRowIndex);
                if (headerRow == null) {
                    continue;
                }
                List<String> headers = readHeaders(headerRow, formatter);
                int readRows = 0;
                for (int rowIndex = startRowIndex; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                    if (maxRowsPerSheet != null && readRows >= maxRowsPerSheet) {
                        break;
                    }
                    Row row = sheet.getRow(rowIndex);
                    if (row == null) {
                        continue;
                    }
                    ExcelRowData rowData = buildExcelRowData(sheet.getSheetName(), rowIndex + 1, row,
                            headers, evaluator);
                    if (rowData.isEmpty()) {
                        continue;
                    }
                    readRows++;
                    ExcelStreamRowData streamRowData = new ExcelStreamRowData(sheet.getSheetName(), sheetIndex, rowData);
                    if (rowHandler != null && !rowHandler.handle(streamRowData)) {
                        return;
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Parse excel failed: " + filePath, e);
        }
    }

    private static void readExcelByRowWithWorkbook(String filePath, int sheetIndex, int headerRowIndex,
                                                   int startRowIndex, Integer maxRows,
                                                   ExcelRowHandler rowHandler) {
        try (InputStream inputStream = Files.newInputStream(Paths.get(filePath));
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(sheetIndex);
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            DataFormatter formatter = new DataFormatter();
            Row headerRow = sheet.getRow(headerRowIndex);
            if (headerRow == null) {
                return;
            }
            List<String> headers = readHeaders(headerRow, formatter);
            int readRows = 0;
            for (int rowIndex = startRowIndex; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                if (maxRows != null && readRows >= maxRows) {
                    return;
                }
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }
                Map<String, Object> rowData = buildRowData(row, headers, evaluator);
                if (!rowData.isEmpty()) {
                    readRows++;
                    if (rowHandler != null && !rowHandler.handle(rowData)) {
                        return;
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Parse excel failed: " + filePath, e);
        }
    }

    private static Map<String, Object> buildRowData(Row row, List<String> headers, FormulaEvaluator evaluator) {
        Map<String, Object> rowData = new LinkedHashMap<>();
        boolean hasValue = false;
        for (int cellIndex = 0; cellIndex < headers.size(); cellIndex++) {
            String header = headers.get(cellIndex);
            if (header == null || header.trim().isEmpty()) {
                continue;
            }
            Object value = getCellValue(row.getCell(cellIndex), evaluator);
            if (!isBlankValue(value)) {
                hasValue = true;
            }
            rowData.put(header, value);
        }
        return hasValue ? rowData : Collections.emptyMap();
    }

    private static ExcelRowData buildExcelRowData(String sheetName, int rowIndex, Row row, List<String> headers,
                                                  FormulaEvaluator evaluator) {
        Map<String, List<ExcelCellData>> cellDataByHeader = new LinkedHashMap<>();
        Map<String, Object> rowData = new LinkedHashMap<>();
        boolean hasValue = false;
        for (int cellIndex = 0; cellIndex < headers.size(); cellIndex++) {
            String header = headers.get(cellIndex);
            if (header == null || header.trim().isEmpty()) {
                continue;
            }
            Object value = getCellValue(row.getCell(cellIndex), evaluator);
            if (!isBlankValue(value)) {
                hasValue = true;
            }
            ExcelCellData cellData = new ExcelCellData(header, cellIndex + 1, value);
            List<ExcelCellData> cells = cellDataByHeader.get(header);
            if (cells == null) {
                cells = new ArrayList<>();
                cellDataByHeader.put(header, cells);
            }
            cells.add(cellData);
            if (!rowData.containsKey(header) || isBlankValue(rowData.get(header))) {
                rowData.put(header, value);
            }
        }
        if (!hasValue) {
            return ExcelRowData.empty(sheetName, rowIndex);
        }
        return new ExcelRowData(sheetName, rowIndex, rowData, cellDataByHeader);
    }

    private static List<String> readHeaders(Row headerRow, DataFormatter formatter) {
        List<String> headers = new ArrayList<>();
        short lastCellNum = headerRow.getLastCellNum();
        for (int cellIndex = 0; cellIndex < lastCellNum; cellIndex++) {
            Cell cell = headerRow.getCell(cellIndex);
            String header = cell == null ? null : formatter.formatCellValue(cell);
            headers.add(normalizeHeader(header));
        }
        return headers;
    }

    private static String normalizeHeader(String header) {
        if (header == null) {
            return null;
        }
        String value = header.replace("\uFEFF", "").trim();
        int index = value.indexOf('(');
        int closeIndex = index < 0 ? -1 : value.indexOf(')', index + 1);
        if (index < 0) {
            index = value.indexOf('\uFF08');
            closeIndex = index < 0 ? -1 : value.indexOf('\uFF09', index + 1);
        }
        String qualifier = index >= 0 && closeIndex > index
                ? value.substring(index + 1, closeIndex).trim()
                : null;
        if (index >= 0) {
            value = value.substring(0, index).trim();
        }
        if (HEADER_SIGN_TYPE_CODE.equals(value) && qualifier != null) {
            if (qualifier.contains("先盖章方")) {
                return HEADER_SIGN_TYPE_CODE_SEAL_PARTY;
            }
            if (qualifier.contains("签约形式")) {
                return HEADER_SIGN_TYPE_CODE_SIGN_FORM;
            }
        }
        return value;
    }

    private static Object getCellValue(Cell cell, FormulaEvaluator evaluator) {
        if (cell == null) {
            return null;
        }
        CellType cellType = cell.getCellType();
        if (cellType == CellType.FORMULA) {
            CellValue cellValue = evaluator.evaluate(cell);
            return getFormulaValue(cell, cellValue);
        }
        return getCellValueByType(cell, cellType);
    }

    private static Object getFormulaValue(Cell cell, CellValue cellValue) {
        if (cellValue == null) {
            return null;
        }
        if (cellValue.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getDateCellValue();
        }
        switch (cellValue.getCellType()) {
            case BOOLEAN:
                return cellValue.getBooleanValue();
            case NUMERIC:
                return normalizeNumber(cellValue.getNumberValue());
            case STRING:
                return trimToNull(cellValue.getStringValue());
            case BLANK:
            case ERROR:
            default:
                return null;
        }
    }

    private static Object getCellValueByType(Cell cell, CellType cellType) {
        switch (cellType) {
            case STRING:
                return trimToNull(cell.getStringCellValue());
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue();
                }
                return normalizeNumber(cell.getNumericCellValue());
            case BOOLEAN:
                return cell.getBooleanCellValue();
            case BLANK:
            case ERROR:
            default:
                return null;
        }
    }

    private static Object normalizeNumber(double number) {
        BigDecimal decimal = BigDecimal.valueOf(number).stripTrailingZeros();
        if (decimal.scale() <= 0) {
            try {
                return decimal.longValueExact();
            } catch (ArithmeticException ignored) {
                return decimal.toPlainString();
            }
        }
        return decimal;
    }

    private static boolean isBlankValue(Object value) {
        return value == null || String.valueOf(value).trim().isEmpty();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimValue = value.trim();
        return trimValue.isEmpty() ? null : trimValue;
    }

    private static Field findField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    public static class ExcelSheetData {

        private final String sheetName;
        private final int sheetIndex;
        private final List<String> headers;
        private final List<ExcelRowData> rows;

        private ExcelSheetData(String sheetName, int sheetIndex, List<String> headers, List<ExcelRowData> rows) {
            this.sheetName = sheetName;
            this.sheetIndex = sheetIndex;
            this.headers = Collections.unmodifiableList(new ArrayList<>(headers));
            this.rows = Collections.unmodifiableList(new ArrayList<>(rows));
        }

        public String getSheetName() {
            return sheetName;
        }

        public int getSheetIndex() {
            return sheetIndex;
        }

        public List<String> getHeaders() {
            return headers;
        }

        public List<ExcelRowData> getRows() {
            return rows;
        }
    }

    public static class ExcelRowData {

        private final String sheetName;
        private final int rowIndex;
        private final Map<String, Object> rowData;
        private final Map<String, List<ExcelCellData>> cellDataByHeader;

        private ExcelRowData(String sheetName, int rowIndex, Map<String, Object> rowData,
                             Map<String, List<ExcelCellData>> cellDataByHeader) {
            this.sheetName = sheetName;
            this.rowIndex = rowIndex;
            this.rowData = Collections.unmodifiableMap(new LinkedHashMap<>(rowData));
            this.cellDataByHeader = copyCellDataByHeader(cellDataByHeader);
        }

        private static ExcelRowData empty(String sheetName, int rowIndex) {
            return new ExcelRowData(sheetName, rowIndex, Collections.emptyMap(), Collections.emptyMap());
        }

        private static Map<String, List<ExcelCellData>> copyCellDataByHeader(
                Map<String, List<ExcelCellData>> source) {
            Map<String, List<ExcelCellData>> target = new LinkedHashMap<>();
            for (Map.Entry<String, List<ExcelCellData>> entry : source.entrySet()) {
                target.put(entry.getKey(), Collections.unmodifiableList(new ArrayList<>(entry.getValue())));
            }
            return Collections.unmodifiableMap(target);
        }

        public String getSheetName() {
            return sheetName;
        }

        public int getRowIndex() {
            return rowIndex;
        }

        public Map<String, Object> getRowData() {
            return rowData;
        }

        public Map<String, List<ExcelCellData>> getCellDataByHeader() {
            return cellDataByHeader;
        }

        public Object getFirstValue(String header) {
            return rowData.get(header);
        }

        public List<Object> getValues(String header) {
            List<ExcelCellData> cellDataList = cellDataByHeader.get(header);
            if (cellDataList == null || cellDataList.isEmpty()) {
                return Collections.emptyList();
            }
            List<Object> values = new ArrayList<>();
            for (ExcelCellData cellData : cellDataList) {
                values.add(cellData.getValue());
            }
            return Collections.unmodifiableList(values);
        }

        public boolean isEmpty() {
            return rowData.isEmpty();
        }
    }

    public static class ExcelStreamRowData {

        private final String sheetName;
        private final int sheetIndex;
        private final ExcelRowData rowData;

        private ExcelStreamRowData(String sheetName, int sheetIndex, ExcelRowData rowData) {
            this.sheetName = sheetName;
            this.sheetIndex = sheetIndex;
            this.rowData = rowData;
        }

        public String getSheetName() {
            return sheetName;
        }

        public int getSheetIndex() {
            return sheetIndex;
        }

        public int getRowIndex() {
            return rowData.getRowIndex();
        }

        public Map<String, Object> getRowData() {
            return rowData.getRowData();
        }

        public Map<String, List<ExcelCellData>> getCellDataByHeader() {
            return rowData.getCellDataByHeader();
        }

        public Object getFirstValue(String header) {
            return rowData.getFirstValue(header);
        }

        public List<Object> getValues(String header) {
            return rowData.getValues(header);
        }

        public ExcelRowData toExcelRowData() {
            return rowData;
        }
    }

    public static class ExcelCellData {

        private final String header;
        private final int columnIndex;
        private final Object value;

        private ExcelCellData(String header, int columnIndex, Object value) {
            this.header = header;
            this.columnIndex = columnIndex;
            this.value = value;
        }

        public String getHeader() {
            return header;
        }

        public int getColumnIndex() {
            return columnIndex;
        }

        public Object getValue() {
            return value;
        }
    }

    private static class XlsxSheetHandler extends DefaultHandler {

        private final ReadOnlySharedStringsTable sharedStringsTable;
        private final StylesTable stylesTable;
        private final String sheetName;
        private final int sheetIndex;
        private final int headerRowIndex;
        private final int startRowIndex;
        private final Integer maxRows;
        private final ExcelRowHandler rowHandler;
        private final ExcelStreamRowHandler streamRowHandler;
        private final StringBuilder valueBuffer = new StringBuilder();
        private final Map<Integer, Object> currentRowValues = new TreeMap<>();

        private List<String> headers = new ArrayList<>();
        private boolean readingValue;
        private String currentCellType;
        private String currentCellStyle;
        private int currentCellIndex = -1;
        private int currentRowIndex = -1;
        private int handledRows;

        private XlsxSheetHandler(ReadOnlySharedStringsTable sharedStringsTable,
                                 StylesTable stylesTable,
                                 String sheetName,
                                 int sheetIndex,
                                 int headerRowIndex,
                                 int startRowIndex,
                                 Integer maxRows,
                                 ExcelRowHandler rowHandler,
                                 ExcelStreamRowHandler streamRowHandler) {
            this.sharedStringsTable = sharedStringsTable;
            this.stylesTable = stylesTable;
            this.sheetName = sheetName;
            this.sheetIndex = sheetIndex;
            this.headerRowIndex = headerRowIndex;
            this.startRowIndex = startRowIndex;
            this.maxRows = maxRows;
            this.rowHandler = rowHandler;
            this.streamRowHandler = streamRowHandler;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            String elementName = getElementName(localName, qName);
            if ("row".equals(elementName)) {
                currentRowValues.clear();
                currentRowIndex = parseRowIndex(attributes.getValue("r"));
                currentCellIndex = -1;
                return;
            }
            if ("c".equals(elementName)) {
                currentCellType = attributes.getValue("t");
                currentCellStyle = attributes.getValue("s");
                currentCellIndex = parseColumnIndex(attributes.getValue("r"), currentCellIndex + 1);
                valueBuffer.setLength(0);
                return;
            }
            if ("v".equals(elementName) || "t".equals(elementName)) {
                readingValue = true;
                valueBuffer.setLength(0);
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            if (readingValue) {
                valueBuffer.append(ch, start, length);
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            String elementName = getElementName(localName, qName);
            if ("v".equals(elementName) || "t".equals(elementName)) {
                readingValue = false;
                return;
            }
            if ("c".equals(elementName)) {
                Object value = parseXlsxCellValue(valueBuffer.toString(), currentCellType, currentCellStyle);
                if (value != null) {
                    currentRowValues.put(currentCellIndex, value);
                }
                return;
            }
            if ("row".equals(elementName)) {
                handleRow();
            }
        }

        private void handleRow() throws StopReadingException {
            if (currentRowIndex == headerRowIndex) {
                headers = buildHeaders();
                return;
            }
            if (currentRowIndex < startRowIndex || headers == null || headers.isEmpty()) {
                return;
            }
            if (maxRows != null && handledRows >= maxRows) {
                throw new StopReadingException(false);
            }
            ExcelRowData rowData = buildCurrentExcelRowData();
            if (rowData.isEmpty()) {
                return;
            }
            handledRows++;
            boolean keepReading;
            if (streamRowHandler != null) {
                keepReading = streamRowHandler.handle(new ExcelStreamRowData(sheetName, sheetIndex, rowData));
            } else {
                keepReading = rowHandler == null || rowHandler.handle(rowData.getRowData());
            }
            if (!keepReading) {
                throw new StopReadingException(true);
            }
            if (maxRows != null && handledRows >= maxRows) {
                throw new StopReadingException(false);
            }
        }

        private List<String> buildHeaders() {
            List<String> headerList = new ArrayList<>();
            int maxColumnIndex = currentRowValues.isEmpty() ? -1 : Collections.max(currentRowValues.keySet());
            for (int index = 0; index <= maxColumnIndex; index++) {
                headerList.add(normalizeHeader(toStringValue(currentRowValues.get(index))));
            }
            return headerList;
        }

        private ExcelRowData buildCurrentExcelRowData() {
            Map<String, Object> rowData = new LinkedHashMap<>();
            Map<String, List<ExcelCellData>> cellDataByHeader = new LinkedHashMap<>();
            boolean hasValue = false;
            for (int index = 0; index < headers.size(); index++) {
                String header = headers.get(index);
                if (header == null || header.trim().isEmpty()) {
                    continue;
                }
                Object value = currentRowValues.get(index);
                if (!isBlankValue(value)) {
                    hasValue = true;
                }
                ExcelCellData cellData = new ExcelCellData(header, index + 1, value);
                List<ExcelCellData> cells = cellDataByHeader.get(header);
                if (cells == null) {
                    cells = new ArrayList<>();
                    cellDataByHeader.put(header, cells);
                }
                cells.add(cellData);
                if (!rowData.containsKey(header) || isBlankValue(rowData.get(header))) {
                    rowData.put(header, value);
                }
            }
            if (!hasValue) {
                return ExcelRowData.empty(sheetName, currentRowIndex + 1);
            }
            return new ExcelRowData(sheetName, currentRowIndex + 1, rowData, cellDataByHeader);
        }

        private Object parseXlsxCellValue(String rawValue, String cellType, String styleIndex) {
            String value = trimToNull(rawValue);
            if (value == null) {
                return null;
            }
            try {
                if ("s".equals(cellType)) {
                    int sharedStringIndex = Integer.parseInt(value);
                    return trimToNull(sharedStringsTable.getItemAt(sharedStringIndex).getString());
                }
                if ("inlineStr".equals(cellType) || "str".equals(cellType)) {
                    return value;
                }
                if ("b".equals(cellType)) {
                    return "1".equals(value) || "true".equalsIgnoreCase(value);
                }
                if ("e".equals(cellType)) {
                    return null;
                }
                double numberValue = Double.parseDouble(value);
                if (isDateStyle(styleIndex)) {
                    return DateUtil.getJavaDate(numberValue);
                }
                return normalizeNumber(numberValue);
            } catch (Exception ignored) {
                return value;
            }
        }

        private boolean isDateStyle(String styleIndex) {
            if (styleIndex == null || stylesTable == null) {
                return false;
            }
            try {
                XSSFCellStyle style = stylesTable.getStyleAt(Integer.parseInt(styleIndex));
                if (style == null) {
                    return false;
                }
                return DateUtil.isADateFormat(style.getDataFormat(), style.getDataFormatString());
            } catch (Exception ignored) {
                return false;
            }
        }

        private int parseRowIndex(String rowRef) {
            if (rowRef == null || rowRef.trim().isEmpty()) {
                return currentRowIndex + 1;
            }
            return Integer.parseInt(rowRef) - 1;
        }

        private int parseColumnIndex(String cellRef, int defaultIndex) {
            if (cellRef == null || cellRef.trim().isEmpty()) {
                return defaultIndex;
            }
            int columnIndex = 0;
            for (int index = 0; index < cellRef.length(); index++) {
                char ch = cellRef.charAt(index);
                if (ch >= '0' && ch <= '9') {
                    break;
                }
                columnIndex = columnIndex * 26 + (Character.toUpperCase(ch) - 'A' + 1);
            }
            return columnIndex - 1;
        }

        private String getElementName(String localName, String qName) {
            return localName == null || localName.isEmpty() ? qName : localName;
        }

        private String toStringValue(Object value) {
            return value == null ? null : String.valueOf(value);
        }
    }

    private static class StopReadingException extends SAXException {
        private static final long serialVersionUID = 1L;

        private final boolean stopAll;

        private StopReadingException() {
            this(false);
        }

        private StopReadingException(boolean stopAll) {
            this.stopAll = stopAll;
        }

        private boolean isStopAll() {
            return stopAll;
        }
    }
}
