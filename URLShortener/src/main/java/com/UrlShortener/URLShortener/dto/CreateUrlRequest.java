package com.UrlShortener.URLShortener.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateUrlRequest {

    @NotBlank(message = "Original URL cannot be empty")
    @URL(message = "Invalid URL format")
    private String originalUrl;

    @Pattern(regexp = "^[a-zA-Z0-9_-]{3,30}$", message = "Custom alias must be 3-30 alphanumeric characters, hyphens, or underscores")
    private String customAlias;

    private LocalDateTime expiresAt;
}
