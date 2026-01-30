package com.xiji.parser.impl;

import com.xiji.entity.dto.response.BillParseResult;
import com.xiji.parser.BillParser;
import com.xiji.parser.model.BillTransaction;
import com.xiji.utils.ExcelUtil;
import com.xiji.utils.CsvUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * 招商银行账单解析器
 * @author liberty
 */
@Slf4j
@Component
public class CmbBillParser implements BillParser {
    
    @Override
    public String getPlatformName() {
        return "招商银行";
    }
    
    @Override
    public String getPlatformCode() {
        return "cmb";
    }
    
    @Override
    public boolean canParse(String fileName, InputStream inputStream, String fileType) {
        try {
            // 招商银行账单只支持PDF格式
            if (!"pdf".equalsIgnoreCase(fileType)) {
                return false;
            }
            
            // PDF文件识别：检查文件头是否为PDF格式
            byte[] bytes = inputStream.readAllBytes();
            if (bytes.length < 4) {
                return false;
            }
            
            // PDF文件头：%PDF
            String header = new String(bytes, 0, Math.min(4, bytes.length));
            if (!header.startsWith("%PDF")) {
                return false;
            }
            
            // 检查文件名是否包含招商银行相关关键词
            String lowerFileName = fileName.toLowerCase();
            return lowerFileName.contains("cmb") || 
                   lowerFileName.contains("招商") || 
                   lowerFileName.contains("招行");
        } catch (Exception e) {
            log.warn("识别招商银行账单格式失败", e);
            return false;
        }
    }
    
