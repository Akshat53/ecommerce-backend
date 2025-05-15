package com.ecommerce.service.impl;

import com.ecommerce.dto.request.UpdateProfileRequest;
import com.ecommerce.dto.response.UserProfileResponse;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.model.User;
import com.ecommerce.model.UserDetail;
import com.ecommerce.repository.UserDetailRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserDetailRepository userDetailRepository;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, UserDetailRepository userDetailRepository) {
        this.userRepository = userRepository;
        this.userDetailRepository = userDetailRepository;
    }

    @Override
    public UserProfileResponse getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        UserDetail userDetail = userDetailRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User details not found for user id: " + userId));
        
        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(userDetail.getFullName())
                .email(userDetail.getEmail())
                .phoneNumber(userDetail.getPhoneNumber())
                .role(user.getRole().name())
                .build();
    }
    
    @Override
    public UserProfileResponse getUserProfileByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
        
        UserDetail userDetail = userDetailRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User details not found for user id: " + user.getId()));
        
        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(userDetail.getFullName())
                .email(userDetail.getEmail())
                .phoneNumber(userDetail.getPhoneNumber())
                .role(user.getRole().name())
                .build();
    }

    @Override
    @Transactional
    public UserProfileResponse updateUserProfile(Long userId, UpdateProfileRequest request) {
        UserDetail userDetail = userDetailRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User details not found for user id: " + userId));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        userDetail.setFullName(request.getFullName());
        userDetail.setEmail(request.getEmail());
        userDetail.setPhoneNumber(request.getPhoneNumber());
        
        userDetailRepository.save(userDetail);
        
        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(userDetail.getFullName())
                .email(userDetail.getEmail())
                .phoneNumber(userDetail.getPhoneNumber())
                .role(user.getRole().name())
                .build();
    }
}