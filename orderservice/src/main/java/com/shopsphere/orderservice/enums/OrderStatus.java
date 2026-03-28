package com.shopsphere.orderservice.enums;



/*
* This enum will:
* Control order lifecycle
* Be used in state transitions
* Prevent invalid operations (VERY IMPORTANT)*/
public enum OrderStatus {

    DRAFT,
    CHECKOUT,
    PAYMENT_PENDING,    // Payment started
    PAID,               // Payment successful
    FAILED,
    PACKED,
    SHIPPED,
    DELIVERED,
    CANCELLED
}