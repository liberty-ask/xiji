package com.xiji.entity.dto.response;

import com.xiji.entity.domain.BillTask;
import lombok.Data;

/**
 * 包含解析结果的账单任务响应类
 * 用于查询任务状态时，返回任务状态和解析结果
 * @author liberty
 */
@Data
public class BillTaskWithResultResponse {

    /**
     * 任务信息
     */
    private BillTask task;

    /**
     * 解析结果（仅当任务类型为上传解析且状态为成功时包含）
     */
    private BillParseResult parseResult;
}
