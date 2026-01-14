package com.marketplace.orderservice.controller;

import com.marketplace.orderservice.dto.request.OrderRequestDto;
import com.marketplace.orderservice.dto.request.UpdateOrderStatusDto;
import com.marketplace.orderservice.dto.response.OrderResponseDto;
import com.marketplace.orderservice.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponseDto> createOrder(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody OrderRequestDto requestDto) {
        log.info("Received create order request from user: {}", userId);
        UUID userUuid = UUID.fromString(userId);
        OrderResponseDto response = orderService.createOrder(userUuid, requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<OrderResponseDto>> getUserOrders(
            @RequestHeader("X-User-Id") String userId) {
        log.info("Received get orders request from user: {}", userId);
        UUID userUuid = UUID.fromString(userId);
        List<OrderResponseDto> orders = orderService.getUserOrders(userUuid);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponseDto> getOrderById(
            @PathVariable UUID orderId,
            @RequestHeader("X-User-Id") String userId) {
        log.info("Received get order request for order: {} from user: {}", orderId, userId);
        UUID userUuid = UUID.fromString(userId);
        OrderResponseDto order = orderService.getOrderById(orderId, userUuid);
        return ResponseEntity.ok(order);
    }

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<OrderResponseDto> updateOrderStatus(
            @PathVariable UUID orderId,
            @Valid @RequestBody UpdateOrderStatusDto statusDto) {
        log.info("Received update order status request for order: {}", orderId);
        OrderResponseDto order = orderService.updateOrderStatus(orderId, statusDto);
        return ResponseEntity.ok(order);
    }
}
