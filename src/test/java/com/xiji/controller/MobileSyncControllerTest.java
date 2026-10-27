package com.xiji.controller;

import com.xiji.entity.dto.response.SyncPullRecord;
import com.xiji.entity.dto.response.SyncPullResponse;
import com.xiji.entity.dto.response.SyncPushItemResult;
import com.xiji.entity.dto.response.SyncPushResponse;
import com.xiji.service.FamilyMemberService;
import com.xiji.service.SyncRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MobileSyncController接口测试：家庭成员归属校验、未登录拦截、推/拉参数传递。
 * LWW仲裁与幂等逻辑见 SyncRecordServiceImplTest。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MobileSyncControllerTest {

    @Mock
    private SyncRecordService syncRecordService;
    @Mock
    private FamilyMemberService familyMemberService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MobileSyncController controller = new MobileSyncController(syncRecordService, familyMemberService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private static final String PUSH_BODY =
            "{\"familyId\":\"1\",\"records\":[{"
                    + "\"id\":\"r1\",\"version\":1,\"updatedAt\":1000,\"deleted\":0,"
                    + "\"nonce\":\"AQID\",\"ciphertext\":\"BAUG\"}]}";

    private SyncPushResponse pushResponse() {
        SyncPushResponse response = new SyncPushResponse();
        response.setTotal(1);
        response.setAccepted(1);
        response.setRejected(0);
        response.setResults(List.of(new SyncPushItemResult("r1", true)));
        return response;
    }

    private SyncPullResponse pullResponse() {
        SyncPullRecord record = new SyncPullRecord();
        record.setId("r1");
        record.setVersion(1L);
        record.setUpdatedAt(1000L);
        record.setDeleted(0);
        record.setNonce("AQID");
        record.setCiphertext("BAUG");

        SyncPullResponse response = new SyncPullResponse();
        response.setRecords(List.of(record));
        response.setHasMore(false);
        return response;
    }

    // ========== push ==========

    @Test
    void push_success_returnsPerRecordResults() throws Exception {
        when(familyMemberService.isMemberOfFamily(1L, 7L)).thenReturn(true);
        when(syncRecordService.push(eq(1L), anyList())).thenReturn(pushResponse());

        mockMvc.perform(post("/api/v1/sync/push")
                        .requestAttr("userId", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PUSH_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.accepted").value(1))
                .andExpect(jsonPath("$.data.results[0].id").value("r1"))
                .andExpect(jsonPath("$.data.results[0].accepted").value(true));

        verify(syncRecordService).push(eq(1L), anyList());
    }

    @Test
    void push_notFamilyMember_rejected() throws Exception {
        when(familyMemberService.isMemberOfFamily(1L, 7L)).thenReturn(false);

        mockMvc.perform(post("/api/v1/sync/push")
                        .requestAttr("userId", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PUSH_BODY))
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("无该家庭的访问权限"));

        verify(syncRecordService, never()).push(anyLong(), anyList());
    }

    @Test
    void push_notLoggedIn_rejected() throws Exception {
        mockMvc.perform(post("/api/v1/sync/push")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PUSH_BODY))
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("用户未登录"));

        verify(syncRecordService, never()).push(anyLong(), anyList());
    }

    // ========== pull ==========

    @Test
    void pull_success_passesCursorAndLimit() throws Exception {
        when(familyMemberService.isMemberOfFamily(1L, 7L)).thenReturn(true);
        when(syncRecordService.pull(1L, 123L, "abc", 50, null)).thenReturn(pullResponse());

        mockMvc.perform(get("/api/v1/sync/pull")
                        .requestAttr("userId", 7L)
                        .param("familyId", "1")
                        .param("cursorTime", "123")
                        .param("cursorId", "abc")
                        .param("limit", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].id").value("r1"))
                .andExpect(jsonPath("$.data.records[0].nonce").value("AQID"))
                .andExpect(jsonPath("$.data.hasMore").value(false));

        verify(syncRecordService).pull(1L, 123L, "abc", 50, null);
    }

    @Test
    void pull_newDevice_noCursor_fullPull() throws Exception {
        when(familyMemberService.isMemberOfFamily(1L, 7L)).thenReturn(true);
        when(syncRecordService.pull(1L, null, null, null, null)).thenReturn(pullResponse());

        mockMvc.perform(get("/api/v1/sync/pull")
                        .requestAttr("userId", 7L)
                        .param("familyId", "1"))
                .andExpect(jsonPath("$.code").value(200));

        verify(syncRecordService).pull(1L, null, null, null, null);
    }

    @Test
    void pull_byRecordId_passesThrough() throws Exception {
        when(familyMemberService.isMemberOfFamily(1L, 7L)).thenReturn(true);
        when(syncRecordService.pull(1L, null, null, null, "vault-u1")).thenReturn(pullResponse());

        mockMvc.perform(get("/api/v1/sync/pull")
                        .requestAttr("userId", 7L)
                        .param("familyId", "1")
                        .param("recordId", "vault-u1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(syncRecordService).pull(1L, null, null, null, "vault-u1");
    }

    @Test
    void pull_notFamilyMember_rejected() throws Exception {
        when(familyMemberService.isMemberOfFamily(1L, 7L)).thenReturn(false);

        mockMvc.perform(get("/api/v1/sync/pull")
                        .requestAttr("userId", 7L)
                        .param("familyId", "1"))
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("无该家庭的访问权限"));

        verify(syncRecordService, never()).pull(anyLong(), any(), anyString(), any(), any());
    }
}
