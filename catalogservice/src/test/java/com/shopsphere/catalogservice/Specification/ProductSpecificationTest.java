package com.shopsphere.catalogservice.Specification;

import com.shopsphere.catalogservice.dto.ProductFilterRequest;
import com.shopsphere.catalogservice.entities.Product;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings({"unchecked", "rawtypes"})
class ProductSpecificationTest {

    @Test
    void filterProducts_whenKeywordOnly_buildsLikePredicate() {
        ProductFilterRequest filter = new ProductFilterRequest();
        filter.setKeyword("Laptop");

        Root<Product> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        Predicate base = mock(Predicate.class);
        Predicate pAvailable = mock(Predicate.class);
        Predicate afterAvailable = mock(Predicate.class);
        Predicate pKeyword = mock(Predicate.class);
        Predicate finalPredicate = mock(Predicate.class);
        Path<Boolean> availablePath = mock(Path.class);
        Path<String> productNamePath = mock(Path.class);
        Expression<String> loweredName = mock(Expression.class);

        when(cb.conjunction()).thenReturn(base);
        when(root.get("isAvailable")).thenReturn((Path) availablePath);
        when(cb.equal(availablePath, true)).thenReturn(pAvailable);
        when(cb.and(base, pAvailable)).thenReturn(afterAvailable);
        when(root.get("productName")).thenReturn((Path) productNamePath);
        when(cb.lower(productNamePath)).thenReturn(loweredName);
        when(cb.like(loweredName, "%laptop%")).thenReturn(pKeyword);
        when(cb.and(afterAvailable, pKeyword)).thenReturn(finalPredicate);

        Predicate result = ProductSpecification.filterProducts(filter).toPredicate(root, query, cb);

        assertSame(finalPredicate, result);
        verify(cb).like(loweredName, "%laptop%");
    }

    @Test
    void filterProducts_whenCategoryOnly_buildsCategoryPredicate() {
        ProductFilterRequest filter = new ProductFilterRequest();
        filter.setCategoryId(21L);

        Root<Product> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        Predicate base = mock(Predicate.class);
        Predicate pAvailable = mock(Predicate.class);
        Predicate afterAvailable = mock(Predicate.class);
        Predicate pCategory = mock(Predicate.class);
        Predicate finalPredicate = mock(Predicate.class);
        Path<Boolean> availablePath = mock(Path.class);
        Path<Object> categoryPath = mock(Path.class);
        Path<Long> categoryIdPath = mock(Path.class);

        when(cb.conjunction()).thenReturn(base);
        when(root.get("isAvailable")).thenReturn((Path) availablePath);
        when(cb.equal(availablePath, true)).thenReturn(pAvailable);
        when(cb.and(base, pAvailable)).thenReturn(afterAvailable);
        when(root.get("category")).thenReturn((Path) categoryPath);
        when(categoryPath.get("categoryId")).thenReturn((Path) categoryIdPath);
        when(cb.equal(categoryIdPath, 21L)).thenReturn(pCategory);
        when(cb.and(afterAvailable, pCategory)).thenReturn(finalPredicate);

        Predicate result = ProductSpecification.filterProducts(filter).toPredicate(root, query, cb);

        assertSame(finalPredicate, result);
        verify(root).get("category");
        verify(categoryPath).get("categoryId");
    }

    @Test
    void filterProducts_whenMinPriceOnly_buildsMinPredicate() {
        ProductFilterRequest filter = new ProductFilterRequest();
        filter.setMinPrice(250.0);

        Root<Product> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        Predicate base = mock(Predicate.class);
        Predicate pAvailable = mock(Predicate.class);
        Predicate afterAvailable = mock(Predicate.class);
        Predicate pMin = mock(Predicate.class);
        Predicate finalPredicate = mock(Predicate.class);
        Path<Boolean> availablePath = mock(Path.class);
        Path<BigDecimal> pricePath = mock(Path.class);

        when(cb.conjunction()).thenReturn(base);
        when(root.get("isAvailable")).thenReturn((Path) availablePath);
        when(cb.equal(availablePath, true)).thenReturn(pAvailable);
        when(cb.and(base, pAvailable)).thenReturn(afterAvailable);
        when(root.get("price")).thenReturn((Path) pricePath);
        when(cb.greaterThanOrEqualTo(eq(pricePath), eq(BigDecimal.valueOf(250.0)))).thenReturn(pMin);
        when(cb.and(afterAvailable, pMin)).thenReturn(finalPredicate);

        Predicate result = ProductSpecification.filterProducts(filter).toPredicate(root, query, cb);

        assertSame(finalPredicate, result);
        verify(cb).greaterThanOrEqualTo(eq(pricePath), eq(BigDecimal.valueOf(250.0)));
    }

