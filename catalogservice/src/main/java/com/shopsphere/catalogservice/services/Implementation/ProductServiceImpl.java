package com.shopsphere.catalogservice.services.Implementation;


import com.shopsphere.catalogservice.Specification.ProductSpecification;
import com.shopsphere.catalogservice.dto.PaginationResponse;
import com.shopsphere.catalogservice.dto.ProductFilterRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import com.shopsphere.catalogservice.services.ProductService;
import com.shopsphere.catalogservice.repository.CategoryRepository;
import com.shopsphere.catalogservice.repository.ProductRepository;
import com.shopsphere.catalogservice.dto.ProductRequest;
import com.shopsphere.catalogservice.dto.ProductResponse;
import com.shopsphere.catalogservice.entities.Category;
import com.shopsphere.catalogservice.entities.Product;
import com.shopsphere.catalogservice.exception.CategoryNotFoundException;
import com.shopsphere.catalogservice.exception.ProductNotFoundException;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ModelMapper modelMapper;
    /*
    * Cacheable means: Store + fetch cache
    * @Cacheable stores method results in cache using a unique key
    * CacheEvict means: Clear cache
    * @CacheEvict removes cached data to maintain consistency after updates.
    * */


    // GET BY ID with caching
    @Cacheable(value = "products", key = "#id")
    @Override
    public ProductResponse getProductById(Long id) {

        log.info("Fetching product from DB (NOT CACHE)");

        log.info("Fetching product with id: {}", id);

        Product product = productRepository.findByProductIdAndIsAvailableTrue(id)
                .orElseThrow(() -> {
                    log.warn("Product not found with id: {}", id);
                    return new ProductNotFoundException("Product not found with id: " + id);
                });

        log.info("Product fetched successfully with id: {}", id);

        return modelMapper.map(product, ProductResponse.class);
    }

    //Create the product with validation of category and mapping DTO to Entity
    @CacheEvict(value = {"productsList", "productSearch"}, allEntries = true)
    @Override
    public ProductResponse createProduct(ProductRequest productRequest) {

        log.info("Creating product: {}", productRequest.getProductName());

        // Validate Category
        Category category = categoryRepository.findById(productRequest.getCategoryId())
                .orElseThrow(() -> {
                    log.warn("Category not found with id: {}", productRequest.getCategoryId());
                    return new CategoryNotFoundException("Category not found");
                });

        //  Map DTO → Entity
        Product product = modelMapper.map(productRequest, Product.class);

        // Set relationship manually
        product.setCategory(category);  //means hey product belongs to this category
        product.setIsAvailable(true); // Default to available when creating

        Product savedProduct = productRepository.save(product);
        log.info("Product created successfully with id: {}", savedProduct.getProductId());
        return modelMapper.map(savedProduct, ProductResponse.class);
    }

    // UPDATE
    @CacheEvict(value = {"products", "productsList", "productSearch"}, allEntries = true)
    @Override
    public ProductResponse updateProduct(Long id, ProductRequest productRequest) {

        log.info("Updating product with id: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Product not found for update with id: {}", id);
                    return new ProductNotFoundException("Product not found with id: " + id);
                });

        // Validate Category
        Category category = categoryRepository.findById(productRequest.getCategoryId())
                .orElseThrow(() -> {
                    log.warn("Category not found with id in Product updation: {}", productRequest.getCategoryId());
                    return new CategoryNotFoundException("Category not found");
                });
        // Update fields
        modelMapper.map(productRequest, product);
        product.setCategory(category);

        Product updatedProduct = productRepository.save(product);

        log.info("Product updated successfully with id: {}", id);

        return modelMapper.map(updatedProduct, ProductResponse.class);
    }

    // DELETE
    @CacheEvict(value = {"products", "productsList", "productSearch"}, allEntries = true)
    @Override
    public void deleteProduct(Long id) {
        log.info("Deleting product with id: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Product not found for deletion with id: {}", id);
                    return new ProductNotFoundException("Product not found with id: " + id);
                });

        product.setIsAvailable(false); // Mark as unavailable instead of deleting
        productRepository.save(product);
        log.info("Product soft deleted successfully with id: {}", id);

    }
    //Created a constant list of allowed sort fields to prevent invalid sorting parameters
    private static final List<String> allowedSortFields =
            List.of("productId", "productName", "price", "stock", "createdAt", "updatedAt");

    // Get All the data with pagination and sorting
    @Cacheable(value = "productsList", key = "#page + '-' + #size + '-' + #sortBy + '-' + #sortDir")
    @Override
    public PaginationResponse<ProductResponse> getAllProducts(int page, int size, String sortBy, String sortDir) {

        log.info("Fetching products - page: {}, size: {}, sortBy: {}, sortDir: {}", page, size, sortBy, sortDir);
        // Validate sort field
        if (!allowedSortFields.contains(sortBy)) {
            log.warn("Invalid sort field '{}', defaulting to productId", sortBy);

            sortBy = "productId";
        }

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Product> productPage = productRepository.findByIsAvailableTrue(pageable);

        List<ProductResponse> content = productPage.getContent()
                .stream()
                .map(p -> modelMapper.map(p, ProductResponse.class))
                .toList();

        return new PaginationResponse<>(
                content,
                productPage.getNumber(),
                productPage.getSize(),
                productPage.getTotalElements(),
                productPage.getTotalPages(),
                productPage.isLast()
        );
    }

    //Paginating and sorting with filtering
    @Cacheable(value = "productSearch",
            key = "#filter.keyword + '-' + #filter.categoryId + '-' + #filter.minPrice + '-' + #filter.maxPrice + '-' + #page")

    @Override
    public PaginationResponse<ProductResponse> searchProducts(
            ProductFilterRequest filter,
            int page,
            int size,
            String sortBy,
            String sortDir) {

        log.info("Searching products with filters: keyword={}, categoryId={}, minPrice={}, maxPrice={}",
                filter.getKeyword(), filter.getCategoryId(), filter.getMinPrice(), filter.getMaxPrice());

            // Validate sort field
        if (!allowedSortFields.contains(sortBy)) {
            sortBy = "productId";
        }
        //Sorting
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        //Fetch Data using specification
        Page<Product> productPage = productRepository.findAll(
                ProductSpecification.filterProducts(filter),
                pageable
        );

        //Convert Entity → DTO
        List<ProductResponse> content = productPage.getContent()
                .stream()
                .map(product -> modelMapper.map(product, ProductResponse.class))
                .toList();

        // Return custom pagination response
        return new PaginationResponse<>(
                content,
                productPage.getNumber(),
                productPage.getSize(),
                productPage.getTotalElements(),
                productPage.getTotalPages(),
                productPage.isLast()
        );
    }

}

