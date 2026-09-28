package com.UrlShortener.URLShortener.service;

import com.UrlShortener.URLShortener.dto.CreateUrlRequest;
import com.UrlShortener.URLShortener.dto.CreateUrlResponse;
import com.UrlShortener.URLShortener.dto.UrlStatsResponse;
import com.UrlShortener.URLShortener.exception.CustomAliasAlreadyInUseException;
import com.UrlShortener.URLShortener.exception.InvalidUrlException;
import com.UrlShortener.URLShortener.exception.ShortUrlNotFoundException;
import com.UrlShortener.URLShortener.exception.UrlExpiredException;
import com.UrlShortener.URLShortener.model.Url;
import com.UrlShortener.URLShortener.repository.UrlRepository;
import com.UrlShortener.URLShortener.strategy.ShortCodeStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;

@Service
public class UrlService {

    private static final Logger log = LoggerFactory.getLogger(UrlService.class);
    private static final String REDIS_PREFIX = "url:code:";
    private static final int MAX_COLLISION_RETRIES = 5;
    private static final Set<String> RESERVED_WORDS = Set.of("api", "actuator", "health", "metrics", "favicon.ico", "swagger-ui");

    private final UrlRepository urlRepository;
    private final ShortCodeStrategy shortCodeStrategy;
    private final StringRedisTemplate stringRedisTemplate;
    private final String baseUrl;
    private final int defaultTtlDays;

    public UrlService(
            UrlRepository urlRepository,
            ShortCodeStrategy shortCodeStrategy,
            StringRedisTemplate stringRedisTemplate,
            @Value("${app.base-url:http://localhost:8080}") String baseUrl,
            @Value("${app.default-ttl-days:30}") int defaultTtlDays) {
        this.urlRepository = urlRepository;
        this.shortCodeStrategy = shortCodeStrategy;
        this.stringRedisTemplate = stringRedisTemplate;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.defaultTtlDays = defaultTtlDays;
    }

    @Transactional
    public CreateUrlResponse createShortUrl(CreateUrlRequest request) {
        String originalUrl = validateAndNormalizeUrl(request.getOriginalUrl());
        LocalDateTime expiresAt = determineExpirationDate(request.getExpiresAt());

        String shortCode;
        if (request.getCustomAlias() != null && !request.getCustomAlias().trim().isEmpty()) {
            shortCode = request.getCustomAlias().trim();
            validateCustomAlias(shortCode);
            if (urlRepository.existsByShortCode(shortCode)) {
                throw new CustomAliasAlreadyInUseException("Custom alias '" + shortCode + "' is already in use.");
            }
        } else {
            shortCode = generateUniqueShortCode(originalUrl);
        }

        Url url = new Url(originalUrl, shortCode, expiresAt);
        Url saved = urlRepository.save(url);

        cacheInRedis(shortCode, originalUrl, expiresAt);

        return CreateUrlResponse.builder()
                .shortCode(saved.getShortCode())
                .originalUrl(saved.getOriginalUrl())
                .shortUrl(buildShortUrl(saved.getShortCode()))
                .createdAt(saved.getCreatedAt())
                .expiresAt(saved.getExpiresAt())
                .clickCount(saved.getClickCount())
                .build();
    }

    public String getOriginalUrlAndTrackClick(String shortCode) {
        if (shortCode == null || shortCode.trim().isEmpty()) {
            throw new ShortUrlNotFoundException("Short code cannot be blank.");
        }

        String normalizedCode = shortCode.trim();

        // 1. Try Redis Cache first
        String cachedOriginalUrl = getFromRedisCache(normalizedCode);
        if (cachedOriginalUrl != null) {
            urlRepository.incrementClickCountByShortCode(normalizedCode);
            return cachedOriginalUrl;
        }

        // 2. Query Database on Cache Miss
        Url url = urlRepository.findByShortCode(normalizedCode)
                .orElseThrow(() -> new ShortUrlNotFoundException("Short URL not found for code: " + normalizedCode));

        if (url.isExpired()) {
            deleteFromRedisCache(normalizedCode);
            throw new UrlExpiredException("Short URL with code '" + normalizedCode + "' has expired.");
        }

        // Atomically increment click count
        urlRepository.incrementClickCountByShortCode(normalizedCode);

        // Populate cache for subsequent requests
        cacheInRedis(normalizedCode, url.getOriginalUrl(), url.getExpiresAt());

        return url.getOriginalUrl();
    }

