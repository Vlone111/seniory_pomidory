package com.example.userservice.controller;

import com.example.userservice.dto.*;
import com.example.userservice.service.RecipientService;
import com.example.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "User profile and recipients management")
public class UserController {

    private final UserService userService;
    private final RecipientService recipientService;

    @GetMapping
    @Operation(summary = "Get all users", description = "Admin only - returns all user profiles")
    public ResponseEntity<List<UserProfileDTO>> getAllUsers(
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<UserProfileDTO> users = userService.getAllUsers(userId);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{targetUserId}")
    @Operation(summary = "Get user by ID", description = "Admin can view any user, others can only view themselves")
    public ResponseEntity<UserProfileDTO> getUserById(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @PathVariable String targetUserId) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UserProfileDTO profile = userService.getUserById(userId, targetUserId);
        return ResponseEntity.ok(profile);
    }

    @DeleteMapping("/{targetUserId}")
    @Operation(summary = "Delete user", description = "Admin can delete any user/owner. Users can delete only themselves")
    public ResponseEntity<Void> deleteUser(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @PathVariable String targetUserId) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        userService.deleteUser(userId, targetUserId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user profile", description = "Returns profile based on X-User-Id header")
    public ResponseEntity<UserProfileDTO> getMyProfile(
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UserProfileDTO profile = userService.getUserProfile(userId);
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user profile", description = "Updates profile fields (phone, name)")
    public ResponseEntity<UserProfileDTO> updateMyProfile(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Valid @RequestBody UpdateProfileDTO dto) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UserProfileDTO profile = userService.updateUserProfile(userId, dto);
        return ResponseEntity.ok(profile);
    }

    @DeleteMapping("/me")
    @Operation(summary = "Delete my account", description = "Deletes the current user's account")
    public ResponseEntity<Void> deleteMyAccount(
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        userService.deleteMyAccount(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/me/logout")
    @Operation(summary = "Logout", description = "Logs out current user from all sessions")
    public ResponseEntity<Void> logout(
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        userService.logout(userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me/recipients")
    @Operation(summary = "Get all recipients", description = "Returns all delivery addresses for current user")
    public ResponseEntity<List<RecipientDTO>> getMyRecipients(
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<RecipientDTO> recipients = recipientService.getRecipientsByUserId(userId);
        return ResponseEntity.ok(recipients);
    }

    @PostMapping("/me/recipients")
    @Operation(summary = "Create recipient", description = "Adds a new delivery address")
    public ResponseEntity<RecipientDTO> createRecipient(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Valid @RequestBody CreateRecipientDTO dto) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        RecipientDTO recipient = recipientService.createRecipient(userId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(recipient);
    }

    @GetMapping("/me/recipients/{recipientId}")
    @Operation(summary = "Get recipient by ID", description = "Returns specific delivery address")
    public ResponseEntity<RecipientDTO> getRecipient(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @PathVariable UUID recipientId) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        RecipientDTO recipient = recipientService.getRecipientById(userId, recipientId);
        return ResponseEntity.ok(recipient);
    }

    @PutMapping("/me/recipients/{recipientId}")
    @Operation(summary = "Update recipient", description = "Updates delivery address")
    public ResponseEntity<RecipientDTO> updateRecipient(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @PathVariable UUID recipientId,
            @Valid @RequestBody CreateRecipientDTO dto) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        RecipientDTO recipient = recipientService.updateRecipient(userId, recipientId, dto);
        return ResponseEntity.ok(recipient);
    }

    @DeleteMapping("/me/recipients/{recipientId}")
    @Operation(summary = "Delete recipient", description = "Removes delivery address")
    public ResponseEntity<Void> deleteRecipient(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @PathVariable UUID recipientId) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        recipientService.deleteRecipient(userId, recipientId);
        return ResponseEntity.noContent().build();
    }
}
