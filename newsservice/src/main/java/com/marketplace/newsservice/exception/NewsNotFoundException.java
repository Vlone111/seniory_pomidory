package com.marketplace.newsservice.exception;

public class NewsNotFoundException extends RuntimeException {
    
    public NewsNotFoundException(String message) {
        super(message);
    }
}
