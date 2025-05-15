package com.ecommerce.service.impl;

import com.ecommerce.dto.request.AddressRequest;
import com.ecommerce.dto.response.ShippingAddressResponse;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.model.ShippingAddress;
import com.ecommerce.model.User;
import com.ecommerce.repository.ShippingAddressRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.ShippingAddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ShippingAddressServiceImpl implements ShippingAddressService {
    
    private final ShippingAddressRepository shippingAddressRepository;
    private final UserRepository userRepository;
    
    @Autowired
    public ShippingAddressServiceImpl(ShippingAddressRepository shippingAddressRepository,
                                    UserRepository userRepository) {
        this.shippingAddressRepository = shippingAddressRepository;
        this.userRepository = userRepository;
    }
    
    @Override
    public ShippingAddressResponse createAddress(Long userId, AddressRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        ShippingAddress address = new ShippingAddress();
        address.setUser(user);
        address.setAddressLine1(request.getAddressLine1());
        address.setAddressLine2(request.getAddressLine2());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPostalCode(request.getPostalCode());
        address.setCountry(request.getCountry());
        address.setIsDefault(request.getIsDefault());
        
        // If this is the first address or is set as default, update other addresses
        if (request.getIsDefault()) {
            updateDefaultAddress(userId);
        }
        
        ShippingAddress savedAddress = shippingAddressRepository.save(address);
        
        return convertToShippingAddressResponse(savedAddress);
    }
    
    @Override
    public ShippingAddressResponse updateAddress(Long addressId, Long userId, AddressRequest request) {
        ShippingAddress address = shippingAddressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + addressId));
        
        // Check if the address belongs to the user
        if (!address.getUser().getId().equals(userId)) {
            throw new RuntimeException("You don't have permission to update this address");
        }
        
        address.setAddressLine1(request.getAddressLine1());
        address.setAddressLine2(request.getAddressLine2());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPostalCode(request.getPostalCode());
        address.setCountry(request.getCountry());
        
        // Update default status if needed
        if (request.getIsDefault() && !address.getIsDefault()) {
            updateDefaultAddress(userId);
            address.setIsDefault(true);
        }
        
        ShippingAddress updatedAddress = shippingAddressRepository.save(address);
        
        return convertToShippingAddressResponse(updatedAddress);
    }
    
    @Override
    public void deleteAddress(Long addressId, Long userId) {
        ShippingAddress address = shippingAddressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + addressId));
        
        // Check if the address belongs to the user
        if (!address.getUser().getId().equals(userId)) {
            throw new RuntimeException("You don't have permission to delete this address");
        }
        
        shippingAddressRepository.delete(address);
    }
    
    @Override
    public ShippingAddressResponse getAddressById(Long addressId, Long userId) {
        ShippingAddress address = shippingAddressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + addressId));
        
        // Check if the address belongs to the user
        if (!address.getUser().getId().equals(userId)) {
            throw new RuntimeException("You don't have permission to view this address");
        }
        
        return convertToShippingAddressResponse(address);
    }
    
    @Override
    public List<ShippingAddressResponse> getUserAddresses(Long userId) {
        List<ShippingAddress> addresses = shippingAddressRepository.findByUserId(userId);
        
        return addresses.stream()
                .map(this::convertToShippingAddressResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public ShippingAddressResponse setDefaultAddress(Long addressId, Long userId) {
        ShippingAddress address = shippingAddressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + addressId));
        
        // Check if the address belongs to the user
        if (!address.getUser().getId().equals(userId)) {
            throw new RuntimeException("You don't have permission to update this address");
        }
        
        // Update all addresses to not default
        updateDefaultAddress(userId);
        
        // Set this address as default
        address.setIsDefault(true);
        ShippingAddress updatedAddress = shippingAddressRepository.save(address);
        
        return convertToShippingAddressResponse(updatedAddress);
    }
    
    // Helper method to update default address
    private void updateDefaultAddress(Long userId) {
        shippingAddressRepository.findByUserIdAndIsDefaultTrue(userId)
                .ifPresent(a -> {
                    a.setIsDefault(false);
                    shippingAddressRepository.save(a);
                });
    }
    
    // Helper method to convert ShippingAddress entity to ShippingAddressResponse
    private ShippingAddressResponse convertToShippingAddressResponse(ShippingAddress address) {
        return ShippingAddressResponse.builder()
                .id(address.getId())
                .addressLine1(address.getAddressLine1())
                .addressLine2(address.getAddressLine2())
                .city(address.getCity())
                .state(address.getState())
                .postalCode(address.getPostalCode())
                .country(address.getCountry())
                .isDefault(address.getIsDefault())
                .build();
    }
}
