package com.xiji.controller;

import com.xiji.config.CustomConfig;
import com.xiji.entity.domain.User;
import com.xiji.service.FamilyMemberService;
import com.xiji.service.FamilyService;
import com.xiji.service.RefreshTokenService;
import com.xiji.service.SmsService;
import com.xiji.service.UserService;
import com.xiji.utils.JwtUtils;
import com.xiji.utils.LoginAttemptUtils;
import com.xiji.utils.PasswordUtils;
import org.junit.jupiter.api.BeforeAll;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MobileAuthController接口测试：refresh轮换 / logout撤销 / 登录双token签发
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MobileAuthControllerTest {

    @Mock
    private UserService userService;
    @Mock
    private SmsService smsService;
    @Mock
    private FamilyService familyService;
    @Mock
    private FamilyMemberService familyMemberService;
    @Mock
    private LoginAttemptUtils loginAttemptUtils;
    @Mock
    private RefreshTokenService refreshTokenService;

    private MockMvc mockMvc;

    @BeforeAll
    static void setUpJwt() {
        CustomConfig config = new CustomConfig();
        CustomConfig.JwtConfig jwtConfig = new CustomConfig.JwtConfig();
        jwtConfig.setSignKey("unit-test-sign-key-with-at-least-32-bytes!");
        jwtConfig.setExpire(604800000L);
        config.setJwt(jwtConfig);
        new JwtUtils().setCustomConfig(config);
    }

    @BeforeEach
    void setUp() {
        MobileAuthController controller = new MobileAuthController(
                userService, smsService, familyService, familyMemberService, loginAttemptUtils, refreshTokenService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private User mockUser() {
        User user = new User();
        user.setId("1");
        user.setUsername("13800000000");
        user.setName("测试用户");
        user.setPhone("13800000000");
        user.setPassword(PasswordUtils.encode("123456"));
        user.setStatus(0);
        return user;
    }

    // ========== /auth/refresh：spec"访问令牌刷新与轮换"3个场景 ==========

    @Test
    void refresh_validToken_returnsNewTokensAndRotates() throws Exception {
        when(refreshTokenService.validate("valid-rt")).thenReturn("1");
        when(userService.getById("1")).thenReturn(mockUser());
        when(refreshTokenService.rotate("valid-rt", "1")).thenReturn("new-rt");

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"valid-rt\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").value("new-rt"));

        verify(refreshTokenService).rotate("valid-rt", "1");
    }

    @Test
    void refresh_rotatedOldToken_returns401() throws Exception {
        // 已被轮换替换的旧refresh token（服务端已删除）
        when(refreshTokenService.validate("old-rt")).thenReturn(null);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"old-rt\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("重新登录")));
    }

    @Test
    void refresh_noRefreshToken_returns401WithDistinguishableMessage() throws Exception {
        // 无refresh token的会话
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("缺少刷新凭证")));
    }

    @Test
    void refresh_disabledUser_returns401() throws Exception {
        when(refreshTokenService.validate("rt")).thenReturn("1");
        User disabled = mockUser();
        disabled.setStatus(1);
        when(userService.getById("1")).thenReturn(disabled);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"rt\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    // ========== /auth/login：双token签发 ==========

    @Test
    void loginPassword_returnsDualTokens() throws Exception {
        when(userService.getOne(any())).thenReturn(mockUser());
        when(loginAttemptUtils.isLoginBlocked(anyString())).thenReturn(false);
        when(refreshTokenService.generate("1")).thenReturn("login-rt");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"password\",\"account\":\"13800000000\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                // 仅返回双token，无legacy token字段
                .andExpect(jsonPath("$.data.token").doesNotExist())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").value("login-rt"))
                .andExpect(jsonPath("$.data.user.id").value("1"));

        verify(refreshTokenService).generate("1");
    }

    // ========== /auth/logout：全端撤销与幂等 ==========

    @Test
    void logout_revokesAllRefreshTokens() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .requestAttr("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(refreshTokenService).revokeAll("1");
    }

    @Test
    void logout_repeat_isIdempotent() throws Exception {
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/v1/auth/logout")
                            .requestAttr("userId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }
        verify(refreshTokenService, times(2)).revokeAll("1");
    }
}
