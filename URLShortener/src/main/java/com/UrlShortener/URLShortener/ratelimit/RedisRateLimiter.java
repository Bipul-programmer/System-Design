package com.UrlShortener.URLShortener.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RedisRateLimiter implements RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RedisRateLimiter.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final int maxRequests;
    private final Duration windowDuration;

    public RedisRateLimiter(
            StringRedisTemplate stringRedisTemplate,
            @Value("${app.rate-limit.max-requests:30}") int maxRequests,
            @Value("${app.rate-limit.window-seconds:60}") int windowSeconds) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.maxRequests = maxRequests;
        this.windowDuration = Duration.ofSeconds(windowSeconds);
    }

    @Override
    public boolean isAllowed(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            clientId = "anonymous";
        }

        String key = "rate_limit:" + clientId;
        try {
            Long requestCount = stringRedisTemplate.opsForValue().increment(key);

            if (requestCount == null) {
                return true;
            }

            // Set TTL on first request or if key has no expiration set
            if (requestCount == 1) {
                stringRedisTemplate.expire(key, windowDuration);
            } else {
                Long expire = stringRedisTemplate.getExpire(key);
                if (expire != null && expire == -1) {
                    stringRedisTemplate.expire(key, windowDuration);
                }
            }

            return requestCount <= maxRequests;
        } catch (Exception e) {
            log.warn("Redis rate limiter failed for clientId {}: {}. Failing open to preserve service availability.", clientId, e.getMessage());
            return true;
        }
    }
}
