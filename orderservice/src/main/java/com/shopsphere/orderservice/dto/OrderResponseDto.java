package com.shopsphere.orderservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.shopsphere.orderservice.enums.OrderStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponseDto {

    private Long orderId;
    private OrderStatus status;
    private BigDecimal totalAmount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "Asia/Kolkata")
    private LocalDateTime createdAt;

    private List<OrderItemResponseDto> items;
    private AddressDto address;
}