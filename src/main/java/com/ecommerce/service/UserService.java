package com.ecommerce.service;

import com.ecommerce.dto.request.UpdateProfileRequest;
import com.ecommerce.dto.response.UserProfileResponse;

public interface UserService {
    UserProfileResponse getUserProfile(Long userId);
    UserProfileResponse getUserProfileByUsername(String username);
    UserProfileResponse updateUserProfile(Long userId, UpdateProfileRequest request);
}