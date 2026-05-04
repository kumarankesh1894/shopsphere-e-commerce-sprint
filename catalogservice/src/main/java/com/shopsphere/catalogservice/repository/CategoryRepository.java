package com.shopsphere.catalogservice.repository;

import com.shopsphere.catalogservice.entities.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

	boolean existsByCategoryNameIgnoreCase(String categoryName);

	//optional is use to handle the nullpointer exception if category is not found.
	Optional<Category> findByCategoryNameIgnoreCase(String categoryName);
}
