package com.shopsphere.adminservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderAdminDto {
    private Long orderId;
    private Long userId;
    private String userName;
    private BigDecimal totalAmount;
    private String status;
    private LocalDateTime placedAt;
    private List<OrderAdminItemDto> items;
}

