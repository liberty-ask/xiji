package com.xiji.service;

import com.xiji.entity.dto.request.BillImportRequest;
import com.xiji.entity.dto.response.BillImportResult;
import com.xiji.parser.model.BillTransaction;

import java.util.List;

/**
 * 账单导入服务接口
 * @author liberty
 */
public interface BillImportService {
    
    /**
     * 导入账单数据为交易记录
     * @param userId 用户ID
     * @param familyId 家庭ID
     * @param transactions 交易记录列表（完整的解析结果，不仅仅是预览）
     * @param platform 平台来源（支付宝、微信、京东、招商银行等）
     * @param request 导入请求
     * @return 导入结果
     */
    BillImportResult importTransactions(Long userId, Long familyId, 
                                       List<BillTransaction> transactions, String platform, BillImportRequest request);
}

