package com.shopsphere.adminservice.client;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "catalogservice")
/*
 * What:
 * Feign client for catalogservice admin product endpoints.
 *
 * Why:
 * Adminservice delegates product CRUD actions to catalogservice.
 *
 * How:
 * These methods map directly to catalog private product APIs.
 */
public interface AdminCatalogClient {

    /*
     * What: Create product.
     * Why: Admin product onboarding.
     * How: Calls POST /api/catalog/products/private.
     */
    @PostMapping("/api/catalog/products/private")
    Object createProduct(@RequestBody com.shopsphere.adminservice.dto.ProductRequestDto request);

    /*
     * What: Update product.
     * Why: Admin product maintenance.
     * How: Calls PUT /api/catalog/products/private/{id}.
     */
    @PutMapping("/api/catalog/products/private/{id}")
    Object updateProduct(@PathVariable("id") Long id, @RequestBody com.shopsphere.adminservice.dto.ProductRequestDto request);

    /*
     * What: Delete product.
     * Why: Admin can remove/deactivate products.
     * How: Calls DELETE /api/catalog/products/private/{id}.
     */
    @DeleteMapping("/api/catalog/products/private/{id}")
    Object deleteProduct(@PathVariable("id") Long id);
}

