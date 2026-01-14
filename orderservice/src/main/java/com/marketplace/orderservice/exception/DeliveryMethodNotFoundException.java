package com.marketplace.orderservice.exception;

public class DeliveryMethodNotFoundException extends RuntimeException {
    public DeliveryMethodNotFoundException(String message) {
        super(message);
    }
}
