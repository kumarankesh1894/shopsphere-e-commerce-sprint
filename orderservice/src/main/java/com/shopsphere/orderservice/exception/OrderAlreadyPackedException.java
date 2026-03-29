package com.shopsphere.orderservice.exception;

public class OrderAlreadyPackedException extends RuntimeException {

    public OrderAlreadyPackedException(String message) {
        super(message);
    }
}

