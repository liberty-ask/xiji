package com.xiji.parser.impl;

import com.xiji.entity.dto.response.BillParseResult;
import com.xiji.parser.BillParser;
import com.xiji.parser.model.BillTransaction;
import com.xiji.utils.ExcelUtil;
import com.xiji.utils.CsvUtil;
import com.xiji.utils.HeaderDetector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

/**
 * 支付宝账单解析器
 * @author liberty
 */
@Slf4j
@Component
public class AlipayBillParser implements BillParser {
    
    @Override
    public String getPlatformName() {
        return "支付宝";
    }
    
    @Override
    public String getPlatformCode() {
        return "alipay";
    }
    
    @Override
    public boolean canParse(String fileName, InputStream inputStream, String fileType) {
        try {
            // 支付宝账单只支持CSV格式
            if (!"csv".equalsIgnoreCase(fileType)) {
                return false;
            }
            
            // 将输入流转换为字节数组，以便重复读取
            byte[] bytes = inputStream.readAllBytes();
            ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            
            // 尝试读取第4行（索引为3，从0开始计数）的内容
            // 支付宝CSV账单使用GBK编码
            List<String> fourthRowCells = CsvUtil.readHeaders(bis, 3, Charset.forName("GBK"));
            
            if (fourthRowCells == null || fourthRowCells.isEmpty()) {
                return false;
            }
            
            // 获取第4行第一个单元格的内容
            String firstCellValue = fourthRowCells.get(0);
            if (firstCellValue == null || firstCellValue.trim().isEmpty()) {
                return false;
            }
            
            // 判断第4行第一个单元格是否包含"支付宝账户"
            return firstCellValue.contains("支付宝账户");
        } catch (Exception e) {
            log.warn("识别支付宝账单格式失败", e);
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
            
            // 支付宝账单只支持CSV格式
            if (!"csv".equalsIgnoreCase(fileType)) {
                addError(result, 0, "支付宝账单只支持CSV格式", null);
                return result;
            }
            
            // 自动识别表头位置
            int headerRowIndex = HeaderDetector.detectHeaderRow(new ByteArrayInputStream(bytes), fileType, getPlatformCode());
            if (headerRowIndex == -1) {
                addError(result, 0, "无法识别支付宝账单表头", null);
                return result;
            }
            
            // 读取表头和数据（尝试不同编码）
            List<String> headers = null;
            List<Map<String, String>> csvRows = null;
            Charset charset = Charset.forName("GBK");

            try {
                bis = new ByteArrayInputStream(bytes);
                headers = CsvUtil.readHeaders(bis, headerRowIndex, charset);
                bis = new ByteArrayInputStream(bytes);
                csvRows = CsvUtil.readDataRows(bis, headerRowIndex, charset);
            } catch (Exception e) {
                // 编码错误，尝试下一种编码
            }
            
            if (headers == null || headers.isEmpty()) {
                addError(result, 0, "无法读取表头", null);
                return result;
            }
            
            // 解析每一行数据
            // CSV格式：使用列名作为key
            int csvHeaderRow = headerRowIndex + 1; // 表头所在行号（从1开始）
            for (int rowIndex = 0; rowIndex < csvRows.size(); rowIndex++) {
                Map<String, String> row = csvRows.get(rowIndex);
                int actualRowNumber = csvHeaderRow + 1 + rowIndex; // 实际行号 = 表头行 + 1 + 数据索引
                try {
                    BillTransaction transaction = parseRowFromMap(row, headers);
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
            log.error("解析支付宝账单失败", e);
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
     * 解析单行数据（从CSV格式的Map，key为列名）
     */
    private BillTransaction parseRowFromMap(Map<String, String> row, List<String> headers) {
        BillTransaction transaction = new BillTransaction();
        
        // 交易订单号（优先使用"交易订单号"，其次"交易号"，最后"交易单号"）
        transaction.setTradeNo(getCellValueFromMap(row, "交易订单号"));

        //商家订单号
        transaction.setMerchantOrderNo(getCellValueFromMap(row, "商家订单号"));
        
        // 交易日期（优先使用"交易时间"，其次"交易创建时间"，最后"付款时间"）
        transaction.setDate(ExcelUtil.parseDate(getCellValueFromMap(row, "交易时间")));
        
        // 金额（优先使用"金额"，其次"金额（元）"）
        transaction.setAmount(ExcelUtil.parseAmount(getCellValueFromMap(row, "金额")));
        
        // 收/支类型
        String incomeExpense = getCellValueFromMap(row, "收/支");
        if ("收入".equals(incomeExpense) || "收款".equals(incomeExpense)) {
            transaction.setType(0);
        } else if ("支出".equals(incomeExpense) || "付款".equals(incomeExpense)) {
            transaction.setType(1);
        } else if ("不计收支".equals(incomeExpense)) {
            transaction.setType(2); // 2-不计收支
        } else {
            // 如果没有明确标识，根据金额正负判断（支付宝账单通常支出为正）
            if (transaction.getAmount() != null && transaction.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                transaction.setType(1); // 默认支出
            }
        }
        
        // 交易对方
        transaction.setCounterparty(getCellValueFromMap(row, "交易对方"));
        
        // 商品说明/备注（优先使用"商品说明"，其次"商品名称"，最后"备注"）
        String description = getCellValueFromMap(row, "商品说明");
        transaction.setDescription(description);
        
        // 支付方式
        transaction.setPayMethod(getCellValueFromMap(row, "收/付款方式"));
        // 交易分类
        transaction.setCategory(getCellValueFromMap(row, "交易分类"));

        // 交易状态
        transaction.setStatus(getCellValueFromMap(row, "交易状态"));
        
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
