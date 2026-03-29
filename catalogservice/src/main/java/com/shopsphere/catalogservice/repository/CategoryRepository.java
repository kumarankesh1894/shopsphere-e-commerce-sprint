package com.shopsphere.catalogservice.repository;

import com.shopsphere.catalogservice.entities.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

	boolean existsByCategoryNameIgnoreCase(String categoryName);

	Optional<Category> findByCategoryNameIgnoreCase(String categoryName);

}
