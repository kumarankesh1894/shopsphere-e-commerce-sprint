package com.shopsphere.paymentservice.enums;


/*
 * This enum controls the payment lifecycle.
 * It is internal to Payment Service and must NOT be shared with other services.
 */
public enum PaymentStatus {

    INITIATED,        // Payment request created
    PROCESSING,       // Payment is being processed (gateway interaction)
    SUCCESS,          // Payment completed successfully
    FAILED,           // Payment failed
    CANCELLED,        // User cancelled payment
    REFUNDED          // Payment refunded (future use)
}