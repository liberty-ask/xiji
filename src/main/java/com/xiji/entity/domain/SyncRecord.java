package com.xiji.entity.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 端到端加密同步记录实体类
 * 服务端密文哑仓库：业务字段全部在 ciphertext 密文内，仅同步机制必要字段明文。
 * 注意：不继承 BaseEntity——updated_at 由客户端提供参与 LWW 仲裁（不走自动填充），
 * deleted 是墓碑业务字段（不走 @TableLogic，墓碑必须可查询、必须参与同步）。
 * @author liberty
 */
@TableName(value = "sync_records")
@Data
public class SyncRecord {

    /**
     * 记录ID（客户端生成UUIDv7）
     */
    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    /**
     * 家庭ID（明文，路由与鉴权用）
     */
    private String familyId;

    /**
     * 版本号（明文，LWW仲裁，高者胜）
     */
    private Long version;

    /**
     * 更新时间（明文，客户端提供，拉取游标）
     */
    private LocalDateTime updatedAt;

    /**
     * 墓碑标识：0-正常，1-已删除
     */
    private Integer deleted;

    /**
     * AES-GCM nonce
     */
    private byte[] nonce;

    /**
     * 业务密文（含 record_type + 业务 JSON）
     */
    private byte[] ciphertext;
}
