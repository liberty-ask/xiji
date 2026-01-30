package com.xiji.service.impl;

import com.xiji.entity.dto.response.BillParseResult;
import com.xiji.parser.BillParser;
import com.xiji.parser.model.BillTransaction;
import com.xiji.service.BillParserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 账单解析服务实现类
 * @author liberty
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BillParserServiceImpl implements BillParserService {
    
    private final List<BillParser> parsers;
    
    @Override
    public BillParseResult parseBill(String fileName, InputStream inputStream, String fileType, String platform) {
        // 将输入流转换为字节数组，以便重复读取
        byte[] bytes;
        try {
            bytes = inputStream.readAllBytes();
        } catch (Exception e) {
            log.error("读取文件失败", e);
            throw new RuntimeException("读取文件失败：" + e.getMessage(), e);
        }
        
        BillParser parser = getParser(fileName, fileType, platform, bytes);
        
        log.info("使用解析器：{}，解析文件：{}", parser.getPlatformName(), fileName);
        
        // 使用解析器解析文件
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        
        return parser.parse(fileName, bis, fileType);
    }
    
    @Override
    public List<BillTransaction> parseBillToTransactions(String fileName, InputStream inputStream, String fileType, String platform) {
        // 将输入流转换为字节数组，以便重复读取
        byte[] bytes;
        try {
            bytes = inputStream.readAllBytes();
        } catch (Exception e) {
            log.error("读取文件失败", e);
            throw new RuntimeException("读取文件失败：" + e.getMessage(), e);
        }
        
        BillParser parser = getParser(fileName, fileType, platform, bytes);
        
        log.info("使用解析器：{}，解析文件：{}（获取完整交易列表）", parser.getPlatformName(), fileName);
        throw new UnsupportedOperationException("需要修改BillParser接口添加parseToTransactions方法");
    }
    
    /**
     * 获取解析器
     */
    private BillParser getParser(String fileName, String fileType, String platform, byte[] bytes) {
        BillParser parser = null;
        
        // 如果指定了平台，使用指定的解析器
        if (platform != null && !platform.trim().isEmpty()) {
            parser = getParserByCode(platform);
            if (parser == null) {
                throw new IllegalArgumentException("不支持的平台：" + platform);
            }
        } else {
            // 自动识别平台
            for (BillParser p : parsers) {
                try {
                    ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
                    if (p.canParse(fileName, bis, fileType)) {
                        parser = p;
                        log.info("自动识别到平台：{}", parser.getPlatformName());
                        break;
                    }
                } catch (Exception e) {
                    log.warn("解析器 {} 识别失败", p.getPlatformName(), e);
                    // 继续尝试下一个解析器
                }
            }
            
            if (parser == null) {
                throw new IllegalArgumentException("无法识别账单格式，请手动指定平台");
            }
        }
        
        return parser;
    }
    
    @Override
    public List<BillParser> getAllParsers() {
        return parsers;
    }
    
    @Override
    public BillParser getParserByCode(String platformCode) {
        return parsers.stream()
            .filter(p -> p.getPlatformCode().equals(platformCode))
            .findFirst()
            .orElse(null);
    }
}
