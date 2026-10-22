package com.xiji.entity.dto.response;

import lombok.Data;

import java.util.List;

/**
 * 同步推送响应DTO
 * @author liberty
 */
@Data
public class SyncPushResponse {

    /**
     * 推送总条数
     */
    private int total;

    /**
     * 接受条数（含幂等重复推送）
     */
    private int accepted;

    /**
     * 拒绝条数（LWW仲裁：服务端已有更高版本）
     */
    private int rejected;

    /**
     * 逐条结果
     */
    private List<SyncPushItemResult> results;
}
