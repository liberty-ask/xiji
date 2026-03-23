package com.xiji.parser.impl;

import cn.hutool.core.util.StrUtil;
import com.xiji.entity.dto.response.BillParseResult;
import com.xiji.parser.BillParser;
import com.xiji.parser.model.BillTransaction;
import com.xiji.utils.ExcelUtil;
import com.xiji.utils.HeaderDetector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * 微信账单解析器
 * @author liberty
 */
@Slf4j
@Component
public class WechatBillParser implements BillParser {
    
    @Override
    public String getPlatformName() {
        return "微信";
    }
    
    @Override
    public String getPlatformCode() {
        return "wechat";
    }
    
    @Override
    public boolean canParse(String fileName, InputStream inputStream, String fileType) {
        try {
            // 微信账单只支持XLSX格式
            if (!"xlsx".equalsIgnoreCase(fileType) && !"xls".equalsIgnoreCase(fileType)) {
                return false;
            }
            
            // 将输入流转换为字节数组，以便重复读取
            byte[] bytes = inputStream.readAllBytes();
            ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            
            // 尝试读取第一行内容（使用ExcelUtil.readHeaders方法，跳过0行，即读取第一行）
            List<String> firstRowCells = ExcelUtil.readHeaders(bis, fileType, 0);
            
            if (firstRowCells == null || firstRowCells.isEmpty()) {
                return false;
            }
            
            // 获取第一行第一个单元格的内容
            String firstCellValue = firstRowCells.get(0);
            if (firstCellValue == null || firstCellValue.trim().isEmpty()) {
                return false;
            }
            
            // 判断第一行是否包含"微信支付账单"
            return firstCellValue.contains("微信支付账单");
        } catch (Exception e) {
            log.warn("识别微信账单格式失败", e);
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
            // 将输入流转换为字节数组，以便重复读取
            byte[] bytes = inputStream.readAllBytes();
            ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            
            // 微信账单只支持Excel格式
            if (!"xlsx".equalsIgnoreCase(fileType) && !"xls".equalsIgnoreCase(fileType)) {
                addError(result, 0, "微信账单只支持XLSX/XLS格式", null);
                return result;
            }
            
            // 自动识别表头位置
            int headerRowIndex = HeaderDetector.detectHeaderRow(new ByteArrayInputStream(bytes), fileType, getPlatformCode());
            if (headerRowIndex == -1) {
                addError(result, 0, "无法识别微信账单表头", null);
                return result;
            }
            
            // 读取表头和数据
            bis = new ByteArrayInputStream(bytes);
            List<String> headers = ExcelUtil.readHeaders(bis, fileType, headerRowIndex);
            bis = new ByteArrayInputStream(bytes); // 重新创建流
            List<Map<Integer, String>> excelRows = ExcelUtil.readDataRows(bis, fileType, headerRowIndex);
            
            // 构建列索引映射（Excel使用）
            Map<String, Integer> columnIndexMap = new HashMap<>();
            for (int i = 0; i < headers.size(); i++) {
                columnIndexMap.put(headers.get(i), i);
            }
            
            // 解析每一行数据
            // Excel格式：使用列索引作为key
            int excelHeaderRow = headerRowIndex + 1; // 表头所在行号（从1开始）
            for (int rowIndex = 0; rowIndex < excelRows.size(); rowIndex++) {
                Map<Integer, String> row = excelRows.get(rowIndex);
                int actualRowNumber = excelHeaderRow + 1 + rowIndex; // 实际行号 = 表头行 + 1 + 数据索引
                try {
                    BillTransaction transaction = parseRowFromIndexMap(row, columnIndexMap);
                    if (transaction.getDate() != null && transaction.getAmount() != null) {
                        transactions.add(transaction);
                        successCount++;
                    } else {
                        errorCount++;
                        addError(result, actualRowNumber, "数据不完整", row.toString());
                    }
                } catch (Exception e) {
                    errorCount++;
                    addError(result, actualRowNumber, "解析失败：" + e.getMessage(), row.toString());
                    log.warn("解析第{}行数据失败", actualRowNumber, e);
                }
            }
            
        } catch (Exception e) {
            log.error("解析微信账单失败", e);
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
        
        // 交易订单号（优先使用"交易订单号"，其次"交易单号"，最后"商户单号"）
        String tradeNo = getCellValueFromIndexMap(row, columnIndexMap, "交易单号");
        // 如果没有交易订单号，使用交易时间+金额+对方账户生成一个唯一标识
        String time = getCellValueFromIndexMap(row, columnIndexMap, "交易时间");
        String amount = getCellValueFromIndexMap(row, columnIndexMap,"金额(元)");
        if (StrUtil.isBlank(tradeNo)) {
            String counterparty = getCellValueFromIndexMap(row, columnIndexMap, "交易对方");
            tradeNo = "wechat_" + (time != null ? time : "") + "_" + (amount != null ? amount : "") + "_" + (counterparty != null ? counterparty : "");
        }
        transaction.setTradeNo(tradeNo);
        //商户单号
        transaction.setMerchantOrderNo(getCellValueFromIndexMap(row, columnIndexMap, "商户单号"));
        
        // 交易日期（优先使用"交易时间"，其次"支付时间"）
        transaction.setDate(ExcelUtil.parseDate(time));
        
        // 金额（优先使用"金额"，其次"金额(元)"）
        transaction.setAmount(ExcelUtil.parseAmount(amount));
        
        // 收/支类型
        String incomeExpense = getCellValueFromIndexMap(row, columnIndexMap, "收/支");
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
        // 交易类型
        transaction.setCategory(getCellValueFromIndexMap(row, columnIndexMap, "交易类型"));
        // 交易对方
        transaction.setCounterparty(getCellValueFromIndexMap(row, columnIndexMap, "交易对方"));
        
        // 商品说明/备注（优先使用"商品说明"，其次"商品名称"，最后"备注"）
        String description = getCellValueFromIndexMap(row, columnIndexMap, "商品");
        if(StrUtil.isEmpty(description) || "/".equals(description)){
            description = transaction.getCategory() + "/" + transaction.getCounterparty();
        }
        transaction.setDescription(description);
        // 支付方式
        transaction.setPayMethod(getCellValueFromIndexMap(row, columnIndexMap, "支付方式"));
        
        // 交易状态（优先使用"当前状态"，其次"交易状态"）
        transaction.setStatus(getCellValueFromIndexMap(row, columnIndexMap, "当前状态"));
        
        // 保存原始数据
        for (Map.Entry<Integer, String> entry : row.entrySet()) {
            transaction.getRawData().put("col_" + entry.getKey(), entry.getValue());
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
