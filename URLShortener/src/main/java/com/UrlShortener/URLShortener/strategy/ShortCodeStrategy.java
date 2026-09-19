package com.UrlShortener.URLShortener.strategy;

public interface ShortCodeStrategy {
    String generate(String originalUrl);
}
