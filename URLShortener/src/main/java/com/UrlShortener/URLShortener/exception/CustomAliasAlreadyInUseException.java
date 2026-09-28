package com.UrlShortener.URLShortener.exception;

public class CustomAliasAlreadyInUseException extends RuntimeException {
    public CustomAliasAlreadyInUseException(String message) {
        super(message);
    }
}
