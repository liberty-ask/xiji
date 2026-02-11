package com.xiji.service.impl;

import com.xiji.entity.domain.Category;
import com.xiji.entity.domain.Transactions;
import com.xiji.entity.dto.request.BillImportRequest;
import com.xiji.entity.dto.response.BillImportResult;
import com.xiji.entity.dto.response.BillCategoryAiResult;
import com.xiji.parser.model.BillTransaction;
import com.xiji.service.BillImportService;
import com.xiji.service.BillCategoryAiService;
import com.xiji.service.CategoryService;
import com.xiji.service.TransactionsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 账单导入服务实现类
 * @author liberty
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BillImportServiceImpl implements BillImportService {
    
    private final TransactionsService transactionsService;
    private final CategoryService categoryService;
    private final BillCategoryAiService billCategoryAiService;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BillImportResult importTransactions(Long userId, Long familyId, 
                                               List<BillTransaction> transactions, String platform, BillImportRequest request) {
        BillImportResult result = new BillImportResult();
        result.setErrors(new ArrayList<>());
        result.setTransactionIds(new ArrayList<>());
        
        int totalCount = transactions.size();
        int successCount = 0;
        int skipCount = 0;
        int failCount = 0;
        
        // 获取所有分类（按类型分组）
        Map<Integer, List<Category>> categoryMap = new HashMap<>();
        List<Category> allCategories = categoryService.getEnabledCategoriesByFamily(familyId, null);
        List<Category> incomeCategories = allCategories.stream().filter(c -> c.getType() == 0).collect(Collectors.toList());
        List<Category> expenseCategories = allCategories.stream().filter(c -> c.getType() == 1).collect(Collectors.toList());
        categoryMap.put(0, incomeCategories);
        categoryMap.put(1, expenseCategories);
        
        // 构建分类名称到ID的映射
        Map<String, Long> categoryNameToIdMap = new HashMap<>();
        for (Category category : allCategories) {
            categoryNameToIdMap.put(category.getName(), category.getId());
        }
        
        // 识别需要AI归类的交易（分类为空或不匹配）
        List<BillTransaction> transactionsNeedAi = new ArrayList<>();
        for (BillTransaction t : transactions) {
            String categoryName = t.getCategory();
            
            // 需要AI归类的情况：
            // 1. 分类为空
            // 2. 分类名称在系统中不存在
            boolean needAi = (categoryName == null || categoryName.isEmpty()) ||
                    !categoryNameToIdMap.containsKey(categoryName);
            
            if (needAi) {
                transactionsNeedAi.add(t);
            }
        }
        
        // 如果有需要AI归类的交易，批量调用AI服务
        if (!transactionsNeedAi.isEmpty() && request.getAutoMatchCategory() != null && request.getAutoMatchCategory()) {
            try {
                log.info("开始AI归类，需要归类的交易数量：{}", transactionsNeedAi.size());
                List<BillCategoryAiResult> aiResults = billCategoryAiService.categorizeTransactions(
                        transactionsNeedAi, incomeCategories, expenseCategories);
                
                // 更新交易的分类信息
                for (BillCategoryAiResult aiResult : aiResults) {
                    int index = aiResult.getIndex();
                    if (index >= 0 && index < transactionsNeedAi.size()) {
                        BillTransaction transaction = transactionsNeedAi.get(index);
                        if (aiResult.getCategory() != null && !aiResult.getCategory().isEmpty()) {
                            // 使用AI返回的分类
                            String originalCategory = transaction.getCategory();
                            transaction.setCategory(aiResult.getCategory());
                            log.debug("AI归类成功，索引：{}，原始分类：{}，AI分类：{}，置信度：{}",
                                    index, originalCategory, aiResult.getCategory(), aiResult.getConfidence());
                        }
                    }
                }
                log.info("AI归类完成，成功归类：{}条", aiResults.size());
            } catch (Exception e) {
                log.error("AI归类失败，将使用默认分类", e);
                // AI归类失败不影响导入，继续使用原有逻辑
            }
        }
        
        // 获取已存在的交易（交易单号 + 类型）组合（用于去重）
        Set<String> existingTradeNoTypePairs = new HashSet<>();
        if (request.getSkipDuplicates() != null && request.getSkipDuplicates()) {
            // 从账单交易中提取所有非空的交易单号
            Set<String> tradeNosToCheck = transactions.stream()
                .map(BillTransaction::getTradeNo)
                .filter(tradeNo -> tradeNo != null && !tradeNo.isEmpty())
                .collect(Collectors.toSet());
            
            if (!tradeNosToCheck.isEmpty()) {
                // 查询该家庭中已存在的交易
                List<Transactions> existingTransactions = transactionsService.list(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Transactions>()
                        .eq(Transactions::getFamilyId, familyId)
                        .in(Transactions::getTradeNo, tradeNosToCheck)
                        .isNotNull(Transactions::getTradeNo)
                );
                
                existingTradeNoTypePairs = existingTransactions.stream()
                    .filter(t -> t.getTradeNo() != null && !t.getTradeNo().isEmpty() && t.getType() != null)
                    .map(t -> t.getTradeNo() + "_" + t.getType())
                    .collect(Collectors.toSet());
                
                log.info("查询到已存在的交易（单号+类型）组合数量：{}，待检查单号数量：{}", 
                    existingTradeNoTypePairs.size(), tradeNosToCheck.size());
            }
        }
        
        // 转换交易记录
        List<Transactions> transactionsToSave = new ArrayList<>();
        
        for (BillTransaction billTransaction : transactions) {
            try {
                // 检查是否为退款记录
                boolean isRefund = billTransaction.getDescription() != null && 
                    (billTransaction.getDescription().contains("退款") || 
                     billTransaction.getStatus() != null && billTransaction.getStatus().contains("退款"));
                
                // 如果是退款记录，无论解析器设置的类型是什么，都设置为收入类型
                if (isRefund) {
                    billTransaction.setType(0); // 退款作为收入类型
                    log.debug("识别为退款交易，设置为收入类型，tradeNo：{}", billTransaction.getTradeNo());
                }
                
                // 去重检查（基于交易单号+类型组合）
                if (request.getSkipDuplicates() != null && request.getSkipDuplicates()) {
                    if (billTransaction.getTradeNo() != null && !billTransaction.getTradeNo().isEmpty() && billTransaction.getType() != null) {
                        String tradeNoTypePair = billTransaction.getTradeNo() + "_" + billTransaction.getType();
                        // 检查是否已存在（数据库或本次导入中）
                        if (existingTradeNoTypePairs.contains(tradeNoTypePair)) {
                            // 交易单号和类型组合已存在，跳过
                            skipCount++;
                            addImportError(result, billTransaction.getTradeNo(), "交易单号和类型组合重复，需要写入的记录已跳过", billTransaction.getDescription());
                            log.debug("跳过重复交易，tradeNo：{}，type：{}", billTransaction.getTradeNo(), billTransaction.getType());
                            continue;
                        } else {
                            // 添加到已处理集合，避免本次导入中重复
                            existingTradeNoTypePairs.add(tradeNoTypePair);
                        }
                    }
                }
                
                // 匹配分类
                String categoryName = billTransaction.getCategory();
                if (categoryName == null || categoryName.isEmpty()) {
                    categoryName = "其他";
                }
                
                Long categoryId = categoryNameToIdMap.get(categoryName);
                if (categoryId == null) {
                    // 如果分类不存在，使用"其他"分类
                    List<Category> categories;
                    if (billTransaction.getType() == 2) {
                        // 不计收支类型，使用支出分类的"其他"分类
                        categories = categoryMap.get(1);
                    } else {
                        categories = categoryMap.get(billTransaction.getType());
                    }
                    Category defaultCategory = categories != null ? categories.stream()
                        .filter(c -> "其他".equals(c.getName()))
                        .findFirst()
                        .orElse(!categories.isEmpty() ? categories.get(0) : null)
                        : null;
                    
                    if (defaultCategory == null) {
                        failCount++;
                        addImportError(result, billTransaction.getTradeNo(), "分类不存在且无默认分类", billTransaction.getDescription());
                        continue;
                    }
                    categoryId = defaultCategory.getId();
                }
                
                // 创建交易记录
                Transactions transaction = new Transactions();
                transaction.setFamilyId(familyId);
                transaction.setType(billTransaction.getType());
                transaction.setAmount(billTransaction.getAmount());
                transaction.setCategoryId(categoryId);
                transaction.setDate(billTransaction.getDate());
                transaction.setDescription(billTransaction.getDescription());
                transaction.setPayMethod(billTransaction.getPayMethod());
                transaction.setTradeNo(billTransaction.getTradeNo()); // 设置交易单号
                transaction.setPlatform(platform); // 设置平台来源
                transaction.setCreatedBy(userId);
                transaction.setCounterparty(billTransaction.getCounterparty());
                
                transactionsToSave.add(transaction);
                
            } catch (Exception e) {
                failCount++;
                addImportError(result, billTransaction.getTradeNo(), "处理失败：" + e.getMessage(), billTransaction.getDescription());
                log.warn("处理交易记录失败，tradeNo：{}", billTransaction.getTradeNo(), e);
            }
        }
        
        // 批量保存交易记录
        if (!transactionsToSave.isEmpty()) {
            boolean saved = transactionsService.saveBatch(transactionsToSave);
            if (saved) {
                successCount = transactionsToSave.size();
                result.setTransactionIds(transactionsToSave.stream()
                    .map(Transactions::getId)
                    .collect(Collectors.toList()));
            } else {
                failCount += transactionsToSave.size();
            }
        }
        
        result.setTotalCount(totalCount);
        result.setSuccessCount(successCount);
        result.setSkipCount(skipCount);
        result.setFailCount(failCount);
        
        return result;
    }
    
    /**
     * 添加导入错误
     */
    private void addImportError(BillImportResult result, String tradeNo, String reason, String rawData) {
        BillImportResult.BillImportError error = new BillImportResult.BillImportError();
        error.setTradeNo(tradeNo);
        error.setReason(reason);
        error.setRawData(rawData);
        result.getErrors().add(error);
    }
}
