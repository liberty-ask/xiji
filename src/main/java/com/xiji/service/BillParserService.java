package com.xiji.service;

import com.xiji.entity.dto.response.BillParseResult;
import com.xiji.parser.BillParser;
import com.xiji.parser.model.BillTransaction;

import java.io.InputStream;
import java.util.List;

/**
 * 账单解析服务接口
 * @author liberty
 */
public interface BillParserService {
    
    /**
     * 解析账单文件
     * @param fileName 文件名
     * @param inputStream 文件输入流
     * @param fileType 文件类型（xlsx, xls, csv）
     * @param platform 平台代码（可选，如果指定则使用指定解析器，否则自动识别）
     * @return 解析结果
     */
    BillParseResult parseBill(String fileName, InputStream inputStream, String fileType, String platform);
    
    /**
     * 解析账单文件并返回完整交易列表（用于导入）
     * @param fileName 文件名
     * @param inputStream 文件输入流
     * @param fileType 文件类型（xlsx, xls, csv）
     * @param platform 平台代码（可选，如果指定则使用指定解析器，否则自动识别）
     * @return 完整交易列表
     */
    List<BillTransaction> parseBillToTransactions(String fileName, InputStream inputStream, String fileType, String platform);
    
    /**
     * 获取所有注册的解析器
     * @return 解析器列表
     */
    List<BillParser> getAllParsers();
    
    /**
     * 根据平台代码获取解析器
     * @param platformCode 平台代码
     * @return 解析器
     */
    BillParser getParserByCode(String platformCode);
}
