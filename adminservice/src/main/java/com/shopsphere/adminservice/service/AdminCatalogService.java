package com.shopsphere.adminservice.service;

import com.shopsphere.adminservice.dto.ProductRequestDto;

public interface AdminCatalogService {
    /*
     * What: Creates product.
     * Why: Admin onboarding for catalog items.
     * How: Delegates to catalogservice via Feign.
     */
    Object createProduct(ProductRequestDto request);

    /*
     * What: Updates product.
     * Why: Admin can maintain product details.
     * How: Delegates update request to catalogservice.
     */
    Object updateProduct(Long id, ProductRequestDto request);

    /*
     * What: Deletes product.
     * Why: Admin can remove product from catalog.
     * How: Delegates delete request to catalogservice.
     */
    Object deleteProduct(Long id);
}

