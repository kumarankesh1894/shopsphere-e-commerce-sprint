package com.shopsphere.orderservice.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentResponseDto {

    private Long orderId;
    private String paymentStatus;
    private String orderStatus;
    private String gateway;
    private String paymentMethod;
    private String razorpayOrderId;
    private Long amountInPaise;
    private String currency;
    private String razorpayKeyId;
    private String failureReason;
    private String message;
}
