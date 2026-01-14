package com.example.userservice.service;

import com.example.userservice.dto.RegisterRequestDTO;
import com.example.userservice.exception.KeycloakException;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakService {

    private final Keycloak keycloak;

    @Value("${keycloak.realm:main_one}")
    private String realmName;

    private static final String DEFAULT_ROLE = "user";

    public String createUser(RegisterRequestDTO dto) {
        UserRepresentation user = new UserRepresentation();
        user.setEnabled(true);
        user.setEmail(dto.getEmail());
        user.setUsername(dto.getEmail());
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setEmailVerified(false);

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(dto.getPassword());
        credential.setTemporary(false);
        user.setCredentials(Collections.singletonList(credential));

        RealmResource realm = keycloak.realm(realmName);

        try (Response response = realm.users().create(user)) {
            if (response.getStatus() == 201) {
                String userId = CreatedResponseUtil.getCreatedId(response);
                log.info("User created in Keycloak with ID: {}", userId);

                // Assign role
                String roleToAssign = (dto.getRole() != null && 
                        ("owner".equals(dto.getRole()) || "user".equals(dto.getRole())))
                        ? dto.getRole() : DEFAULT_ROLE;
                assignRoleToUser(userId, roleToAssign);

                // Send verification email asynchronously with proper classloader context
                ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
                CompletableFuture.runAsync(() -> {
                    Thread.currentThread().setContextClassLoader(contextClassLoader);
                    try {
                        UserResource userResource = realm.users().get(userId);
                        userResource.sendVerifyEmail();
                        log.info("Verification email sent for user: {}", userId);
                    } catch (Exception e) {
                        log.warn("Failed to send verification email for user {}: {}", userId, e.getMessage());
                    }
                });

                return userId;
            } else {
                String errorMessage = response.readEntity(String.class);
                log.error("Keycloak user creation failed. Status: {}, Body: {}", response.getStatus(), errorMessage);
                throw new KeycloakException("User creation failed: " + response.getStatus() + " - " + errorMessage);
            }
        }
    }

    public void deleteUser(String userId) {
        try {
            RealmResource realm = keycloak.realm(realmName);
            realm.users().get(userId).remove();
            log.info("User deleted from Keycloak: {}", userId);
        } catch (Exception e) {
            log.error("Failed to delete user from Keycloak: {}", userId, e);
            throw new KeycloakException("Failed to delete user from Keycloak: " + e.getMessage());
        }
    }

    @CacheEvict(value = "userRoles", key = "#userId")
    public void assignRoleToUser(String userId, String roleName) {
        try {
            RealmResource realm = keycloak.realm(realmName);
            UserResource userResource = realm.users().get(userId);
            RoleRepresentation role = realm.roles().get(roleName).toRepresentation();
            userResource.roles().realmLevel().add(Collections.singletonList(role));
            log.info("Role '{}' assigned to user '{}'", roleName, userId);
        } catch (Exception e) {
            log.error("Failed to assign role '{}' to user '{}': {}", roleName, userId, e.getMessage());
            throw new KeycloakException("Failed to assign role: " + e.getMessage());
        }
    }

    public boolean hasRole(String userId, String roleName) {
        try {
            RealmResource realm = keycloak.realm(realmName);
            UserResource userResource = realm.users().get(userId);
            List<RoleRepresentation> roles = userResource.roles().realmLevel().listAll();
            return roles.stream().anyMatch(role -> roleName.equals(role.getName()));
        } catch (Exception e) {
            log.error("Failed to check role for user '{}': {}", userId, e.getMessage());
            return false;
        }
    }

    @Cacheable(value = "userRoles", key = "#userId")
    public List<String> getUserRoles(String userId) {
        try {
            RealmResource realm = keycloak.realm(realmName);
            UserResource userResource = realm.users().get(userId);
            List<RoleRepresentation> roles = userResource.roles().realmLevel().listAll();
            return roles.stream()
                    .map(RoleRepresentation::getName)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to get roles for user '{}': {}", userId, e.getMessage());
            return Collections.emptyList();
        }
    }

    public void assignRoleWithPermission(String currentUserId, String targetUserId, String roleName) {
        if ("admin".equals(roleName) && !hasRole(currentUserId, "admin")) {
            throw new SecurityException("Only admins can assign admin role");
        }
        assignRoleToUser(targetUserId, roleName);
    }

    public void logoutUser(String userId) {
        try {
            RealmResource realm = keycloak.realm(realmName);
            UserResource userResource = realm.users().get(userId);
            userResource.logout();
            log.info("User logged out from all sessions: {}", userId);
        } catch (Exception e) {
            log.error("Failed to logout user '{}': {}", userId, e.getMessage());
            throw new KeycloakException("Failed to logout user: " + e.getMessage());
        }
    }
}
