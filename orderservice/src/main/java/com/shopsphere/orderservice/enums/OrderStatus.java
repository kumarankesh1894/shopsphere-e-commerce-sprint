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
    PAYMENT_DUE,        // COD chosen; payment will be collected on delivery
    PAID,               // Payment successful
    PAYMENT_FAILED,
    PACKED,
    SHIPPED,
    DELIVERED,
    CANCELLED
}