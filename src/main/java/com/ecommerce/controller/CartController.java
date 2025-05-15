package com.ecommerce.controller;

import com.ecommerce.dto.request.AddToCartRequest;
import com.ecommerce.dto.response.CartItemResponse;
import com.ecommerce.dto.response.CartResponse;
import com.ecommerce.model.Cart;
import com.ecommerce.model.CartItem;
import com.ecommerce.model.Product;
import com.ecommerce.model.User;
import com.ecommerce.repository.CartItemRepository;
import com.ecommerce.repository.CartRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Autowired
    public CartController(CartRepository cartRepository,
                        CartItemRepository cartItemRepository,
                        UserRepository userRepository,
                        ProductRepository productRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    @GetMapping
    public ResponseEntity<CartResponse> getCart(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Cart cart = cartRepository.findByCustomerId(user.getId())
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setCustomer(user);
                    return cartRepository.save(newCart);
                });
        
        return ResponseEntity.ok(convertToCartResponse(cart));
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addToCart(
            @Valid @RequestBody AddToCartRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));
        
        // Check stock
        if (product.getStockQuantity() < request.getQuantity()) {
            throw new RuntimeException("Not enough stock available");
        }
        
        // Get or create cart
        Cart cart = cartRepository.findByCustomerId(user.getId())
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setCustomer(user);
                    return cartRepository.save(newCart);
                });
        
        // Check if product already in cart
        // Create a final reference to cart
        final Cart finalCart = cart;
        CartItem cartItem = cartItemRepository.findByCartIdAndProductId(finalCart.getId(), product.getId())
                .orElseGet(() -> {
                    CartItem newItem = new CartItem();
                    newItem.setCart(finalCart);
                    newItem.setProduct(product);
                    newItem.setQuantity(0);
                    newItem.setPrice(product.getPrice());
                    return newItem;
                });
        
        // Update quantity
        cartItem.setQuantity(cartItem.getQuantity() + request.getQuantity());
        cartItemRepository.save(cartItem);
        
        // Refresh cart from database
        Cart refreshedCart = cartRepository.findById(finalCart.getId()).orElseThrow();
        
        return ResponseEntity.ok(convertToCartResponse(refreshedCart));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartResponse> updateCartItem(
            @PathVariable Long itemId,
            @RequestBody Map<String, Integer> request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        CartItem cartItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));
        
        // Check if the cart belongs to the user
        if (!cartItem.getCart().getCustomer().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();  // Forbidden
        }
        
        Integer quantity = request.get("quantity");
        if (quantity == null || quantity < 1) {
            throw new RuntimeException("Invalid quantity");
        }
        
        // Check stock
        if (cartItem.getProduct().getStockQuantity() < quantity) {
            throw new RuntimeException("Not enough stock available");
        }
        
        cartItem.setQuantity(quantity);
        cartItemRepository.save(cartItem);
        
        // Refresh cart from database
        Cart cart = cartRepository.findById(cartItem.getCart().getId()).orElseThrow();
        
        return ResponseEntity.ok(convertToCartResponse(cart));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CartResponse> removeFromCart(
            @PathVariable Long itemId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        CartItem cartItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));
        
        // Check if the cart belongs to the user
        if (!cartItem.getCart().getCustomer().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();  // Forbidden
        }
        
        // Store cart ID before deleting
        Long cartId = cartItem.getCart().getId();
        
        cartItemRepository.delete(cartItem);
        
        // Refresh cart from database
        Cart cart = cartRepository.findById(cartId).orElseThrow();
        
        return ResponseEntity.ok(convertToCartResponse(cart));
    }

    @DeleteMapping
    public ResponseEntity<CartResponse> clearCart(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Cart cart = cartRepository.findByCustomerId(user.getId())
                .orElseThrow(() -> new RuntimeException("Cart not found"));
        
        // Remove all items
        cart.getCartItems().clear();
        cartRepository.save(cart);
        
        return ResponseEntity.ok(convertToCartResponse(cart));
    }

    // Helper method to convert Cart entity to CartResponse
    private CartResponse convertToCartResponse(Cart cart) {
        List<CartItemResponse> itemResponses = cart.getCartItems().stream()
                .map(this::convertToCartItemResponse)
                .collect(Collectors.toList());
        
        BigDecimal total = cart.getCartItems().stream()
                .map(item -> item.getPrice().multiply(new BigDecimal(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        int itemCount = cart.getCartItems().stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
        
        return CartResponse.builder()
                .id(cart.getId())
                .items(itemResponses)
                .total(total)
                .itemCount(itemCount)
                .build();
    }

    // Helper method to convert CartItem entity to CartItemResponse
    private CartItemResponse convertToCartItemResponse(CartItem item) {
        BigDecimal subtotal = item.getPrice().multiply(new BigDecimal(item.getQuantity()));
        
        return CartItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .productImage(item.getProduct().getImageUrl())
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .subtotal(subtotal)
                .build();
    }
}
