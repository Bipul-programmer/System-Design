package com.UrlShortener.URLShortener.ratelimit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RedisRateLimiter implements RateLimiter {

    private final StringRedisTemplate stringRedisTemplate;
    private static final int MAX_REQUESTS = 10;
    private static final Duration DURATION_WINDOW = Duration.ofMinutes(1);

    public RedisRateLimiter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean isAllowed(String clientId) {
        String key = "rate_limit:" + clientId;
        Long requestCount = stringRedisTemplate.opsForValue().increment(key);

        if (requestCount == null) {
            return false;
        }

        if (requestCount == 1) {
            stringRedisTemplate.expire(key, DURATION_WINDOW);
        }
        return requestCount <= MAX_REQUESTS;
    }
}
