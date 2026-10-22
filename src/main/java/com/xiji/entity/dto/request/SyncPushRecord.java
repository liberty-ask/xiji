package com.xiji.entity.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 同步推送单条记录DTO
 * 业务字段全部以 AES-GCM 密文传输（nonce/ciphertext 为 base64 编码），
 * updated_at 为客户端毫秒时间戳（UTC），与 version 共同参与 LWW 仲裁。
 * @author liberty
 */
@Data
public class SyncPushRecord {

    /**
     * 记录ID（客户端生成UUIDv7）
     */
    @NotBlank(message = "记录ID不能为空")
    @Size(max = 36, message = "记录ID长度不能超过36")
    private String id;

    /**
     * 版本号（LWW仲裁，高者胜）
     */
    @NotNull(message = "版本号不能为空")
    @PositiveOrZero(message = "版本号不能为负数")
    private Long version;

    /**
     * 更新时间（客户端毫秒时间戳，UTC）
     */
    @NotNull(message = "更新时间不能为空")
    @PositiveOrZero(message = "更新时间不能为负数")
    private Long updatedAt;

    /**
     * 墓碑标识：0-正常，1-已删除
     */
    @NotNull(message = "墓碑标识不能为空")
    @Min(value = 0, message = "墓碑标识只能为0或1")
    @Max(value = 1, message = "墓碑标识只能为0或1")
    private Integer deleted;

    /**
     * AES-GCM nonce（base64编码）
     */
    @NotBlank(message = "nonce不能为空")
    private String nonce;

    /**
     * 业务密文（base64编码，含record_type+业务JSON）
     */
    @NotBlank(message = "密文不能为空")
    private String ciphertext;
}
