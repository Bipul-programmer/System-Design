package com.UrlShortener.URLShortener.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateUrlRequest {
    private String originalUrl;
    private String customAlias;
}
