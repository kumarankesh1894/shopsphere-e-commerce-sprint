package com.shopsphere.catalogservice.repository;

import com.shopsphere.catalogservice.entities.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>,
        JpaSpecificationExecutor<Product> {
        Optional<Product> findByProductIdAndIsAvailableTrue(Long id);
        Page<Product> findByIsAvailableTrue(Pageable pageable);
}
