package com.shopsphere.catalogservice.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "categories")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long categoryId;
    @Column(nullable = false, unique = true, name = "category_name")
    private String categoryName;
    private String description;
    @Column(name = "image_url")
    private String imageUrl;
    //One Category ->  many products
    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)

    /*
    * orphanRemoval= true means If a product is removed from category then it
    * Delete it from DB
    * mappedBy = "category" means
    * Hey Hibernate The relationship is controlled by Product.category NOT by Category.products
    */

    private List<Product> products;

    @CreationTimestamp
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime updatedAt;

}
