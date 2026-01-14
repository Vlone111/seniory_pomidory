package com.marketplace.cartservice.repository;

import com.marketplace.cartservice.model.Cart;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Slf4j
public class CartRedisRepository {

    private static final String CART_KEY_PREFIX = "cart:";
    private static final Duration CART_TTL = Duration.ofDays(30);

    private final RedisTemplate<String, Object> redisTemplate;

    public Cart save(Cart cart) {
        String key = buildKey(cart.getUserId());
        cart.setLastUpdated(LocalDateTime.now());
        redisTemplate.opsForValue().set(key, cart, CART_TTL);
        log.debug("Cart saved for user: {}", cart.getUserId());
        return cart;
    }

    public Optional<Cart> findByUserId(String userId) {
        String key = buildKey(userId);
        Object result = redisTemplate.opsForValue().get(key);
        if (result instanceof Cart cart) {
            log.debug("Cart found for user: {}", userId);
            return Optional.of(cart);
        }
        log.debug("No cart found for user: {}", userId);
        return Optional.empty();
    }

    public void deleteByUserId(String userId) {
        String key = buildKey(userId);
        Boolean deleted = redisTemplate.delete(key);
        log.debug("Cart deleted for user: {}, success: {}", userId, deleted);
    }

    public boolean existsByUserId(String userId) {
        String key = buildKey(userId);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public void refreshTtl(String userId) {
        String key = buildKey(userId);
        redisTemplate.expire(key, CART_TTL);
        log.debug("TTL refreshed for user: {}", userId);
    }

    private String buildKey(String userId) {
        return CART_KEY_PREFIX + userId;
    }
}
