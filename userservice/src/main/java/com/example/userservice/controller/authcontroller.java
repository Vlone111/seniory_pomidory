package com.example.userservice.controller;

import com.example.userservice.dto.AssignRoleDto;
import com.example.userservice.dto.RegisterRequestDTO;
import com.example.userservice.dto.UserProfileDTO;
import com.example.userservice.service.KeycloakService;
import com.example.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User registration and role management")
public class AuthController {

    private final UserService userService;
    private final KeycloakService keycloakService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates user in Keycloak and saves profile to database")
    public ResponseEntity<UserProfileDTO> register(@Valid @RequestBody RegisterRequestDTO dto) {
        UserProfileDTO profile = userService.registerUser(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(profile);
    }

    @PostMapping("/registerowner")
    @Operation(summary = "Register a new shop owner", description = "Creates user in Keycloak with owner role and saves profile to database")
    public ResponseEntity<UserProfileDTO> registerOwner(@Valid @RequestBody RegisterRequestDTO dto) {
        dto.setRole("owner");
        UserProfileDTO profile = userService.registerUser(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(profile);
    }

    @PostMapping("/assign-role")
    @Operation(summary = "Assign role to user", description = "Only admins can assign admin role")
    public ResponseEntity<?> assignRole(
            @RequestHeader(value = "X-User-Id", required = false) String currentUserId,
            @RequestBody AssignRoleDto dto) {
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("User ID not found in headers. Please ensure you are authenticated through KrakenD.");
        }

        keycloakService.assignRoleWithPermission(currentUserId, dto.getTargetUserId(), dto.getRoleName());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/user/{userId}/roles")
    @Operation(summary = "Get user roles", description = "Returns list of roles assigned to the user")
    public ResponseEntity<?> getUserRoles(@PathVariable String userId) {
        return ResponseEntity.ok(keycloakService.getUserRoles(userId));
    }

    @GetMapping("/me")
    @Operation(summary = "Test endpoint", description = "Returns current user info from headers")
    public ResponseEntity<?> getCurrentUser(
            @RequestHeader(value = "X-User-Id", required = false) String currentUserId,
            @RequestHeader(value = "X-User-Role", required = false) String currentUserRole) {
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not authenticated");
        }
        return ResponseEntity.ok("User ID: " + currentUserId + ", Role: " + currentUserRole);
    }
}
