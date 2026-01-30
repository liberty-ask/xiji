package com.xiji.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * Excel文件处理工具类
 * @author liberty
 */
@Slf4j
public class ExcelUtil {
    
    /**
     * 读取Excel文件的第一行（表头）
     * @param inputStream 文件输入流
     * @param fileType 文件类型（xlsx或xls）
     * @return 表头列表
     */
    public static List<String> readHeaders(InputStream inputStream, String fileType) {
        return readHeaders(inputStream, fileType, 0);
    }
    
    /**
     * 读取Excel文件的表头（支持跳过前面的行）
     * @param inputStream 文件输入流
     * @param fileType 文件类型（xlsx或xls）
     * @param skipRows 跳过的行数（表头在第skipRows行，从0开始计数）
     * @return 表头列表
     */
    public static List<String> readHeaders(InputStream inputStream, String fileType, int skipRows) {
        List<String> headers = new ArrayList<>();
        Workbook workbook = null;
        try {
            if ("xlsx".equalsIgnoreCase(fileType)) {
                workbook = new XSSFWorkbook(inputStream);
            } else if ("xls".equalsIgnoreCase(fileType)) {
                workbook = new HSSFWorkbook(inputStream);
            } else {
                throw new IllegalArgumentException("不支持的文件类型：" + fileType);
            }
            
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                return headers;
            }
            
            Row headerRow = sheet.getRow(skipRows);
            if (headerRow == null) {
                return headers;
            }
            
            for (Cell cell : headerRow) {
                String value = getCellValueAsString(cell);
                if (value != null && !value.trim().isEmpty()) {
                    headers.add(value.trim());
                }
            }
        } catch (Exception e) {
            log.error("读取Excel表头失败，跳过行数：{}", skipRows, e);
            throw new RuntimeException("读取Excel表头失败：" + e.getMessage(), e);
        } finally {
            closeWorkbook(workbook);
        }
        
        return headers;
    }
    
    /**
     * 读取Excel文件的所有数据行（跳过第一行表头）
     * @param inputStream 文件输入流
     * @param fileType 文件类型（xlsx或xls）
     * @return 数据行列表，每行是一个Map，key为列索引，value为单元格值
     */
    public static List<Map<Integer, String>> readDataRows(InputStream inputStream, String fileType) {
        return readDataRows(inputStream, fileType, 0);
    }
    
    /**
     * 读取Excel文件的所有数据行（支持跳过前面的行）
     * @param inputStream 文件输入流
     * @param fileType 文件类型（xlsx或xls）
     * @param skipRows 跳过的行数（表头在第skipRows行，数据从第skipRows+1行开始，从0开始计数）
     * @return 数据行列表，每行是一个Map，key为列索引，value为单元格值
     */
    public static List<Map<Integer, String>> readDataRows(InputStream inputStream, String fileType, int skipRows) {
        List<Map<Integer, String>> rows = new ArrayList<>();
        Workbook workbook = null;
        try {
            if ("xlsx".equalsIgnoreCase(fileType)) {
                workbook = new XSSFWorkbook(inputStream);
            } else if ("xls".equalsIgnoreCase(fileType)) {
                workbook = new HSSFWorkbook(inputStream);
            } else {
                throw new IllegalArgumentException("不支持的文件类型：" + fileType);
            }
            
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                return rows;
            }
            
            int lastRowNum = sheet.getLastRowNum();
            // 从表头的下一行开始读取数据（跳过表头行）
            int dataStartRow = skipRows + 1;
            for (int i = dataStartRow; i <= lastRowNum; i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                
                Map<Integer, String> rowData = new HashMap<>();
                boolean hasData = false;
                
                for (Cell cell : row) {
                    String value = getCellValueAsString(cell);
                    if (value != null && !value.trim().isEmpty()) {
                        rowData.put(cell.getColumnIndex(), value.trim());
                        hasData = true;
                    }
                }
                
                // 只添加有数据的行
                if (hasData) {
                    rows.add(rowData);
                }
            }
        } catch (Exception e) {
            log.error("读取Excel数据行失败，跳过行数：{}", skipRows, e);
            throw new RuntimeException("读取Excel数据行失败：" + e.getMessage(), e);
        } finally {
            closeWorkbook(workbook);
        }
        
        return rows;
    }
    
    /**
     * 获取单元格值（字符串格式）
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
                    // 日期类型
                    Date date = cell.getDateCellValue();
                    return date != null ? date.toString() : null;
                } else {
                    // 数字类型
                    double numericValue = cell.getNumericCellValue();
                    // 如果是整数，去掉小数点
                    if (numericValue == (long) numericValue) {
                        return String.valueOf((long) numericValue);
                    } else {
                        return String.valueOf(numericValue);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                // 公式单元格，尝试获取计算后的值
                try {
                    return getCellValueAsString(cell);
                } catch (Exception e) {
                    return cell.getCellFormula();
                }
            case BLANK:
                return null;
            default:
                return null;
        }
    }
    
    /**
     * 关闭Workbook
     */
    private static void closeWorkbook(Workbook workbook) {
        if (workbook != null) {
            try {
                workbook.close();
            } catch (Exception e) {
                log.warn("关闭Workbook失败", e);
            }
        }
    }
    
    /**
     * 解析日期字符串（支持多种格式）
     */
    public static LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        
        // 移除空格
        dateStr = dateStr.trim();
        
        // 尝试多种日期格式
        String[] formats = {
            "yyyy-MM-dd",
            "yyyy/MM/dd",
            "yyyy年MM月dd日",
            "MM/dd/yyyy",
            "dd/MM/yyyy"
        };
        
        for (String format : formats) {
            try {
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern(format);
                return LocalDate.parse(dateStr, formatter);
            } catch (Exception e) {
                // 继续尝试下一个格式
            }
        }
        
        // 如果都失败，尝试使用Java内置的日期解析
        try {
            java.util.Date date = new java.text.SimpleDateFormat("yyyy-MM-dd").parse(dateStr);
            return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        } catch (Exception e) {
            log.warn("无法解析日期：{}", dateStr);
            return null;
        }
    }
    
    /**
     * 解析金额字符串
     */
    public static BigDecimal parseAmount(String amountStr) {
        if (amountStr == null || amountStr.trim().isEmpty()) {
            return null;
        }
        
        try {
            // 移除空格、逗号等
            String cleaned = amountStr.trim()
                .replace(",", "")
                .replace("，", "")
                .replace("￥", "")
                .replace("¥", "")
                .replace("$", "")
                .trim();
            
            return new BigDecimal(cleaned);
        } catch (Exception e) {
            log.warn("无法解析金额：{}", amountStr);
            return null;
        }
    }
}


