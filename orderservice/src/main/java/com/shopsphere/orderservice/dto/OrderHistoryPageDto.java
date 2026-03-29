package com.shopsphere.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderHistoryPageDto {
    private List<OrderResponseDto> orders;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;
}

