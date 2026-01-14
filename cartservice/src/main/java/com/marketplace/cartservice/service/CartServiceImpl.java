package com.marketplace.cartservice.service;

import com.marketplace.cartservice.dto.request.AddItemRequest;
import com.marketplace.cartservice.dto.request.UpdateQuantityRequest;
import com.marketplace.cartservice.dto.response.CartResponse;
import com.marketplace.cartservice.exception.ResourceNotFoundException;
import com.marketplace.cartservice.mapper.CartMapper;
import com.marketplace.cartservice.model.Cart;
import com.marketplace.cartservice.model.CartItem;
import com.marketplace.cartservice.repository.CartRedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartServiceImpl {

    private final CartRedisRepository cartRepository;
    private final CartMapper cartMapper;

    public CartResponse getCart(String userId) {
        log.info("Getting cart for user: {}", userId);
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> createEmptyCart(userId));
        return cartMapper.toResponse(cart);
    }

    public CartResponse addItem(String userId, AddItemRequest request) {
        log.info("Adding item to cart for user: {}, productId: {}", userId, request.getProductId());
        
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> createEmptyCart(userId));

        // Check if product already exists in cart
        CartItem existingItem = cart.getItems().stream()
                .filter(item -> item.getProductId().equals(request.getProductId()))
                .findFirst()
                .orElse(null);

        if (existingItem != null) {
            // Increment quantity
            existingItem.setQuantity(existingItem.getQuantity() + request.getQuantity());
            log.debug("Incremented quantity for product: {}", request.getProductId());
        } else {
            // Add new item
            CartItem newItem = cartMapper.toCartItem(request);
            cart.getItems().add(newItem);
            log.debug("Added new item to cart: {}", request.getProductId());
        }

        cart.setLastUpdated(LocalDateTime.now());
        Cart savedCart = cartRepository.save(cart);
        return cartMapper.toResponse(savedCart);
    }

    public CartResponse updateItemQuantity(String userId, UUID productId, UpdateQuantityRequest request) {
        log.info("Updating quantity for user: {}, productId: {}, newQuantity: {}", 
                userId, productId, request.getQuantity());
        
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user: " + userId));

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getProductId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Product not found in cart: " + productId));

        item.setQuantity(request.getQuantity());
        cart.setLastUpdated(LocalDateTime.now());
        
        Cart savedCart = cartRepository.save(cart);
        return cartMapper.toResponse(savedCart);
    }

    public CartResponse removeItem(String userId, UUID productId) {
        log.info("Removing item from cart for user: {}, productId: {}", userId, productId);
        
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user: " + userId));

        boolean removed = cart.getItems().removeIf(item -> item.getProductId().equals(productId));
        
        if (!removed) {
            throw new ResourceNotFoundException("Product not found in cart: " + productId);
        }

        cart.setLastUpdated(LocalDateTime.now());
        Cart savedCart = cartRepository.save(cart);
        return cartMapper.toResponse(savedCart);
    }

    public void clearCart(String userId) {
        log.info("Clearing cart for user: {}", userId);
        cartRepository.deleteByUserId(userId);
    }

    private Cart createEmptyCart(String userId) {
        return Cart.builder()
                .userId(userId)
                .items(new ArrayList<>())
                .lastUpdated(LocalDateTime.now())
                .build();
    }
}
