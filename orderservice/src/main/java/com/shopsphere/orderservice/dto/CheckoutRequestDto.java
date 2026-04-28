package com.shopsphere.orderservice.dto;

import com.shopsphere.orderservice.entity.Address;
import com.shopsphere.orderservice.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutRequestDto {

    @NotBlank(message = "Idempotency key is required")
    private String idempotencyKey;

    @Valid
    @NotNull(message = "Address is required")
    private AddressDto address;

    // Optional; defaults to COD when omitted.
    private PaymentMethod paymentMethod;
}