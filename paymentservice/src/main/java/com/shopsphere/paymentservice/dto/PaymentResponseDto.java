package com.shopsphere.paymentservice.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentResponseDto {

    private Long orderId;
    private String paymentStatus;
    private String gateway;
    private String razorpayOrderId;
    private Long amountInPaise;
    private String currency;
    private String razorpayKeyId;
    private String failureReason;
    private String message;
}