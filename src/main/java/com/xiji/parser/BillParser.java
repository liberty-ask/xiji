package com.xiji.parser;

import com.xiji.entity.dto.response.BillParseResult;

import java.io.InputStream;

/**
 * 账单解析器接口
 * 使用策略模式，不同平台实现不同的解析器
 * @author liberty
 */
public interface BillParser {
    
    /**
     * 识别是否为该平台的账单
     * @param fileName 文件名
     * @param inputStream 文件输入流（用于读取表头）
     * @param fileType 文件类型（xlsx, xls, csv）
     * @return 是否匹配
     */
    boolean canParse(String fileName, InputStream inputStream, String fileType);
    
    /**
     * 解析账单文件
     * @param fileName 文件名
     * @param inputStream 文件输入流
     * @param fileType 文件类型
     * @return 解析结果
     */
    BillParseResult parse(String fileName, InputStream inputStream, String fileType);
    
    /**
     * 获取平台名称
     * @return 平台名称（支付宝、微信、招商银行等）
     */
    String getPlatformName();
    
    /**
     * 获取平台代码
     * @return 平台代码（alipay, wechat, cmb等）
     */
    String getPlatformCode();
}


