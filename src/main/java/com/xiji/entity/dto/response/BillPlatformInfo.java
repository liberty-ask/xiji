package com.xiji.entity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 支持的账单平台信息
 * @author liberty
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BillPlatformInfo {
    
    /**
     * 平台代码（alipay, wechat, cmb等）
     */
    private String code;
    
    /**
     * 平台名称（支付宝、微信、招商银行等）
     */
    private String name;
    
    /**
     * 支持的文件格式
     */
    private List<String> supportedFormats;
    
    /**
     * 示例文件名
     */
    private String sampleFileName;
}


