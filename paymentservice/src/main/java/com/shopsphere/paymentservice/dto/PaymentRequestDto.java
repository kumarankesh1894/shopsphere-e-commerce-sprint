package com.shopsphere.paymentservice.dto;

import com.shopsphere.paymentservice.enums.PaymentMethod;
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

    // Optional; defaults to COD if not provided by caller.
    private PaymentMethod paymentMethod;
}
