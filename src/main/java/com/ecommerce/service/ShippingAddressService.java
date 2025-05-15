package com.ecommerce.service;

import com.ecommerce.dto.request.AddressRequest;
import com.ecommerce.dto.response.ShippingAddressResponse;

import java.util.List;

public interface ShippingAddressService {
    ShippingAddressResponse createAddress(Long userId, AddressRequest request);
    ShippingAddressResponse updateAddress(Long addressId, Long userId, AddressRequest request);
    void deleteAddress(Long addressId, Long userId);
    ShippingAddressResponse getAddressById(Long addressId, Long userId);
    List<ShippingAddressResponse> getUserAddresses(Long userId);
    ShippingAddressResponse setDefaultAddress(Long addressId, Long userId);
}
