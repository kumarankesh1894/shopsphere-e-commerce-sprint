package com.shopsphere.adminservice.service.implementation;

import com.shopsphere.adminservice.client.AdminCatalogClient;
import com.shopsphere.adminservice.dto.ProductRequestDto;
import com.shopsphere.adminservice.service.AdminCatalogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Caching;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
/*
 * What:
 * Implements admin product management operations.
 *
 * Why:
 * Adminservice acts as orchestration layer and forwards product write actions
 * to catalogservice.
 *
 * How:
 * - Receive request from controller.
 * - Call catalog Feign client.
 * - Evict report caches so dashboard data stays fresh.
 */
public class AdminCatalogServiceImpl implements AdminCatalogService {

    private final AdminCatalogClient catalogClient;

    /*
     * What:
     * Creates a new product.
     *
     * Why:
     * Admin needs to add products from admin panel.
     *
     * How:
     * Forwards request to catalogservice and evicts related caches.
     */
    @Override
    @Caching(evict = {
            @CacheEvict(value = "dashboard", allEntries = true),
            @CacheEvict(value = "sales", allEntries = true),
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "users", allEntries = true),
            @CacheEvict(value = "orders", allEntries = true)
    })
    public Object createProduct(ProductRequestDto request) {
        log.info("admin.products.create.start name={}", request.getProductName());
        return catalogClient.createProduct(request);
    }

    /*
     * What:
     * Updates existing product data.
     *
     * Why:
     * Admin needs to change stock/price/details later.
     *
     * How:
     * Calls catalogservice update endpoint and clears report caches.
     */
    @Override
    @Caching(evict = {
            @CacheEvict(value = "dashboard", allEntries = true),
            @CacheEvict(value = "sales", allEntries = true),
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "users", allEntries = true),
            @CacheEvict(value = "orders", allEntries = true)
    })
    public Object updateProduct(Long id, ProductRequestDto request) {
        log.info("admin.products.update.start productId={}", id);
        return catalogClient.updateProduct(id, request);
    }

    /*
     * What:
     * Deletes a product.
     *
     * Why:
     * Admin needs product removal capability.
     *
     * How:
     * Calls catalogservice delete endpoint and clears report caches.
     */
    @Override
    @Caching(evict = {
            @CacheEvict(value = "dashboard", allEntries = true),
            @CacheEvict(value = "sales", allEntries = true),
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "users", allEntries = true),
            @CacheEvict(value = "orders", allEntries = true)
    })
    public Object deleteProduct(Long id) {
        log.info("admin.products.delete.start productId={}", id);
        return catalogClient.deleteProduct(id);
    }
}

