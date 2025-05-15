package com.ecommerce.service;

import com.ecommerce.dto.request.AddToCartRequest;
import com.ecommerce.dto.response.CartResponse;

public interface CartService {
    CartResponse getCart(Long customerId);
    CartResponse addToCart(Long customerId, AddToCartRequest request);
    CartResponse updateCartItem(Long customerId, Long cartItemId, Integer quantity);
    CartResponse removeFromCart(Long customerId, Long cartItemId);
    void clearCart(Long customerId);
}
