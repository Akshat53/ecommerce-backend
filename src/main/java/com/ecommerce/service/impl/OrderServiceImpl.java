package com.ecommerce.service.impl;

import com.ecommerce.dto.request.CheckoutRequest;
import com.ecommerce.dto.response.OrderItemResponse;
import com.ecommerce.dto.response.OrderResponse;
import com.ecommerce.dto.response.ShippingAddressResponse;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.model.*;
import com.ecommerce.repository.*;
import com.ecommerce.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {
    
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ShippingAddressRepository shippingAddressRepository;
    
    @Autowired
    public OrderServiceImpl(OrderRepository orderRepository,
                          OrderItemRepository orderItemRepository,
                          CartRepository cartRepository,
                          CartItemRepository cartItemRepository,
                          UserRepository userRepository,
                          ProductRepository productRepository,
                          ShippingAddressRepository shippingAddressRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.shippingAddressRepository = shippingAddressRepository;
    }
    
    @Override
    @Transactional
    public OrderResponse createOrder(Long customerId, CheckoutRequest request) {
        User user = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + customerId));
        
        Cart cart = cartRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart is empty"));
        
        if (cart.getCartItems().isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }
        
        ShippingAddress shippingAddress = shippingAddressRepository.findById(request.getShippingAddressId())
                .orElseThrow(() -> new ResourceNotFoundException("Shipping address not found"));
        
        // Check if the address belongs to the user
        if (!shippingAddress.getUser().getId().equals(customerId)) {
            throw new RuntimeException("Shipping address does not belong to the user");
        }
        
        // Create order
        Order order = new Order();
        order.setCustomer(user);
        order.setShippingAddress(shippingAddress);
        order.setOrderDate(LocalDateTime.now());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setStatus(Order.OrderStatus.PENDING);
        
        // Calculate total amount
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CartItem cartItem : cart.getCartItems()) {
            totalAmount = totalAmount.add(cartItem.getPrice().multiply(new BigDecimal(cartItem.getQuantity())));
        }
        order.setTotalAmount(totalAmount);
        
        Order savedOrder = orderRepository.save(order);
        
        // Create order items
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cart.getCartItems()) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(savedOrder);
            orderItem.setProduct(cartItem.getProduct());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPriceAtPurchase(cartItem.getPrice());
            
            orderItems.add(orderItemRepository.save(orderItem));
            
            // Update product stock
            Product product = cartItem.getProduct();
            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            productRepository.save(product);
        }
        
        // Clear cart
        cart.getCartItems().clear();
        cartRepository.save(cart);
        
        return convertToOrderResponse(savedOrder);
    }
    
    @Override
    public OrderResponse getOrderById(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        
        // Check if the order belongs to the user
        if (!order.getCustomer().getId().equals(userId) && 
                !userRepository.findById(userId).orElseThrow().getRole().equals(User.Role.ADMIN)) {
            throw new RuntimeException("You don't have permission to view this order");
        }
        
        return convertToOrderResponse(order);
    }
    
    @Override
    public List<OrderResponse> getCustomerOrders(Long customerId, Pageable pageable) {
        Page<Order> orders = orderRepository.findByCustomerId(customerId, pageable);
        
        return orders.getContent().stream()
                .map(this::convertToOrderResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public OrderResponse updateOrderStatus(Long orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        
        try {
            Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(status.trim().toUpperCase());
            order.setStatus(orderStatus);
            Order updatedOrder = orderRepository.save(order);
            
            return convertToOrderResponse(updatedOrder);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid order status: " + status);
        }
    }
    
    @Override
    @Transactional
    public void cancelOrder(Long orderId, Long customerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        
        // Check if the order belongs to the user
        if (!order.getCustomer().getId().equals(customerId)) {
            throw new RuntimeException("You don't have permission to cancel this order");
        }
        
        // Check if order is in a cancellable state
        if (order.getStatus() == Order.OrderStatus.SHIPPED || 
                order.getStatus() == Order.OrderStatus.DELIVERED) {
            throw new RuntimeException("Cannot cancel order that has been shipped or delivered");
        }
        
        // Update order status
        order.setStatus(Order.OrderStatus.CANCELLED);
        orderRepository.save(order);
        
        // Return items to inventory
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
        for (OrderItem item : orderItems) {
            Product product = item.getProduct();
            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
            productRepository.save(product);
        }
    }
    
    // Helper method to convert Order entity to OrderResponse
    private OrderResponse convertToOrderResponse(Order order) {
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getId());
        
        List<OrderItemResponse> itemResponses = orderItems.stream()
                .map(this::convertToOrderItemResponse)
                .collect(Collectors.toList());
        
        ShippingAddressResponse addressResponse = ShippingAddressResponse.builder()
                .id(order.getShippingAddress().getId())
                .addressLine1(order.getShippingAddress().getAddressLine1())
                .addressLine2(order.getShippingAddress().getAddressLine2())
                .city(order.getShippingAddress().getCity())
                .state(order.getShippingAddress().getState())
                .postalCode(order.getShippingAddress().getPostalCode())
                .country(order.getShippingAddress().getCountry())
                .isDefault(order.getShippingAddress().getIsDefault())
                .build();
        
        return OrderResponse.builder()
                .id(order.getId())
                .orderDate(order.getOrderDate())
                .totalAmount(order.getTotalAmount())
                .paymentMethod(order.getPaymentMethod())
                .status(order.getStatus().name())
                .shippingAddress(addressResponse)
                .items(itemResponses)
                .build();
    }
    
    // Helper method to convert OrderItem entity to OrderItemResponse
    private OrderItemResponse convertToOrderItemResponse(OrderItem item) {
        BigDecimal subtotal = item.getPriceAtPurchase().multiply(new BigDecimal(item.getQuantity()));
        
        return OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .productImage(item.getProduct().getImageUrl())
                .quantity(item.getQuantity())
                .priceAtPurchase(item.getPriceAtPurchase())
                .subtotal(subtotal)
                .build();
    }
}
