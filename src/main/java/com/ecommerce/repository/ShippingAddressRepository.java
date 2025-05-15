package com.ecommerce.repository;

import com.ecommerce.model.ShippingAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShippingAddressRepository extends JpaRepository<ShippingAddress, Long> {
    List<ShippingAddress> findByUserId(Long userId);
    Optional<ShippingAddress> findByUserIdAndIsDefaultTrue(Long userId);
}
