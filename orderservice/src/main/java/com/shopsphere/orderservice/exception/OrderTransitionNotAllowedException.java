package com.shopsphere.orderservice.exception;

public class OrderTransitionNotAllowedException extends RuntimeException {

    public OrderTransitionNotAllowedException(String message) {
        super(message);
    }
}

