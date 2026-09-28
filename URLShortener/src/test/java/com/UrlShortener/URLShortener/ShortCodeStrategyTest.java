package com.UrlShortener.URLShortener;

import com.UrlShortener.URLShortener.strategy.CounterShortCodeStrategy;
import com.UrlShortener.URLShortener.strategy.RandomShortCodeStrategy;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ShortCodeStrategyTest {

    @Test
    void testRandomShortCodeStrategy_generatesSevenCharAlphanumeric() {
        RandomShortCodeStrategy strategy = new RandomShortCodeStrategy();
        String code = strategy.generate("https://example.com");

        assertNotNull(code);
        assertEquals(7, code.length());
        assertTrue(code.matches("^[a-zA-Z0-9]{7}$"));
    }

    @Test
    void testRandomShortCodeStrategy_generatesDiverseCodes() {
        RandomShortCodeStrategy strategy = new RandomShortCodeStrategy();
        Set<String> set = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            set.add(strategy.generate("https://example.com/" + i));
        }
        assertEquals(100, set.size(), "All 100 generated codes should be unique");
    }

    @Test
    void testCounterShortCodeStrategy_encodesBase62() {
        CounterShortCodeStrategy strategy = new CounterShortCodeStrategy();
        String code1 = strategy.generate("https://example.com");
        String code2 = strategy.generate("https://example.com");

        assertNotNull(code1);
        assertNotNull(code2);
        assertNotEquals(code1, code2);
        assertTrue(code1.matches("^[a-zA-Z0-9]+$"));
    }
}
