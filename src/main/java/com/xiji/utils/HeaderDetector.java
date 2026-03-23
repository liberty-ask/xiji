package com.xiji.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 表头自动识别工具类
 * 支持Excel和CSV格式的表头自动识别
 * @author liberty
 */
@Slf4j
public class HeaderDetector {
    
    // 通用关键字段集合
    private static final Set<String> COMMON_KEYWORDS = new HashSet<>(Arrays.asList(
        "交易时间", "金额", "收/支", "交易单号", "订单号", 
        "交易状态", "交易对方", "商户名称", "商品", "支付方式"
    ));
    
    // 各平台特定关键字段
    private static final Map<String, Set<String>> PLATFORM_KEYWORDS = new HashMap<>();
    static {
        Set<String> wechatKeywords = new HashSet<>(Arrays.asList(
            "交易单号", "交易时间", "金额(元)", "收/支", "交易类型", 
            "交易对方", "商品", "支付方式", "当前状态", "商户单号"
        ));
        PLATFORM_KEYWORDS.put("wechat", wechatKeywords);
        
        Set<String> alipayKeywords = new HashSet<>(Arrays.asList(
            "交易订单号", "交易时间", "金额", "收/支", "交易对方", 
            "商品说明", "收/付款方式", "交易分类", "交易状态", "商家订单号"
        ));
        PLATFORM_KEYWORDS.put("alipay", alipayKeywords);
        
        Set<String> jdKeywords = new HashSet<>(Arrays.asList(
            "交易时间", "商户名称", "交易说明", "金额", "收/付款方式", 
            "交易状态", "收/支", "交易分类", "交易订单号", "商家订单号", "备注"
        ));
        PLATFORM_KEYWORDS.put("jd", jdKeywords);
    }
    
    /**
     * 自动识别表头位置
     * @param inputStream 文件输入流
     * @param fileType 文件类型（xlsx, xls, csv）
     * @param platformCode 平台代码（wechat, alipay, jd）
     * @return 表头行索引（从0开始），未找到返回-1
     */
    public static int detectHeaderRow(InputStream inputStream, String fileType, String platformCode) {
        try {
            byte[] bytes = inputStream.readAllBytes();
            
            if ("csv".equalsIgnoreCase(fileType)) {
                return detectCsvHeaderRow(bytes, platformCode);
            } else if ("xlsx".equalsIgnoreCase(fileType) || "xls".equalsIgnoreCase(fileType)) {
                return detectExcelHeaderRow(bytes, fileType, platformCode);
            } else {
                throw new IllegalArgumentException("不支持的文件类型：" + fileType);
            }
        } catch (Exception e) {
            log.warn("自动识别表头位置失败", e);
            return -1;
        }
    }
    
    /**
     * 检测CSV文件表头位置
     */
    private static int detectCsvHeaderRow(byte[] bytes, String platformCode) {
        Set<String> keywords = PLATFORM_KEYWORDS.getOrDefault(platformCode, COMMON_KEYWORDS);
        Charset[] charsets = {StandardCharsets.UTF_8, Charset.forName("GBK")};
        
        for (Charset charset : charsets) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bytes), charset))) {
                String line;
                int rowIndex = 0;
                int maxSearchRows = 30;
                
                while ((line = reader.readLine()) != null && rowIndex < maxSearchRows) {
                    if (line.trim().isEmpty()) {
                        rowIndex++;
                        continue;
                    }
                    
                    // 简单解析CSV行
                    List<String> cells = parseCsvLine(line);
                    int matchedCount = 0;
                    
                    for (String cell : cells) {
                        String cellValue = cell.trim();
                        if (keywords.contains(cellValue)) {
                            matchedCount++;
                        }
                    }
                    
                    if (matchedCount >= 5) { // 匹配到5个以上关键字段
                        return rowIndex;
                    }
                    
                    rowIndex++;
                }
            } catch (Exception e) {
                // 编码错误，尝试下一种编码
                continue;
            }
        }
        
        return -1;
    }
    
    /**
     * 检测Excel文件表头位置
     */
    private static int detectExcelHeaderRow(byte[] bytes, String fileType, String platformCode) {
        Set<String> keywords = PLATFORM_KEYWORDS.getOrDefault(platformCode, COMMON_KEYWORDS);
        
        Workbook workbook = null;
        try (InputStream is = new ByteArrayInputStream(bytes)) {
            if ("xlsx".equalsIgnoreCase(fileType)) {
                workbook = new XSSFWorkbook(is);
            } else if ("xls".equalsIgnoreCase(fileType)) {
                workbook = new HSSFWorkbook(is);
            }
            
            if (workbook != null) {
                Sheet sheet = workbook.getSheetAt(0);
                if (sheet != null) {
                    int maxSearchRows = Math.min(30, sheet.getLastRowNum() + 1);
                    
                    for (int i = 0; i < maxSearchRows; i++) {
                        Row row = sheet.getRow(i);
                        if (row == null) {
                            continue;
                        }
                        
                        int matchedCount = 0;
                        for (Cell cell : row) {
                            String cellValue = getCellValueAsString(cell);
                            if (cellValue != null && keywords.contains(cellValue.trim())) {
                                matchedCount++;
                            }
                        }
                        
                        if (matchedCount >= 5) { // 匹配到5个以上关键字段
                            return i;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("检测Excel表头失败", e);
        } finally {
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (Exception e) {
                    log.warn("关闭Workbook失败", e);
                }
            }
        }
        
        return -1;
    }
    
    /**
     * 简单解析CSV行
     */
    private static List<String> parseCsvLine(String line) {
        List<String> cells = new ArrayList<>();
        StringBuilder currentCell = new StringBuilder();
        boolean inQuotes = false;
        
        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                cells.add(currentCell.toString());
                currentCell.setLength(0);
            } else {
                currentCell.append(c);
            }
        }
        
        if (currentCell.length() > 0) {
            cells.add(currentCell.toString());
        }
        
        return cells;
    }
    
    /**
     * 获取单元格值
     */
    private static String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return null;
        }
        
        CellType cellType = cell.getCellType();
        switch (cellType) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    java.util.Date date = cell.getDateCellValue();
                    return date != null ? date.toString() : null;
                } else {
                    double numericValue = cell.getNumericCellValue();
                    if (numericValue == (long) numericValue) {
                        return String.valueOf((long) numericValue);
                    } else {
                        return String.valueOf(numericValue);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    DataFormatter formatter = new DataFormatter();
                    return formatter.formatCellValue(cell);
                } catch (Exception e) {
                    return cell.getCellFormula();
                }
            case BLANK:
                return null;
            default:
                return null;
        }
    }
}