package com.xiji.utils;

import com.xiji.config.CustomConfig;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtUtils单元测试
 */
class JwtUtilsTest {

    private static final String SIGN_KEY = "unit-test-sign-key-with-at-least-32-bytes!";

    @BeforeAll
    static void setUp() {
        CustomConfig config = new CustomConfig();
        CustomConfig.JwtConfig jwtConfig = new CustomConfig.JwtConfig();
        jwtConfig.setSignKey(SIGN_KEY);
        jwtConfig.setExpire(604800000L);
        jwtConfig.setAccessExpire(7200000L);
        config.setJwt(jwtConfig);
        new JwtUtils().setCustomConfig(config);
    }

    private Map<String, Object> claims() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", 1L);
        claims.put("username", "testuser");
        return claims;
    }

    @Test
    void generateWithCustomExpire_andParse_success() {
        String jwt = JwtUtils.generateJwt(claims(), 3600000L);
        assertNotNull(jwt);

        Claims parsed = JwtUtils.parseJwt(jwt);
        assertNotNull(parsed);
        assertEquals(1L, ((Number) parsed.get("id")).longValue());
        assertEquals("testuser", parsed.get("username"));
    }

    @Test
    void expiredToken_parse_returnsNull() {
        String jwt = JwtUtils.generateJwt(claims(), -1000L);
        assertNull(JwtUtils.parseJwt(jwt));
    }

    @Test
    void invalidToken_parse_returnsNull() {
        assertNull(JwtUtils.parseJwt("not-a-jwt"));
    }

    @Test
    void webGenerate_usesConfiguredExpire() {
        String jwt = JwtUtils.generateJwt(claims());
        Claims parsed = JwtUtils.parseJwt(jwt);
        assertNotNull(parsed);

        long remaining = parsed.getExpiration().getTime() - System.currentTimeMillis();
        // web端过期时间为7天，剩余时间应接近604800000ms（允许10秒误差）
        assertTrue(remaining > 604800000L - 10000 && remaining <= 604800000L,
                "web端token剩余时间应接近7天，实际=" + remaining);
    }

    @Test
    void accessExpire_returnsConfiguredValue() {
        assertEquals(7200000L, JwtUtils.getAccessExpire());
    }
}
