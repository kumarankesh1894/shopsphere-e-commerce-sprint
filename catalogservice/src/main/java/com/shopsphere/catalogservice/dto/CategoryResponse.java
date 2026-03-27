package com.shopsphere.catalogservice.dto;

import lombok.Data;

@Data
public class CategoryResponse {
    private Long id;
    private String name;
    private String description;
    private String imageUrl;
}
