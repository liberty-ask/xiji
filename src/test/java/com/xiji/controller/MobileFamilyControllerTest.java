package com.xiji.controller;

import com.xiji.entity.domain.User;
import com.xiji.service.FamilyApplicationService;
import com.xiji.service.FamilyMemberService;
import com.xiji.service.FamilyService;
import com.xiji.service.RefreshTokenService;
import com.xiji.service.UserService;
import com.xiji.utils.PasswordUtils;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MobileFamilyController接口测试：修改密码后全端撤销refresh token
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MobileFamilyControllerTest {

    @Mock
    private UserService userService;
    @Mock
    private FamilyService familyService;
    @Mock
    private FamilyApplicationService familyApplicationService;
    @Mock
    private FamilyMemberService familyMemberService;
    @Mock
    private RefreshTokenService refreshTokenService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MobileFamilyController controller = new MobileFamilyController(
                userService, familyService, familyApplicationService,
                familyMemberService, refreshTokenService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void changePassword_success_revokesAllRefreshTokens() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPassword(PasswordUtils.encode("old123"));
        when(userService.getById("1")).thenReturn(user);
        when(userService.updateById(any(User.class))).thenReturn(true);

        mockMvc.perform(post("/api/v1/user/change-password")
                        .requestAttr("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"old123\",\"newPassword\":\"new123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(refreshTokenService).revokeAll("1");
    }

    @Test
    void changePassword_wrongOldPassword_noRevoke() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPassword(PasswordUtils.encode("old123"));
        when(userService.getById("1")).thenReturn(user);

        mockMvc.perform(post("/api/v1/user/change-password")
                        .requestAttr("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"wrongpwd\",\"newPassword\":\"new123456\"}"))
                .andExpect(jsonPath("$.code").value(500));

        verify(refreshTokenService, never()).revokeAll(any());
    }

    // ── 8.2：同步开关投影上报（开关切换时全家庭行更新） ──

    @Test
    void reportSyncStatus_enabled_updatesAllFamilyRows() throws Exception {
        when(familyMemberService.updateSyncEnabledForAllFamilies("1", 1)).thenReturn(true);

        mockMvc.perform(post("/api/v1/families/sync-status")
                        .requestAttr("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(familyMemberService).updateSyncEnabledForAllFamilies("1", 1);
    }

    @Test
    void reportSyncStatus_disabled_projectsZero() throws Exception {
        when(familyMemberService.updateSyncEnabledForAllFamilies("1", 0)).thenReturn(true);

        mockMvc.perform(post("/api/v1/families/sync-status")
                        .requestAttr("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(familyMemberService).updateSyncEnabledForAllFamilies("1", 0);
    }

    @Test
    void reportSyncStatus_missingEnabled_rejected() throws Exception {
        mockMvc.perform(post("/api/v1/families/sync-status")
                        .requestAttr("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(jsonPath("$.code").value(500));

        verify(familyMemberService, never()).updateSyncEnabledForAllFamilies(any(), any());
    }

    @Test
    void reportSyncStatus_notSignedIn_rejected() throws Exception {
        mockMvc.perform(post("/api/v1/families/sync-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true}"))
                .andExpect(jsonPath("$.code").value(500));

        verify(familyMemberService, never()).updateSyncEnabledForAllFamilies(any(), any());
    }
}
