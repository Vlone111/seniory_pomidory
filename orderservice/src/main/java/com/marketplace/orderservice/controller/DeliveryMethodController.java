package com.marketplace.orderservice.controller;

import com.marketplace.orderservice.dto.response.DeliveryMethodDto;
import com.marketplace.orderservice.service.DeliveryMethodService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/delivery-methods")
@RequiredArgsConstructor
@Slf4j
public class DeliveryMethodController {

    private final DeliveryMethodService deliveryMethodService;

    @GetMapping
    public ResponseEntity<List<DeliveryMethodDto>> getAllDeliveryMethods() {
        log.info("Received get all delivery methods request");
        List<DeliveryMethodDto> methods = deliveryMethodService.getAllDeliveryMethods();
        return ResponseEntity.ok(methods);
    }
}