    @Test
    void filterProducts_whenMaxPriceOnly_buildsMaxPredicate() {
        ProductFilterRequest filter = new ProductFilterRequest();
        filter.setMaxPrice(999.0);

        Root<Product> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        Predicate base = mock(Predicate.class);
        Predicate pAvailable = mock(Predicate.class);
        Predicate afterAvailable = mock(Predicate.class);
        Predicate pMax = mock(Predicate.class);
        Predicate finalPredicate = mock(Predicate.class);
        Path<Boolean> availablePath = mock(Path.class);
        Path<BigDecimal> pricePath = mock(Path.class);

        when(cb.conjunction()).thenReturn(base);
        when(root.get("isAvailable")).thenReturn((Path) availablePath);
        when(cb.equal(availablePath, true)).thenReturn(pAvailable);
        when(cb.and(base, pAvailable)).thenReturn(afterAvailable);
        when(root.get("price")).thenReturn((Path) pricePath);
        when(cb.lessThanOrEqualTo(eq(pricePath), eq(BigDecimal.valueOf(999.0)))).thenReturn(pMax);
        when(cb.and(afterAvailable, pMax)).thenReturn(finalPredicate);

        Predicate result = ProductSpecification.filterProducts(filter).toPredicate(root, query, cb);

        assertSame(finalPredicate, result);
        verify(cb).lessThanOrEqualTo(eq(pricePath), eq(BigDecimal.valueOf(999.0)));
    }

    @Test
    void filterProducts_whenAllFiltersPresent_buildsCombinedPredicate() {
        ProductFilterRequest filter = new ProductFilterRequest();
        filter.setKeyword("Phone");
        filter.setCategoryId(8L);
        filter.setMinPrice(100.0);
        filter.setMaxPrice(500.0);

        @SuppressWarnings("unchecked")
        Root<Product> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        Predicate base = mock(Predicate.class);
        Predicate pAvailable = mock(Predicate.class);
        Predicate pKeyword = mock(Predicate.class);
        Predicate pCategory = mock(Predicate.class);
        Predicate pMin = mock(Predicate.class);
        Predicate pMax = mock(Predicate.class);

        @SuppressWarnings("unchecked")
        Path<Boolean> availablePath = mock(Path.class);
        @SuppressWarnings("unchecked")
        Path<String> productNamePath = mock(Path.class);
        @SuppressWarnings("unchecked")
        Expression<String> loweredName = mock(Expression.class);
        @SuppressWarnings("unchecked")
        Path<Object> categoryPath = mock(Path.class);
        @SuppressWarnings("unchecked")
        Path<Long> categoryIdPath = mock(Path.class);
        @SuppressWarnings("unchecked")
        Path<BigDecimal> pricePath = mock(Path.class);

        when(cb.conjunction()).thenReturn(base);

        when(root.get("isAvailable")).thenReturn((Path) availablePath);
        when(cb.equal(availablePath, true)).thenReturn(pAvailable);

        when(root.get("productName")).thenReturn((Path) productNamePath);
        when(cb.lower(productNamePath)).thenReturn(loweredName);
        when(cb.like(loweredName, "%phone%")).thenReturn(pKeyword);

        when(root.get("category")).thenReturn((Path) categoryPath);
        when(categoryPath.get("categoryId")).thenReturn((Path) categoryIdPath);
        when(cb.equal(categoryIdPath, 8L)).thenReturn(pCategory);

        when(root.get("price")).thenReturn((Path) pricePath);
        when(cb.greaterThanOrEqualTo(eq(pricePath), eq(BigDecimal.valueOf(100.0)))).thenReturn(pMin);
        when(cb.lessThanOrEqualTo(eq(pricePath), eq(BigDecimal.valueOf(500.0)))).thenReturn(pMax);

        Predicate afterAvailable = mock(Predicate.class);
        Predicate afterKeyword = mock(Predicate.class);
        Predicate afterCategory = mock(Predicate.class);
        Predicate afterMin = mock(Predicate.class);

        when(cb.and(base, pAvailable)).thenReturn(afterAvailable);
        when(cb.and(afterAvailable, pKeyword)).thenReturn(afterKeyword);
        when(cb.and(afterKeyword, pCategory)).thenReturn(afterCategory);
        when(cb.and(afterCategory, pMin)).thenReturn(afterMin);
        when(cb.and(afterMin, pMax)).thenReturn(pMax);

        Predicate result = ProductSpecification.filterProducts(filter).toPredicate(root, query, cb);

        assertSame(pMax, result);
        verify(cb).lessThanOrEqualTo(eq(pricePath), eq(BigDecimal.valueOf(500.0)));
    }

    @Test
    void filterProducts_whenOnlyAvailabilityRequired_returnsAvailabilityPredicate() {
        ProductFilterRequest filter = new ProductFilterRequest();

        @SuppressWarnings("unchecked")
        Root<Product> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        Predicate base = mock(Predicate.class);
        Predicate pAvailable = mock(Predicate.class);
        Predicate finalPredicate = mock(Predicate.class);
        @SuppressWarnings("unchecked")
        Path<Boolean> availablePath = mock(Path.class);

        when(cb.conjunction()).thenReturn(base);
        when(root.get("isAvailable")).thenReturn((Path) availablePath);
        when(cb.equal(availablePath, true)).thenReturn(pAvailable);
        when(cb.and(base, pAvailable)).thenReturn(finalPredicate);

        Predicate result = ProductSpecification.filterProducts(filter).toPredicate(root, query, cb);

        assertSame(finalPredicate, result);
        verify(cb).conjunction();
        verify(cb).equal(availablePath, true);
        verify(cb).and(base, pAvailable);
        verify(cb).and(any(Predicate.class), any(Predicate.class));
    }
}

