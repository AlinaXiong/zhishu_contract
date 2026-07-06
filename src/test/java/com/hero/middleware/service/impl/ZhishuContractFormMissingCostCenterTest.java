package com.hero.middleware.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hero.middleware.client.zhishu.ZhishuContractClient;
import com.hero.middleware.client.zhishu.request.ContractsSearchRequest;
import com.hero.middleware.client.zhishu.response.ContractQueryResponse;
import com.hero.middleware.client.zhishu.response.ContractResponse;
import com.hero.middleware.client.zhishu.response.ContractsSearchResponse;
import com.hero.middleware.enums.ZhishuAndYecaiFiledEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@SpringBootTest
class ZhishuContractFormMissingCostCenterTest {

    private static final String DEFAULT_CONTRACT_NUMBER_FILE = "E:\\lidongliang\\code\\hero\\file\\zhishu_history_success_contracts_20260630.txt";

    private static final DateTimeFormatter FILE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private static final String ORDER_INFO_KEY = ZhishuAndYecaiFiledEnum.ORDERHT_DOCUMENT_INFO.getZhishuFiled();

    private static final String ORDER_NAME_KEY = ZhishuAndYecaiFiledEnum.ORDERHT_DOCUMENT_NAME.getZhishuFiled();

    private static final String COST_CENTER_KEY = ZhishuAndYecaiFiledEnum.ORDERHT_COST_CENTER.getZhishuFiled();

    @Autowired
    private ZhishuContractClient zhishuContractClient;

    /**
     * 查询合同是否存在form 前置单据信息，
     * @throws Exception
     */
    @Test
    void exportOrderRowsMissingCostCenterToExcel() throws Exception {
        Path contractNumberFile = Paths.get(DEFAULT_CONTRACT_NUMBER_FILE);
        Assumptions.assumeTrue(Files.exists(contractNumberFile),
                "合同编码txt文件不存在：" + contractNumberFile.toAbsolutePath());
//        List<String> contractNumbers = parseTxtFile(contractNumberFile);
        List<String> contractNumbers = Arrays.asList( "H-P2025100001".split(";"));
        List<MissingCostCenterRecord> records = new ArrayList<>();
        for (String contractNumber : contractNumbers) {
            records.addAll(checkOneContract(contractNumber));
        }
        Path excelPath = writeMissingCostCenterExcel(records);
        int missingCount = 0;
        int errorCount = 0;
        for (MissingCostCenterRecord record : records) {
            if (hasText(record.getErrorMessage())) {
                errorCount++;
            } else {
                missingCount++;
            }
        }
        log.info("智书合同订单信息成本中心缺失检查完成，读取合同数量:{}，命中缺失数量:{}，异常数量:{}，Excel路径:{}",
                contractNumbers.size(), missingCount, errorCount, excelPath.toAbsolutePath());
    }

    private List<String> parseTxtFile(Path filePath) throws Exception {
        List<String> result = new ArrayList<>();
        List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
        for (String line : lines) {
            if (!hasText(line)) {
                continue;
            }
            result.add(line.trim());
        }
        return result;
    }

    private List<MissingCostCenterRecord> checkOneContract(String contractNumber) {
        if (!hasText(contractNumber)) {
            return Collections.emptyList();
        }
        try {
            ContractQueryResponse searchContract = searchFirstContract(contractNumber);
            if (searchContract == null) {
                return Collections.singletonList(errorRecord(contractNumber, null, "合同搜索无结果"));
            }
            Long contractId = searchContract.getContractId();
            if (contractId == null) {
                return Collections.singletonList(errorRecord(contractNumber, null, "合同搜索结果缺少合同主键"));
            }
            ContractQueryResponse contractDetail = getContractDetail(contractId);
            if (contractDetail == null) {
                return Collections.singletonList(errorRecord(contractNumber, String.valueOf(contractId), "合同详情查询无结果"));
            }
            if (!hasText(contractDetail.getForm())) {
                return Collections.singletonList(errorRecord(contractNumber, String.valueOf(contractId), "合同详情form为空"));
            }
            return checkMissingCostCenter(contractNumber, String.valueOf(contractId), contractDetail.getForm());
        } catch (Exception e) {
            log.warn("检查智书合同订单信息成本中心缺失异常，合同编码:{}", contractNumber, e);
            return Collections.singletonList(errorRecord(contractNumber, null, e.getMessage()));
        }
    }

