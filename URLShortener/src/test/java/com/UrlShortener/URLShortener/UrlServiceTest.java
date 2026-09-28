package com.UrlShortener.URLShortener;

import com.UrlShortener.URLShortener.dto.CreateUrlRequest;
import com.UrlShortener.URLShortener.dto.CreateUrlResponse;
import com.UrlShortener.URLShortener.dto.UrlStatsResponse;
import com.UrlShortener.URLShortener.exception.CustomAliasAlreadyInUseException;
import com.UrlShortener.URLShortener.exception.InvalidUrlException;
import com.UrlShortener.URLShortener.exception.ShortUrlNotFoundException;
import com.UrlShortener.URLShortener.exception.UrlExpiredException;
import com.UrlShortener.URLShortener.model.Url;
import com.UrlShortener.URLShortener.repository.UrlRepository;
import com.UrlShortener.URLShortener.service.UrlService;
import com.UrlShortener.URLShortener.strategy.ShortCodeStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlServiceTest {

    @Mock
    private UrlRepository urlRepository;

    @Mock
    private ShortCodeStrategy shortCodeStrategy;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private UrlService urlService;

    @BeforeEach
    void setUp() {
        urlService = new UrlService(urlRepository, shortCodeStrategy, stringRedisTemplate, "http://localhost:8080", 30);
    }

    @Test
    void testCreateShortUrl_withRandomStrategy_success() {
        CreateUrlRequest request = new CreateUrlRequest("https://google.com", null, null);
        when(shortCodeStrategy.generate("https://google.com")).thenReturn("abc1234");
        when(urlRepository.existsByShortCode("abc1234")).thenReturn(false);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        Url savedUrl = new Url("https://google.com", "abc1234");
        savedUrl.setId(1L);
        savedUrl.setCreatedAt(LocalDateTime.now());
        when(urlRepository.save(any(Url.class))).thenReturn(savedUrl);

        CreateUrlResponse response = urlService.createShortUrl(request);

        assertNotNull(response);
        assertEquals("abc1234", response.getShortCode());
        assertEquals("https://google.com", response.getOriginalUrl());
        assertEquals("http://localhost:8080/abc1234", response.getShortUrl());
        verify(urlRepository).save(any(Url.class));
    }

    @Test
    void testCreateShortUrl_withCustomAlias_success() {
        CreateUrlRequest request = new CreateUrlRequest("https://github.com", "my-alias", null);
        when(urlRepository.existsByShortCode("my-alias")).thenReturn(false);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        Url savedUrl = new Url("https://github.com", "my-alias");
        savedUrl.setId(2L);
        savedUrl.setCreatedAt(LocalDateTime.now());
        when(urlRepository.save(any(Url.class))).thenReturn(savedUrl);

        CreateUrlResponse response = urlService.createShortUrl(request);

        assertNotNull(response);
        assertEquals("my-alias", response.getShortCode());
        assertEquals("http://localhost:8080/my-alias", response.getShortUrl());
    }

    @Test
    void testCreateShortUrl_customAliasAlreadyExists_throwsException() {
        CreateUrlRequest request = new CreateUrlRequest("https://github.com", "existing-alias", null);
        when(urlRepository.existsByShortCode("existing-alias")).thenReturn(true);

        assertThrows(CustomAliasAlreadyInUseException.class, () -> urlService.createShortUrl(request));
    }

    @Test
    void testCreateShortUrl_invalidUrlScheme_throwsException() {
        CreateUrlRequest request = new CreateUrlRequest("ftp://invalid.com", null, null);

        assertThrows(InvalidUrlException.class, () -> urlService.createShortUrl(request));
    }

    @Test
    void testCreateShortUrl_pastExpiration_throwsException() {
        CreateUrlRequest request = new CreateUrlRequest("https://example.com", null, LocalDateTime.now().minusDays(1));

        assertThrows(InvalidUrlException.class, () -> urlService.createShortUrl(request));
    }

    @Test
    void testGetOriginalUrlAndTrackClick_fromCache_success() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("url:code:abc1234")).thenReturn("https://example.com");

        String result = urlService.getOriginalUrlAndTrackClick("abc1234");

        assertEquals("https://example.com", result);
        verify(urlRepository).incrementClickCountByShortCode("abc1234");
    }

    @Test
    void testGetOriginalUrlAndTrackClick_fromDatabase_success() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("url:code:dbcode")).thenReturn(null);

        Url url = new Url("https://db-example.com", "dbcode");
        when(urlRepository.findByShortCode("dbcode")).thenReturn(Optional.of(url));

        String result = urlService.getOriginalUrlAndTrackClick("dbcode");

        assertEquals("https://db-example.com", result);
        verify(urlRepository).incrementClickCountByShortCode("dbcode");
    }

    @Test
    void testGetOriginalUrlAndTrackClick_expired_throwsException() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("url:code:expiredCode")).thenReturn(null);

        Url url = new Url("https://expired.com", "expiredCode", LocalDateTime.now().minusHours(1));
        when(urlRepository.findByShortCode("expiredCode")).thenReturn(Optional.of(url));

        assertThrows(UrlExpiredException.class, () -> urlService.getOriginalUrlAndTrackClick("expiredCode"));
    }

    @Test
    void testGetOriginalUrlAndTrackClick_notFound_throwsException() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("url:code:unknown")).thenReturn(null);
        when(urlRepository.findByShortCode("unknown")).thenReturn(Optional.empty());

        assertThrows(ShortUrlNotFoundException.class, () -> urlService.getOriginalUrlAndTrackClick("unknown"));
    }

    @Test
    void testGetUrlStats_success() {
        Url url = new Url("https://stats.com", "stats123");
        url.setCreatedAt(LocalDateTime.now().minusDays(2));
        url.setClickCount(42L);
        when(urlRepository.findByShortCode("stats123")).thenReturn(Optional.of(url));

        UrlStatsResponse stats = urlService.getUrlStats("stats123");

        assertNotNull(stats);
        assertEquals("stats123", stats.getShortCode());
        assertEquals("https://stats.com", stats.getOriginalUrl());
        assertEquals(42L, stats.getClickCount());
        assertFalse(stats.isExpired());
    }
}
