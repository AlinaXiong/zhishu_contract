package com.hero.middleware.service;

import com.hero.middleware.client.yuecai.response.DocumentListResponse;
import com.hero.middleware.client.zhishu.request.PrecedingDocRequest;
import com.hero.middleware.client.zhishu.response.ResultResponse;
import com.hero.middleware.dto.DocumentQueryDTO;

import java.util.Map;

public interface DocumentService {

    DocumentListResponse getDocumentList(DocumentQueryDTO dto);

    /**
     * 获取订单信息
     * @param request
     * @return
     */
    ResultResponse getOrderInfo(PrecedingDocRequest request);

    /**
     * 获取订单详情
     * @param paramMap
     * @return
     */
    Map<String,Object> getOrderDetail(Map<String,Object> paramMap);

    /**
     * 获取主播卡片信息
     * @param request
     * @return
     */
    ResultResponse getAnchorCardInfo(PrecedingDocRequest request);

    /**
     * 获取主播详情信息
     * @param paramMap
     * @return
     */
    Map<String,Object> getAnchorCardDetail(Map<String,Object> paramMap);
    /**
     * 获取采购申请信息
     * @param request
     * @return
     */
    ResultResponse getProcurementInfo(PrecedingDocRequest request);

    /**
     * 获取采购申请详情信息
     * @param paramMap
     * @return
     */
    Map<String,Object> getProcurementDetail(Map<String,Object> paramMap);

    Map<String,Object> getspecCategoryList(Map<String,Object> paramMap);
}
