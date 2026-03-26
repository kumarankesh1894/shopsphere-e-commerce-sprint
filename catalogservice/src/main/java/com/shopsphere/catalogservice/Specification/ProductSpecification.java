package com.shopsphere.catalogservice.Specification;


import com.shopsphere.catalogservice.dto.ProductFilterRequest;
import com.shopsphere.catalogservice.entities.Product;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;


/*
* This is the helper class
* where we will write the logic of filtering the products
* based on the parameters which we will pass in the URL
* Specification class = Query Builder Utility
* Predicate is functional interface which give true or false based on the condition for each input
* A Predicate is a single rule in a database query
*/
public class ProductSpecification {

    /*
    * This is pure method(which means it is stateless i.e. it does not store anything
    * Doesn’t depend on object fields
    * Just takes input and returns output
    * we are making it static because we don’t need to create an object of this class to call this method
    * */
    public static Specification<Product> filterProducts(ProductFilterRequest filter) {

        /*
        * root is the root of the query, it represents the Product entity
        * query is the query being built, we can use it to add order by, group by etc.
        * cb is the criteria builder(like a toolbox), we use it to create predicates and other query components
        * */
        return (Root<Product> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {

            /*
            * This is a fancy way of saying "Start with a blank TRUE."
            * In SQL terms, it’s like writing WHERE 1=1.
            * It gives you a base to start attaching your "AND" conditions to.
            * If the user provides no filters, this ensures the query still works (it just returns everything).
            */
            Predicate predicate = cb.conjunction();

            /*
            * These "if" are the heart of the code
            * The code checks each filter one by one.
            * If a filter exists,
            * it adds a new Predicate (a rule) to the existing list of rules
            **/
            if (filter.getKeyword() != null) {
                predicate = cb.and(predicate,
                        cb.like(cb.lower(root.get("productName")),
                                "%" + filter.getKeyword().toLowerCase() + "%"));
            }

            if (filter.getCategoryId() != null) {
                predicate = cb.and(predicate,
                        cb.equal(root.get("category").get("categoryId"), filter.getCategoryId()));
            }

            if (filter.getMinPrice() != null) {
                predicate = cb.and(predicate,
                        cb.greaterThanOrEqualTo(
                                root.get("price"),
                                BigDecimal.valueOf(filter.getMinPrice())
                        ));
            }

            if (filter.getMaxPrice() != null) {
                predicate = cb.and(predicate,
                        cb.greaterThanOrEqualTo(
                                root.get("price"),
                                BigDecimal.valueOf(filter.getMaxPrice())
                        ));
            }

            return predicate;
        };
    }

}
