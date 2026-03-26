package com.shopsphere.catalogservice.dto;

import lombok.Data;


/*
* In this class, i mentioned all the parameters which will pass in the query of URL
* And we will directly pass the reference of this class*/
@Data
public class ProductFilterRequest {

    private String keyword;
    private Long categoryId;
    private Double minPrice;
    private Double maxPrice;
}
