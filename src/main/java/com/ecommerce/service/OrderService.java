package com.ecommerce.service;

import com.ecommerce.dto.request.CheckoutRequest;
import com.ecommerce.dto.response.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OrderService {
    OrderResponse createOrder(Long customerId, CheckoutRequest request);
    OrderResponse getOrderById(Long orderId, Long userId);
    List<OrderResponse> getCustomerOrders(Long customerId, Pageable pageable);
    OrderResponse updateOrderStatus(Long orderId, String status);
    void cancelOrder(Long orderId, Long customerId);
}
