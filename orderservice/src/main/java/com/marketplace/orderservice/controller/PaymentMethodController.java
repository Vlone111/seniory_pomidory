package com.marketplace.orderservice.controller;

import com.marketplace.orderservice.dto.response.PaymentMethodDto;
import com.marketplace.orderservice.service.PaymentMethodService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/payment-methods")
@RequiredArgsConstructor
@Slf4j
public class PaymentMethodController {

    private final PaymentMethodService paymentMethodService;

    @GetMapping
    public ResponseEntity<List<PaymentMethodDto>> getAllPaymentMethods() {
        log.info("Received get all payment methods request");
        List<PaymentMethodDto> methods = paymentMethodService.getAllPaymentMethods();
        return ResponseEntity.ok(methods);
    }
}
