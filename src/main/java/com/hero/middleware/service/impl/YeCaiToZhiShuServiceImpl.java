package com.hero.middleware.service.impl;

import cn.hutool.core.util.URLUtil;
import com.alibaba.fastjson.JSONObject;
import com.hero.middleware.annotation.SkipApiLogTable;
import com.hero.middleware.client.yuecai.YuecaiContractClient;
import com.hero.middleware.client.yuecai.response.MasterDataRes;
import com.hero.middleware.client.yuecai.response.masterdata.*;
import com.hero.middleware.client.zhishu.ZhiShuVendorClient;
import com.hero.middleware.client.zhishu.request.CreateVendorRequest;
import com.hero.middleware.client.zhishu.request.UpdateFixedExchangeRateRequest;
import com.hero.middleware.client.zhishu.response.CreateVendorResponse;
import com.hero.middleware.client.zhishu.response.QueryAllVendorResponse;
import com.hero.middleware.client.zhishu.response.VendorInfoResponse;
import com.hero.middleware.config.YeCaiDataConfig;
import com.hero.middleware.context.ApiLogTaskContext;
import com.hero.middleware.context.ApiLogTableContext;
import com.hero.middleware.dto.CreateAntiBriberyContractResultDTO;
import com.hero.middleware.enums.MasterDataTypeEnum;
import com.hero.middleware.enums.YesOrNoEnum;
import com.hero.middleware.service.ContractService;
import com.hero.middleware.service.YeCaiToZhiShuService;
import com.hero.middleware.utils.DateUtils;
import com.hero.middleware.utils.StrUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Slf4j
@Service
@SkipApiLogTable
public class YeCaiToZhiShuServiceImpl implements YeCaiToZhiShuService {

    private static final int VENDOR_SYNC_THREAD_COUNT = 5;
    private static final int VENDOR_SYNC_BATCH_SIZE = 50;
    private static final String ADD_LIST_KEY = "addList";
    private static final String UPDATE_LIST_KEY = "updateList";
    private static final String MAIN_BANK_RESOURCE_PATH = "file/汉得主行导出_202606291808.xlsx";
    private static final String MAIN_BANK_CODE_HEADER = "bank_code";
    private static final String MAIN_BANK_COUNTRY_CODE_HEADER = "country_code";

    @Autowired
    private YuecaiContractClient yuecaiContractClient;

    @Autowired
    private ZhiShuVendorClient zhiShuVendorClient;

    @Autowired
    private YeCaiDataConfig yeCaiDataConfig;

    @Autowired
    private ContractService contractService;

    private volatile Map<String, String> employeeCodeAndUserIdMap = Collections.emptyMap();

    @PostConstruct
    public void initEmployeeCodeAndUserIdMap() {
        refreshEmployeeCodeAndUserIdMap();
    }

    @Override
    public void synMasterData(String businessType) {
        synMasterData(businessType, null, null, null, null, null);
    }

    @Override
    public void synMasterData(String businessType, String certificationId) {
        synMasterData(businessType, certificationId, null, null, null, null);
    }

    @Override
    public void synMasterData(String businessType, String startTime, String endTime) {
        synMasterData(businessType, null, startTime, endTime, null, null);
    }

    @Override
    public void synMasterData(String businessType, String certificationId, String startTime, String endTime) {
        synMasterData(businessType, certificationId, startTime, endTime, null, null);
    }

    @Override
    public void synMasterData(String businessType, String certificationId, String startTime, String endTime, Integer page, Integer size) {
        if(MasterDataTypeEnum.COMPANY.getCode().equals(businessType)){//公司

        }else if(MasterDataTypeEnum.UNIT.getCode().equals(businessType)){//部门

        }else if(MasterDataTypeEnum.EMPLOYEE.getCode().equals(businessType)){//员工

        } else if (MasterDataTypeEnum.EMPLOYEE_ACCOUNT.getCode().equals(businessType)) {//员工账号

        } else if (MasterDataTypeEnum.VENDER.getCode().equals(businessType)) {//供应商
            String normalizedStartTime = isBlank(startTime) ? null : startTime.trim();
            String normalizedEndTime = isBlank(endTime) ? null : endTime.trim();
            if ((normalizedStartTime == null) != (normalizedEndTime == null)) {
                log.error("供应商主数据同步起始时间和终止时间必须同时传入，起始时间：{}，终止时间：{}",
                        startTime, endTime);
                return;
            }
            List<VenderRes> allVendorList = (List<VenderRes>) getAllMasterData(
                    businessType, normalizedStartTime, normalizedEndTime, page, size);
            log.info("业财供应商查询完成，起始时间：{}，终止时间：{}，页码：{}，每页数量：{}，查询数量：{}",
                    normalizedStartTime == null ? "未指定" : normalizedStartTime,
                    normalizedEndTime == null ? "未指定" : normalizedEndTime,
                    page == null ? "全部" : page,
                    size == null ? "全部" : size,
                    allVendorList == null ? 0 : allVendorList.size());
            allVendorList = filterVendorListByCertificationId(allVendorList, certificationId);
            if (allVendorList == null || allVendorList.isEmpty()) {
                log.info("未匹配到需要同步的供应商数据，证件 ID：{}", isBlank(certificationId) ? "全部" : certificationId);
                return;
            }
            QueryAllVendorResponse vendorAll = getVendorForSync(certificationId);
            boolean multiThread = !Boolean.FALSE.equals(yeCaiDataConfig.getVendorSyncMultiThread());
            log.info("供应商主数据同步模式：{}，证件 ID：{}，起始时间：{}，终止时间：{}，页码：{}，每页数量：{}，待同步数量：{}",
                    multiThread ? "多线程" : "单线程",
                    isBlank(certificationId) ? "全部" : certificationId,
                    normalizedStartTime == null ? "未指定" : normalizedStartTime,
                    normalizedEndTime == null ? "未指定" : normalizedEndTime,
                    page == null ? "全部" : page,
                    size == null ? "全部" : size,
                    allVendorList.size());
            if (multiThread) {
                syncVendorMasterDataInParallel(vendorAll, allVendorList);
            } else {
                syncVendorMasterDataSingleThread(vendorAll, allVendorList);
            }
        } else if (MasterDataTypeEnum.VENDER_ACCOUNT.getCode().equals(businessType)) {//供应商账号

        } else if (MasterDataTypeEnum.CUSTOMER.getCode().equals(businessType)) {//客户
            String normalizedStartTime = isBlank(startTime) ? null : startTime.trim();
            String normalizedEndTime = isBlank(endTime) ? null : endTime.trim();
            if ((normalizedStartTime == null) != (normalizedEndTime == null)) {
                log.error("客户主数据同步起始时间和终止时间必须同时传入，起始时间：{}，终止时间：{}",
                        startTime, endTime);
                return;
            }
            List<CustomerRes> allCustomerList = (List<CustomerRes>) getAllMasterData(
                    businessType, normalizedStartTime, normalizedEndTime, page, size);
            log.info("业财客户查询完成，起始时间：{}，终止时间：{}，页码：{}，每页数量：{}，查询数量：{}",
                    normalizedStartTime == null ? "未指定" : normalizedStartTime,
                    normalizedEndTime == null ? "未指定" : normalizedEndTime,
                    page == null ? "全部" : page,
                    size == null ? "全部" : size,
                    allCustomerList == null ? 0 : allCustomerList.size());
            allCustomerList = filterCustomerListByCertificationId(allCustomerList, certificationId);
            if (allCustomerList == null || allCustomerList.isEmpty()) {
                log.info("未匹配到需要同步的客户数据，证件 ID：{}", isBlank(certificationId) ? "全部" : certificationId);
                return;
            }
            QueryAllVendorResponse vendorAll = getVendorForSync(certificationId);
            boolean multiThread = !Boolean.FALSE.equals(yeCaiDataConfig.getVendorSyncMultiThread());
            log.info("客户主数据同步模式：{}，证件 ID：{}，起始时间：{}，终止时间：{}，页码：{}，每页数量：{}，待同步数量：{}",
                    multiThread ? "多线程" : "单线程",
                    isBlank(certificationId) ? "全部" : certificationId,
                    normalizedStartTime == null ? "未指定" : normalizedStartTime,
                    normalizedEndTime == null ? "未指定" : normalizedEndTime,
                    page == null ? "全部" : page,
                    size == null ? "全部" : size,
                    allCustomerList.size());
            if (multiThread) {
                syncCustomerMasterDataInParallel(vendorAll, allCustomerList);
            } else {
                syncCustomerMasterDataSingleThread(vendorAll, allCustomerList);
            }
        } else if (MasterDataTypeEnum.CUSTOMER_ACCOUNT.getCode().equals(businessType)) {//客户账号

        } else if (MasterDataTypeEnum.BANK.getCode().equals(businessType)) {//银行

        } else if (MasterDataTypeEnum.CNAPS.getCode().equals(businessType)) {//银行分行

        } else if (MasterDataTypeEnum.CURRENCY.getCode().equals(businessType)) {//币种

        } else if (MasterDataTypeEnum.EXCHANGE_RATE.getCode().equals(businessType)) {//汇率
            String normalizedStartTime = isBlank(startTime) ? null : startTime.trim();
            String normalizedEndTime = isBlank(endTime) ? null : endTime.trim();
            if ((normalizedStartTime == null) != (normalizedEndTime == null)) {
                log.error("汇率主数据同步起始时间和终止时间必须同时传入，起始时间：{}，终止时间：{}",
                        startTime, endTime);
                return;
            }
            List<ExchangeRateRes> allExchangeRateList = (List<ExchangeRateRes>) getAllMasterData(
                    businessType, normalizedStartTime, normalizedEndTime, page, size);
            log.info("业财汇率查询完成，起始时间：{}，终止时间：{}，页码：{}，每页数量：{}，查询数量：{}",
                    normalizedStartTime == null ? "未指定" : normalizedStartTime,
                    normalizedEndTime == null ? "未指定" : normalizedEndTime,
                    page == null ? "全部" : page,
                    size == null ? "全部" : size,
                    allExchangeRateList == null ? 0 : allExchangeRateList.size());
            if (allExchangeRateList == null || allExchangeRateList.isEmpty()) {
                log.info("未查询到需要同步的汇率数据，起始时间：{}，终止时间：{}",
                        normalizedStartTime == null ? "未指定" : normalizedStartTime,
                        normalizedEndTime == null ? "未指定" : normalizedEndTime);
                return;
            }
            int successCount = 0;
            int failCount = 0;
            for (ExchangeRateRes exchangeRateRes : allExchangeRateList) {
                try {
                    UpdateFixedExchangeRateRequest request = new UpdateFixedExchangeRateRequest();
                    request.setStatus(1);
                    request.setEffectiveDate(DateUtils.convertDateToString(exchangeRateRes.getRateDate(), "yyyy-MM-dd"));
                    request.setTargetCurrency(exchangeRateRes.getToCurrencyCode());
                    request.setSourceCurrency(exchangeRateRes.getFromCurrencyCode());
                    request.setExchangeRate(String.valueOf(exchangeRateRes.getRate()));
                    JSONObject resultJson = zhiShuVendorClient.updateFixedExchangeRate(request);
                    if (resultJson != null && "0".equals(resultJson.getString("code"))) {
                        successCount++;
                        log.info("更新汇率信息完成，sourceCurrency={}，targetCurrency={}，effectiveDate={}，exchangeRate={}",
                                request.getSourceCurrency(), request.getTargetCurrency(),
                                request.getEffectiveDate(), request.getExchangeRate());
                    } else {
                        failCount++;
                        log.warn("更新汇率信息失败，sourceCurrency={}，targetCurrency={}，effectiveDate={}，返回={}",
                                request.getSourceCurrency(), request.getTargetCurrency(),
                                request.getEffectiveDate(), resultJson);
                    }
                } catch (Exception e) {
                    failCount++;
                    log.error("更新汇率信息异常，sourceCurrency={}，targetCurrency={}，rateDate={}，错误={}",
                            exchangeRateRes == null ? null : exchangeRateRes.getFromCurrencyCode(),
                            exchangeRateRes == null ? null : exchangeRateRes.getToCurrencyCode(),
                            exchangeRateRes == null ? null : exchangeRateRes.getRateDate(),
                            e.getMessage(), e);
                }
            }
            log.info("汇率同步完成，查询数量：{}，成功数量：{}，失败数量：{}",
                    allExchangeRateList.size(), successCount, failCount);

        } else if (MasterDataTypeEnum.RESP_CENTER.getCode().equals(businessType)) {//成本中心

        } else if (MasterDataTypeEnum.ORDER.getCode().equals(businessType)) {//项目订单

        }

    }

