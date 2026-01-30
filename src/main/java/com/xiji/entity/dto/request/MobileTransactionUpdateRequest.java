package com.xiji.entity.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 手机端更新交易请求DTO
 * @author liberty
 */
@Data
public class MobileTransactionUpdateRequest {
    
    /**
     * 交易类型：1(支出) 或 0(收入)
     */
    @NotNull(message = "交易类型不能为空")
    @Max(value = 1, message = "交易类型不正确")
    @Min(value = 0, message = "交易类型不正确")
    private Integer type;
    
    /**
     * 金额（单位：元）
     */
    @NotNull(message = "金额不能为空")
    @DecimalMin(value = "0.01", message = "金额必须大于0")
    @DecimalMax(value = "1000000", message = "单笔交易金额不能超过1000000")
    @Digits(integer = 10, fraction = 2, message = "金额格式不正确，最多10位整数和2位小数")
    private BigDecimal amount;
    
    /**
     * 分类名称
     */
    @NotBlank(message = "分类不能为空")
    private String category;
    
    /**
     * 日期，格式：YYYY-MM-DD
     */
    @NotBlank(message = "日期不能为空")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "日期格式不正确，应为YYYY-MM-DD")
    private String date;
    
    /**
     * 备注（可选）
     */
    private String note;
    
    /**
     * 位置（可选）
     */
    private String location;
}