    private ContractQueryResponse searchFirstContract(String contractNumber) {
        ContractsSearchRequest request = new ContractsSearchRequest();
        request.setContractNumber(contractNumber);
        request.setPageSize(10);
        ContractsSearchResponse response = zhishuContractClient.searchContracts(request);
        if (response == null || response.getData() == null
                || response.getData().getItems() == null
                || response.getData().getItems().isEmpty()) {
            return null;
        }
        return response.getData().getItems().get(0);
    }

    private ContractQueryResponse getContractDetail(Long contractId) {
        Map<String, Object> params = new HashMap<>();
        params.put("user_id_type", "user_id");
        ContractResponse response = zhishuContractClient.getContract(String.valueOf(contractId), params);
        if (response == null || response.getData() == null || response.getData().get("contract") == null) {
            return null;
        }
        return JSONObject.parseObject(JSON.toJSONString(response.getData().get("contract")), ContractQueryResponse.class);
    }

    private List<MissingCostCenterRecord> checkMissingCostCenter(String contractNumber,
                                                                 String contractId,
                                                                 String form) {
        JSONArray formArray;
        try {
            formArray = JSONArray.parseArray(form);
        } catch (Exception e) {
            return Collections.singletonList(errorRecord(contractNumber, contractId, "合同详情form解析失败：" + e.getMessage()));
        }
        List<MissingCostCenterRecord> records = new ArrayList<>();
        for (Object formItem : formArray) {
            JSONObject attribute = toJSONObject(formItem);
            if (attribute == null || !ORDER_INFO_KEY.equals(getAttributeKey(attribute))) {
                continue;
            }
            List<JSONArray> rows = getAttributeRows(attribute.get("attribute_value"));
            for (JSONArray row : rows) {
                JSONObject orderNameAttribute = findAttribute(row, ORDER_NAME_KEY);
                if (orderNameAttribute == null || !hasAttributeValue(orderNameAttribute.get("attribute_value"))) {
                    continue;
                }
                JSONObject costCenterAttribute = findAttribute(row, COST_CENTER_KEY);
                if (costCenterAttribute == null) {
                    MissingCostCenterRecord record = new MissingCostCenterRecord();
                    record.setContractNumber(contractNumber);
                    record.setContractId(contractId);
                    record.setOrderName(attributeValueToText(orderNameAttribute.get("attribute_value")));
                    record.setMissingAttributeKey(COST_CENTER_KEY);
                    record.setRowJson(row.toJSONString());
                    records.add(record);
                }
            }
        }
        return records;
    }

    private List<JSONArray> getAttributeRows(Object attributeValue) {
        List<JSONArray> rows = new ArrayList<>();
        collectAttributeRows(toJsonValue(attributeValue), rows);
        return rows;
    }

    private void collectAttributeRows(Object value, List<JSONArray> rows) {
        if (value instanceof JSONArray) {
            JSONArray array = (JSONArray) value;
            if (isAttributeRow(array)) {
                rows.add(array);
                return;
            }
            for (Object item : array) {
                collectAttributeRows(toJsonValue(item), rows);
            }
            return;
        }
        if (value instanceof JSONObject) {
            JSONObject object = (JSONObject) value;
            if (hasText(getAttributeKey(object))) {
                JSONArray row = new JSONArray();
                row.add(object);
                rows.add(row);
            }
        }
    }

    private boolean isAttributeRow(JSONArray array) {
        boolean hasAttribute = false;
        for (Object item : array) {
            Object json = toJsonValue(item);
            if (json instanceof JSONArray) {
                return false;
            }
            if (json instanceof JSONObject && hasText(getAttributeKey((JSONObject) json))) {
                hasAttribute = true;
            }
        }
        return hasAttribute;
    }

    private JSONObject findAttribute(JSONArray row, String attributeKey) {
        for (Object item : row) {
            JSONObject attribute = toJSONObject(item);
            if (attribute != null && attributeKey.equals(getAttributeKey(attribute))) {
                return attribute;
            }
        }
        return null;
    }

    private JSONObject toJSONObject(Object value) {
        Object json = toJsonValue(value);
        if (json instanceof JSONObject) {
            return (JSONObject) json;
        }
        return null;
    }

