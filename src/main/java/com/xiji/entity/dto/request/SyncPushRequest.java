package com.xiji.entity.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 同步推送请求DTO
 * 客户端发件箱联网后批量推送变更记录，按家庭维度整批推送。
 * @author liberty
 */
@Data
public class SyncPushRequest {

    /**
     * 推送目标家庭ID
     */
    @NotNull(message = "家庭ID不能为空")
    private Long familyId;

    /**
     * 推送的记录列表
     */
    @Valid
    @NotEmpty(message = "推送记录列表不能为空")
    @Size(max = 500, message = "单次推送最多500条记录")
    private List<SyncPushRecord> records;
}