    @Transactional(readOnly = true)
    public UrlStatsResponse getUrlStats(String shortCode) {
        if (shortCode == null || shortCode.trim().isEmpty()) {
            throw new ShortUrlNotFoundException("Short code cannot be blank.");
        }

        String normalizedCode = shortCode.trim();
        Url url = urlRepository.findByShortCode(normalizedCode)
                .orElseThrow(() -> new ShortUrlNotFoundException("Short URL not found for code: " + normalizedCode));

        return UrlStatsResponse.builder()
                .shortCode(url.getShortCode())
                .originalUrl(url.getOriginalUrl())
                .shortUrl(buildShortUrl(url.getShortCode()))
                .createdAt(url.getCreatedAt())
                .expiresAt(url.getExpiresAt())
                .clickCount(url.getClickCount())
                .isExpired(url.isExpired())
                .build();
    }

    @Transactional
    public void deleteUrl(String shortCode) {
        if (shortCode == null || shortCode.trim().isEmpty()) {
            return;
        }
        String normalizedCode = shortCode.trim();
        urlRepository.findByShortCode(normalizedCode).ifPresent(urlRepository::delete);
        deleteFromRedisCache(normalizedCode);
    }

    public String buildShortUrl(String shortCode) {
        return baseUrl + "/" + shortCode;
    }

    private String generateUniqueShortCode(String originalUrl) {
        for (int i = 0; i < MAX_COLLISION_RETRIES; i++) {
            String candidate = shortCodeStrategy.generate(originalUrl);
            if (candidate != null && !urlRepository.existsByShortCode(candidate) && !RESERVED_WORDS.contains(candidate.toLowerCase())) {
                return candidate;
            }
        }
        // Fallback: append timestamp slice if high collision occurs
        String base = shortCodeStrategy.generate(originalUrl);
        return (base != null ? base : "u") + Long.toHexString(System.currentTimeMillis() % 10000);
    }

    private void validateCustomAlias(String alias) {
        if (RESERVED_WORDS.contains(alias.toLowerCase())) {
            throw new InvalidUrlException("Custom alias '" + alias + "' is reserved and cannot be used.");
        }
    }

    private String validateAndNormalizeUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.trim().isEmpty()) {
            throw new InvalidUrlException("Original URL cannot be empty.");
        }

        String trimmed = rawUrl.trim();
        try {
            URI uri = URI.create(trimmed);
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                throw new InvalidUrlException("URL must use http or https protocol.");
            }
            if (uri.getHost() == null || uri.getHost().isEmpty()) {
                throw new InvalidUrlException("URL must include a valid host.");
            }
            return trimmed;
        } catch (IllegalArgumentException e) {
            throw new InvalidUrlException("Invalid URL syntax: " + e.getMessage());
        }
    }

    private LocalDateTime determineExpirationDate(LocalDateTime requestedExpiration) {
        if (requestedExpiration != null) {
            if (requestedExpiration.isBefore(LocalDateTime.now())) {
                throw new InvalidUrlException("Expiration date cannot be in the past.");
            }
            return requestedExpiration;
        }
        if (defaultTtlDays > 0) {
            return LocalDateTime.now().plusDays(defaultTtlDays);
        }
        return null;
    }

    private void cacheInRedis(String shortCode, String originalUrl, LocalDateTime expiresAt) {
        try {
            String key = REDIS_PREFIX + shortCode;
            if (expiresAt != null) {
                Duration ttl = Duration.between(LocalDateTime.now(), expiresAt);
                if (!ttl.isNegative() && !ttl.isZero()) {
                    stringRedisTemplate.opsForValue().set(key, originalUrl, ttl);
                    return;
                }
            }
            stringRedisTemplate.opsForValue().set(key, originalUrl);
        } catch (Exception e) {
            log.warn("Failed to cache shortCode {} in Redis: {}", shortCode, e.getMessage());
        }
    }

    private String getFromRedisCache(String shortCode) {
        try {
            return stringRedisTemplate.opsForValue().get(REDIS_PREFIX + shortCode);
        } catch (Exception e) {
            log.warn("Failed to read shortCode {} from Redis: {}", shortCode, e.getMessage());
            return null;
        }
    }

    private void deleteFromRedisCache(String shortCode) {
        try {
            stringRedisTemplate.delete(REDIS_PREFIX + shortCode);
        } catch (Exception e) {
            log.warn("Failed to delete shortCode {} from Redis: {}", shortCode, e.getMessage());
        }
    }
}
