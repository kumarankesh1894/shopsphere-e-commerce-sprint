package com.shopsphere.orderservice.dto;

import com.shopsphere.orderservice.entity.Address;
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
}