package com.xiji.entity.dto.response;

import lombok.Data;

/**
 * 同步拉取单条记录DTO
 * 业务字段全部为密文（nonce/ciphertext 为 base64 编码），updated_at 为客户端毫秒时间戳（UTC）。
 * @author liberty
 */
@Data
public class SyncPullRecord {

    /**
     * 记录ID（客户端生成UUIDv7）
     */
    private String id;

    /**
     * 版本号
     */
    private Long version;

    /**
     * 更新时间（客户端毫秒时间戳，UTC）
     */
    private Long updatedAt;

    /**
     * 墓碑标识：0-正常，1-已删除
     */
    private Integer deleted;

    /**
     * AES-GCM nonce（base64编码）
     */
    private String nonce;

    /**
     * 业务密文（base64编码，含record_type+业务JSON）
     */
    private String ciphertext;
}
