package com.xiji.entity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 同步推送单条结果DTO
 * accepted=false 表示服务端已有更高version/更新updated_at的记录（LWW拒绝覆盖），
 * 客户端应随后拉取该记录完成对账收敛。
 * @author liberty
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SyncPushItemResult {

    /**
     * 记录ID
     */
    private String id;

    /**
     * 是否接受：true-已落库/幂等重复，false-LWW仲裁拒绝
     */
    private boolean accepted;
}
