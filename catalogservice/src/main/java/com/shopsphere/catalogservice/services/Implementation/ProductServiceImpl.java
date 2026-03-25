package com.shopsphere.catalogservice.services.Implementation;


import com.shopsphere.catalogservice.dto.ProductRequest;
import com.shopsphere.catalogservice.dto.ProductResponse;
import com.shopsphere.catalogservice.entities.Category;
import com.shopsphere.catalogservice.entities.Product;
import com.shopsphere.catalogservice.exception.CategoryNotFoundException;
import com.shopsphere.catalogservice.exception.ProductNotFoundException;
import com.shopsphere.catalogservice.repository.CategoryRepository;
import com.shopsphere.catalogservice.repository.ProductRepository;
import com.shopsphere.catalogservice.services.ProductService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ModelMapper modelMapper;

    // GET BY ID
    @Override
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new ProductNotFoundException("Product not found with id: " + id));
        return modelMapper.map(product, ProductResponse.class);
    }

    //Create
    @Override
    public ProductResponse createProduct(ProductRequest productRequest) {

        // Validate Category
        Category category = categoryRepository.findById(productRequest.getCategoryId())
                .orElseThrow(() ->
                        new CategoryNotFoundException("Category not found"));

        //  Map DTO → Entity
        Product product = modelMapper.map(productRequest, Product.class);

        // Set relationship manually
        product.setCategory(category);
        product.setIsAvailable(true); // Default to available when creating

        Product savedProduct = productRepository.save(product);

        return modelMapper.map(savedProduct, ProductResponse.class);
    }

    // UPDATE
    @Override
    public ProductResponse updateProduct(Long id, ProductRequest productRequest) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new ProductNotFoundException("Product not found with id: " + id));

        // Validate Category
        Category category = categoryRepository.findById(productRequest.getCategoryId())
                .orElseThrow(() ->
                        new CategoryNotFoundException("Category not found"));

        // Update fields
        modelMapper.map(productRequest, product);
        product.setCategory(category);

        Product updatedProduct = productRepository.save(product);

        return modelMapper.map(updatedProduct, ProductResponse.class);
    }

    // DELETE
    @Override
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new ProductNotFoundException("Product not found with id: " + id));
        product.setIsAvailable(false); // Mark as unavailable instead of deleting
        productRepository.save(product);
    }

    // GET ALL
    @Override
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(category -> modelMapper.map(productRepository, ProductResponse.class))
                .toList();
    }
}

