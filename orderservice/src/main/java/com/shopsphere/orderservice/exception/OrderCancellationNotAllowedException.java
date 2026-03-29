package com.shopsphere.orderservice.exception;

public class OrderCancellationNotAllowedException extends RuntimeException {

    public OrderCancellationNotAllowedException(String message) {
        super(message);
    }
}

