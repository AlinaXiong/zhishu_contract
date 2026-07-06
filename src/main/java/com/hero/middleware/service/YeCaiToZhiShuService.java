package com.hero.middleware.service;

import com.hero.middleware.client.yuecai.response.masterdata.CustomerRes;
import com.hero.middleware.client.yuecai.response.masterdata.EmployeeRes;
import com.hero.middleware.client.yuecai.response.masterdata.VenderRes;
import com.hero.middleware.client.zhishu.response.QueryAllVendorResponse;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface YeCaiToZhiShuService {

    /**
     * 获取主数据信息
     * @param businessType
     * @return
     */
    Object getAllMasterData(String businessType);
    Object getAllMasterData(String businessType,String startTime,String endTime);
    Object getAllMasterData(String businessType, String startTime, String endTime, Integer page, Integer size);

    /**
     * 获取账号信息
     * @param businessType 查询账号类型
     * @param accountCode 查询用户编码
     * @return
     */
    Object getAllAccountData(String businessType,String accountCode);

    /**
     * 同步主数据信息
     * @param businessType
     */
    void synMasterData(String businessType);

    /**
     * 同步主数据信息
     * @param businessType 主数据类型
     * @param certificationId 供应商证件 ID
     */
    void synMasterData(String businessType, String certificationId);

    /**
     * 按时间范围同步主数据信息
     * @param businessType 主数据类型
     * @param startTime 起始时间
     * @param endTime 终止时间
     */
    void synMasterData(String businessType, String startTime, String endTime);

    /**
     * 按证件 ID 和时间范围同步主数据信息
     * @param businessType 主数据类型
     * @param certificationId 供应商证件 ID
     * @param startTime 起始时间
     * @param endTime 终止时间
     */
    void synMasterData(String businessType, String certificationId, String startTime, String endTime);

    /**
     * 按证件 ID、时间范围和指定分页同步主数据信息
     * @param businessType 主数据类型
     * @param certificationId 供应商证件 ID
     * @param startTime 起始时间
     * @param endTime 终止时间
     * @param page 查询页码，从 0 开始
     * @param size 每页数量
     */
    void synMasterData(String businessType, String certificationId, String startTime, String endTime, Integer page, Integer size);

    /**
     * 按智书交易方编码逐条查询后同步业财主数据
     */
    void synMasterDataByVendorCode(String businessType, String certificationId, String startTime, String endTime, Integer page, Integer size);

    void synMasterDataByBusinessCodes(String businessType, Collection<String> businessCodes);

    /**
     * 比较供应商差异
     * @param vendorAll 所有交易方信息
     * @param allVendorList 所有供应商信息
     * @return
     */
    Map<String,Object> compareVendor(QueryAllVendorResponse vendorAll,List<VenderRes> allVendorList);

    /**
     * 比较客户差异
     * @param vendorAll 所有交易方信息
     * @param allCustomerList 所有客户信息
     * @return
     */
    Map<String,Object> compareCustomer(QueryAllVendorResponse vendorAll, List<CustomerRes> allCustomerList);

    Map<String,String> getEmployeeCodeAndUserIdMap();

    void refreshEmployeeCodeAndUserIdMap();
}
