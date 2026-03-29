package com.shopsphere.orderservice.exception;

public class OrderAlreadyDeliveredException extends RuntimeException {

    public OrderAlreadyDeliveredException(String message) {
        super(message);
    }
}

