package com.shopsphere.catalogservice.repository;

import com.shopsphere.catalogservice.dto.FeaturedProductResponse;
import com.shopsphere.catalogservice.entities.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>,
        JpaSpecificationExecutor<Product> {

        // i am using optional here because we want to return an empty result if the product is not found or not available, instead of throwing an exception. This allows the service layer to handle the case where a product is not found or not available more gracefully, without having to catch exceptions. The method will return an Optional containing the Product if it exists and is available, or an empty Optional if it does not exist or is not available.
        Optional<Product> findByProductIdAndIsAvailableTrue(Long id);
        Page<Product> findByIsAvailableTrue(Pageable pageable);

        List<Product> findByFeaturedTrueAndIsAvailableTrue();
}