    @Override
    public BillParseResult parse(String fileName, InputStream inputStream, String fileType) {
        BillParseResult result = new BillParseResult();
        result.setPlatform(getPlatformName());
        result.setPreview(new ArrayList<>());
        result.setErrors(new ArrayList<>());
        result.setMetadata(new HashMap<>());
        
        List<BillTransaction> transactions = new ArrayList<>();
        int successCount = 0;
        int errorCount = 0;
        
        try {
            // 招商银行账单只支持PDF格式
            if (!"pdf".equalsIgnoreCase(fileType)) {
                addError(result, 0, "招商银行账单只支持PDF格式", null);
                return result;
            }
            
            // TODO: PDF解析功能需要添加PDF解析库（如Apache PDFBox）
            // 目前暂不支持PDF解析，返回错误提示
            addError(result, 0, "PDF解析功能暂未实现，请使用CSV或Excel格式的账单", null);
            log.warn("PDF解析功能暂未实现，文件：{}", fileName);
            return result;
            
        } catch (Exception e) {
            log.error("解析招商银行账单失败", e);
            addError(result, 0, "解析文件失败：" + e.getMessage(), null);
            errorCount++;
        }
        
        // 设置结果
        result.setTotalCount(successCount + errorCount);
        result.setSuccessCount(successCount);
        result.setErrorCount(errorCount);
        
        // 设置完整的交易记录列表（用于导入）
        result.setTransactions(transactions);
        
        // 设置预览数据（返回全部数据）
        for (BillTransaction t : transactions) {
            BillParseResult.BillTransactionPreview preview = new BillParseResult.BillTransactionPreview();
            preview.setTradeNo(t.getTradeNo());
            preview.setDate(t.getDate());
            preview.setType(t.getType());
            preview.setAmount(t.getAmount());
            preview.setCategory(t.getCategory() != null ? t.getCategory() : "其他");
            preview.setDescription(t.getDescription());
            preview.setPayMethod(t.getPayMethod());
            preview.setCounterparty(t.getCounterparty());
            preview.setStatus(t.getStatus());
            result.getPreview().add(preview);
        }
        
        // 设置元数据
        if (!transactions.isEmpty()) {
            LocalDate minDate = transactions.stream()
                .map(BillTransaction::getDate)
                .filter(Objects::nonNull)
                .min(LocalDate::compareTo)
                .orElse(null);
            LocalDate maxDate = transactions.stream()
                .map(BillTransaction::getDate)
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(null);
            
            BigDecimal totalIncome = transactions.stream()
                .filter(t -> t.getType() != null && t.getType() == 0)
                .map(BillTransaction::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal totalExpense = transactions.stream()
                .filter(t -> t.getType() != null && t.getType() == 1)
                .map(BillTransaction::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            result.getMetadata().put("startDate", minDate != null ? minDate.toString() : null);
            result.getMetadata().put("endDate", maxDate != null ? maxDate.toString() : null);
            result.getMetadata().put("totalIncome", totalIncome);
            result.getMetadata().put("totalExpense", totalExpense);
        }
        
        return result;
    }
    
    /**
     * 解析单行数据（从Excel格式的Map，key为列索引）
     */
    private BillTransaction parseRowFromIndexMap(Map<Integer, String> row, Map<String, Integer> columnIndexMap) {
        BillTransaction transaction = new BillTransaction();
        
        // 交易单号（招商银行账单可能有交易流水号）
        String tradeNo = getCellValueFromIndexMap(row, columnIndexMap, "交易流水号", "流水号", "交易编号", "交易序号");
        if (tradeNo == null || tradeNo.isEmpty()) {
            // 如果没有交易单号，使用交易日期+金额+对方账户生成一个唯一标识
            String date = getCellValueFromIndexMap(row, columnIndexMap, "交易日期", "日期");
            String amount = getCellValueFromIndexMap(row, columnIndexMap, "交易金额", "金额", "支出金额", "收入金额");
            String counterparty = getCellValueFromIndexMap(row, columnIndexMap, "交易对手", "交易对手名称", "对方账户");
            tradeNo = "cmb_" + (date != null ? date : "") + "_" + (amount != null ? amount : "") + "_" + (counterparty != null ? counterparty : "");
        }
        transaction.setTradeNo(tradeNo);
        
        // 交易日期
        String dateStr = getCellValueFromIndexMap(row, columnIndexMap, "交易日期", "日期");
        transaction.setDate(ExcelUtil.parseDate(dateStr));
        
        // 金额（招商银行可能有单独的支出金额和收入金额列，或者一个交易金额列）
        String amountStr = getCellValueFromIndexMap(row, columnIndexMap, "交易金额", "金额");
        if (amountStr == null || amountStr.isEmpty()) {
            // 尝试获取支出金额或收入金额
            String expenseStr = getCellValueFromIndexMap(row, columnIndexMap, "支出金额");
            String incomeStr = getCellValueFromIndexMap(row, columnIndexMap, "收入金额");
            
            if (expenseStr != null && !expenseStr.isEmpty()) {
                amountStr = expenseStr;
                transaction.setType(1); // 支出
            } else if (incomeStr != null && !incomeStr.isEmpty()) {
                amountStr = incomeStr;
                transaction.setType(0); // 收入
            }
        }
        transaction.setAmount(ExcelUtil.parseAmount(amountStr));
        
        // 收/支类型（如果还没有设置）
        if (transaction.getType() == null) {
            String incomeExpense = getCellValueFromIndexMap(row, columnIndexMap, "交易类型", "类型", "收支类型");
            if ("收入".equals(incomeExpense) || "收款".equals(incomeExpense) || "转入".equals(incomeExpense)) {
                transaction.setType(0);
            } else if ("支出".equals(incomeExpense) || "付款".equals(incomeExpense) || "转出".equals(incomeExpense)) {
                transaction.setType(1);
            } else {
                // 如果没有明确标识，根据金额正负判断
                if (transaction.getAmount() != null) {
                    transaction.setType(1); // 默认支出
                }
            }
        }
        
        // 交易对方
        transaction.setCounterparty(getCellValueFromIndexMap(row, columnIndexMap, "交易对手", "交易对手名称", "对方账户", "对方户名"));
        
        // 交易摘要/说明
        transaction.setDescription(getCellValueFromIndexMap(row, columnIndexMap, "交易摘要", "摘要", "交易说明", "备注", "用途"));
        
        // 支付方式
        transaction.setPayMethod(getCellValueFromIndexMap(row, columnIndexMap, "支付方式", "交易渠道", "渠道"));
        
        // 交易状态
        transaction.setStatus(getCellValueFromIndexMap(row, columnIndexMap, "交易状态", "状态"));
        
        // 保存原始数据
        for (Map.Entry<Integer, String> entry : row.entrySet()) {
            transaction.getRawData().put("col_" + entry.getKey(), entry.getValue());
        }
        
        return transaction;
    }
    
    /**
     * 解析单行数据（从CSV格式的Map，key为列名）
     */
    private BillTransaction parseRowFromMap(Map<String, String> row, List<String> headers) {
        BillTransaction transaction = new BillTransaction();
        
        // 交易单号（招商银行账单可能有交易流水号）
        String tradeNo = getCellValueFromMap(row, "交易流水号", "流水号", "交易编号", "交易序号");
        if (tradeNo == null || tradeNo.isEmpty()) {
            // 如果没有交易单号，使用交易日期+金额+对方账户生成一个唯一标识
            String date = getCellValueFromMap(row, "交易日期", "日期");
            String amount = getCellValueFromMap(row, "交易金额", "金额", "支出金额", "收入金额");
            String counterparty = getCellValueFromMap(row, "交易对手", "交易对手名称", "对方账户");
            tradeNo = "cmb_" + (date != null ? date : "") + "_" + (amount != null ? amount : "") + "_" + (counterparty != null ? counterparty : "");
        }
        transaction.setTradeNo(tradeNo);
        
        // 交易日期
        String dateStr = getCellValueFromMap(row, "交易日期", "日期");
        transaction.setDate(ExcelUtil.parseDate(dateStr));
        
        // 金额（招商银行可能有单独的支出金额和收入金额列，或者一个交易金额列）
        String amountStr = getCellValueFromMap(row, "交易金额", "金额");
        if (amountStr == null || amountStr.isEmpty()) {
            // 尝试获取支出金额或收入金额
            String expenseStr = getCellValueFromMap(row, "支出金额");
            String incomeStr = getCellValueFromMap(row, "收入金额");
            
            if (expenseStr != null && !expenseStr.isEmpty()) {
                amountStr = expenseStr;
                transaction.setType(1); // 支出
            } else if (incomeStr != null && !incomeStr.isEmpty()) {
                amountStr = incomeStr;
                transaction.setType(0); // 收入
            }
        }
        transaction.setAmount(ExcelUtil.parseAmount(amountStr));
        
        // 收/支类型（如果还没有设置）
        if (transaction.getType() == null) {
            String incomeExpense = getCellValueFromMap(row, "交易类型", "类型", "收支类型");
            if ("收入".equals(incomeExpense) || "收款".equals(incomeExpense) || "转入".equals(incomeExpense)) {
                transaction.setType(0);
            } else if ("支出".equals(incomeExpense) || "付款".equals(incomeExpense) || "转出".equals(incomeExpense)) {
                transaction.setType(1);
            } else {
                // 如果没有明确标识，根据金额正负判断
                if (transaction.getAmount() != null) {
                    transaction.setType(1); // 默认支出
                }
            }
        }
        
        // 交易对方
        transaction.setCounterparty(getCellValueFromMap(row, "交易对手", "交易对手名称", "对方账户", "对方户名"));
        
        // 交易摘要/说明
        transaction.setDescription(getCellValueFromMap(row, "交易摘要", "摘要", "交易说明", "备注", "用途"));
        
        // 支付方式
        transaction.setPayMethod(getCellValueFromMap(row, "支付方式", "交易渠道", "渠道"));
        
        // 交易状态
        transaction.setStatus(getCellValueFromMap(row, "交易状态", "状态"));
        
        // 保存原始数据
        for (String header : headers) {
            String value = row.get(header);
            if (value != null) {
                transaction.getRawData().put(header, value);
            }
        }
        
        return transaction;
    }
    
    /**
     * 获取单元格值（支持多个可能的列名）- Excel格式
     */
    private String getCellValueFromIndexMap(Map<Integer, String> row, Map<String, Integer> columnIndexMap, String... columnNames) {
        for (String columnName : columnNames) {
            Integer index = columnIndexMap.get(columnName);
            if (index != null) {
                String value = row.get(index);
                if (value != null && !value.trim().isEmpty()) {
                    return value.trim();
                }
            }
        }
        return null;
    }
    
    /**
     * 获取单元格值（支持多个可能的列名）- CSV格式
     */
    private String getCellValueFromMap(Map<String, String> row, String... columnNames) {
        for (String columnName : columnNames) {
            String value = row.get(columnName);
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }
    
    /**
     * 添加错误信息
     */
    private void addError(BillParseResult result, int row, String reason, String rawData) {
        BillParseResult.BillParseError error = new BillParseResult.BillParseError();
        error.setRow(row);
        error.setReason(reason);
        error.setRawData(rawData);
        result.getErrors().add(error);
    }
}
