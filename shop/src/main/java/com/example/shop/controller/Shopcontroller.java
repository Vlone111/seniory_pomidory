package com.example.shop.controller;

import com.example.shop.dto.request.RegistrationRequest;
import com.example.shop.dto.request.UpdateShopRequest;
import com.example.shop.dto.response.ShopResponseDto;
import com.example.shop.service.ShopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/shops")
@RequiredArgsConstructor
@Tag(name = "Shop Management", description = "APIs for managing shops in the marketplace")
public class ShopController {

    private final ShopService shopService;

    @PostMapping
    @Operation(summary = "Create a new shop", description = "Creates a new shop for the authenticated owner")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Shop created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "409", description = "Shop name or URL already exists")
    })
    public ResponseEntity<ShopResponseDto> createShop(
        @Parameter(description = "User ID from authentication", required = true)
        @RequestHeader("X-User-Id") String userId,
        @Valid @RequestBody RegistrationRequest dto
    ) {
        UUID userUuid = UUID.fromString(userId);
        ShopResponseDto response = shopService.createShop(dto, userUuid);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{shopId}")
    @Operation(summary = "Get shop by ID", description = "Returns shop details by its UUID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Shop found"),
        @ApiResponse(responseCode = "404", description = "Shop not found")
    })
    public ResponseEntity<ShopResponseDto> getShopById(
        @Parameter(description = "Shop UUID", required = true)
        @PathVariable UUID shopId
    ) {
        ShopResponseDto response = shopService.getShopById(shopId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/url/{shopUrl}")
    @Operation(summary = "Get shop by URL", description = "Returns shop details by its unique URL slug")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Shop found"),
        @ApiResponse(responseCode = "404", description = "Shop not found")
    })
    public ResponseEntity<ShopResponseDto> getShopByUrl(
        @Parameter(description = "Shop URL slug", required = true)
        @PathVariable String shopUrl
    ) {
        ShopResponseDto response = shopService.getShopByUrl(shopUrl);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/owner/{ownerId}")
    @Operation(summary = "Get shops by owner", description = "Returns all shops owned by a specific user")
    public ResponseEntity<List<ShopResponseDto>> getShopsByOwner(
        @Parameter(description = "Owner UUID", required = true)
        @PathVariable UUID ownerId
    ) {
        List<ShopResponseDto> response = shopService.getShopsByOwner(ownerId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-shops")
    @Operation(summary = "Get my shops", description = "Returns all shops owned by the authenticated user")
    public ResponseEntity<List<ShopResponseDto>> getMyShops(
        @Parameter(description = "User ID from authentication", required = true)
        @RequestHeader("X-User-Id") String userId
    ) {
        UUID userUuid = UUID.fromString(userId);
        List<ShopResponseDto> response = shopService.getShopsByOwner(userUuid);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get all shops", description = "Returns all shops in the marketplace")
    public ResponseEntity<List<ShopResponseDto>> getAllShops() {
        List<ShopResponseDto> response = shopService.getAllShops();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{shopId}")
    @Operation(summary = "Update shop", description = "Updates shop details. Only the owner can update their shop")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Shop updated successfully"),
        @ApiResponse(responseCode = "403", description = "Not the owner of this shop"),
        @ApiResponse(responseCode = "404", description = "Shop not found"),
        @ApiResponse(responseCode = "409", description = "Shop name already exists")
    })
    public ResponseEntity<ShopResponseDto> updateShop(
        @Parameter(description = "Shop UUID", required = true)
        @PathVariable UUID shopId,
        @Parameter(description = "User ID from authentication", required = true)
        @RequestHeader("X-User-Id") String userId,
        @Valid @RequestBody UpdateShopRequest dto
    ) {
        UUID userUuid = UUID.fromString(userId);
        ShopResponseDto response = shopService.updateShop(shopId, dto, userUuid);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{shopId}")
    @Operation(summary = "Delete shop", description = "Deletes a shop. Only the owner can delete their shop")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Shop deleted successfully"),
        @ApiResponse(responseCode = "403", description = "Not the owner of this shop"),
        @ApiResponse(responseCode = "404", description = "Shop not found")
    })
    public ResponseEntity<Void> deleteShop(
        @Parameter(description = "Shop UUID", required = true)
        @PathVariable UUID shopId,
        @Parameter(description = "User ID from authentication", required = true)
        @RequestHeader("X-User-Id") String userId
    ) {
        UUID userUuid = UUID.fromString(userId);
        shopService.deleteShop(shopId, userUuid);
        return ResponseEntity.noContent().build();
        
    }
}
