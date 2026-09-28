package com.UrlShortener.URLShortener;

import com.UrlShortener.URLShortener.ratelimit.RedisRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisRateLimiterTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new RedisRateLimiter(stringRedisTemplate, 5, 60);
    }

    @Test
    void testIsAllowed_underLimit_returnsTrue() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L);

        boolean allowed = rateLimiter.isAllowed("client-1");

        assertTrue(allowed);
        verify(stringRedisTemplate).expire(eq("rate_limit:client-1"), eq(Duration.ofSeconds(60)));
    }

    @Test
    void testIsAllowed_exceedLimit_returnsFalse() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(6L);
        when(stringRedisTemplate.getExpire(anyString())).thenReturn(50L);

        boolean allowed = rateLimiter.isAllowed("client-1");

        assertFalse(allowed);
    }

    @Test
    void testIsAllowed_redisFailure_failsOpen() {
        when(stringRedisTemplate.opsForValue()).thenThrow(new RedisConnectionFailureException("Redis is down"));

        boolean allowed = rateLimiter.isAllowed("client-1");

        assertTrue(allowed, "Rate limiter should fail open on Redis errors to prevent outage");
    }
}
