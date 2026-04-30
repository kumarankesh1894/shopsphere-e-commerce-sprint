package com.shopsphere.orderservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemRequestDto {

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    // Optional: client can pass a display name override (e.g. "Men T-Shirt (M)")
    // so size selection is preserved in the cart item label.
    // If null, the product name from catalogservice is used.
    private String productName;
}