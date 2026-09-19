package com.UrlShortener.URLShortener.strategy;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component("counterShortCodeStrategy")
public class CounterShortCodeStrategy implements ShortCodeStrategy {

    private static final String BASE62 = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private final AtomicLong counter = new AtomicLong(1000000L);

    @Override
    public String generate(String originalUrl) {
        long num = counter.incrementAndGet();
        return encodeBase62(num);
    }

    private String encodeBase62(long value) {
        StringBuilder sb = new StringBuilder();
        while (value > 0) {
            sb.append(BASE62.charAt((int) (value % 62)));
            value /= 62;
        }
        return sb.reverse().toString();
    }
}