    /**
     * 获取账号数据
     * @param businessType
     * @return
     */
    @Override
    public void synMasterDataByVendorCode(String businessType, String certificationId, String startTime, String endTime, Integer page, Integer size) {
        if (MasterDataTypeEnum.VENDER.getCode().equals(businessType)) {
            String normalizedStartTime = isBlank(startTime) ? null : startTime.trim();
            String normalizedEndTime = isBlank(endTime) ? null : endTime.trim();
            if ((normalizedStartTime == null) != (normalizedEndTime == null)) {
                log.error("按交易方编码同步供应商主数据时，起始时间和终止时间必须同时传入，起始时间：{}，终止时间：{}",
                        startTime, endTime);
                return;
            }
            List<VenderRes> allVendorList = (List<VenderRes>) getAllMasterData(
                    businessType, normalizedStartTime, normalizedEndTime, page, size);
            allVendorList = filterVendorListByCertificationId(allVendorList, certificationId);
            if (allVendorList == null || allVendorList.isEmpty()) {
                log.info("按交易方编码同步供应商主数据时，未匹配到需要同步的数据，证件 ID：{}",
                        isBlank(certificationId) ? "全部" : certificationId);
                return;
            }
            syncVendorMasterDataByVendorCode(allVendorList);
        } else if (MasterDataTypeEnum.CUSTOMER.getCode().equals(businessType)) {
            String normalizedStartTime = isBlank(startTime) ? null : startTime.trim();
            String normalizedEndTime = isBlank(endTime) ? null : endTime.trim();
            if ((normalizedStartTime == null) != (normalizedEndTime == null)) {
                log.error("按交易方编码同步客户主数据时，起始时间和终止时间必须同时传入，起始时间：{}，终止时间：{}",
                        startTime, endTime);
                return;
            }
            List<CustomerRes> allCustomerList = (List<CustomerRes>) getAllMasterData(
                    businessType, normalizedStartTime, normalizedEndTime, page, size);
            allCustomerList = filterCustomerListByCertificationId(allCustomerList, certificationId);
            if (allCustomerList == null || allCustomerList.isEmpty()) {
                log.info("按交易方编码同步客户主数据时，未匹配到需要同步的数据，证件 ID：{}",
                        isBlank(certificationId) ? "全部" : certificationId);
                return;
            }
            syncCustomerMasterDataByVendorCode(allCustomerList);
        } else {
            log.warn("按交易方编码同步主数据暂不支持该类型：{}", businessType);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void synMasterDataByBusinessCodes(String businessType, Collection<String> businessCodes) {
        LinkedHashSet<String> targetCodes = normalizeBusinessCodes(businessCodes);
        if (targetCodes.isEmpty()) {
            log.info("按业财编码集合同步交易方时，编码集合为空，本次不执行同步，类型：{}", businessType);
            return;
        }

        if (MasterDataTypeEnum.VENDER.getCode().equals(businessType)) {
            List<VenderRes> allVendorList = (List<VenderRes>) getAllMasterData(businessType);
            List<VenderRes> matchedVendorList = filterVendorListByBusinessCodes(allVendorList, targetCodes);
            if (matchedVendorList.isEmpty()) {
                log.info("按业财供应商编码集合同步交易方时，未匹配到业财供应商数据，供应商编码集合：{}", targetCodes);
                return;
            }
            log.info("按业财供应商编码集合同步交易方，传入编码数：{}，匹配供应商数：{}，供应商编码集合：{}",
                    targetCodes.size(), matchedVendorList.size(), targetCodes);
            syncVendorMasterDataByVendorCode(matchedVendorList);
        } else if (MasterDataTypeEnum.CUSTOMER.getCode().equals(businessType)) {
            List<CustomerRes> allCustomerList = (List<CustomerRes>) getAllMasterData(businessType);
            List<CustomerRes> matchedCustomerList = filterCustomerListByBusinessCodes(allCustomerList, targetCodes);
            if (matchedCustomerList.isEmpty()) {
                log.info("按业财客户编码集合同步交易方时，未匹配到业财客户数据，客户编码集合：{}", targetCodes);
                return;
            }
            log.info("按业财客户编码集合同步交易方，传入编码数：{}，匹配客户数：{}，客户编码集合：{}",
                    targetCodes.size(), matchedCustomerList.size(), targetCodes);
            syncCustomerMasterDataByVendorCode(matchedCustomerList);
        } else {
            log.warn("按业财编码集合同步交易方暂不支持该类型：{}，编码集合：{}", businessType, targetCodes);
        }
    }

    public Object getAllAccountData(String businessType,String accountCode){
        List<Object> resultList = new ArrayList<>();
        Map<String,Object> params = new HashMap<>();
        params.put("dataType", businessType);
        List<Object> content = new ArrayList<>();
        if(MasterDataTypeEnum.VENDER_ACCOUNT.getCode().equals(businessType)){//供应商账号
            params.put("venderCode", accountCode);
            MasterDataRes masterData = yuecaiContractClient.getMasterData(params);
            content = masterData.getContent();
            for(int i = 0;i<content.size();i++){
                VenderAccountRes data = JSONObject.parseObject(content.get(i).toString(), VenderAccountRes.class);
                resultList.add(data);
            }
        }else if(MasterDataTypeEnum.CUSTOMER_ACCOUNT.getCode().equals(businessType)){//客户账号
            params.put("customerCode", accountCode);
            MasterDataRes masterData = yuecaiContractClient.getMasterData(params);
            content = masterData.getContent();
            for(int i = 0;i<content.size();i++){
                CustomerAccountRes data = JSONObject.parseObject(content.get(i).toString(), CustomerAccountRes.class);
                resultList.add(data);
            }
        }
        return resultList;
    }

    @Override
    public Object getAllMasterData(String businessType) {
        return getAllMasterData(businessType,null,null, null, null);
    }
    @Override
    public Object getAllMasterData(String businessType,String startTime,String endTime) {
        return getAllMasterData(businessType, startTime, endTime, null, null);
    }

    @Override
    public Object getAllMasterData(String businessType, String startTime, String endTime, Integer page, Integer size) {
        List<Object> resultList = new ArrayList<>();
        Map<String,Object> params = new HashMap<>();
        if ((page == null) != (size == null)) {
            log.error("主数据查询页码和每页数量必须同时传入，页码：{}，每页数量：{}", page, size);
            return resultList;
        }
        if (page != null && page < 0) {
            log.error("主数据查询页码不能小于0，页码：{}", page);
            return resultList;
        }
        if (size != null && size <= 0) {
            log.error("主数据查询每页数量必须大于0，每页数量：{}", size);
            return resultList;
        }
        boolean singlePage = page != null;
        int currentPage = singlePage ? page : 0;
        boolean nextPage = true;
        while (nextPage) {
            params.clear();
            params.put("page", currentPage);
            params.put("size", singlePage ? size : yeCaiDataConfig.getPageSize());
//            params.put("page", 0);
//            params.put("size", 10);
            params.put("dataType", businessType);
            if(startTime != null){
                params.put("startTime", URLUtil.encode(startTime));
            }else{
                params.put("startTime", URLUtil.encode(yeCaiDataConfig.getStartTime()));
            }
            if(endTime != null){
                params.put("endTime", URLUtil.encode(endTime));
            }
            MasterDataRes masterData = yuecaiContractClient.getMasterData(params);
            List<Object> content = masterData == null || masterData.getContent() == null
                    ? Collections.emptyList() : masterData.getContent();
            if(MasterDataTypeEnum.VENDER.getCode().equals(businessType)){//供应商
                for(int i = 0;i<content.size();i++){
                    VenderRes data = JSONObject.parseObject(content.get(i).toString(), VenderRes.class);
                    Boolean enabledFlag = data.getEnabledFlag();
                    if(enabledFlag){
                        resultList.add(data);
                    }
                }
            }else if(MasterDataTypeEnum.CUSTOMER.getCode().equals(businessType)){//客户
                for(int i = 0;i<content.size();i++){
                    CustomerRes data = JSONObject.parseObject(content.get(i).toString(), CustomerRes.class);
                    Boolean enabledFlag = data.getEnabledFlag();
                    if(enabledFlag){
                        resultList.add(data);
                    }
                }
            }else if(MasterDataTypeEnum.EXCHANGE_RATE.getCode().equals(businessType)){//费率
                for(int i = 0;i<content.size();i++){
                    ExchangeRateRes data = JSONObject.parseObject(content.get(i).toString(), ExchangeRateRes.class);
                    resultList.add(data);
                }
            }else if(MasterDataTypeEnum.BANK.getCode().equals(businessType)){//银行信息
                for(int i = 0;i<content.size();i++){
                    BankRes data = JSONObject.parseObject(content.get(i).toString(), BankRes.class);
                    resultList.add(data);
                }
            }else if(MasterDataTypeEnum.CNAPS.getCode().equals(businessType)){
                for(int i = 0;i<content.size();i++){
                    BankBranchRes data = JSONObject.parseObject(content.get(i).toString(), BankBranchRes.class);
                    resultList.add(data);
                }
            }else if(MasterDataTypeEnum.EMPLOYEE.getCode().equals(businessType)){//员工信息
                for(int i = 0;i<content.size();i++){
                    EmployeeRes data = JSONObject.parseObject(content.get(i).toString(), EmployeeRes.class);
                    resultList.add(data);
                }
            }
            if (singlePage) {
                nextPage = false;
                continue;
            }
            if (masterData == null) {
                nextPage = false;
                continue;
            }
            int totalPages = masterData.getTotalPages()-1;//总页数
            if(totalPages>currentPage){
                currentPage++;
            }else{
                nextPage = false;
            }
        }
        return resultList;
    }

    private CreateVendorRequest buildVendorUpdateRequest(QueryAllVendorResponse.Item item, VenderRes venderRes) {
        VendorInfoResponse vendorV2 = zhiShuVendorClient.getVendorV2(item.getId());
        CreateVendorRequest updateRequest = copyVendorItemToUpdateRequest(vendorV2);

        if (!Objects.equals(item.getAdCountry(), venderRes.getPkCountry())) {
            updateRequest.setAdCountry(venderRes.getPkCountry());
        }
        if (!Objects.equals(item.getVendorText(), venderRes.getDescription())) {
            updateRequest.setVendorText(venderRes.getDescription());
        }
        if (!Objects.equals(item.getCertificationId(), venderRes.getTaxpayerNumber())) {
            updateRequest.setCertificationId(venderRes.getTaxpayerNumber());
        }

        return updateRequest;
    }

    private CreateVendorRequest buildCustomerUpdateRequest(QueryAllVendorResponse.Item item, CustomerRes customerRes) {
        VendorInfoResponse vendorV2 = zhiShuVendorClient.getVendorV2(item.getId());
        CreateVendorRequest updateRequest = copyVendorItemToUpdateRequest(vendorV2);
        String pkCountry = customerRes.getPkCountry() == null ? "CN" : customerRes.getPkCountry();

        if (!Objects.equals(item.getAdCountry(), pkCountry)) {
            updateRequest.setAdCountry(pkCountry);
        }
        if (!Objects.equals(item.getVendorText(), customerRes.getDescription())) {
            updateRequest.setVendorText(customerRes.getDescription());
        }
        if (!Objects.equals(item.getCertificationId(), customerRes.getTaxpayerNumber())) {
            updateRequest.setCertificationId(customerRes.getTaxpayerNumber());
        }

        return updateRequest;
    }

    private CreateVendorRequest copyVendorItemToUpdateRequest(VendorInfoResponse item) {
        CreateVendorRequest updateRequest = new CreateVendorRequest();
        updateRequest.setId(item.getId());
        updateRequest.setAdCountry(item.getAdCountry());
        updateRequest.setAdProvince(item.getAdProvince());
        updateRequest.setAdCity(item.getAdCity());
        updateRequest.setAddress(item.getAddress());
        updateRequest.setAdPostcode(item.getAdPostcode());
        updateRequest.setLegalPerson(item.getLegalPerson());
        updateRequest.setCertificationType(item.getCertificationType());
        updateRequest.setCertificationId(item.getCertificationId());
        updateRequest.setContactPerson(item.getContactPerson());
        updateRequest.setContactTelephone(item.getContactTelephone());
        updateRequest.setContactMobilePhone(item.getContactMobilePhone());
        updateRequest.setFax(item.getFax());
        updateRequest.setEmail(item.getEmail());
        updateRequest.setStatus(item.getStatus() == null ? null : item.getStatus().intValue());
        updateRequest.setVendor(item.getVendor());
        updateRequest.setVendorText(item.getVendorText());
        updateRequest.setShortText(item.getShortText());
        updateRequest.setVendorType(item.getVendorType());
        updateRequest.setVendorCategory(item.getVendorCategory());
        updateRequest.setVendorNature(item.getVendorNature());
        updateRequest.setLinkedEmployee(item.getLinkedEmployee());
        updateRequest.setLinkedCustomer(item.getLinkedCustomer());
        updateRequest.setIsRisked(item.getIsRisked());
        updateRequest.setAssociatedWithLegalEntity(item.getAssociatedWithLegalEntity());
        updateRequest.setAppendix(copyAppendixList(item.getAppendix()));
        updateRequest.setExtendInfo(copyExtendInfoList(item.getExtendInfo()));
        updateRequest.setVendorAccounts(copyVendorAccountList(item.getVendorAccounts()));
        updateRequest.setVendorAddresses(copyVendorAddressList(item.getVendorAddresses()));
        updateRequest.setVendorCompanyViews(copyVendorCompanyViewList(item.getVendorCompanyViews()));
        updateRequest.setVendorContacts(copyVendorContactList(item.getVendorContacts()));
        updateRequest.setGlAccount(item.getGlAccount());
        updateRequest.setDownPaymentTerm(item.getDownPaymentTerm());
        updateRequest.setPaymentTerm(item.getPaymentTerm());
        updateRequest.setVendorSiteCode(item.getVendorSiteCode());
        updateRequest.setOwnerDepts(item.getOwnerDepts());
        return updateRequest;
    }

    private List<CreateVendorRequest.Appendix> copyAppendixList(List<VendorInfoResponse.Appendix> source) {
        if (source == null) {
            return null;
        }
        List<CreateVendorRequest.Appendix> target = new ArrayList<>();
        for (VendorInfoResponse.Appendix sourceItem : source) {
            CreateVendorRequest.Appendix targetItem = new CreateVendorRequest.Appendix();
            targetItem.setTenantId(sourceItem.getTenantId());
            targetItem.setFileId(sourceItem.getFileId());
            targetItem.setFileName(sourceItem.getFileName());
            targetItem.setFileType(sourceItem.getFileType());
            targetItem.setFileSize(sourceItem.getFileSize());
            targetItem.setDownloadUrl(sourceItem.getDownloadUrl());
            target.add(targetItem);
        }
        return target;
    }

    private List<CreateVendorRequest.ExtendInfo> copyExtendInfoList(List<VendorInfoResponse.ExtendInfo> source) {
        if (source == null) {
            return null;
        }
        List<CreateVendorRequest.ExtendInfo> target = new ArrayList<>();
        for (VendorInfoResponse.ExtendInfo sourceItem : source) {
            CreateVendorRequest.ExtendInfo targetItem = new CreateVendorRequest.ExtendInfo();
            targetItem.setFieldType(sourceItem.getFieldType());
            targetItem.setFieldValue(sourceItem.getFieldValue());
            targetItem.setOptions(sourceItem.getOptions());
            targetItem.setNum(sourceItem.getNum());
            targetItem.setDate(sourceItem.getDate());
            targetItem.setRangeDate(sourceItem.getRangeDate());
            targetItem.setFieldCode(sourceItem.getFieldCode());
            targetItem.setAppendix(copyAppendixList(sourceItem.getAppendix()));
            target.add(targetItem);
        }
        return target;
    }

    private List<CreateVendorRequest.VendorAccount> copyVendorAccountList(List<VendorInfoResponse.VendorAccount> source) {
        if (source == null) {
            return null;
        }
        List<CreateVendorRequest.VendorAccount> target = new ArrayList<>();
        for (VendorInfoResponse.VendorAccount sourceItem : source) {
            CreateVendorRequest.VendorAccount targetItem = new CreateVendorRequest.VendorAccount();
            targetItem.setAccount(sourceItem.getAccount());
            targetItem.setIban(sourceItem.getIban());
            targetItem.setAccountName(sourceItem.getAccountName());
            targetItem.setBankId(sourceItem.getBankId());
            targetItem.setBankCode(sourceItem.getBankCode());
            targetItem.setSwiftCode(sourceItem.getSwiftCode());
            targetItem.setVendorSiteCode(sourceItem.getVendorSiteCode());
            targetItem.setBankName(sourceItem.getBankName());
            targetItem.setBankAcronym(sourceItem.getBankAcronym());
            targetItem.setCountry(sourceItem.getCountry());
            targetItem.setBankControlCode(sourceItem.getBankControlCode());
            targetItem.setExtendInfo(copyExtendInfoList(sourceItem.getExtendInfo()));
            target.add(targetItem);
        }
        return target;
    }

    private List<CreateVendorRequest.VendorAddress> copyVendorAddressList(List<VendorInfoResponse.VendorAddress> source) {
        if (source == null) {
            return null;
        }
        List<CreateVendorRequest.VendorAddress> target = new ArrayList<>();
        for (VendorInfoResponse.VendorAddress sourceItem : source) {
            CreateVendorRequest.VendorAddress targetItem = new CreateVendorRequest.VendorAddress();
            targetItem.setCountry(sourceItem.getCountry());
            targetItem.setProvince(sourceItem.getProvince());
            targetItem.setCity(sourceItem.getCity());
            targetItem.setCounty(sourceItem.getCounty());
            targetItem.setAddress(sourceItem.getAddress());
            targetItem.setExtendInfo(copyExtendInfoList(sourceItem.getExtendInfo()));
            target.add(targetItem);
        }
        return target;
    }

    private List<CreateVendorRequest.VendorCompanyView> copyVendorCompanyViewList(List<VendorInfoResponse.VendorCompanyView> source) {
        if (source == null) {
            return null;
        }
        List<CreateVendorRequest.VendorCompanyView> target = new ArrayList<>();
        for (VendorInfoResponse.VendorCompanyView sourceItem : source) {
            CreateVendorRequest.VendorCompanyView targetItem = new CreateVendorRequest.VendorCompanyView();
            targetItem.setCompanyCode(sourceItem.getCompanyCode());
            targetItem.setGlAccount(sourceItem.getGlAccount());
            targetItem.setVendorSiteCode(sourceItem.getVendorSiteCode());
            targetItem.setPaymentTerm(sourceItem.getPaymentTerm());
            targetItem.setDownPaymentTerm(sourceItem.getDownPaymentTerm());
            targetItem.setExtendInfo(copyExtendInfoList(sourceItem.getExtendInfo()));
            target.add(targetItem);
        }
        return target;
    }

    private List<CreateVendorRequest.VendorContact> copyVendorContactList(List<VendorInfoResponse.VendorContact> source) {
        if (source == null) {
            return null;
        }
        List<CreateVendorRequest.VendorContact> target = new ArrayList<>();
        for (VendorInfoResponse.VendorContact sourceItem : source) {
            CreateVendorRequest.VendorContact targetItem = new CreateVendorRequest.VendorContact();
            targetItem.setName(sourceItem.getName());
            targetItem.setPosition(sourceItem.getPosition());
            targetItem.setEmail(sourceItem.getEmail());
            targetItem.setPhone(sourceItem.getPhone());
            targetItem.setRemark(sourceItem.getRemark());
            targetItem.setExtendInfo(copyExtendInfoList(sourceItem.getExtendInfo()));
            target.add(targetItem);
        }
        return target;
    }

    private LinkedHashSet<String> normalizeBusinessCodes(Collection<String> businessCodes) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (businessCodes == null || businessCodes.isEmpty()) {
            return result;
        }
        for (String businessCode : businessCodes) {
            if (isBlank(businessCode)) {
                continue;
            }
            result.add(businessCode.trim());
        }
        return result;
    }

    private List<VenderRes> filterVendorListByBusinessCodes(List<VenderRes> allVendorList, LinkedHashSet<String> targetCodes) {
        if (allVendorList == null || allVendorList.isEmpty() || targetCodes == null || targetCodes.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, List<VenderRes>> vendorListByCode = new LinkedHashMap<>();
        for (VenderRes venderRes : allVendorList) {
            String venderCode = venderRes == null ? null : venderRes.getVenderCode();
            if (isBlank(venderCode)) {
                continue;
            }
            String normalizedCode = venderCode.trim();
            if (!targetCodes.contains(normalizedCode)) {
                continue;
            }
            vendorListByCode.computeIfAbsent(normalizedCode, key -> new ArrayList<>()).add(venderRes);
        }
        List<VenderRes> result = new ArrayList<>();
        List<String> unmatchedCodes = new ArrayList<>();
        for (String targetCode : targetCodes) {
            List<VenderRes> vendorList = vendorListByCode.get(targetCode);
            if (vendorList == null || vendorList.isEmpty()) {
                unmatchedCodes.add(targetCode);
                continue;
            }
            result.addAll(vendorList);
        }
        if (!unmatchedCodes.isEmpty()) {
            log.info("按业财供应商编码集合同步交易方时，以下供应商编码未匹配到业财数据：{}", unmatchedCodes);
        }
        return result;
    }

    private List<CustomerRes> filterCustomerListByBusinessCodes(List<CustomerRes> allCustomerList,
                                                                LinkedHashSet<String> targetCodes) {
        if (allCustomerList == null || allCustomerList.isEmpty() || targetCodes == null || targetCodes.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, List<CustomerRes>> customerListByCode = new LinkedHashMap<>();
        for (CustomerRes customerRes : allCustomerList) {
            String customerCode = customerRes == null ? null : customerRes.getCustomerCode();
            if (isBlank(customerCode)) {
                continue;
            }
            String normalizedCode = customerCode.trim();
            if (!targetCodes.contains(normalizedCode)) {
                continue;
            }
            customerListByCode.computeIfAbsent(normalizedCode, key -> new ArrayList<>()).add(customerRes);
        }
        List<CustomerRes> result = new ArrayList<>();
        List<String> unmatchedCodes = new ArrayList<>();
        for (String targetCode : targetCodes) {
            List<CustomerRes> customerList = customerListByCode.get(targetCode);
            if (customerList == null || customerList.isEmpty()) {
                unmatchedCodes.add(targetCode);
                continue;
            }
            result.addAll(customerList);
        }
        if (!unmatchedCodes.isEmpty()) {
            log.info("按业财客户编码集合同步交易方时，以下客户编码未匹配到业财数据：{}", unmatchedCodes);
        }
        return result;
    }

    private List<VenderRes> filterVendorListByCertificationId(List<VenderRes> allVendorList, String certificationId) {
        if (allVendorList == null || allVendorList.isEmpty()) {
            return Collections.emptyList();
        }
        if (isBlank(certificationId)) {
            log.info("供应商主数据同步未传证件 ID，本次同步全部供应商");
            return allVendorList;
        }
        String targetCertificationId = certificationId.trim();
        List<VenderRes> vendorList = new ArrayList<>();
        int matchedCount = 0;
        for (VenderRes venderRes : allVendorList) {
            String taxpayerNumber = venderRes == null ? null : venderRes.getTaxpayerNumber();
            if (taxpayerNumber != null && targetCertificationId.equals(taxpayerNumber.trim())) {
                matchedCount++;
                if (vendorList.isEmpty()) {
                    vendorList.add(venderRes);
                }
            }
        }
        if (matchedCount > 1) {
            log.warn("证件 ID：{} 在业财供应商中匹配到 {} 条数据，本次仅同步第一条", targetCertificationId, matchedCount);
        }
        log.info("供应商主数据同步传入证件 ID：{}，匹配到供应商数量：{}", targetCertificationId, matchedCount);
        return vendorList;
    }

    private List<CustomerRes> filterCustomerListByCertificationId(List<CustomerRes> allCustomerList,
                                                                  String certificationId) {
        if (allCustomerList == null || allCustomerList.isEmpty()) {
            return Collections.emptyList();
        }
        if (isBlank(certificationId)) {
            log.info("客户主数据同步未传证件 ID，本次同步全部客户");
            return allCustomerList;
        }
        String targetCertificationId = certificationId.trim();
        List<CustomerRes> customerList = new ArrayList<>();
        int matchedCount = 0;
        for (CustomerRes customerRes : allCustomerList) {
            String taxpayerNumber = customerRes == null ? null : customerRes.getTaxpayerNumber();
            if (taxpayerNumber != null && targetCertificationId.equals(taxpayerNumber.trim())) {
                matchedCount++;
                if (customerList.isEmpty()) {
                    customerList.add(customerRes);
                }
            }
        }
        if (matchedCount > 1) {
            log.warn("证件 ID：{} 在业财客户中匹配到 {} 条数据，本次仅同步第一条", targetCertificationId, matchedCount);
        }
        log.info("客户主数据同步传入证件 ID：{}，匹配到客户数量：{}", targetCertificationId, matchedCount);
        return customerList;
    }

    private QueryAllVendorResponse getVendorForSync(String certificationId) {
        if (isBlank(certificationId)) {
            return getVendorAll();
        }

        String targetCertificationId = certificationId.trim();
        VendorInfoResponse vendorInfo = zhiShuVendorClient.getVendorByCertificationId(targetCertificationId);
        QueryAllVendorResponse vendorResponse = new QueryAllVendorResponse();
        List<QueryAllVendorResponse.Item> items = new ArrayList<>();
        if (vendorInfo != null) {
            QueryAllVendorResponse.Item item = JSONObject.parseObject(
                    JSONObject.toJSONString(vendorInfo), QueryAllVendorResponse.Item.class);
            if (item != null) {
                items.add(item);
                log.info("根据证件 ID：{} 查询到智书交易方，交易方编码：{}", targetCertificationId, item.getVendor());
            }
        } else {
            log.info("根据证件 ID：{} 未查询到智书交易方，本次将按新增处理", targetCertificationId);
        }
        vendorResponse.setItems(items);
        vendorResponse.setHasMore(false);
        return vendorResponse;
    }

    private void syncVendorMasterDataByVendorCode(List<VenderRes> allVendorList) {
        BankLookupContext bankLookupContext = createBankLookupContext();
        VendorSyncResult result = new VendorSyncResult();
        result.vendorCount = allVendorList == null ? 0 : allVendorList.size();
        log.info("供应商主数据按交易方编码同步开始，总数：{}", result.vendorCount);
        if (allVendorList == null) {
            return;
        }
        List<String> addVendorCode = new ArrayList<>();
        List<String> updateVendorCode = new ArrayList<>();
        for (VenderRes venderRes : allVendorList) {
            if (Thread.currentThread().isInterrupted()) {
                log.warn("供应商主数据按交易方编码同步被中断");
                break;
            }
            String vendorCode = buildVendorCode(venderRes);
            try {
                if (isBlank(vendorCode)) {
                    result.itemFailCount++;
                    log.info("供应商主数据按交易方编码同步跳过，交易方编码为空，供应商编码：{}",
                            venderRes == null ? null : venderRes.getVenderCode());
                    continue;
                }
                QueryAllVendorResponse vendorResponse = getExactVendorForCode(vendorCode);
                if(vendorResponse.getItems().isEmpty()){
                    vendorResponse = getExactVendorForCode(venderRes.getVenderCode());
                    if(vendorResponse.getItems().isEmpty()){
                        vendorResponse = getExactVendorForCode(venderRes.getCustomerCode());
                    }
                }
                Map<String, Object> resultMap = compareVendor(
                        vendorResponse, Collections.singletonList(venderRes), bankLookupContext);
                List<CreateVendorRequest> addList = getVendorRequestList(resultMap, ADD_LIST_KEY);
                for (CreateVendorRequest createVendorRequest : addList) {
                    createVendorAndAntiBriberyContract(createVendorRequest, result);
                    addVendorCode.add(createVendorRequest.getVendor()+"---"+createVendorRequest.getVendorType());
                }
                List<CreateVendorRequest> updateList = getVendorRequestList(resultMap, UPDATE_LIST_KEY);
                for (CreateVendorRequest updateVendorRequest : updateList) {
                    updateVendor(updateVendorRequest, result);
                    updateVendorCode.add(updateVendorRequest.getVendor()+"---"+updateVendorRequest.getVendorType());
                }
                if (addList.isEmpty() && updateList.isEmpty()) {
                    log.info("供应商主数据按交易方编码同步未生成新增或更新请求，交易方编码：{}", vendorCode);
                }
            } catch (Exception e) {
                result.itemFailCount++;
                log.error("供应商主数据按交易方编码同步失败，交易方编码：{}", vendorCode, e);
            }
        }
        log.info("供应商主数据按交易方编码同步完成，总数：{}，数据处理失败：{}，新增成功：{}，新增失败：{}，更新成功：{}，更新失败：{}，反贿赂协议创建失败：{}",
                result.vendorCount, result.itemFailCount, result.addSuccessCount, result.addFailCount,
                result.updateSuccessCount, result.updateFailCount, result.antiBriberyFailCount);
        log.info("新增供应商个数：{} 编码：{}",addVendorCode.size(),addVendorCode);
        log.info("修改供应商个数：{} 编码：{}",updateVendorCode.size(),updateVendorCode);
    }

    private void syncCustomerMasterDataByVendorCode(List<CustomerRes> allCustomerList) {
        BankLookupContext bankLookupContext = createBankLookupContext();
        CustomerSyncResult result = new CustomerSyncResult();
        result.customerCount = allCustomerList == null ? 0 : allCustomerList.size();
        log.info("客户主数据按交易方编码同步开始，总数：{}", result.customerCount);
        if (allCustomerList == null) {
            return;
        }
        List<String> addCustomerCode = new ArrayList<>();
        List<String> updateCustomerCode = new ArrayList<>();
        for (CustomerRes customerRes : allCustomerList) {
            if (Thread.currentThread().isInterrupted()) {
                log.warn("客户主数据按交易方编码同步被中断");
                break;
            }
            String vendorCode = buildCustomerVendorCode(customerRes);
            try {
                if (isBlank(vendorCode)) {
                    result.itemFailCount++;
                    log.info("客户主数据按交易方编码同步跳过，交易方编码为空，客户编码：{}",
                            customerRes == null ? null : customerRes.getCustomerCode());
                    continue;
                }
                QueryAllVendorResponse vendorResponse = getExactVendorForCode(vendorCode);
                Map<String, Object> resultMap = compareCustomer(
                        vendorResponse, Collections.singletonList(customerRes), bankLookupContext);
                List<CreateVendorRequest> addList = getVendorRequestList(resultMap, ADD_LIST_KEY);
                for (CreateVendorRequest createVendorRequest : addList) {
                    createCustomerAndAntiBriberyContract(createVendorRequest, result);
                    addCustomerCode.add(createVendorRequest.getVendor() + "---" + createVendorRequest.getVendorType());
                }
                List<CreateVendorRequest> updateList = getVendorRequestList(resultMap, UPDATE_LIST_KEY);
                for (CreateVendorRequest updateCustomerRequest : updateList) {
                    updateCustomer(updateCustomerRequest, result);
                    updateCustomerCode.add(updateCustomerRequest.getVendor() + "---" + updateCustomerRequest.getVendorType());
                }
                if (addList.isEmpty() && updateList.isEmpty()) {
                    log.info("客户主数据按交易方编码同步未生成新增或更新请求，交易方编码：{}", vendorCode);
                }
            } catch (Exception e) {
                result.itemFailCount++;
                log.error("客户主数据按交易方编码同步失败，交易方编码：{}", vendorCode, e);
            }
        }
        log.info("客户主数据按交易方编码同步完成，总数：{}，数据处理失败：{}，新增成功：{}，新增失败：{}，更新成功：{}，更新失败：{}，反贿赂协议创建失败：{}",
                result.customerCount, result.itemFailCount, result.addSuccessCount, result.addFailCount,
                result.updateSuccessCount, result.updateFailCount, result.antiBriberyFailCount);
        log.info("新增客户个数：{} 编码：{}", addCustomerCode.size(), addCustomerCode);
        log.info("修改客户个数：{} 编码：{}", updateCustomerCode.size(), updateCustomerCode);
    }

    private QueryAllVendorResponse getExactVendorForCode(String vendorCode) {
        QueryAllVendorResponse exactResponse = new QueryAllVendorResponse();
        exactResponse.setItems(new ArrayList<>());
        exactResponse.setHasMore(false);
        QueryAllVendorResponse vendorResponse = zhiShuVendorClient.getVendorByCode(vendorCode);
        List<QueryAllVendorResponse.Item> items = vendorResponse == null || vendorResponse.getItems() == null
                ? Collections.emptyList() : vendorResponse.getItems();
        int matchCount = 0;
        for (QueryAllVendorResponse.Item item : items) {
            if (item != null && Objects.equals(vendorCode, item.getVendor())) {
                matchCount++;
                if (exactResponse.getItems().isEmpty()) {
                    exactResponse.getItems().add(item);
                }
            }
        }
        if (matchCount > 1) {
            log.warn("按交易方编码查询到多条完全匹配的智书交易方，匹配数量：{}，交易方编码：{}", matchCount, vendorCode);
        }
        return exactResponse;
    }

    private String buildVendorCode(VenderRes venderRes) {
        if (venderRes == null) {
            return null;
        }
        String venderCode = venderRes.getVenderCode();
        String customerCode = venderRes.getCustomerCode();
        if (customerCode != null && !customerCode.isEmpty()) {
            venderCode = venderCode + ";" + customerCode;
        }
        return venderCode;
    }

    private String buildCustomerVendorCode(CustomerRes customerRes) {
        if (customerRes == null) {
            return null;
        }
        String vendorCode = customerRes.getCustomerCode();
        String venderCode = customerRes.getVenderCode();
        if (venderCode != null && !venderCode.isEmpty()) {
            vendorCode = venderCode + ";" + vendorCode;
        }
        return vendorCode;
    }

    private void syncVendorMasterDataSingleThread(QueryAllVendorResponse vendorAll, List<VenderRes> allVendorList) {
        if (allVendorList == null || allVendorList.isEmpty()) {
            log.info("未获取到供应商主数据，本次不执行供应商同步");
            return;
        }
        BankLookupContext bankLookupContext = createBankLookupContext();
        log.info("供应商主数据单线程同步开始，总数：{}", allVendorList.size());
        VendorSyncResult result = syncVendorBatch(vendorAll, allVendorList, bankLookupContext, 1);
        log.info("供应商主数据单线程同步完成，总数：{}，分片失败数：{}，新增成功：{}，新增失败：{}，更新成功：{}，更新失败：{}，反贿赂协议创建失败：{}",
                result.vendorCount, result.batchFailCount,
                result.addSuccessCount, result.addFailCount,
                result.updateSuccessCount, result.updateFailCount,
                result.antiBriberyFailCount);
    }

    private void syncVendorMasterDataInParallel(QueryAllVendorResponse vendorAll, List<VenderRes> allVendorList) {
        if (allVendorList == null || allVendorList.isEmpty()) {
            log.info("未获取到供应商主数据，本次不执行供应商同步");
            return;
        }
        BankLookupContext bankLookupContext = createBankLookupContext();

        int totalCount = allVendorList.size();
        int batchCount = (totalCount + VENDOR_SYNC_BATCH_SIZE - 1) / VENDOR_SYNC_BATCH_SIZE;
        log.info("供应商主数据同步开始，总数：{}，分片数：{}，线程数：{}，分片大小：{}",
                totalCount, batchCount, VENDOR_SYNC_THREAD_COUNT, VENDOR_SYNC_BATCH_SIZE);

        ExecutorService executorService = Executors.newFixedThreadPool(VENDOR_SYNC_THREAD_COUNT);
        List<Future<VendorSyncResult>> futures = new ArrayList<>();
        try {
            for (int start = 0, batchNo = 1; start < totalCount; start += VENDOR_SYNC_BATCH_SIZE, batchNo++) {
                int end = Math.min(start + VENDOR_SYNC_BATCH_SIZE, totalCount);
                List<VenderRes> batchVendorList = new ArrayList<>(allVendorList.subList(start, end));
                final int currentBatchNo = batchNo;
                final BankLookupContext bankLookup = bankLookupContext;
                futures.add(executorService.submit(ApiLogTaskContext.wrap(
                        ApiLogTableContext.wrap(
                                () -> syncVendorBatch(vendorAll, batchVendorList, bankLookup, currentBatchNo)))));
            }

            VendorSyncResult totalResult = new VendorSyncResult();
            totalResult.batchCount = batchCount;
            totalResult.vendorCount = totalCount;
            for (Future<VendorSyncResult> future : futures) {
                try {
                    totalResult.merge(future.get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("供应商主数据并行同步等待线程结果时被中断", e);
                    break;
                } catch (ExecutionException e) {
                    totalResult.batchFailCount++;
                    log.error("供应商主数据并行同步分片执行失败", e);
                }
            }
            log.info("供应商主数据同步完成，总数：{}，分片数：{}，分片失败数：{}，新增成功：{}，新增失败：{}，更新成功：{}，更新失败：{}，反贿赂协议创建失败：{}",
                    totalResult.vendorCount, totalResult.batchCount, totalResult.batchFailCount,
                    totalResult.addSuccessCount, totalResult.addFailCount,
                    totalResult.updateSuccessCount, totalResult.updateFailCount,
                    totalResult.antiBriberyFailCount);
        } finally {
            executorService.shutdown();
        }
    }

    private VendorSyncResult syncVendorBatch(QueryAllVendorResponse vendorAll, List<VenderRes> vendorList,
                                             BankLookupContext bankLookupContext, int batchNo) {
        VendorSyncResult result = new VendorSyncResult();
        result.vendorCount = vendorList == null ? 0 : vendorList.size();
        log.info("供应商主数据分片{}开始同步，数量：{}", batchNo, result.vendorCount);
        List<String> addVendorCode = new ArrayList<>();
        List<String> updateVendorCode = new ArrayList<>();
        try {
            Map<String, Object> resultMap = compareVendor(vendorAll, vendorList, bankLookupContext);
            List<CreateVendorRequest> addList = getVendorRequestList(resultMap, ADD_LIST_KEY);
            for (CreateVendorRequest createVendorRequest : addList) {
//                createVendorAndAntiBriberyContract(createVendorRequest, result);
//                pauseSync();//休眠1秒保证一秒钟处理一次
                addVendorCode.add(createVendorRequest.getVendor()+"---"+createVendorRequest.getVendorType());
            }

            List<CreateVendorRequest> updateList = getVendorRequestList(resultMap, UPDATE_LIST_KEY);
            for (CreateVendorRequest updateVendorRequest : updateList) {
//                updateVendor(updateVendorRequest, result);
//                pauseSync();//休眠1秒保证一秒钟处理一次
                updateVendorCode.add(updateVendorRequest.getVendor()+"---"+updateVendorRequest.getVendorType());
            }
            log.info("供应商主数据分片{}同步完成，新增成功：{}，新增失败：{}，更新成功：{}，更新失败：{}，反贿赂协议创建失败：{}",
                    batchNo, result.addSuccessCount, result.addFailCount,
                    result.updateSuccessCount, result.updateFailCount, result.antiBriberyFailCount);

            log.info("新增供应商个数：{} 编码：{}",addVendorCode.size(),addVendorCode);
            log.info("修改供应商个数：{} 编码：{}",updateVendorCode.size(),updateVendorCode);
        } catch (Exception e) {
            result.batchFailCount++;
            log.error("供应商主数据分片{}同步失败，数量：{}", batchNo, result.vendorCount, e);
        }
        return result;
    }

    private void createVendorAndAntiBriberyContract(CreateVendorRequest createVendorRequest, VendorSyncResult result) {
        if (createVendorRequest == null) {
            result.addFailCount++;
            log.info("供应商创建交易方请求为空，跳过本条数据");
            return;
        }
        CreateVendorResponse vendorResponse;
        try {
            vendorResponse = zhiShuVendorClient.createVendor(createVendorRequest);
        } catch (Exception e) {
            result.addFailCount++;
            log.error("供应商:{}信息创建交易方异常", createVendorRequest.getVendor(), e);
            return;
        }

        if (vendorResponse == null) {
            result.addFailCount++;
            log.info("供应商:{}信息创建交易方失败！！！", createVendorRequest.getVendor());
            return;
        }
        result.addSuccessCount++;
        createAntiBriberyContractAfterVendorCreated(createVendorRequest, vendorResponse, result);
    }

    private void createAntiBriberyContractAfterVendorCreated(CreateVendorRequest createVendorRequest,
                                                             CreateVendorResponse vendorResponse,
                                                             VendorSyncResult result) {
        try {
            VendorInfoResponse vendorV2 = zhiShuVendorClient.getVendorV2(vendorResponse.getId());
            QueryAllVendorResponse.Item item = JSONObject.parseObject(JSONObject.toJSONString(vendorV2), QueryAllVendorResponse.Item.class);
            CreateAntiBriberyContractResultDTO antiBriberyResult = contractService.createAntiBriberyContract(item);
            if (antiBriberyResult == null || !Boolean.TRUE.equals(antiBriberyResult.getSuccess())) {
                result.antiBriberyFailCount++;
                String errMessage = antiBriberyResult == null ? "返回结果为空" : antiBriberyResult.getErrMessage();
                log.info("同步供应商后，反贿赂协议创建失败：{}--->{}",
                        vendorV2 == null ? createVendorRequest.getVendor() : vendorV2.getVendor(), errMessage);
            }
        } catch (Exception e) {
            result.antiBriberyFailCount++;
            log.error("同步供应商后，反贿赂协议创建异常：{}", createVendorRequest.getVendor(), e);
        }
    }

    private void updateVendor(CreateVendorRequest createVendorRequest, VendorSyncResult result) {
        if (createVendorRequest == null) {
            result.updateFailCount++;
            log.info("供应商更新交易方请求为空，跳过本条数据");
            return;
        }
        try {
            String id = createVendorRequest.getId();
            CreateVendorResponse vendorResponse = zhiShuVendorClient.updateVendor(createVendorRequest, id);
            if (vendorResponse == null) {
                result.updateFailCount++;
                log.info("供应商:{}信息更新交易方失败！！！", createVendorRequest.getVendor());
            } else {
                result.updateSuccessCount++;
            }
        } catch (Exception e) {
            result.updateFailCount++;
            log.error("供应商:{}信息更新交易方异常", createVendorRequest.getVendor(), e);
        }
    }

    private void syncCustomerMasterDataSingleThread(QueryAllVendorResponse vendorAll,
                                                    List<CustomerRes> allCustomerList) {
        if (allCustomerList == null || allCustomerList.isEmpty()) {
            log.info("未获取到客户主数据，本次不执行客户同步");
            return;
        }
        BankLookupContext bankLookupContext = createBankLookupContext();
        log.info("客户主数据单线程同步开始，总数：{}", allCustomerList.size());
        CustomerSyncResult result = syncCustomerBatch(vendorAll, allCustomerList, bankLookupContext, 1);
        log.info("客户主数据单线程同步完成，总数：{}，数据处理失败：{}，新增成功：{}，新增失败：{}，更新成功：{}，更新失败：{}，反贿赂协议创建失败：{}",
                result.customerCount, result.itemFailCount,
                result.addSuccessCount, result.addFailCount,
                result.updateSuccessCount, result.updateFailCount,
                result.antiBriberyFailCount);
    }

    private void syncCustomerMasterDataInParallel(QueryAllVendorResponse vendorAll,
                                                  List<CustomerRes> allCustomerList) {
        if (allCustomerList == null || allCustomerList.isEmpty()) {
            log.info("未获取到客户主数据，本次不执行客户同步");
            return;
        }
        BankLookupContext bankLookupContext = createBankLookupContext();


        int totalCount = allCustomerList.size();
        int batchCount = (totalCount + VENDOR_SYNC_BATCH_SIZE - 1) / VENDOR_SYNC_BATCH_SIZE;
        log.info("客户主数据同步开始，总数：{}，分片数：{}，线程数：{}，分片大小：{}",
                totalCount, batchCount, VENDOR_SYNC_THREAD_COUNT, VENDOR_SYNC_BATCH_SIZE);

        ExecutorService executorService = Executors.newFixedThreadPool(VENDOR_SYNC_THREAD_COUNT);
        List<Future<CustomerSyncResult>> futures = new ArrayList<>();
        try {
            for (int start = 0, batchNo = 1; start < totalCount; start += VENDOR_SYNC_BATCH_SIZE, batchNo++) {
                int end = Math.min(start + VENDOR_SYNC_BATCH_SIZE, totalCount);
                List<CustomerRes> batchCustomerList = new ArrayList<>(allCustomerList.subList(start, end));
                final int currentBatchNo = batchNo;
                final BankLookupContext bankLookup = bankLookupContext;
                futures.add(executorService.submit(ApiLogTaskContext.wrap(
                        ApiLogTableContext.wrap(
                                () -> syncCustomerBatch(vendorAll, batchCustomerList, bankLookup, currentBatchNo)))));
            }

            CustomerSyncResult totalResult = new CustomerSyncResult();
            totalResult.batchCount = batchCount;
            totalResult.customerCount = totalCount;
            for (Future<CustomerSyncResult> future : futures) {
                try {
                    totalResult.merge(future.get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("客户主数据并行同步等待线程结果时被中断", e);
                    break;
                } catch (ExecutionException e) {
                    totalResult.batchFailCount++;
                    log.error("客户主数据并行同步分片执行失败", e);
                }
            }
            log.info("客户主数据同步完成，总数：{}，分片数：{}，分片失败：{}，数据处理失败：{}，新增成功：{}，新增失败：{}，更新成功：{}，更新失败：{}，反贿赂协议创建失败：{}",
                    totalResult.customerCount, totalResult.batchCount, totalResult.batchFailCount,
                    totalResult.itemFailCount, totalResult.addSuccessCount, totalResult.addFailCount,
                    totalResult.updateSuccessCount, totalResult.updateFailCount,
                    totalResult.antiBriberyFailCount);
        } finally {
            executorService.shutdown();
        }
    }

    private CustomerSyncResult syncCustomerBatch(QueryAllVendorResponse vendorAll,
                                                  List<CustomerRes> customerList,
                                                  BankLookupContext bankLookupContext,
                                                  int batchNo) {
        CustomerSyncResult result = new CustomerSyncResult();
        result.customerCount = customerList == null ? 0 : customerList.size();
        log.info("客户主数据分片{}开始同步，数量：{}", batchNo, result.customerCount);
        if (customerList == null) {
            return result;
        }

        List<String> addCustomerCode = new ArrayList<>();
        List<String> updateCustomerCode = new ArrayList<>();
        for (CustomerRes customerRes : customerList) {
            if (Thread.currentThread().isInterrupted()) {
                log.warn("客户主数据分片{}线程已中断，停止处理剩余数据", batchNo);
                break;
            }
            try {
                Map<String, Object> resultMap = compareCustomer(
                        vendorAll, Collections.singletonList(customerRes), bankLookupContext);
                List<CreateVendorRequest> addList = getVendorRequestList(resultMap, ADD_LIST_KEY);
                for (CreateVendorRequest createVendorRequest : addList) {
//                    createCustomerAndAntiBriberyContract(createVendorRequest, result);
//                    pauseSync();
                    addCustomerCode.add(createVendorRequest.getVendor() + "---" + createVendorRequest.getVendorType());
                }

                List<CreateVendorRequest> updateList = getVendorRequestList(resultMap, UPDATE_LIST_KEY);
                for (CreateVendorRequest updateCustomerRequest : updateList) {
//                    updateCustomer(updateCustomerRequest, result);
//                    pauseSync();
                    updateCustomerCode.add(updateCustomerRequest.getVendor() + "---" + updateCustomerRequest.getVendorType());
                }
                pauseSync();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("客户主数据分片{}处理客户时被中断，客户编码：{}",
                        batchNo, customerRes == null ? null : customerRes.getCustomerCode(), e);
                break;
            } catch (Exception e) {
                result.itemFailCount++;
                log.error("客户主数据分片{}处理客户失败，客户编码：{}",
                        batchNo, customerRes == null ? null : customerRes.getCustomerCode(), e);
            }
        }
        log.info("客户主数据分片{}同步完成，数据处理失败：{}，新增成功：{}，新增失败：{}，更新成功：{}，更新失败：{}，反贿赂协议创建失败：{}",
                batchNo, result.itemFailCount, result.addSuccessCount, result.addFailCount,
                result.updateSuccessCount, result.updateFailCount, result.antiBriberyFailCount);
        log.info("新增客户个数：{} 编码：{}", addCustomerCode.size(), addCustomerCode);
        log.info("修改客户个数：{} 编码：{}", updateCustomerCode.size(), updateCustomerCode);
        return result;
    }

    private void createCustomerAndAntiBriberyContract(CreateVendorRequest createVendorRequest,
                                                      CustomerSyncResult result) {
        if (createVendorRequest == null) {
            result.addFailCount++;
            log.info("客户创建交易方请求为空，跳过本条数据");
            return;
        }
        CreateVendorResponse vendorResponse;
        try {
            vendorResponse = zhiShuVendorClient.createVendor(createVendorRequest);
        } catch (Exception e) {
            result.addFailCount++;
            log.error("客户:{}信息创建交易方异常", createVendorRequest.getVendor(), e);
            return;
        }
        if (vendorResponse == null) {
            result.addFailCount++;
            log.info("客户:{}信息创建交易方失败", createVendorRequest.getVendor());
            return;
        }
        result.addSuccessCount++;
        try {
            VendorInfoResponse vendorV2 = zhiShuVendorClient.getVendorV2(vendorResponse.getId());
            QueryAllVendorResponse.Item item = JSONObject.parseObject(
                    JSONObject.toJSONString(vendorV2), QueryAllVendorResponse.Item.class);
            CreateAntiBriberyContractResultDTO antiBriberyResult = contractService.createAntiBriberyContract(item);
            if (antiBriberyResult == null || !Boolean.TRUE.equals(antiBriberyResult.getSuccess())) {
                result.antiBriberyFailCount++;
                String errMessage = antiBriberyResult == null ? "返回结果为空" : antiBriberyResult.getErrMessage();
                log.info("同步客户后，反贿赂协议创建失败：{}--->{}",
                        vendorV2 == null ? createVendorRequest.getVendor() : vendorV2.getVendor(), errMessage);
            }
        } catch (Exception e) {
            result.antiBriberyFailCount++;
            log.error("同步客户后，反贿赂协议创建异常：{}", createVendorRequest.getVendor(), e);
        }
    }

    private void updateCustomer(CreateVendorRequest createVendorRequest, CustomerSyncResult result) {
        if (createVendorRequest == null) {
            result.updateFailCount++;
            log.info("客户更新交易方请求为空，跳过本条数据");
            return;
        }
        try {
            CreateVendorResponse vendorResponse = zhiShuVendorClient.updateVendor(
                    createVendorRequest, createVendorRequest.getId());
            if (vendorResponse == null) {
                result.updateFailCount++;
                log.info("客户:{}信息更新交易方失败", createVendorRequest.getVendor());
            } else {
                result.updateSuccessCount++;
            }
        } catch (Exception e) {
            result.updateFailCount++;
            log.error("客户:{}信息更新交易方异常", createVendorRequest.getVendor(), e);
        }
    }

    private void pauseSync() throws InterruptedException {
        Thread.sleep(1000);
    }

    @SuppressWarnings("unchecked")
    private List<CreateVendorRequest> getVendorRequestList(Map<String, Object> resultMap, String key) {
        if (resultMap == null || resultMap.get(key) == null) {
            return Collections.emptyList();
        }
        return (List<CreateVendorRequest>) resultMap.get(key);
    }

    private void syncVendorAccountList(List<VenderAccountRes> list,
                                       List<CreateVendorRequest.VendorAccount> vendorAccountList,
                                       BankLookupContext bankLookupContext) {
        if (vendorAccountList == null) {
            return;
        }

        Map<String, VenderAccountRes> yecaiAccountMap = new LinkedHashMap<>();
        if (list != null) {
            for (VenderAccountRes accountRes : list) {
                String account = normalizeAccount(accountRes == null ? null : accountRes.getBankAccountNumber());
                if (account == null) {
                    continue;
                }
                yecaiAccountMap.put(account, accountRes);
            }
        }

        Iterator<CreateVendorRequest.VendorAccount> iterator = vendorAccountList.iterator();
        while (iterator.hasNext()) {
            CreateVendorRequest.VendorAccount vendorAccount = iterator.next();
            String account = normalizeAccount(vendorAccount == null ? null : vendorAccount.getAccount());
            VenderAccountRes accountRes = account == null ? null : yecaiAccountMap.get(account);
            if (accountRes == null) {
                continue;
            }
            fillVendorAccount(vendorAccount, accountRes, bankLookupContext);
            yecaiAccountMap.remove(account);
        }

        for (VenderAccountRes accountRes : yecaiAccountMap.values()) {
            CreateVendorRequest.VendorAccount vendorAccount = new CreateVendorRequest.VendorAccount();
            fillVendorAccount(vendorAccount, accountRes, bankLookupContext);
            vendorAccountList.add(vendorAccount);
        }
    }

    private void fillVendorAccount(CreateVendorRequest.VendorAccount vendorAccount, VenderAccountRes accountRes,
                                   BankLookupContext bankLookupContext) {
        if (vendorAccount == null || accountRes == null) {
            return;
        }
        BankRes bankRes = bankLookupContext == null ? null : bankLookupContext.findBank(accountRes.getBankCode());
        String bankName = bankLookupContext == null ? null
                : bankLookupContext.resolveBankName(accountRes.getBankCode(), accountRes.getBankLocationCode());
        if (isBlank(bankRes == null ? null : bankRes.getBankName()) && !isBlank(bankName)) {
            BankRes fallbackBankRes = new BankRes();
            fallbackBankRes.setCountryCode(bankRes == null ? null : bankRes.getCountryCode());
            fallbackBankRes.setBankName(bankName);
            bankRes = fallbackBankRes;
        }
        String countryCode = bankLookupContext == null ? null
                : bankLookupContext.resolveBankCountryCode(accountRes.getBankCode());
        vendorAccount.setCountry(countryCode);//银行国家
        vendorAccount.setBankName(bankRes == null ? null : StrUtils.substringByLength(bankRes.getBankName(),0,100));//银行名称
        vendorAccount.setAccountName(accountRes.getBankAccountName());//账户名
        vendorAccount.setAccount(StrUtils.removeAllSpaces(accountRes.getBankAccountNumber()));//账号
        vendorAccount.setBankCode(accountRes.getBankLocationCode());//联行号
    }

    private void syncCustomerAccountList(List<CustomerAccountRes> list,
                                         List<CreateVendorRequest.VendorAccount> vendorAccountList,
                                         BankLookupContext bankLookupContext) {
        if (vendorAccountList == null) {
            return;
        }

        Map<String, CustomerAccountRes> yecaiAccountMap = new LinkedHashMap<>();
        if (list != null) {
            for (CustomerAccountRes accountRes : list) {
                String account = normalizeAccount(accountRes == null ? null : accountRes.getBankAccountNumber());
                if (account == null) {
                    continue;
                }
                yecaiAccountMap.put(account, accountRes);
            }
        }

        Iterator<CreateVendorRequest.VendorAccount> iterator = vendorAccountList.iterator();
        while (iterator.hasNext()) {
            CreateVendorRequest.VendorAccount vendorAccount = iterator.next();
            String account = normalizeAccount(vendorAccount == null ? null : vendorAccount.getAccount());
            CustomerAccountRes accountRes = account == null ? null : yecaiAccountMap.get(account);
            if (accountRes == null) {
                continue;
            }
            fillCustomerAccount(vendorAccount, accountRes, bankLookupContext);
            yecaiAccountMap.remove(account);
        }

        for (CustomerAccountRes accountRes : yecaiAccountMap.values()) {
            CreateVendorRequest.VendorAccount vendorAccount = new CreateVendorRequest.VendorAccount();
            fillCustomerAccount(vendorAccount, accountRes, bankLookupContext);
            vendorAccountList.add(vendorAccount);
        }
    }

    private void fillCustomerAccount(CreateVendorRequest.VendorAccount vendorAccount, CustomerAccountRes accountRes,
                                     BankLookupContext bankLookupContext) {
        if (vendorAccount == null || accountRes == null) {
            return;
        }
        BankRes bankRes = bankLookupContext == null ? null : bankLookupContext.findBank(accountRes.getBankCode());
        String bankName = bankLookupContext == null ? null
                : bankLookupContext.resolveBankName(accountRes.getBankCode(), accountRes.getBankLocationCode());
        if (isBlank(bankRes == null ? null : bankRes.getBankName()) && !isBlank(bankName)) {
            BankRes fallbackBankRes = new BankRes();
            fallbackBankRes.setCountryCode(bankRes == null ? null : bankRes.getCountryCode());
            fallbackBankRes.setBankName(bankName);
            bankRes = fallbackBankRes;
        }
        String countryCode = bankLookupContext == null ? null
                : bankLookupContext.resolveBankCountryCode(accountRes.getBankCode());
        vendorAccount.setCountry(countryCode);//银行国家
        vendorAccount.setBankName(bankRes == null ? null : StrUtils.substringByLength(bankRes.getBankName(),0,100));//银行名称
        vendorAccount.setAccountName(accountRes.getBankAccountName());//账户名
        vendorAccount.setAccount(StrUtils.removeAllSpaces(accountRes.getBankAccountNumber()));//账号
        vendorAccount.setBankCode(accountRes.getBankLocationCode());//联行号
    }

    @SuppressWarnings("unchecked")
    private BankLookupContext createBankLookupContext() {
        List<BankRes> allBankResList = (List<BankRes>) getAllMasterData(MasterDataTypeEnum.BANK.getCode());
        return new BankLookupContext(allBankResList);
    }

    private class BankLookupContext {
        private final Map<String, BankRes> bankByCode;
        private volatile Map<String, String> cnapsNameByLocationCode;
        private volatile Map<String, String> bankCountryCodeByBankCode;

        private BankLookupContext(List<BankRes> bankList) {
            this.bankByCode = buildBankMap(bankList);
        }

        private BankRes findBank(String bankCode) {
            if (isBlank(bankCode)) {
                return null;
            }
            return bankByCode.get(bankCode);
        }

        private String resolveBankName(String bankCode, String bankLocationCode) {
            BankRes bankRes = findBank(bankCode);
            if (bankRes != null && !isBlank(bankRes.getBankName())) {
                return bankRes.getBankName();
            }
            if (isBlank(bankLocationCode)) {
                return null;
            }
            return getCnapsNameByLocationCode().get(bankLocationCode);
        }

        private String resolveBankCountryCode(String bankCode) {
            if (isBlank(bankCode)) {
                return null;
            }
            String countryCode = getBankCountryCodeByBankCode().get(bankCode);
            if (!isBlank(countryCode)) {
                return countryCode;
            }
            BankRes bankRes = findBank(bankCode);
            return bankRes == null ? null : bankRes.getCountryCode();
        }

        private Map<String, BankRes> buildBankMap(List<BankRes> bankList) {
            Map<String, BankRes> result = new HashMap<>();
            if (bankList == null || bankList.isEmpty()) {
                return result;
            }
            for (BankRes bankRes : bankList) {
                if (bankRes == null || isBlank(bankRes.getBankCode())) {
                    continue;
                }
                result.put(bankRes.getBankCode(), bankRes);
            }
            return result;
        }

        private Map<String, String> getBankCountryCodeByBankCode() {
            Map<String, String> localMap = bankCountryCodeByBankCode;
            if (localMap == null) {
                synchronized (this) {
                    localMap = bankCountryCodeByBankCode;
                    if (localMap == null) {
                        localMap = loadBankCountryCodeByBankCode();
                        bankCountryCodeByBankCode = localMap;
                    }
                }
            }
            return localMap;
        }

        private Map<String, String> loadBankCountryCodeByBankCode() {
            Map<String, String> result = new HashMap<>();
            ClassPathResource resource = new ClassPathResource(MAIN_BANK_RESOURCE_PATH);
            if (!resource.exists()) {
                log.warn("主行国家代码文件不存在，文件路径：{}", MAIN_BANK_RESOURCE_PATH);
                return result;
            }
            try (InputStream inputStream = resource.getInputStream();
                 Workbook workbook = WorkbookFactory.create(inputStream)) {
                if (workbook.getNumberOfSheets() <= 0) {
                    log.warn("主行国家代码文件没有工作表，文件路径：{}", MAIN_BANK_RESOURCE_PATH);
                    return result;
                }
                Sheet sheet = workbook.getSheetAt(0);
                Row headerRow = sheet.getRow(0);
                if (headerRow == null) {
                    log.warn("主行国家代码文件没有表头，文件路径：{}", MAIN_BANK_RESOURCE_PATH);
                    return result;
                }
                DataFormatter formatter = new DataFormatter();
                int bankCodeIndex = findHeaderIndex(headerRow, formatter, MAIN_BANK_CODE_HEADER);
                int countryCodeIndex = findHeaderIndex(headerRow, formatter, MAIN_BANK_COUNTRY_CODE_HEADER);
                if (bankCodeIndex < 0 || countryCodeIndex < 0) {
                    log.warn("主行国家代码文件缺少必要表头，bank_code列：{}，country_code列：{}，文件路径：{}",
                            bankCodeIndex, countryCodeIndex, MAIN_BANK_RESOURCE_PATH);
                    return result;
                }
                for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                    Row row = sheet.getRow(rowIndex);
                    if (row == null) {
                        continue;
                    }
                    String bankCode = formatCell(row.getCell(bankCodeIndex), formatter);
                    String countryCode = formatCell(row.getCell(countryCodeIndex), formatter);
                    if (isBlank(bankCode) || isBlank(countryCode)) {
                        continue;
                    }
                    result.put(bankCode, countryCode);
                }
                log.info("已加载主行国家代码{}条，文件路径：{}", result.size(), MAIN_BANK_RESOURCE_PATH);
            } catch (Exception e) {
                log.error("加载主行国家代码文件失败，文件路径：{}", MAIN_BANK_RESOURCE_PATH, e);
            }
            return result;
        }

        private int findHeaderIndex(Row headerRow, DataFormatter formatter, String headerName) {
            if (headerRow == null) {
                return -1;
            }
            int lastCellNum = headerRow.getLastCellNum();
            for (int cellIndex = 0; cellIndex < lastCellNum; cellIndex++) {
                String value = formatCell(headerRow.getCell(cellIndex), formatter);
                if (headerName.equals(value)) {
                    return cellIndex;
                }
            }
            return -1;
        }

        private String formatCell(Cell cell, DataFormatter formatter) {
            if (cell == null) {
                return null;
            }
            String value = formatter.formatCellValue(cell);
            return value == null ? null : value.trim();
        }

        private Map<String, String> getCnapsNameByLocationCode() {
            Map<String, String> localMap = cnapsNameByLocationCode;
            if (localMap == null) {
                synchronized (this) {
                    localMap = cnapsNameByLocationCode;
                    if (localMap == null) {
                        localMap = loadCnapsNameByLocationCode();
                        cnapsNameByLocationCode = localMap;
                    }
                }
            }
            return localMap;
        }

        @SuppressWarnings("unchecked")
        private Map<String, String> loadCnapsNameByLocationCode() {
            List<BankBranchRes> cnapsList = (List<BankBranchRes>) getAllMasterData(MasterDataTypeEnum.CNAPS.getCode());
            Map<String, String> result = new HashMap<>();
            if (cnapsList == null || cnapsList.isEmpty()) {
                return result;
            }
            for (BankBranchRes bankBranchRes : cnapsList) {
                if (bankBranchRes == null || isBlank(bankBranchRes.getBankLocationCode())
                        || isBlank(bankBranchRes.getBankLocationName())) {
                    continue;
                }
                result.put(bankBranchRes.getBankLocationCode(), bankBranchRes.getBankLocationName());
            }
            return result;
        }
    }

    private String normalizeAccount(String account) {
        if (account == null || account.trim().isEmpty()) {
            return null;
        }
        return account.trim();
    }

    /**
     * 比较供应商与交易方
     * @return
     */
    public Map<String,Object> compareVendor(QueryAllVendorResponse vendorAll,List<VenderRes> allVendorList){
        return compareVendor(vendorAll, allVendorList, createBankLookupContext());
    }

    /**
     * 比较供应商与交易方
     * @param vendorAll 智书交易方
     * @param allVendorList 业财供应商
     * @return
     */
    private Map<String,Object> compareVendor(QueryAllVendorResponse vendorAll,List<VenderRes> allVendorList,
                                             BankLookupContext bankLookupContext){
        Map<String,Object> resultMap = new HashMap<>();
        List<CreateVendorRequest> createVendorRequestList = new ArrayList<>();
        CreateVendorRequest createVendorRequest = null;
        List<QueryAllVendorResponse.Item> items = vendorAll == null || vendorAll.getItems() == null
                ? Collections.emptyList() : vendorAll.getItems();
        if (allVendorList == null) {
            allVendorList = Collections.emptyList();
        }
        if (bankLookupContext == null) {
            bankLookupContext = new BankLookupContext(Collections.emptyList());
        }
        List<CreateVendorRequest> updateList = new ArrayList<>();
        log.info("获取到 {} 条供应商信息",allVendorList.size());
        for (VenderRes venderRes : allVendorList) {
            boolean isExist = false;
            String venderCode = venderRes.getVenderCode();//供应商编码<--->交易方编码
            String customerCode = venderRes.getCustomerCode();//客户编码
            String vendorType = "2";
            if(customerCode!=null&&!customerCode.isEmpty()){//如果客户编码存在，则拼接交易方编码“供应商编码+;+客户编码”
                venderCode = venderCode + ";" + customerCode;
                vendorType = "1,2";
            }
//            String taxpayerNumber = venderRes.getTaxpayerNumber();//证件号码
//            if(taxpayerNumber==null||taxpayerNumber.isEmpty()){//TODO 现在只处理证件号为空的
////                    log.info("{}：证件id为空，跳过",venderCode);
//                continue;
//            }
            for (QueryAllVendorResponse.Item item : items) {
                String vendor = item.getVendor();//交易方编码<--->供应商编码
                if(Objects.equals(vendor, venderCode)||Objects.equals(vendor, venderRes.getVenderCode())||Objects.equals(vendor, venderRes.getCustomerCode())){//比较证件号码判断是否为同一个交易方
                    log.info("更新：{}", vendor);
                    isExist = true;
                    //TODO 此处需要比对信息看数据是否需要修改
                    CreateVendorRequest updateRequest = buildVendorUpdateRequest(item, venderRes);
                    if (updateRequest != null) {
//                        String vendorType = item.getVendorType();
                        updateRequest.setVendorType(vendorType);//供应商
                        //处理一下重名的交易方编码 后续可注释掉
//                        vendor = makeVendorCode(vendor);
//                        updateRequest.setVendor(vendor);
//                        if(!vendorType.contains("2")){//如果交易方类型不为供应商，则修改交易方编码
//                            //交易方编码拼接规则 "供应商编码"+";"+"客户编码"
//                            if(!vendor.contains(venderCode)){
//                                String newVendor = venderCode+";"+vendor;
//                                updateRequest.setVendor(newVendor);
//                                updateRequest.setVendorType("1,2");//客户,供应商
//                            }
//                        }
                        updateRequest.setVendor(venderCode);
                        updateRequest.setStatus(1);//状态
                        String companyBusinessType = venderRes.getCompanyBusinessType();
                        boolean isCompany = "C;G".contains(companyBusinessType);//C-企业；G-机关事业单位；H-个体工商户；I-个人
                        boolean isChina = "CN".equals(updateRequest.getAdCountry());
                        updateRequest.setVendorNature(isCompany ? "0" : "1");//0-企业，1-自然人
                        updateRequest.setCertificationType(isCompany
                                ? (isChina ? "0" : "3")//企业：0-社会统一信用代码，3-税号(海外)
                                : (isChina ? "1" : "6"));//自然人：1-中国大陆居民身份证，6-护照
                        updateRequest.setId(item.getId());

                        //经营地址
                        if(venderRes.getDetailedAddress() != null&& !venderRes.getDetailedAddress().isEmpty()){
                            List<CreateVendorRequest.VendorAddress> vendorAddressList = updateRequest.getVendorAddresses();
                            if(vendorAddressList == null){
                                vendorAddressList = new ArrayList<>();
                            }
                            if(!vendorAddressList.isEmpty()){
                                CreateVendorRequest.VendorAddress vendorAddress = vendorAddressList.get(0);//经营地址最多只会有一条
                                vendorAddress.setCountry(venderRes.getPkCountry());
                                vendorAddress.setAddress(venderRes.getDetailedAddress());
                            }else{
                                CreateVendorRequest.VendorAddress vendorAddress = new CreateVendorRequest.VendorAddress();
                                vendorAddress.setCountry(venderRes.getPkCountry());
                                vendorAddress.setAddress(venderRes.getDetailedAddress());
                                vendorAddressList.add(vendorAddress);
                            }
                        }

                        List<CreateVendorRequest.ExtendInfo> extendInfoList = updateRequest.getExtendInfo();
                        CreateVendorRequest.ExtendInfo extendInfo = new CreateVendorRequest.ExtendInfo();
                        extendInfo.setFieldCode("VBI00102003");//是否黑名单
                        extendInfo.setFieldType(3);
                        extendInfo.setFieldValue(YesOrNoEnum.getNameByCode(venderRes.getBlacklist()));
                        extendInfoList.add(extendInfo);

                        //联系人
                        List<CreateVendorRequest.VendorContact> vendorContactList = new ArrayList<>();
                        List<VenderRes.VendorLine> vendorLines = venderRes.getContactList();
                        if(vendorLines!=null){
                            for(VenderRes.VendorLine vendorLine:vendorLines){
                                String contactName = vendorLine.getContactName();
                                String mobilePhone = vendorLine.getMobilePhone();
                                String email = vendorLine.getEmail();
                                CreateVendorRequest.VendorContact vendorContact = new CreateVendorRequest.VendorContact();
                                vendorContact.setName(StrUtils.substringByLength(contactName,0,40));
                                vendorContact.setPhone(StrUtils.substringByLength(mobilePhone,0,20));
                                vendorContact.setEmail(email);
                                vendorContactList.add(vendorContact);
                            }
                        }
                        updateRequest.setVendorContacts(vendorContactList);

                        //银行账户
                        //TODO 获取当前供应商的银行账户信息
                        List<VenderAccountRes> list = (List<VenderAccountRes>) getAllAccountData(MasterDataTypeEnum.VENDER_ACCOUNT.getCode(),venderCode);
//                        List<BankRes> allBankResList = (List<BankRes>) getAllMasterData(MasterDataTypeEnum.BANK.getCode());
                        List<CreateVendorRequest.VendorAccount> vendorAccountList = updateRequest.getVendorAccounts();
                        if(vendorAccountList==null){
                            vendorAccountList = new ArrayList<>();
                        }
                        syncVendorAccountList(list, vendorAccountList, bankLookupContext);
                        updateRequest.setVendorAccounts(vendorAccountList);
                        updateList.add(updateRequest);
                    }
                    break;
                }
            }
            if(!isExist){
                log.info("创建：{}", venderCode);
                createVendorRequest = new CreateVendorRequest();
                createVendorRequest.setVendor(venderCode);//交易方编码
                createVendorRequest.setAdCountry(venderRes.getPkCountry());//交易方注册国家
                createVendorRequest.setStatus(1);//状态
                createVendorRequest.setVendorText(venderRes.getDescription());//交易方名称
                String companyBusinessType = venderRes.getCompanyBusinessType();

                boolean isCompany = "C;G".contains(companyBusinessType);//C-企业；G-机关事业单位；H-个体工商户；I-个人
                boolean isChina = "CN".equals(createVendorRequest.getAdCountry());
                createVendorRequest.setVendorNature(isCompany ? "0" : "1");//0-企业，1-自然人
                createVendorRequest.setCertificationType(isCompany
                        ? (isChina ? "0" : "3")//企业：0-社会统一信用代码，3-税号(海外)
                        : (isChina ? "1" : "6"));//自然人：1-中国大陆居民身份证，6-护照

                createVendorRequest.setCertificationId(venderRes.getTaxpayerNumber());//证件ID
                createVendorRequest.setVendorType(vendorType);//供应商

                //经营地址
                if(venderRes.getDetailedAddress() != null&& !venderRes.getDetailedAddress().isEmpty()){
                    List<CreateVendorRequest.VendorAddress> vendorAddressList = new ArrayList<>();
                    CreateVendorRequest.VendorAddress vendorAddress = new CreateVendorRequest.VendorAddress();
                    vendorAddress.setCountry(venderRes.getPkCountry());
                    vendorAddress.setAddress(venderRes.getDetailedAddress());
                    vendorAddressList.add(vendorAddress);
                    createVendorRequest.setVendorAddresses(vendorAddressList);
                }
                //外部供应商为否 --反贿赂协议
                String pkCustclass = venderRes.getPkCustclass();
                List<CreateVendorRequest.ExtendInfo> extendInfoList = new ArrayList<>();
                CreateVendorRequest.ExtendInfo extendInfo = null;

                if(!"2".equals(pkCustclass)){//2为外部供应商
                    extendInfo = new CreateVendorRequest.ExtendInfo();
                    extendInfo.setFieldCode("VBI00102002");//是否外部供应商
                    extendInfo.setFieldType(3);
                    extendInfo.setFieldValue("否");
                    extendInfoList.add(extendInfo);
                }else{
                    extendInfo = new CreateVendorRequest.ExtendInfo();
                    extendInfo.setFieldCode("VBI00102002");//是否外部供应商
                    extendInfo.setFieldType(3);
                    extendInfo.setFieldValue("是");
                    extendInfoList.add(extendInfo);
                    extendInfo = new CreateVendorRequest.ExtendInfo();
                    extendInfo.setFieldCode("VBI00100001");//是否已签署反贿赂协议
                    extendInfo.setFieldType(3);
                    extendInfo.setFieldValue("否");
                    extendInfoList.add(extendInfo);
                }

                extendInfo = new CreateVendorRequest.ExtendInfo();
                extendInfo.setFieldCode("VBI00102003");//是否黑名单
                extendInfo.setFieldType(3);
                extendInfo.setFieldValue(YesOrNoEnum.getNameByCode(venderRes.getBlacklist()));
                extendInfoList.add(extendInfo);

                createVendorRequest.setExtendInfo(extendInfoList);

                //联系人
                List<CreateVendorRequest.VendorContact> vendorContactList = new ArrayList<>();
                List<VenderRes.VendorLine> vendorLines = venderRes.getContactList();
                if(vendorLines!=null){
                    for(VenderRes.VendorLine vendorLine:vendorLines){
                        String contactName = vendorLine.getContactName();
                        String mobilePhone = vendorLine.getMobilePhone();
                        String email = vendorLine.getEmail();
                        CreateVendorRequest.VendorContact vendorContact = new CreateVendorRequest.VendorContact();
                        vendorContact.setName(StrUtils.substringByLength(contactName,0,40));
                        vendorContact.setPhone(StrUtils.substringByLength(mobilePhone,0,20));
                        vendorContact.setEmail(email);
                        vendorContactList.add(vendorContact);
                    }
                }
                createVendorRequest.setVendorContacts(vendorContactList);
                //银行账户
                //TODO 获取当前供应商的银行账户信息
                List<VenderAccountRes> list = (List<VenderAccountRes>) getAllAccountData(MasterDataTypeEnum.VENDER_ACCOUNT.getCode(),venderCode);

                BankRes bankRes = new BankRes();
                List<CreateVendorRequest.VendorAccount> vendorAccountList = new ArrayList<>();
                for (VenderAccountRes accountRes : list) {
                    String bankCode = accountRes.getBankCode();
                    bankRes = bankLookupContext.findBank(bankCode);
                    String bankName = bankLookupContext.resolveBankName(bankCode, accountRes.getBankLocationCode());
                    if (isBlank(bankRes == null ? null : bankRes.getBankName()) && !isBlank(bankName)) {
                        BankRes fallbackBankRes = new BankRes();
                        fallbackBankRes.setCountryCode(bankRes == null ? null : bankRes.getCountryCode());
                        fallbackBankRes.setBankName(bankName);
                        bankRes = fallbackBankRes;
                    }
                    if (bankRes == null) {
                        bankRes = new BankRes();
                    }
                    String countryCode = bankLookupContext.resolveBankCountryCode(bankCode);
                    CreateVendorRequest.VendorAccount vendorAccount = new CreateVendorRequest.VendorAccount();

                    vendorAccount.setCountry(countryCode);//银行国家
                    vendorAccount.setBankName(StrUtils.substringByLength(bankRes.getBankName(),0,100));//银行名称
                    vendorAccount.setAccountName(accountRes.getBankAccountName());//账户名
                    vendorAccount.setAccount(StrUtils.removeAllSpaces(accountRes.getBankAccountNumber()));//账号
                    vendorAccount.setBankCode(accountRes.getBankLocationCode());//联行号
                    vendorAccountList.add(vendorAccount);
                }
                createVendorRequest.setVendorAccounts(vendorAccountList);
                createVendorRequestList.add(createVendorRequest);
            }
        }
        resultMap.put(ADD_LIST_KEY,createVendorRequestList);
        resultMap.put(UPDATE_LIST_KEY,updateList);
        return resultMap;
    }

    /**
     * 比较客户与交易方
     * @return
     */
    public Map<String,Object> compareCustomer(QueryAllVendorResponse vendorAll,List<CustomerRes> allCustomerList){
        return compareCustomer(vendorAll, allCustomerList, createBankLookupContext());
    }

    /**
     * 比较客户与交易方
     * @param vendorAll 智书交易方信息
     * @param allCustomerList 业财客户信息
     * @return
     */
    private Map<String,Object> compareCustomer(QueryAllVendorResponse vendorAll,
                                               List<CustomerRes> allCustomerList,
                                               BankLookupContext bankLookupContext){
        Map<String,Object> resultMap = new HashMap<>();
        List<CreateVendorRequest> createVendorRequestList = new ArrayList<>();
        CreateVendorRequest createVendorRequest = null;
        List<QueryAllVendorResponse.Item> items = vendorAll == null || vendorAll.getItems() == null
                ? Collections.emptyList() : vendorAll.getItems();
        if (allCustomerList == null) {
            allCustomerList = Collections.emptyList();
        }
        if (bankLookupContext == null) {
            bankLookupContext = new BankLookupContext(Collections.emptyList());
        }
        List<CreateVendorRequest> updateList = new ArrayList<>();
        log.info("获取到 {} 条客户信息", allCustomerList.size());
        for (CustomerRes customerRes : allCustomerList) {
            boolean isExist = false;
            String vendorCode = customerRes.getCustomerCode();//客户编码<--->交易方编码
            String venderCode = customerRes.getVenderCode();//供应商编码
            String vendorType = "1";
            if(venderCode!=null&&!venderCode.isEmpty()){//如果供应商编码存在，则拼接交易方编码“供应商编码+;+客户编码”
                vendorCode = venderCode + ";" + vendorCode;
                vendorType = "1,2";
            }
//            String taxpayerNumber = customerRes.getTaxpayerNumber();//证件号码
//            if(taxpayerNumber==null||taxpayerNumber.isEmpty()){//TODO 现在只处理证件号为空的
////                    log.info("{}：证件id为空，跳过",venderCode);
//                continue;
//            }
            for (QueryAllVendorResponse.Item item : items) {
                String vendor = item.getVendor();//交易方编码<--->客户编码
                if(Objects.equals(vendor, vendorCode)||Objects.equals(vendor, customerRes.getCustomerCode())||Objects.equals(vendor, customerRes.getVenderCode())){
                    isExist = true;
                    //TODO 此处需要比对信息看数据是否需要修改
                    CreateVendorRequest updateRequest = buildCustomerUpdateRequest(item, customerRes);
                    if (updateRequest != null) {
//                        String vendorType = item.getVendorType();
                        updateRequest.setVendorType(vendorType);//客户
//                        if("C".equals(companyBusinessType)){
//                            updateRequest.setVendorNature("0");//交易方性质 企业 0
//                        }else{
//                            updateRequest.setVendorNature("1");//交易方性质 自然人 1
//                        }
                        //处理一下重名的交易方编码 后续可注释掉
//                        updateRequest.setVendor(vendor);
//                        if(!vendorType.contains("1")){//如果交易方类型不为客户，则修改交易方编码
//                            //交易方编码拼接规则 "供应商编码"+";"+"客户编码"
//                            if(!vendor.contains(vendorCode)){
//                                String newVendor = vendor+";"+vendorCode;
//                                updateRequest.setVendor(newVendor);
//                                updateRequest.setVendorType("1,2");//客户,供应商
////                                updateRequest.setVendorNature("0");//如果不为客户，则传企业
//                            }
//                        }

                        updateRequest.setStatus(1);
                        updateRequest.setVendor(vendorCode);
//                        if("CN".equals(updateRequest.getAdCountry())){
//                            updateRequest.setCertificationType("0");
//                        }else{
//                            updateRequest.setCertificationType("6");//非中国的传：6-护照
//                        }
                        String companyBusinessType = customerRes.getCompanyBusinessType();
//                        boolean isCompany = "C".equals(companyBusinessType);
                        boolean isCompany = "C;G".contains(companyBusinessType);//C-企业；G-机关事业单位；H-个体工商户；I-个人
                        boolean isChina = "CN".equals(updateRequest.getAdCountry());
                        updateRequest.setVendorNature(isCompany ? "0" : "1");//0-企业，1-自然人
                        updateRequest.setCertificationType(isCompany
                                ? (isChina ? "0" : "3")//企业：0-社会统一信用代码，3-税号(海外)
                                : (isChina ? "1" : "6"));//自然人：1-中国大陆居民身份证，6-护照

                        updateRequest.setId(item.getId());

                        //联系人
                        List<CreateVendorRequest.VendorContact> vendorContactList = new ArrayList<>();
                        List<CustomerRes.CustomerLine> contactList = customerRes.getContactList();
                        if(contactList!=null){
                            for(CustomerRes.CustomerLine customerLine:contactList){
                                String contactName = customerLine.getContactName();
                                String mobilePhone = customerLine.getMobilePhone();
                                String email = customerLine.getEmail();
                                CreateVendorRequest.VendorContact vendorContact = new CreateVendorRequest.VendorContact();
                                vendorContact.setName(StrUtils.substringByLength(contactName,0,40));
                                vendorContact.setPhone(StrUtils.substringByLength(mobilePhone,0,20));
                                vendorContact.setEmail(email);
                                vendorContactList.add(vendorContact);
                            }
                        }
                        updateRequest.setVendorContacts(vendorContactList);

                        //经营地址
                        if(customerRes.getPaperReceiverAddress() != null&& !customerRes.getPaperReceiverAddress().isEmpty()){
                            List<CreateVendorRequest.VendorAddress> vendorAddressList = new ArrayList<>();
//                            if(vendorAddressList == null){
//                                vendorAddressList = new ArrayList<>();
//                            }
                            CreateVendorRequest.VendorAddress vendorAddress = new CreateVendorRequest.VendorAddress();
                            vendorAddress.setCountry(customerRes.getPkCountry());
                            vendorAddress.setAddress(customerRes.getPaperReceiverAddress());
                            vendorAddressList.add(vendorAddress);
                            updateRequest.setVendorAddresses(vendorAddressList);
                        }
                        //银行账户
                        //TODO 获取当前客户的银行账户信息
                        List<CustomerAccountRes> list = (List<CustomerAccountRes>) getAllAccountData(MasterDataTypeEnum.CUSTOMER_ACCOUNT.getCode(),vendorCode);
                        List<CreateVendorRequest.VendorAccount> vendorAccountList = updateRequest.getVendorAccounts();
                        if(vendorAccountList==null){
                            vendorAccountList = new ArrayList<>();
                        }
                        syncCustomerAccountList(list, vendorAccountList, bankLookupContext);
                        updateRequest.setVendorAccounts(vendorAccountList);

                        updateList.add(updateRequest);
                    }
                    break;
                }
            }
            if(!isExist){
                createVendorRequest = new CreateVendorRequest();
                createVendorRequest.setVendor(vendorCode);//交易方编码
                createVendorRequest.setAdCountry(customerRes.getPkCountry()==null?"CN":customerRes.getPkCountry());//交易方注册国家
                createVendorRequest.setStatus(1);//状态
                createVendorRequest.setVendorText(customerRes.getDescription());//交易方名称
//                createVendorRequest.setVendorNature("1");//个人同步为自然人
                String companyBusinessType = customerRes.getCompanyBusinessType();
//                if("C".equals(companyBusinessType)){
//                    createVendorRequest.setVendorNature("0");//交易方性质 企业 0
//                }else{
//                    createVendorRequest.setVendorNature("1");//交易方性质 自然人 1
//                }
//                if("CN".equals(customerRes.getPkCountry())){
//                    createVendorRequest.setCertificationType("0");//目前下拉框中只有一个选项：0-社会统一信用代码
//                }else{
//                    createVendorRequest.setCertificationType("6");//非中国的传：6-护照
//                }

                boolean isCompany = "C;G".contains(companyBusinessType);//C-企业；G-机关事业单位；H-个体工商户；I-个人
                boolean isChina = "CN".equals(createVendorRequest.getAdCountry());
                createVendorRequest.setVendorNature(isCompany ? "0" : "1");//0-企业，1-自然人
                createVendorRequest.setCertificationType(isCompany
                        ? (isChina ? "0" : "3")//企业：0-社会统一信用代码，3-税号(海外)
                        : (isChina ? "1" : "6"));//自然人：1-中国大陆居民身份证，6-护照
                createVendorRequest.setCertificationId(customerRes.getTaxpayerNumber());//证件ID
                createVendorRequest.setVendorType(vendorType);

                //经营地址
                if(customerRes.getPaperReceiverAddress() != null&& !customerRes.getPaperReceiverAddress().isEmpty()){
                    List<CreateVendorRequest.VendorAddress> vendorAddressList = new ArrayList<>();
                    CreateVendorRequest.VendorAddress vendorAddress = new CreateVendorRequest.VendorAddress();
                    vendorAddress.setCountry(customerRes.getPkCountry());
                    vendorAddress.setAddress(customerRes.getPaperReceiverAddress());
                    vendorAddressList.add(vendorAddress);
                    createVendorRequest.setVendorAddresses(vendorAddressList);
                }

                //外部供应商为否 --反贿赂协议
                String pkCustclass = customerRes.getPkCustclass();
                List<CreateVendorRequest.ExtendInfo> extendInfoList = new ArrayList<>();
                CreateVendorRequest.ExtendInfo extendInfo = null;

                if(!"2".equals(pkCustclass)){//2为外部供应商
                    extendInfo = new CreateVendorRequest.ExtendInfo();
                    extendInfo.setFieldCode("VBI00102002");//是否外部供应商
                    extendInfo.setFieldType(3);
                    extendInfo.setFieldValue("否");
                    extendInfoList.add(extendInfo);
                }else{
                    extendInfo = new CreateVendorRequest.ExtendInfo();
                    extendInfo.setFieldCode("VBI00102002");//是否外部供应商
                    extendInfo.setFieldType(3);
                    extendInfo.setFieldValue("是");
                    extendInfoList.add(extendInfo);
                    extendInfo = new CreateVendorRequest.ExtendInfo();
                    extendInfo.setFieldCode("VBI00100001");//是否已签署反贿赂协议
                    extendInfo.setFieldType(3);
                    extendInfo.setFieldValue("否");
                    extendInfoList.add(extendInfo);
                }
                createVendorRequest.setExtendInfo(extendInfoList);


                //联系人
                List<CreateVendorRequest.VendorContact> vendorContactList = new ArrayList<>();
                List<CustomerRes.CustomerLine> contactList = customerRes.getContactList();
                if(contactList!=null){
                    for(CustomerRes.CustomerLine customerLine:contactList){
                        String contactName = customerLine.getContactName();
                        String mobilePhone = customerLine.getMobilePhone();
                        String email = customerLine.getEmail();
                        CreateVendorRequest.VendorContact vendorContact = new CreateVendorRequest.VendorContact();
                        vendorContact.setName(StrUtils.substringByLength(contactName,0,40));
                        vendorContact.setPhone(StrUtils.substringByLength(mobilePhone,0,20));
                        vendorContact.setEmail(email);
                        vendorContactList.add(vendorContact);
                    }
                }
                createVendorRequest.setVendorContacts(vendorContactList);

                //银行账户
                //TODO 获取当前客户的银行账户信息
                List<CustomerAccountRes> list = (List<CustomerAccountRes>) getAllAccountData(MasterDataTypeEnum.CUSTOMER_ACCOUNT.getCode(),vendorCode);
                BankRes bankRes = new BankRes();
                List<CreateVendorRequest.VendorAccount> vendorAccountList = new ArrayList<>();
                for (CustomerAccountRes customerAccountRes : list) {
                    String bankCode = customerAccountRes.getBankCode();
                    bankRes = bankLookupContext.findBank(bankCode);
                    String bankName = bankLookupContext.resolveBankName(bankCode, customerAccountRes.getBankLocationCode());
                    if (isBlank(bankRes == null ? null : bankRes.getBankName()) && !isBlank(bankName)) {
                        BankRes fallbackBankRes = new BankRes();
                        fallbackBankRes.setCountryCode(bankRes == null ? null : bankRes.getCountryCode());
                        fallbackBankRes.setBankName(bankName);
                        bankRes = fallbackBankRes;
                    }
                    if (bankRes == null) {
                        bankRes = new BankRes();
                    }
                    String countryCode = bankLookupContext.resolveBankCountryCode(bankCode);
                    CreateVendorRequest.VendorAccount vendorAccount = new CreateVendorRequest.VendorAccount();

                    vendorAccount.setCountry(countryCode);//银行国家
                    vendorAccount.setBankName(StrUtils.substringByLength(bankRes.getBankName(),0,100));//银行名称
                    vendorAccount.setAccountName(customerAccountRes.getBankAccountName());//账户名
                    vendorAccount.setAccount(StrUtils.removeAllSpaces(customerAccountRes.getBankAccountNumber()));//账号
                    vendorAccount.setBankCode(customerAccountRes.getBankLocationCode());//联行号
                    vendorAccountList.add(vendorAccount);
                    createVendorRequest.setVendorAccounts(vendorAccountList);

                }
                createVendorRequestList.add(createVendorRequest);
            }
        }
        resultMap.put(ADD_LIST_KEY,createVendorRequestList);
        resultMap.put(UPDATE_LIST_KEY,updateList);
        return resultMap;
    }

    @Override
    public Map<String, String> getEmployeeCodeAndUserIdMap() {
        return employeeCodeAndUserIdMap;
    }

    @Override
    public void refreshEmployeeCodeAndUserIdMap() {
        log.info("开始刷新员工编码与飞书用户ID缓存");
        try {
            List<EmployeeRes> employeeList = (List<EmployeeRes>) getAllMasterData(MasterDataTypeEnum.EMPLOYEE.getCode());
            Map<String, String> employeeMap = buildEmployeeCodeAndUserIdMap(employeeList);
            employeeCodeAndUserIdMap = Collections.unmodifiableMap(employeeMap);
            log.info("员工编码与飞书用户ID缓存刷新成功，缓存数量：{}", employeeCodeAndUserIdMap.size());
        } catch (Exception e) {
            log.error("员工编码与飞书用户ID缓存刷新失败，继续使用上一次缓存数据", e);
        }
    }

    private Map<String, String> buildEmployeeCodeAndUserIdMap(List<EmployeeRes> employeeList) {
        Map<String, String> employeeMap = new HashMap<>();
        if (employeeList == null || employeeList.isEmpty()) {
            return employeeMap;
        }
        for (EmployeeRes employeeRes : employeeList) {
            if (employeeRes == null || isBlank(employeeRes.getEmployeeCode()) || isBlank(employeeRes.getFeishuEmployeeId())) {
                continue;
            }
            employeeMap.put(employeeRes.getEmployeeCode().trim(), employeeRes.getFeishuEmployeeId().trim());
        }
        return employeeMap;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private QueryAllVendorResponse getVendorAll(){
        QueryAllVendorResponse vendorAll = zhiShuVendorClient.getVendorAll(null, null);
        List<QueryAllVendorResponse.Item> items = vendorAll.getItems();
        boolean hasMore = vendorAll.isHasMore();
        String pageToken = vendorAll.getPageToken();
        while (hasMore){
            QueryAllVendorResponse vendorAllMore = zhiShuVendorClient.getVendorAll(pageToken, null);
            if(vendorAllMore!=null&&vendorAllMore.getItems()!=null){
                items.addAll(vendorAllMore.getItems());
                pageToken = vendorAllMore.getPageToken();
                hasMore = vendorAllMore.isHasMore();
            }else{
                hasMore = false;
            }
        }
        return vendorAll;
    }

    private static class VendorSyncResult {
        private int vendorCount;
        private int batchCount;
        private int batchFailCount;
        private int itemFailCount;
        private int addSuccessCount;
        private int addFailCount;
        private int updateSuccessCount;
        private int updateFailCount;
        private int antiBriberyFailCount;

        private void merge(VendorSyncResult other) {
            if (other == null) {
                return;
            }
            batchFailCount += other.batchFailCount;
            itemFailCount += other.itemFailCount;
            addSuccessCount += other.addSuccessCount;
            addFailCount += other.addFailCount;
            updateSuccessCount += other.updateSuccessCount;
            updateFailCount += other.updateFailCount;
            antiBriberyFailCount += other.antiBriberyFailCount;
        }
    }

    private static class CustomerSyncResult {
        private int customerCount;
        private int batchCount;
        private int batchFailCount;
        private int itemFailCount;
        private int addSuccessCount;
        private int addFailCount;
        private int updateSuccessCount;
        private int updateFailCount;
        private int antiBriberyFailCount;

        private void merge(CustomerSyncResult other) {
            if (other == null) {
                return;
            }
            batchFailCount += other.batchFailCount;
            itemFailCount += other.itemFailCount;
            addSuccessCount += other.addSuccessCount;
            addFailCount += other.addFailCount;
            updateSuccessCount += other.updateSuccessCount;
            updateFailCount += other.updateFailCount;
            antiBriberyFailCount += other.antiBriberyFailCount;
        }
    }

    private String makeVendorCode(String vendor){
        String[] vendorArr = vendor.split(";");
        Object[] array = Arrays.stream(vendorArr)
                .distinct()
                .toArray();
        StringBuilder str = new StringBuilder();
        for (Object o : array) {
            if (str.length() > 0) {
                str.append(";").append(o);
            }else{
                str.append(o);
            }
        }
//        System.out.println(str);
        return str.toString();
    }

}
