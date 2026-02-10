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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

/**
 * 京东账单解析器
 * 京东账单是CSV文件，第22行是表头
 * 表头字段：交易时间、商户名称、交易说明、金额、收/付款方式、交易状态、收/支、交易分类、交易订单号、商家订单号、备注
 * @author liberty
 */
@Slf4j
@Component
public class JdBillParser implements BillParser {
    
    // 京东账单表头在第22行（从1开始计数），所以需要跳过前21行（索引从0开始）
    private static final int HEADER_ROW_INDEX = 21; // 跳过21行，第22行是表头
    
    @Override
    public String getPlatformName() {
        return "京东";
    }
    
    @Override
    public String getPlatformCode() {
        return "jd";
    }
    
    @Override
    public boolean canParse(String fileName, InputStream inputStream, String fileType) {
        try {
            // 京东账单只支持CSV格式
            if (!"csv".equalsIgnoreCase(fileType)) {
                return false;
            }
            
            // 将输入流转换为字节数组，以便重复读取
            byte[] bytes = inputStream.readAllBytes();
            ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            
            // 尝试读取表头（跳过前21行，第22行是表头）
            // 京东CSV账单可能使用GBK或UTF-8编码，先尝试UTF-8
            List<String> headers = null;
            try {
                bis = new ByteArrayInputStream(bytes);
                headers = CsvUtil.readHeaders(bis, HEADER_ROW_INDEX, StandardCharsets.UTF_8);
            } catch (Exception e) {
                // 如果UTF-8失败，尝试GBK编码
                try {
                    bis = new ByteArrayInputStream(bytes);
                    headers = CsvUtil.readHeaders(bis, HEADER_ROW_INDEX, Charset.forName("GBK"));
                } catch (Exception e2) {
                    log.warn("读取京东账单表头失败", e2);
                    return false;
                }
            }
            
            if (headers == null || headers.isEmpty()) {
                return false;
            }
            
            // 京东账单特征：包含"交易时间"、"商户名称"、"交易说明"、"金额"、"收/支"、"交易订单号"等字段
            Set<String> headerSet = new HashSet<>(headers);
            boolean hasTradeTime = headerSet.contains("交易时间");
            boolean hasMerchantName = headerSet.contains("商户名称");
            boolean hasTradeDescription = headerSet.contains("交易说明");
            boolean hasAmount = headerSet.contains("金额");
            boolean hasIncomeExpense = headerSet.contains("收/支");
            boolean hasTradeOrderNo = headerSet.contains("交易订单号");
            
            // 至少需要包含这些关键字段
            return hasTradeTime && hasAmount && hasIncomeExpense && hasTradeOrderNo;
        } catch (Exception e) {
            log.warn("识别京东账单格式失败", e);
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
            // 京东账单只支持CSV格式
            if (!"csv".equalsIgnoreCase(fileType)) {
                addError(result, 0, "京东账单只支持CSV格式", null);
                return result;
            }
            
            // 将输入流转换为字节数组，以便重复读取
            byte[] bytes = inputStream.readAllBytes();
            ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            
            // 尝试读取表头和数据（先尝试UTF-8，失败则尝试GBK）
            List<String> headers = null;
            List<Map<String, String>> csvRows = null;
            Charset charset = StandardCharsets.UTF_8;
            
            try {
                headers = CsvUtil.readHeaders(bis, HEADER_ROW_INDEX, charset);
                bis = new ByteArrayInputStream(bytes); // 重新创建流
                csvRows = CsvUtil.readDataRows(bis, HEADER_ROW_INDEX, charset);
            } catch (Exception e) {
                // 如果UTF-8失败，尝试GBK编码
                log.info("使用UTF-8编码读取失败，尝试GBK编码");
                charset = Charset.forName("GBK");
                bis = new ByteArrayInputStream(bytes);
                headers = CsvUtil.readHeaders(bis, HEADER_ROW_INDEX, charset);
                bis = new ByteArrayInputStream(bytes);
                csvRows = CsvUtil.readDataRows(bis, HEADER_ROW_INDEX, charset);
            }
            
            if (headers == null || headers.isEmpty()) {
                addError(result, 0, "无法读取表头", null);
                return result;
            }
            
            // 解析每一行数据
            // 京东账单表头在第22行（从1开始计数），数据从第23行开始
            int csvHeaderRow = 22; // 表头所在行号（从1开始）
            for (int rowIndex = 0; rowIndex < csvRows.size(); rowIndex++) {
                Map<String, String> row = csvRows.get(rowIndex);
                int actualRowNumber = csvHeaderRow + 1 + rowIndex; // 实际行号 = 表头行 + 1 + 数据索引
                try {
                    BillTransaction transaction = parseRowFromMap(row, headers);
                    if (transaction != null && transaction.getDate() != null && transaction.getAmount() != null) {
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
            log.error("解析京东账单失败", e);
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

        // 收/支类型（使用"收/支"）
        String incomeExpense = getCellValueFromMap(row, "收/支");
        if("不计收支".contains(incomeExpense)){
            transaction.setType(2); // 2-不计收支
        }else if ("收入".equals(incomeExpense) || "收款".equals(incomeExpense) || "收".equals(incomeExpense)) {
            transaction.setType(0);
        } else if ("支出".equals(incomeExpense) || "付款".equals(incomeExpense) || "支".equals(incomeExpense)) {
            transaction.setType(1);
        } else {
            // 如果没有明确标识，根据金额正负判断
            if (transaction.getAmount() != null) {
                if (transaction.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                    transaction.setType(1); // 默认支出
                } else {
                    transaction.setType(0); // 收入
                }
            }
        }
        
        // 交易订单号（优先使用"交易订单号"，其次"商家订单号"）
        String tradeNo = getCellValueFromMap(row, "交易订单号", "商家订单号");
        transaction.setTradeNo(tradeNo);
        
        // 交易日期（使用"交易时间"）
        String dateStr = getCellValueFromMap(row, "交易时间");
        transaction.setDate(ExcelUtil.parseDate(dateStr));
        
        // 金额（使用"金额"）
        String amountStr = getCellValueFromMap(row, "金额");
        // 清理金额字符串，去除括号及其内容（如：19.88(已退款2) -> 19.88）
        amountStr = cleanAmountString(amountStr);
        transaction.setAmount(ExcelUtil.parseAmount(amountStr));
        
        // 商户名称（作为交易对方）
        transaction.setCounterparty(getCellValueFromMap(row, "商户名称"));
        
        // 交易说明（作为描述）
        transaction.setDescription(getCellValueFromMap(row, "交易说明"));
        
        // 交易分类
        transaction.setCategory(getCellValueFromMap(row, "交易分类"));
        
        // 收/付款方式（作为支付方式）
        transaction.setPayMethod(getCellValueFromMap(row, "收/付款方式"));
        
        // 交易状态
        transaction.setStatus(getCellValueFromMap(row, "交易状态"));
        
        // 备注（如果交易说明为空，可以使用备注作为描述）
        String note = getCellValueFromMap(row, "备注");
        if ((transaction.getDescription() == null || transaction.getDescription().trim().isEmpty()) 
            && note != null && !note.trim().isEmpty()) {
            transaction.setDescription(note);
        }
        
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
     * 清理金额字符串，去除括号及其内容
     * 例如：19.88(已退款2) -> 19.88
     * @param amountStr 原始金额字符串
     * @return 清理后的金额字符串
     */
    private String cleanAmountString(String amountStr) {
        if (amountStr == null || amountStr.trim().isEmpty()) {
            return amountStr;
        }
        
        // 使用正则表达式去除括号及其内容
        // 匹配：左括号(，后面跟任意字符（非右括号），然后是右括号)
        String cleaned = amountStr.replaceAll("\\([^)]*\\)", "").trim();
        
        return cleaned;
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

