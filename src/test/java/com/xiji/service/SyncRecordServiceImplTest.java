package com.xiji.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.xiji.entity.domain.SyncRecord;
import com.xiji.entity.dto.request.SyncPushRecord;
import com.xiji.entity.dto.response.SyncPullRecord;
import com.xiji.entity.dto.response.SyncPullResponse;
import com.xiji.entity.dto.response.SyncPushResponse;
import com.xiji.mapper.SyncRecordMapper;
import com.xiji.service.impl.SyncRecordServiceImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * SyncRecordServiceImpl测试：推送幂等与LWW仲裁（含墓碑不复活）、
 * (updated_at,id) 游标拉取分页与密文base64/毫秒时间戳映射。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SyncRecordServiceImplTest {

    @Mock
    private SyncRecordMapper syncRecordMapper;

    private SyncRecordServiceImpl syncRecordService;

    @BeforeAll
    static void initTableInfo() {
        // 无Spring上下文时初始化MP实体元数据，否则LambdaQueryWrapper无法解析列名
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), SyncRecord.class);
    }

    @BeforeEach
    void setUp() {
        syncRecordService = new SyncRecordServiceImpl();
        ReflectionTestUtils.setField(syncRecordService, "baseMapper", syncRecordMapper);
    }

    private SyncRecord existing(String id, Long familyId, long version, long updatedAtMillis, int deleted) {
        SyncRecord record = new SyncRecord();
        record.setId(id);
        record.setFamilyId(familyId);
        record.setVersion(version);
        record.setUpdatedAt(LocalDateTime.ofInstant(Instant.ofEpochMilli(updatedAtMillis), ZoneOffset.UTC));
        record.setDeleted(deleted);
        record.setNonce(new byte[]{1});
        record.setCiphertext(new byte[]{2});
        return record;
    }

    private SyncPushRecord pushItem(String id, long version, long updatedAtMillis, int deleted) {
        SyncPushRecord item = new SyncPushRecord();
        item.setId(id);
        item.setVersion(version);
        item.setUpdatedAt(updatedAtMillis);
        item.setDeleted(deleted);
        item.setNonce(Base64.getEncoder().encodeToString(new byte[]{1, 2, 3}));
        item.setCiphertext(Base64.getEncoder().encodeToString(new byte[]{4, 5, 6}));
        return item;
    }

    // ========== push：幂等与LWW仲裁 ==========

    @Test
    void push_newRecord_insertsAndAccepted() {
        when(syncRecordMapper.selectById("r1")).thenReturn(null);
        when(syncRecordMapper.insert(any(SyncRecord.class))).thenReturn(1);

        SyncPushResponse response = syncRecordService.push(1L, List.of(pushItem("r1", 1, 1000, 0)));

        assertEquals(1, response.getTotal());
        assertEquals(1, response.getAccepted());
        assertEquals(0, response.getRejected());
        assertTrue(response.getResults().get(0).isAccepted());
        verify(syncRecordMapper).insert(any(SyncRecord.class));
        verify(syncRecordMapper, never()).updateById(any(SyncRecord.class));
    }

    @Test
    void push_idempotentDuplicate_sameVersionSameUpdatedAt_acceptedWithoutWrite() {
        when(syncRecordMapper.selectById("r1")).thenReturn(existing("r1", 1L,2, 1000, 0));

        SyncPushResponse response = syncRecordService.push(1L, List.of(pushItem("r1", 2, 1000, 0)));

        assertTrue(response.getResults().get(0).isAccepted());
        verify(syncRecordMapper, never()).insert(any(SyncRecord.class));
        verify(syncRecordMapper, never()).updateById(any(SyncRecord.class));
    }

    @Test
    void push_higherVersion_overwrites() {
        when(syncRecordMapper.selectById("r1")).thenReturn(existing("r1", 1L,2, 1000, 0));
        when(syncRecordMapper.updateById(any(SyncRecord.class))).thenReturn(1);

        SyncPushResponse response = syncRecordService.push(1L, List.of(pushItem("r1", 3, 2000, 0)));

        assertTrue(response.getResults().get(0).isAccepted());
        verify(syncRecordMapper).updateById(any(SyncRecord.class));
    }

    @Test
    void push_lowerVersion_lwwRejected() {
        when(syncRecordMapper.selectById("r1")).thenReturn(existing("r1", 1L,3, 2000, 0));

        SyncPushResponse response = syncRecordService.push(1L, List.of(pushItem("r1", 2, 5000, 0)));

        assertFalse(response.getResults().get(0).isAccepted());
        verify(syncRecordMapper, never()).updateById(any(SyncRecord.class));
    }

    @Test
    void push_sameVersionNewerUpdatedAt_overwrites() {
        when(syncRecordMapper.selectById("r1")).thenReturn(existing("r1", 1L,2, 1000, 0));
        when(syncRecordMapper.updateById(any(SyncRecord.class))).thenReturn(1);

        SyncPushResponse response = syncRecordService.push(1L, List.of(pushItem("r1", 2, 2000, 0)));

        assertTrue(response.getResults().get(0).isAccepted());
        verify(syncRecordMapper).updateById(any(SyncRecord.class));
    }

    @Test
    void push_sameVersionOlderUpdatedAt_lwwRejected() {
        when(syncRecordMapper.selectById("r1")).thenReturn(existing("r1", 1L,2, 2000, 0));

        SyncPushResponse response = syncRecordService.push(1L, List.of(pushItem("r1", 2, 1000, 0)));

        assertFalse(response.getResults().get(0).isAccepted());
        verify(syncRecordMapper, never()).updateById(any(SyncRecord.class));
    }

    @Test
    void push_tombstoneHigherVersion_wins() {
        // 设备A删除（墓碑v3）胜过服务端现存编辑（v2）
        when(syncRecordMapper.selectById("r1")).thenReturn(existing("r1", 1L,2, 1000, 0));
        when(syncRecordMapper.updateById(any(SyncRecord.class))).thenReturn(1);

        SyncPushResponse response = syncRecordService.push(1L, List.of(pushItem("r1", 3, 2000, 1)));

        assertTrue(response.getResults().get(0).isAccepted());
        ArgumentCaptor<SyncRecord> captor = ArgumentCaptor.forClass(SyncRecord.class);
        verify(syncRecordMapper).updateById(captor.capture());
        assertEquals(1, captor.getValue().getDeleted());
    }

    @Test
    void push_staleEditAfterTombstone_notRevived() {
        // 服务端已是墓碑（v3），离线旧编辑（v2）后到，墓碑不复活
        when(syncRecordMapper.selectById("r1")).thenReturn(existing("r1", 1L,3, 2000, 1));

        SyncPushResponse response = syncRecordService.push(1L, List.of(pushItem("r1", 2, 5000, 0)));

        assertFalse(response.getResults().get(0).isAccepted());
        verify(syncRecordMapper, never()).updateById(any(SyncRecord.class));
    }

    @Test
    void push_familyMismatch_rejected() {
        // 同ID记录归属其他家庭，拒绝跨家庭覆盖
        when(syncRecordMapper.selectById("r1")).thenReturn(existing("r1", 2L, 1, 1000, 0));

        SyncPushResponse response = syncRecordService.push(1L, List.of(pushItem("r1", 2, 2000, 0)));

        assertFalse(response.getResults().get(0).isAccepted());
        verify(syncRecordMapper, never()).insert(any(SyncRecord.class));
        verify(syncRecordMapper, never()).updateById(any(SyncRecord.class));
    }

    @Test
    void push_invalidBase64_rejected() {
        SyncPushRecord item = pushItem("r1", 1, 1000, 0);
        item.setNonce("@@@非法@@@");

        SyncPushResponse response = syncRecordService.push(1L, List.of(item));

        assertEquals(1, response.getRejected());
        assertFalse(response.getResults().get(0).isAccepted());
        verify(syncRecordMapper, never()).insert(any(SyncRecord.class));
    }

    @Test
    void push_mixedBatch_perRecordArbitration() {
        when(syncRecordMapper.selectById("r1")).thenReturn(existing("r1", 1L,2, 1000, 0));
        when(syncRecordMapper.selectById("r2")).thenReturn(null);
        when(syncRecordMapper.updateById(any(SyncRecord.class))).thenReturn(1);
        when(syncRecordMapper.insert(any(SyncRecord.class))).thenReturn(1);

        // r1低version被拒、r2新记录被收
        SyncPushResponse response = syncRecordService.push(1L,
                List.of(pushItem("r1", 1, 5000, 0), pushItem("r2", 1, 1000, 0)));

        assertEquals(2, response.getTotal());
        assertEquals(1, response.getAccepted());
        assertEquals(1, response.getRejected());
        assertFalse(response.getResults().get(0).isAccepted());
        assertTrue(response.getResults().get(1).isAccepted());
    }

    // ========== pull：游标分页与映射 ==========

    @Test
    void pull_full_noCursor_queriesFromStart() {
        when(syncRecordMapper.selectList(any())).thenReturn(List.of(
                existing("r1", 1L,1, 1000, 0)));

        SyncPullResponse response = syncRecordService.pull(1L, null, null, null, null);

        assertEquals(1, response.getRecords().size());
        assertFalse(response.isHasMore());
        assertNull(response.getNextCursorTime());
        assertNull(response.getNextCursorId());
        ArgumentCaptor<LambdaQueryWrapper<SyncRecord>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(syncRecordMapper).selectList(captor.capture());
        String sql = captor.getValue().getSqlSegment();
        assertTrue(sql.contains("family_id"));
        assertTrue(sql.contains("LIMIT 201"));
    }

    @Test
    void pull_withCursor_buildsTupleCondition() {
        when(syncRecordMapper.selectList(any())).thenReturn(List.of());

        syncRecordService.pull(1L, 5000L, "abc", 50, null);

        ArgumentCaptor<LambdaQueryWrapper<SyncRecord>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(syncRecordMapper).selectList(captor.capture());
        String sql = captor.getValue().getSqlSegment();
        // (updated_at > ? OR (updated_at = ? AND id > ?))
        assertTrue(sql.contains("updated_at"));
        assertTrue(sql.contains("OR"));
        assertTrue(sql.contains("id"));
        assertTrue(sql.contains("LIMIT 51"));
    }

    @Test
    void pull_moreRecords_setsNextCursorAndTrimsPage() {
        when(syncRecordMapper.selectList(any())).thenReturn(List.of(
                existing("r1", 1L,1, 1000, 0),
                existing("r2", 1L,2, 2000, 0),
                existing("r3", 1L,3, 3000, 1)));

        SyncPullResponse response = syncRecordService.pull(1L, null, null, 2, null);

        assertTrue(response.isHasMore());
        assertEquals(2, response.getRecords().size());
        assertEquals(2000L, response.getNextCursorTime());
        assertEquals("r2", response.getNextCursorId());
        // 墓碑记录正常返回（deleted=1 参与同步）
        assertEquals(0, response.getRecords().get(0).getDeleted());
    }

    @Test
    void pull_lastPage_noMore_noNextCursor() {
        when(syncRecordMapper.selectList(any())).thenReturn(List.of(
                existing("r1", 1L,1, 1000, 0),
                existing("r2", 1L,2, 2000, 0)));

        SyncPullResponse response = syncRecordService.pull(1L, null, null, 2, null);

        assertFalse(response.isHasMore());
        assertNull(response.getNextCursorTime());
        assertNull(response.getNextCursorId());
        assertEquals(2, response.getRecords().size());
    }

    @Test
    void pull_mapsBase64AndMillisRoundTrip() {
        SyncRecord record = existing("r1", 1L,7, 1698163200123L, 1);
        byte[] nonce = {10, 20, 30};
        byte[] ciphertext = {40, 50, 60, 70};
        record.setNonce(nonce);
        record.setCiphertext(ciphertext);
        when(syncRecordMapper.selectList(any())).thenReturn(List.of(record));

        SyncPullResponse response = syncRecordService.pull(1L, null, null, 10, null);

        SyncPullRecord item = response.getRecords().get(0);
        assertEquals("r1", item.getId());
        assertEquals(7L, item.getVersion());
        assertEquals(1698163200123L, item.getUpdatedAt());
        assertEquals(1, item.getDeleted());
        assertEquals(Base64.getEncoder().encodeToString(nonce), item.getNonce());
        assertEquals(Base64.getEncoder().encodeToString(ciphertext), item.getCiphertext());
    }

    // ========== pull：recordId 精确拉取（vault 探测）与 limit 上限 ==========

    @Test
    void pull_byRecordId_buildsExactConditionIgnoringCursorAndLimit() {
        when(syncRecordMapper.selectList(any())).thenReturn(List.of(
                existing("vault-u1", 1L,3, 1, 0)));

        SyncPullResponse response =
                syncRecordService.pull(1L, 9999L, "zzz", 5000, "vault-u1");

        assertEquals(1, response.getRecords().size());
        assertEquals("vault-u1", response.getRecords().get(0).getId());
        assertFalse(response.isHasMore());
        assertNull(response.getNextCursorTime());
        ArgumentCaptor<LambdaQueryWrapper<SyncRecord>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(syncRecordMapper).selectList(captor.capture());
        String sql = captor.getValue().getSqlSegment();
        // 双条件：家庭归属 + 精确 id；无游标元组、无 LIMIT 分页
        assertTrue(sql.contains("family_id"));
        assertTrue(sql.contains("id"));
        assertFalse(sql.contains("OR"));
        assertFalse(sql.contains("LIMIT"));
    }

    @Test
    void pull_byRecordId_notFound_returnsEmpty() {
        when(syncRecordMapper.selectList(any())).thenReturn(List.of());

        SyncPullResponse response =
                syncRecordService.pull(1L, null, null, null, "vault-none");

        assertTrue(response.getRecords().isEmpty());
        assertFalse(response.isHasMore());
    }

    @Test
    void pull_limitAboveMax_clampedTo2000() {
        when(syncRecordMapper.selectList(any())).thenReturn(List.of());

        syncRecordService.pull(1L, null, null, 99999, null);

        ArgumentCaptor<LambdaQueryWrapper<SyncRecord>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(syncRecordMapper).selectList(captor.capture());
        String sql = captor.getValue().getSqlSegment();
        assertTrue(sql.contains("LIMIT 2001"));
    }
}
