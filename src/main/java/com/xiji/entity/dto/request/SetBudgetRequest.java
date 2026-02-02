package com.xiji.entity.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 设置预算请求DTO
 * @author liberty
 */
@Data
public class SetBudgetRequest {
    
    @NotNull(message = "预算金额不能为空")
    @DecimalMin(value = "0", message = "预算金额不能为负数")
    @Digits(integer = 10, fraction = 2, message = "预算金额格式不正确，最多10位整数和2位小数")
    private BigDecimal total;
}
