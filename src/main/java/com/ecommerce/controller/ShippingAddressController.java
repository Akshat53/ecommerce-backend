package com.ecommerce.controller;

import com.ecommerce.dto.request.AddressRequest;
import com.ecommerce.dto.response.ShippingAddressResponse;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.ShippingAddressService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
public class ShippingAddressController {

    private final ShippingAddressService shippingAddressService;
    private final UserRepository userRepository;

    @Autowired
    public ShippingAddressController(ShippingAddressService shippingAddressService, 
                                   UserRepository userRepository) {
        this.shippingAddressService = shippingAddressService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<ShippingAddressResponse>> getUserAddresses(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
        
        List<ShippingAddressResponse> addresses = shippingAddressService.getUserAddresses(userId);
        
        return ResponseEntity.ok(addresses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShippingAddressResponse> getAddressById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
        
        ShippingAddressResponse address = shippingAddressService.getAddressById(id, userId);
        
        return ResponseEntity.ok(address);
    }

    @PostMapping
    public ResponseEntity<ShippingAddressResponse> createAddress(
            @Valid @RequestBody AddressRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
        
        ShippingAddressResponse address = shippingAddressService.createAddress(userId, request);
        
        return ResponseEntity.ok(address);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ShippingAddressResponse> updateAddress(
            @PathVariable Long id,
            @Valid @RequestBody AddressRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
        
        ShippingAddressResponse address = shippingAddressService.updateAddress(id, userId, request);
        
        return ResponseEntity.ok(address);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAddress(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
        
        shippingAddressService.deleteAddress(id, userId);
        
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/default")
    public ResponseEntity<ShippingAddressResponse> setDefaultAddress(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
        
        ShippingAddressResponse address = shippingAddressService.setDefaultAddress(id, userId);
        
        return ResponseEntity.ok(address);
    }
}
