package com.shopsphere.orderservice.dto;

import lombok.Getter;
import lombok.Setter;

/*
 * DTO used to send payment request to Payment Service
 *
 * Fields:
 * - orderId: ID of the order for which payment is being made
 * - idempotencyKey: Unique key to prevent duplicate payments
 */
@Getter
@Setter
public class PaymentRequestDto {

    private Long orderId;
    private String idempotencyKey;
}