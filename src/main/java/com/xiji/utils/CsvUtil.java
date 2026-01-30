package com.xiji.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CSV文件处理工具类
 * @author liberty
 */
@Slf4j
public class CsvUtil {
    
    /**
     * 读取CSV文件的第一行（表头）
     * @param inputStream 文件输入流
     * @return 表头列表
     */
    public static List<String> readHeaders(InputStream inputStream) {
        return readHeaders(inputStream, 0, StandardCharsets.UTF_8);
    }
    
    /**
     * 读取CSV文件的表头（支持跳过前面的行）
     * @param inputStream 文件输入流
     * @param skipLines 跳过的行数（表头在第skipLines+1行，从0开始计数）
     * @return 表头列表
     */
    public static List<String> readHeaders(InputStream inputStream, int skipLines) {
        return readHeaders(inputStream, skipLines, StandardCharsets.UTF_8);
    }
    
    /**
     * 读取CSV文件的表头（支持跳过前面的行和指定编码）
     * @param inputStream 文件输入流
     * @param skipLines 跳过的行数（表头在第skipLines+1行，从0开始计数）
     * @param charset 字符编码
     * @return 表头列表
     */
    public static List<String> readHeaders(InputStream inputStream, int skipLines, Charset charset) {
        List<String> headers = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, charset))) {
            // 跳过前面的行
            for (int i = 0; i < skipLines; i++) {
                String line = reader.readLine();
                if (line == null) {
                    return headers; // 文件已读完
                }
            }
            
            // 读取表头行
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return headers; // 没有表头
            }
            
            // 解析表头
            try (CSVParser parser = CSVFormat.DEFAULT.parse(new StringReader(headerLine))) {
                CSVRecord record = parser.getRecords().get(0);
                for (String header : record) {
                    headers.add(header.trim());
                }
            }
        } catch (Exception e) {
            log.error("读取CSV表头失败，跳过行数：{}", skipLines, e);
            throw new RuntimeException("读取CSV表头失败：" + e.getMessage(), e);
        }
        return headers;
    }
    
    /**
     * 读取CSV文件的所有数据行
     * @param inputStream 文件输入流
     * @return 数据行列表，每行是一个Map，key为列名，value为单元格值
     */
    public static List<Map<String, String>> readDataRows(InputStream inputStream) {
        return readDataRows(inputStream, 0, StandardCharsets.UTF_8);
    }
    
    /**
     * 读取CSV文件的所有数据行（支持跳过前面的行）
     * @param inputStream 文件输入流
     * @param skipLines 跳过的行数（表头在第skipLines+1行，从0开始计数）
     * @return 数据行列表，每行是一个Map，key为列名，value为单元格值
     */
    public static List<Map<String, String>> readDataRows(InputStream inputStream, int skipLines) {
        return readDataRows(inputStream, skipLines, StandardCharsets.UTF_8);
    }
    
    /**
     * 读取CSV文件的所有数据行（支持跳过前面的行和指定编码）
     * @param inputStream 文件输入流
     * @param skipLines 跳过的行数（表头在第skipLines+1行，从0开始计数）
     * @param charset 字符编码
     * @return 数据行列表，每行是一个Map，key为列名，value为单元格值
     */
    public static List<Map<String, String>> readDataRows(InputStream inputStream, int skipLines, Charset charset) {
        List<Map<String, String>> rows = new ArrayList<>();
        List<String> headers = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, charset))) {
            // 跳过前面的行
            for (int i = 0; i < skipLines; i++) {
                String line = reader.readLine();
                if (line == null) {
                    return rows; // 文件已读完
                }
            }
            
            // 读取表头行
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return rows; // 没有表头
            }
            
            // 解析表头
            try (CSVParser headerParser = CSVFormat.DEFAULT.parse(new StringReader(headerLine))) {
                CSVRecord headerRecord = headerParser.getRecords().get(0);
                for (String header : headerRecord) {
                    headers.add(header.trim());
                }
            }
            
            // 读取数据行
            String dataLine;
            while ((dataLine = reader.readLine()) != null) {
                if (dataLine.trim().isEmpty()) {
                    continue; // 跳过空行
                }
                
                try (CSVParser dataParser = CSVFormat.DEFAULT.parse(new StringReader(dataLine))) {
                    CSVRecord record = dataParser.getRecords().get(0);
                    Map<String, String> rowData = new HashMap<>();
                    boolean hasData = false;
                    
                    for (int i = 0; i < headers.size() && i < record.size(); i++) {
                        String header = headers.get(i);
                        String value = record.get(i);
                        if (value != null && !value.trim().isEmpty()) {
                            rowData.put(header, value.trim());
                            hasData = true;
                        }
                    }
                    
                    // 只添加有数据的行
                    if (hasData) {
                        rows.add(rowData);
                    }
                }
            }
        } catch (Exception e) {
            log.error("读取CSV数据行失败，跳过行数：{}", skipLines, e);
            throw new RuntimeException("读取CSV数据行失败：" + e.getMessage(), e);
        }
        return rows;
    }
}
