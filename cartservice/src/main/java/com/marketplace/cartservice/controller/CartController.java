package com.marketplace.cartservice.controller;

import com.marketplace.cartservice.dto.request.AddItemRequest;
import com.marketplace.cartservice.dto.request.UpdateQuantityRequest;
import com.marketplace.cartservice.dto.response.CartResponse;
import com.marketplace.cartservice.service.CartServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Cart", description = "Shopping cart management APIs")
public class CartController {

    private final CartServiceImpl cartService;

    @GetMapping
    @Operation(summary = "Get cart", description = "Retrieve the shopping cart for the authenticated user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cart retrieved successfully")
    })
    public ResponseEntity<CartResponse> getCart(
        @Parameter(description = "User ID from KrakenD gateway", required = true)
        @RequestHeader("X-User-Id") String userId
    ) {
        log.info("Getting cart for user: {}", userId);
        CartResponse response = cartService.getCart(userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/add")
    @Operation(summary = "Add item to cart", description = "Add a product to the shopping cart")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Item added successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<CartResponse> addItem(
        @Parameter(description = "User ID from KrakenD gateway", required = true)
        @RequestHeader("X-User-Id") String userId,
        @Valid @RequestBody AddItemRequest request
    ) {
        log.info("Adding item to cart for user: {}, productId: {}", userId, request.getProductId());
        CartResponse response = cartService.addItem(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/items/{productId}")
    @Operation(summary = "Update item quantity", description = "Update the quantity of a product in the cart")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Quantity updated successfully"),
        @ApiResponse(responseCode = "404", description = "Product not found in cart")
    })
    public ResponseEntity<CartResponse> updateItemQuantity(
        @Parameter(description = "User ID from KrakenD gateway", required = true)
        @RequestHeader("X-User-Id") String userId,
        @Parameter(description = "Product ID to update", required = true)
        @PathVariable UUID productId,
        @Valid @RequestBody UpdateQuantityRequest request
    ) {
        log.info("Updating quantity for user: {}, productId: {}", userId, productId);
        CartResponse response = cartService.updateItemQuantity(userId, productId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/items/{productId}")
    @Operation(summary = "Remove item from cart", description = "Remove a specific product from the cart")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Item removed successfully"),
        @ApiResponse(responseCode = "404", description = "Product not found in cart")
    })
    public ResponseEntity<CartResponse> removeItem(
        @Parameter(description = "User ID from KrakenD gateway", required = true)
        @RequestHeader("X-User-Id") String userId,
        @Parameter(description = "Product ID to remove", required = true)
        @PathVariable UUID productId
    ) {
        log.info("Removing item from cart for user: {}, productId: {}", userId, productId);
        CartResponse response = cartService.removeItem(userId, productId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping
    @Operation(summary = "Clear cart", description = "Remove all items from the shopping cart")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Cart cleared successfully")
    })
    public ResponseEntity<Void> clearCart(
        @Parameter(description = "User ID from KrakenD gateway", required = true)
        @RequestHeader("X-User-Id") String userId
    ) {
        log.info("Clearing cart for user: {}", userId);
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }
}