    private Object toJsonValue(Object value) {
        if (value instanceof String) {
            String text = ((String) value).trim();
            if (text.startsWith("[") && text.endsWith("]")) {
                try {
                    return JSONArray.parseArray(text);
                } catch (Exception ignored) {
                    return value;
                }
            }
            if (text.startsWith("{") && text.endsWith("}")) {
                try {
                    return JSONObject.parseObject(text);
                } catch (Exception ignored) {
                    return value;
                }
            }
        }
        return JSON.toJSON(value);
    }

    private String getAttributeKey(JSONObject attribute) {
        if (attribute == null) {
            return null;
        }
        String attributeKey = attribute.getString("attribute_key");
        if (hasText(attributeKey)) {
            return attributeKey;
        }
        return attribute.getString("attribute_code");
    }

    private boolean hasAttributeValue(Object value) {
        if (value == null) {
            return false;
        }
        Object json = toJsonValue(value);
        if (json instanceof JSONArray) {
            return !((JSONArray) json).isEmpty();
        }
        if (json instanceof JSONObject) {
            return !((JSONObject) json).isEmpty();
        }
        return hasText(String.valueOf(value));
    }

    private String attributeValueToText(Object value) {
        if (value == null) {
            return "";
        }
        Object json = toJsonValue(value);
        if (json instanceof JSONObject) {
            JSONObject object = (JSONObject) json;
            String name = object.getString("name");
            if (hasText(name)) {
                return name;
            }
            String content = object.getString("content");
            if (hasText(content)) {
                return content;
            }
            return object.toJSONString();
        }
        if (json instanceof JSONArray) {
            return ((JSONArray) json).toJSONString();
        }
        return String.valueOf(value);
    }

    private Path writeMissingCostCenterExcel(List<MissingCostCenterRecord> records) throws Exception {
        Path outputDir = Paths.get("file");
        Files.createDirectories(outputDir);
        Path outputPath = outputDir.resolve("zhishu_order_missing_cost_center_"
                + FILE_TIME_FORMATTER.format(LocalDateTime.now()) + ".xlsx");
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("成本中心缺失");
            writeHeader(sheet.createRow(0), "合同编码", "合同主键", "订单名称", "缺失字段编码", "同级JSON", "错误信息");
            int rowIndex = 1;
            for (MissingCostCenterRecord record : records) {
                Row row = sheet.createRow(rowIndex++);
                createCell(row, 0, record.getContractNumber());
                createCell(row, 1, record.getContractId());
                createCell(row, 2, record.getOrderName());
                createCell(row, 3, record.getMissingAttributeKey());
                createCell(row, 4, record.getRowJson());
                createCell(row, 5, record.getErrorMessage());
            }
            for (int i = 0; i <= 5; i++) {
                sheet.autoSizeColumn(i);
            }
            try (OutputStream outputStream = Files.newOutputStream(outputPath)) {
                workbook.write(outputStream);
            }
        }
        return outputPath;
    }

    private void writeHeader(Row row, String... headers) {
        for (int i = 0; i < headers.length; i++) {
            createCell(row, i, headers[i]);
        }
    }

    private void createCell(Row row, int columnIndex, Object value) {
        row.createCell(columnIndex).setCellValue(value == null ? "" : String.valueOf(value));
    }

    private MissingCostCenterRecord errorRecord(String contractNumber, String contractId, String errorMessage) {
        MissingCostCenterRecord record = new MissingCostCenterRecord();
        record.setContractNumber(contractNumber);
        record.setContractId(contractId);
        record.setErrorMessage(errorMessage);
        return record;
    }

    private boolean hasText(String value) {
        return value != null && value.trim().length() > 0;
    }

    private static class MissingCostCenterRecord {

        private String contractNumber;

        private String contractId;

        private String orderName;

        private String missingAttributeKey;

        private String rowJson;

        private String errorMessage;

        String getContractNumber() {
            return contractNumber;
        }

        void setContractNumber(String contractNumber) {
            this.contractNumber = contractNumber;
        }

        String getContractId() {
            return contractId;
        }

        void setContractId(String contractId) {
            this.contractId = contractId;
        }

        String getOrderName() {
            return orderName;
        }

        void setOrderName(String orderName) {
            this.orderName = orderName;
        }

        String getMissingAttributeKey() {
            return missingAttributeKey;
        }

        void setMissingAttributeKey(String missingAttributeKey) {
            this.missingAttributeKey = missingAttributeKey;
        }

        String getRowJson() {
            return rowJson;
        }

        void setRowJson(String rowJson) {
            this.rowJson = rowJson;
        }

        String getErrorMessage() {
            return errorMessage;
        }

        void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }
}
