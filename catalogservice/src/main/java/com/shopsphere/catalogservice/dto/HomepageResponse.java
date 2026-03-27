package com.shopsphere.catalogservice.dto;



import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HomepageResponse {

    private List<CategoryResponse> categories;
    private List<FeaturedProductResponse> featuredProducts;


}