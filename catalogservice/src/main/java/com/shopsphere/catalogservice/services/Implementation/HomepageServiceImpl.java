package com.shopsphere.catalogservice.services.Implementation;

import com.shopsphere.catalogservice.dto.CategoryResponse;
import com.shopsphere.catalogservice.dto.FeaturedProductResponse;
import com.shopsphere.catalogservice.dto.HomepageResponse;
import com.shopsphere.catalogservice.repository.CategoryRepository;
import com.shopsphere.catalogservice.repository.ProductRepository;
import com.shopsphere.catalogservice.services.HomepageService;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Builder
@Slf4j
public class HomepageServiceImpl implements HomepageService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ModelMapper modelMapper;

    @Override
    public HomepageResponse getHomepageData() {
        log.info("Featuring all categories and their products for homepage display");
        /// categories
        List<CategoryResponse> categories = categoryRepository.findAll()
                .stream().map(category -> modelMapper.map(category, CategoryResponse.class))
                .toList();

// Inline mapping: ModelMapper for basic fields + manual mapping for nested categoryName
        /// featured Product
        List<FeaturedProductResponse> featuredProducts = productRepository.findByFeaturedTrueAndIsAvailableTrue()
                .stream()
                .map(product -> {FeaturedProductResponse featuredProduct = modelMapper.map(product, FeaturedProductResponse.class);
                    featuredProduct.setCategoryName(product.getCategory().getCategoryName());
                    return featuredProduct;
                }).toList();
        log.info("Total categories and products featured: {} and {}", categories.size(), featuredProducts.size());
        return  HomepageResponse.builder().categories(categories).featuredProducts(featuredProducts).build();
    }
}
