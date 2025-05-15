package com.ecommerce.repository;

import com.ecommerce.model.UserDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserDetailRepository extends JpaRepository<UserDetail, Long> {
    Optional<UserDetail> findByUserId(Long userId);
    Optional<UserDetail> findByEmail(String email);
    boolean existsByEmail(String email);
}
