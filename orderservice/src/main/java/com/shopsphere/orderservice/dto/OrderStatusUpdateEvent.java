package com.shopsphere.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusUpdateEvent {
    private Long orderId;
    private String status;
    private String source;
    private LocalDateTime occurredAt;
}

