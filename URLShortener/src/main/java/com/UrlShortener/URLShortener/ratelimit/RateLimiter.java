package com.UrlShortener.URLShortener.ratelimit;

public interface RateLimiter {
    boolean isAllowed(String clientId);
}
