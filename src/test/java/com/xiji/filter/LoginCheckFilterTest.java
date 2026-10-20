package com.xiji.filter;

import com.xiji.config.CustomConfig;
import com.xiji.utils.JwtUtils;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LoginCheckFilter单元测试
 */
class LoginCheckFilterTest {

    private static final String SIGN_KEY = "unit-test-sign-key-with-at-least-32-bytes!";

    private LoginCheckFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain chain;

    @BeforeAll
    static void setUpJwt() {
        CustomConfig config = new CustomConfig();
        CustomConfig.JwtConfig jwtConfig = new CustomConfig.JwtConfig();
        jwtConfig.setSignKey(SIGN_KEY);
        jwtConfig.setExpire(604800000L);
        config.setJwt(jwtConfig);
        new JwtUtils().setCustomConfig(config);
    }

    @BeforeEach
    void setUp() {
        filter = new LoginCheckFilter();
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/bill/list");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
    }

    private String generateToken(long expireMs) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", 1L);
        claims.put("username", "testuser");
        return JwtUtils.generateJwt(claims, expireMs);
    }

    @Test
    void noToken_returns401WithBodyCode() throws ServletException, IOException {
        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        String body = response.getContentAsString();
        assertTrue(body.contains("\"code\":401"));
        assertTrue(body.contains("请先登录"));
        assertNull(chain.getRequest());
    }

    @Test
    void expiredToken_returns401WithBodyCode() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer " + generateToken(-1000L));
        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"code\":401"));
        assertNull(chain.getRequest());
    }

    @Test
    void validToken_passes_andSetsAttributes() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer " + generateToken(3600000L));
        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertNotNull(chain.getRequest());
        assertEquals(1L, chain.getRequest().getAttribute("userId"));
        assertEquals("testuser", chain.getRequest().getAttribute("username"));
    }

    @Test
    void tokenHeader_passes() throws ServletException, IOException {
        // 仅token请求头（web端调用方式）
        request.addHeader("token", generateToken(604800000L));
        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertNotNull(chain.getRequest());
        assertEquals(1L, chain.getRequest().getAttribute("userId"));
    }

    @Test
    void refreshPath_whitelisted_noTokenRequired() throws ServletException, IOException {
        request.setRequestURI("/api/v1/auth/refresh");
        request.addHeader("Content-Type", "application/json");
        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertNotNull(chain.getRequest());
    }

    @Test
    void loginPath_whitelisted_noTokenRequired() throws ServletException, IOException {
        request.setRequestURI("/api/v1/auth/login");
        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertNotNull(chain.getRequest());
    }
}
