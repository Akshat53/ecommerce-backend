package com.ecommerce.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cart")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Cart {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;
    
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> cartItems = new ArrayList<>();
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Helper method to calculate cart total
    @Transient
    public BigDecimal getTotal() {
        return cartItems.stream()
                .map(item -> item.getProduct().getPrice().multiply(new BigDecimal(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    // Helper method to add item to cart
    public void addItem(Product product, int quantity) {
        // Check if product already in cart
        for (CartItem item : cartItems) {
            if (item.getProduct().getId().equals(product.getId())) {
                item.setQuantity(item.getQuantity() + quantity);
                return;
            }
        }
        
        // If not, add new item
        CartItem newItem = new CartItem();
        newItem.setCart(this);
        newItem.setProduct(product);
        newItem.setQuantity(quantity);
        cartItems.add(newItem);
    }
    
    // Helper method to update item quantity
    public void updateItemQuantity(Long productId, int quantity) {
        for (CartItem item : cartItems) {
            if (item.getProduct().getId().equals(productId)) {
                if (quantity > 0) {
                    item.setQuantity(quantity);
                } else {
                    cartItems.remove(item);
                }
                return;
            }
        }
    }
    
    // Helper method to remove item from cart
    public void removeItem(Long productId) {
        cartItems.removeIf(item -> item.getProduct().getId().equals(productId));
    }
    
    // Helper method to clear cart
    public void clear() {
        cartItems.clear();
    }
}