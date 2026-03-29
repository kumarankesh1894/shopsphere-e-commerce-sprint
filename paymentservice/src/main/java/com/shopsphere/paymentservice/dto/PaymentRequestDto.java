package com.shopsphere.paymentservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentRequestDto {

    @NotNull(message = "orderId is required")
    private Long orderId;

    @NotBlank(message = "idempotencyKey is required")
    private String idempotencyKey;
}

