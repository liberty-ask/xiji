package com.xiji.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiji.entity.domain.SyncRecord;
import com.xiji.entity.dto.request.SyncPushRecord;
import com.xiji.entity.dto.response.SyncPullResponse;
import com.xiji.entity.dto.response.SyncPushResponse;

import java.util.List;

/**
 * 端到端加密同步记录服务
 * 服务端为密文哑仓库：只做按 id 幂等 upsert、LWW 仲裁与游标分页拉取，不理解业务内容。
 * @author liberty
 */
public interface SyncRecordService extends IService<SyncRecord> {

    /**
     * 批量推送同步记录
     * 按 id 幂等 upsert；服务端已有更高 version（或同 version 更新 updated_at）的记录时拒绝覆盖（LWW），
     * 幂等重复推送（同 version 同 updated_at）直接视为接受。
     *
     * @param familyId 家庭ID（调用方已完成成员归属校验）
     * @param records  推送记录列表
     * @return 逐条接受/拒绝结果
     */
    SyncPushResponse push(String familyId, List<SyncPushRecord> records);

    /**
     * 按游标增量拉取同步记录
     * 按 (updated_at, id) 升序分页；游标缺省时从头全量拉取（新设备恢复）。
     *
     * @param familyId    家庭ID（调用方已完成成员归属校验）
     * @param cursorTime  游标-更新时间（毫秒时间戳，UTC），null 表示全量
     * @param cursorId    游标-记录ID，null 表示全量
     * @param limit       每页条数，null/非正数取默认值，超过上限截断
     * @return 本页记录与下一页游标
     */
    SyncPullResponse pull(String familyId, Long cursorTime, String cursorId, Integer limit);
}
