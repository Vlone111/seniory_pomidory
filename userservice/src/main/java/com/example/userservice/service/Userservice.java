package com.example.userservice.service;

import com.example.userservice.dto.RegisterRequestDTO;
import com.example.userservice.dto.UpdateProfileDTO;
import com.example.userservice.dto.UserProfileDTO;
import com.example.userservice.entity.UserEntity;
import com.example.userservice.exception.KeycloakException;
import com.example.userservice.exception.UserNotFoundException;
import com.example.userservice.mapper.UserMapper;
import com.example.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final KeycloakService keycloakService;

    private static final String ROLE_ADMIN = "admin";
    private static final String ROLE_OWNER = "owner";
    private static final String ROLE_USER = "user";

    @Transactional(readOnly = true)
    public UserProfileDTO getUserProfile(String userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
        return userMapper.toProfileDTO(user);
    }

    @Transactional
    public UserProfileDTO updateUserProfile(String userId, UpdateProfileDTO dto) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

        userMapper.updateEntityFromDTO(dto, user);
        UserEntity updatedUser = userRepository.save(user);
        log.info("User profile updated: {}", userId);

        return userMapper.toProfileDTO(updatedUser);
    }

    @Transactional
    public UserProfileDTO registerUser(RegisterRequestDTO dto) {
        long startTime = System.currentTimeMillis();
        String keycloakUserId = null;

        try {
            // Step 1: Create user in Keycloak
            long keycloakStart = System.currentTimeMillis();
            keycloakUserId = keycloakService.createUser(dto);
            long keycloakTime = System.currentTimeMillis() - keycloakStart;
            log.info("User created in Keycloak with ID: {} ({}ms)", keycloakUserId, keycloakTime);

            // Step 2: Save user profile to PostgreSQL with the same ID
            long dbStart = System.currentTimeMillis();
            UserEntity userEntity = UserEntity.builder()
                    .id(keycloakUserId)
                    .email(dto.getEmail())
                    .firstName(dto.getFirstName())
                    .lastName(dto.getLastName())
                    .build();

            UserEntity savedUser = userRepository.save(userEntity);
            long dbTime = System.currentTimeMillis() - dbStart;
            log.info("User profile saved to database: {} ({}ms)", keycloakUserId, dbTime);

            long totalTime = System.currentTimeMillis() - startTime;
            log.info("Total registration time: {}ms", totalTime);

            return userMapper.toProfileDTO(savedUser);

        } catch (Exception e) {
            // Rollback: If DB save fails, delete user from Keycloak
            if (keycloakUserId != null) {
                log.error("Failed to save user to database, rolling back Keycloak user: {}", keycloakUserId);
                try {
                    keycloakService.deleteUser(keycloakUserId);
                    log.info("Successfully rolled back Keycloak user: {}", keycloakUserId);
                } catch (Exception rollbackEx) {
                    log.error("Failed to rollback Keycloak user: {}", keycloakUserId, rollbackEx);
                }
            }
            throw new KeycloakException("Registration failed: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Transactional(readOnly = true)
    public List<UserProfileDTO> getAllUsers(String currentUserId) {
        List<String> currentUserRoles = keycloakService.getUserRoles(currentUserId);
        
        if (!currentUserRoles.contains(ROLE_ADMIN)) {
            throw new SecurityException("Only admins can view all users");
        }
        
        List<UserEntity> users = userRepository.findAll();
        return users.stream()
                .map(userMapper::toProfileDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserProfileDTO getUserById(String currentUserId, String targetUserId) {
        List<String> currentUserRoles = keycloakService.getUserRoles(currentUserId);
        
        // Admin can view any user, others can only view themselves
        if (!currentUserRoles.contains(ROLE_ADMIN) && !currentUserId.equals(targetUserId)) {
            throw new SecurityException("You can only view your own profile");
        }
        
        UserEntity user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + targetUserId));
        return userMapper.toProfileDTO(user);
    }

    @Transactional
    public void deleteUser(String currentUserId, String targetUserId) {
        List<String> currentUserRoles = keycloakService.getUserRoles(currentUserId);
        List<String> targetUserRoles = keycloakService.getUserRoles(targetUserId);
        
        boolean isAdmin = currentUserRoles.contains(ROLE_ADMIN);
        boolean isSelfDelete = currentUserId.equals(targetUserId);
        boolean targetIsAdmin = targetUserRoles.contains(ROLE_ADMIN);
        
        // Authorization rules:
        // 1. Users/Owners can delete their own account
        // 2. Admin can delete any user or owner (but not other admins unless self)
        if (!isSelfDelete) {
            if (!isAdmin) {
                throw new SecurityException("You can only delete your own account");
            }
            if (targetIsAdmin) {
                throw new SecurityException("Admins cannot delete other admin accounts");
            }
        }
        
        // Verify user exists in DB
        if (!userRepository.existsById(targetUserId)) {
            throw new UserNotFoundException("User not found: " + targetUserId);
        }
        
        try {
            // Delete from Keycloak first
            keycloakService.deleteUser(targetUserId);
            log.info("User deleted from Keycloak: {}", targetUserId);
            
            // Then delete from database
            userRepository.deleteById(targetUserId);
            log.info("User deleted from database: {}", targetUserId);
            
        } catch (Exception e) {
            log.error("Failed to delete user: {}", targetUserId, e);
            throw new KeycloakException("Failed to delete user: " + e.getMessage());
        }
    }

    @Transactional
    public void deleteMyAccount(String userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException("User not found: " + userId);
        }
        
        try {
            keycloakService.deleteUser(userId);
            log.info("User deleted from Keycloak: {}", userId);
            
            userRepository.deleteById(userId);
            log.info("User deleted from database: {}", userId);
            
        } catch (Exception e) {
            log.error("Failed to delete user: {}", userId, e);
            throw new KeycloakException("Failed to delete account: " + e.getMessage());
        }
    }

    public void logout(String userId) {
        keycloakService.logoutUser(userId);
        log.info("User logged out: {}", userId);
    }
}
