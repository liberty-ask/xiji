package com.xiji.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiji.entity.domain.SyncRecord;
import com.xiji.entity.dto.request.SyncPushRecord;
import com.xiji.entity.dto.response.SyncPullRecord;
import com.xiji.entity.dto.response.SyncPullResponse;
import com.xiji.entity.dto.response.SyncPushItemResult;
import com.xiji.entity.dto.response.SyncPushResponse;
import com.xiji.mapper.SyncRecordMapper;
import com.xiji.service.SyncRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * 端到端加密同步记录服务实现
 * 密文哑仓库：LWW 仲裁（version 高者胜，同 version 看 updated_at 新者胜，完全相同幂等接受）、
 * (updated_at, id) 二元组游标分页拉取。服务端不解析密文内容。
 * @author liberty
 */
@Slf4j
@Service
public class SyncRecordServiceImpl extends ServiceImpl<SyncRecordMapper, SyncRecord>
        implements SyncRecordService {

    private static final int DEFAULT_PULL_LIMIT = 200;
    private static final int MAX_PULL_LIMIT = 2000;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SyncPushResponse push(String familyId, List<SyncPushRecord> records) {
        List<SyncPushItemResult> results = new ArrayList<>(records.size());
        for (SyncPushRecord record : records) {
            results.add(pushOne(familyId, record));
        }

        SyncPushResponse response = new SyncPushResponse();
        response.setTotal(records.size());
        long acceptedCount = results.stream().filter(SyncPushItemResult::isAccepted).count();
        response.setAccepted((int) acceptedCount);
        response.setRejected(records.size() - (int) acceptedCount);
        response.setResults(results);
        return response;
    }

    /**
     * 单条推送：按 id 幂等 upsert + LWW 仲裁
     */
    private SyncPushItemResult pushOne(String familyId, SyncPushRecord item) {
        SyncPushItemResult result = new SyncPushItemResult();
        result.setId(item.getId());
        result.setAccepted(false);

        SyncRecord incoming;
        try {
            incoming = toEntity(familyId, item);
        } catch (IllegalArgumentException e) {
            log.warn("同步记录base64解码失败，familyId={}，id={}", familyId, item.getId());
            return result;
        }

        SyncRecord existing = getById(item.getId());
        if (existing != null && !familyId.equals(existing.getFamilyId())) {
            // 同 ID 记录归属其他家庭，拒绝跨家庭覆盖
            log.warn("同步记录家庭不匹配，id={}，recordFamily={}，pushFamily={}",
                    item.getId(), existing.getFamilyId(), familyId);
            return result;
        }

        if (existing == null) {
            boolean ok = save(incoming);
            if (!ok) {
                log.error("同步记录插入失败，familyId={}，id={}", familyId, item.getId());
                return result;
            }
            result.setAccepted(true);
            return result;
        }

        int versionCompare = Long.compare(incoming.getVersion(), existing.getVersion());
        if (versionCompare == 0) {
            int timeCompare = Long.compare(item.getUpdatedAt(), toMillis(existing.getUpdatedAt()));
            if (timeCompare == 0) {
                // 幂等重复推送：同version同updated_at，视为已同步成功
                result.setAccepted(true);
                return result;
            }
            if (timeCompare < 0) {
                // 服务端记录更新，LWW拒绝覆盖
                return result;
            }
        } else if (versionCompare < 0) {
            // 服务端版本更高，LWW拒绝覆盖
            return result;
        }

        boolean ok = updateById(incoming);
        if (!ok) {
            log.error("同步记录更新失败，familyId={}，id={}", familyId, item.getId());
            return result;
        }
        result.setAccepted(true);
        return result;
    }

    @Override
    public SyncPullResponse pull(String familyId, Long cursorTime, String cursorId, Integer limit, String recordId) {
        SyncPullResponse response = new SyncPullResponse();

        // 精确拉取单条（vault 探测等专项场景）：忽略游标与分页，
        // 家庭归属由 familyId 条件保证（不匹配即空结果）
        if (recordId != null && !recordId.isEmpty()) {
            LambdaQueryWrapper<SyncRecord> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SyncRecord::getFamilyId, familyId)
                    .eq(SyncRecord::getId, recordId);
            List<SyncRecord> records = list(wrapper);
            response.setRecords(records.stream().map(this::toPullRecord).toList());
            response.setHasMore(false);
            return response;
        }

        int effectiveLimit = (limit == null || limit <= 0) ? DEFAULT_PULL_LIMIT : Math.min(limit, MAX_PULL_LIMIT);

        LambdaQueryWrapper<SyncRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SyncRecord::getFamilyId, familyId);
        if (cursorTime != null && cursorId != null) {
            LocalDateTime cursorAt = toLocalDateTime(cursorTime);
            // (updated_at, id) 二元组游标：严格大于游标位置，保证同毫秒记录不漏页
            wrapper.and(w -> w.gt(SyncRecord::getUpdatedAt, cursorAt)
                    .or(sub -> sub.eq(SyncRecord::getUpdatedAt, cursorAt)
                            .gt(SyncRecord::getId, cursorId)));
        }
        wrapper.orderByAsc(SyncRecord::getUpdatedAt)
                .orderByAsc(SyncRecord::getId)
                .last("LIMIT " + (effectiveLimit + 1));

        List<SyncRecord> records = list(wrapper);

        boolean hasMore = records.size() > effectiveLimit;
        if (hasMore) {
            records = records.subList(0, effectiveLimit);
        }
        response.setRecords(records.stream().map(this::toPullRecord).toList());
        response.setHasMore(hasMore);
        if (hasMore && !records.isEmpty()) {
            SyncRecord last = records.get(records.size() - 1);
            response.setNextCursorTime(toMillis(last.getUpdatedAt()));
            response.setNextCursorId(last.getId());
        }
        return response;
    }

    /**
     * 推送DTO -> 实体（base64解码为原始字节）
     */
    private SyncRecord toEntity(String familyId, SyncPushRecord item) {
        SyncRecord record = new SyncRecord();
        record.setId(item.getId());
        record.setFamilyId(familyId);
        record.setVersion(item.getVersion());
        record.setUpdatedAt(toLocalDateTime(item.getUpdatedAt()));
        record.setDeleted(item.getDeleted());
        record.setNonce(Base64.getDecoder().decode(item.getNonce()));
        record.setCiphertext(Base64.getDecoder().decode(item.getCiphertext()));
        return record;
    }

    /**
     * 实体 -> 拉取DTO（原始字节base64编码传输）
     */
    private SyncPullRecord toPullRecord(SyncRecord record) {
        SyncPullRecord item = new SyncPullRecord();
        item.setId(record.getId());
        item.setVersion(record.getVersion());
        item.setUpdatedAt(toMillis(record.getUpdatedAt()));
        item.setDeleted(record.getDeleted());
        item.setNonce(Base64.getEncoder().encodeToString(record.getNonce()));
        item.setCiphertext(Base64.getEncoder().encodeToString(record.getCiphertext()));
        return item;
    }

    private static LocalDateTime toLocalDateTime(Long epochMillis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneOffset.UTC);
    }

    private static Long toMillis(LocalDateTime updatedAt) {
        return updatedAt.toInstant(ZoneOffset.UTC).toEpochMilli();
    }
}
