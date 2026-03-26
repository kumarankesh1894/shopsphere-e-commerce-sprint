package com.shopsphere.catalogservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")

    private LocalDateTime timestamp;

//    Static helper methods
     public static <T> ApiResponse<T> success(T data, String message) {
         return new ApiResponse<>(true, message, data, LocalDateTime.now());
     }
    public static <T> ApiResponse<T> failure(String message) {
         return new ApiResponse<>(false, message, null, LocalDateTime.now());
     }
}
