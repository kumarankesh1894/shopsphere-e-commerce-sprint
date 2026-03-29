package com.shopsphere.orderservice.exception;

public class OrderAlreadyShippedException extends RuntimeException {

    public OrderAlreadyShippedException(String message) {
        super(message);
    }
}

