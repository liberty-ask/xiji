package com.xiji.service;

import com.xiji.config.CustomConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RefreshTokenService单元测试（基于内存模拟的RedisTemplate）
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RefreshTokenServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOps;
    @Mock
    private SetOperations<String, Object> setOps;

    private RefreshTokenService service;

    // 模拟Redis存储
    private final Map<String, Object> valueStore = new HashMap<>();
    private final Map<String, Set<Object>> setStore = new HashMap<>();
    // 记录set操作传入的TTL（秒）
    private long lastSetTtlSeconds = -1;

    @BeforeEach
    void setUp() {
        CustomConfig config = new CustomConfig();
        CustomConfig.JwtConfig jwtConfig = new CustomConfig.JwtConfig();
        jwtConfig.setRefreshExpire(2592000000L);
        config.setJwt(jwtConfig);
        CustomConfig.RedisConfig redisConfig = new CustomConfig.RedisConfig();
        redisConfig.setKeyPrefix("family:");
        config.setRedis(redisConfig);

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(redisTemplate.opsForSet()).thenReturn(setOps);
        doAnswer(inv -> {
            valueStore.put(inv.getArgument(0), inv.getArgument(1));
            lastSetTtlSeconds = inv.getArgument(2);
            return null;
        }).when(valueOps).set(anyString(), any(), anyLong(), any(TimeUnit.class));
        when(valueOps.get(anyString())).thenAnswer(inv -> valueStore.get(inv.getArgument(0)));
        doAnswer(inv -> {
            setStore.computeIfAbsent(inv.getArgument(0), k -> new HashSet<>()).add(inv.getArgument(1));
            return 1L;
        }).when(setOps).add(anyString(), any());
        doAnswer(inv -> {
            Set<Object> members = setStore.get(inv.getArgument(0));
            if (members != null) {
                members.remove(inv.getArgument(1));
            }
            return 1L;
        }).when(setOps).remove(anyString(), any());
        when(setOps.members(anyString())).thenAnswer(inv -> {
            Set<Object> members = setStore.get(inv.getArgument(0));
            return members != null ? new HashSet<>(members) : null;
        });
        when(redisTemplate.delete(anyString())).thenAnswer(inv -> {
            String key = inv.getArgument(0);
            boolean existed = valueStore.remove(key) != null;
            setStore.remove(key);
            return existed;
        });
        doAnswer(inv -> {
            Collection<?> keys = inv.getArgument(0);
            keys.forEach(k -> valueStore.remove(k.toString()));
            return null;
        }).when(redisTemplate).delete(anyCollection());
        when(redisTemplate.expire(anyString(), anyLong(), any())).thenReturn(true);

        valueStore.clear();
        setStore.clear();
        service = new RefreshTokenService(redisTemplate, config);
    }

    @Test
    void generate_storesTokenWith30dTtl_andIndex() {
        String token = service.generate("1");

        assertNotNull(token);
        assertFalse(token.isEmpty());
        // token key写入，TTL为30天（2592000秒）
        assertEquals("1", valueStore.get("family:refresh:" + token));
        assertEquals(2592000L, lastSetTtlSeconds);
        // 用户索引写入
        assertTrue(setStore.get("family:refresh-user:1").contains(token));
    }

    @Test
    void validate_validToken_returnsUserId() {
        String token = service.generate("42");
        assertEquals("42", service.validate(token));
    }

    @Test
    void validate_unknownToken_returnsNull() {
        assertNull(service.validate("no-such-token"));
    }

    @Test
    void validate_nullOrEmpty_returnsNull() {
        assertNull(service.validate(null));
        assertNull(service.validate(""));
    }

    @Test
    void rotate_oldTokenInvalid_newTokenValid() {
        String oldToken = service.generate("1");
        String newToken = service.rotate(oldToken, "1");

        assertNotEquals(oldToken, newToken);
        // 旧token已删除（key不存在即失效）
        assertNull(service.validate(oldToken));
        // 新token有效
        assertEquals("1", service.validate(newToken));
        // 旧token已从索引移除
        assertFalse(setStore.get("family:refresh-user:1").contains(oldToken));
    }

    @Test
    void revokeAll_allTokensInvalidated() {
        String token1 = service.generate("1");
        String token2 = service.generate("1");
        String otherUserToken = service.generate("2");

        service.revokeAll("1");

        assertNull(service.validate(token1));
        assertNull(service.validate(token2));
        // 其他用户不受影响
        assertEquals("2", service.validate(otherUserToken));
        // 索引key已删除
        assertFalse(setStore.containsKey("family:refresh-user:1"));
    }

    @Test
    void revokeAll_noTokens_isIdempotent() {
        assertDoesNotThrow(() -> service.revokeAll("99"));
    }

    @Test
    void expiredToken_autoCleaned_validateReturnsNull() {
        String token = service.generate("1");
        // 模拟TTL到期后key被Redis自动清理
        valueStore.remove("family:refresh:" + token);
        assertNull(service.validate(token));
    }

    @Test
    void tokenFormat_isOpaqueRandomString() {
        String token1 = service.generate("1");
        String token2 = service.generate("1");
        // 不透明随机串：非JWT（不含点分隔符），每次生成不同
        assertFalse(token1.contains("."));
        assertNotEquals(token1, token2);
        assertEquals(64, token1.length());
    }
}
