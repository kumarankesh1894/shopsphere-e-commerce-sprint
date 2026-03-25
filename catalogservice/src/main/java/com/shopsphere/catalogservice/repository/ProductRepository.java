package com.shopsphere.catalogservice.repository;

import com.shopsphere.catalogservice.entities.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

}
