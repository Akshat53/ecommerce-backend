package com.ecommerce.service;

import com.ecommerce.dto.request.ProductRequest;
import com.ecommerce.dto.response.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductService {
    ProductResponse createProduct(Long sellerId, ProductRequest request);
    ProductResponse updateProduct(Long productId, Long sellerId, ProductRequest request);
    void deleteProduct(Long productId, Long sellerId);
    ProductResponse getProductById(Long productId);
    Page<ProductResponse> getAllProducts(Pageable pageable);
    Page<ProductResponse> getProductsByCategory(Long categoryId, Pageable pageable);
    Page<ProductResponse> getProductsBySeller(Long sellerId, Pageable pageable);
    Page<ProductResponse> searchProducts(String keyword, Pageable pageable);
    List<ProductResponse> getRecentProducts();
}
