package com.xiji.entity.dto.response;

import lombok.Data;

import java.util.List;

/**
 * 同步拉取响应DTO
 * 按 (updated_at, id) 升序分页返回；hasMore=true 时用 nextCursorTime/nextCursorId 继续翻页；
 * 游标缺省（新设备）从头全量拉取。
 * @author liberty
 */
@Data
public class SyncPullResponse {

    /**
     * 本页记录列表
     */
    private List<SyncPullRecord> records;

    /**
     * 是否还有下一页
     */
    private boolean hasMore;

    /**
     * 下一页游标-更新时间（毫秒时间戳，UTC），hasMore=false时为null
     */
    private Long nextCursorTime;

    /**
     * 下一页游标-记录ID，hasMore=false时为null
     */
    private String nextCursorId;
}
