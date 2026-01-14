package com.marketplace.orderservice.controller;

import com.marketplace.orderservice.dto.response.PaymentMethodDto;
import com.marketplace.orderservice.dto.yookassa.YooKassaPaymentResponse;
import com.marketplace.orderservice.service.PaymentMethodService;
import com.marketplace.orderservice.service.YooKassaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.ResponseBody;
import reactor.core.publisher.Mono;

import java.util.List;

@Controller
@RequestMapping("/api/payment-methods")
@RequiredArgsConstructor
@Slf4j
public class PaymentMethodController {

    private final PaymentMethodService paymentMethodService;
    private final YooKassaService yooKassaService;

    @GetMapping
    @ResponseBody
    public ResponseEntity<List<PaymentMethodDto>> getAllPaymentMethods() {
        log.info("Received get all payment methods request");
        List<PaymentMethodDto> methods = paymentMethodService.getAllPaymentMethods();
        return ResponseEntity.ok(methods);
    }
    
    @PostMapping("/yookassa/create-payment")
    @ResponseBody
    public Mono<ResponseEntity<YooKassaPaymentResponse>> createYooKassaPayment(
            @RequestParam String amount,
            @RequestParam(defaultValue = "RUB") String currency,
            @RequestParam String description,
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) String returnUrl) {
        log.info("Received YooKassa payment creation request for amount: {} {}, description: {}, orderId: {}", 
                amount, currency, description, orderId);
        
        return yooKassaService.createPayment(amount, currency, description, orderId, returnUrl)
                .map(ResponseEntity::ok)
                .onErrorResume(error -> {
                    log.error("Error creating payment: {}", error.getMessage(), error);
                    return Mono.just(ResponseEntity.badRequest().build());
                });
    }
    
    @GetMapping("/yookassa/create-payment")
    @ResponseBody
    public Mono<ResponseEntity<YooKassaPaymentResponse>> createYooKassaPaymentGet(
            @RequestParam String amount,
            @RequestParam(defaultValue = "RUB") String currency,
            @RequestParam String description,
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) String returnUrl) {
        log.info("Received YooKassa payment creation GET request for amount: {} {}, description: {}, orderId: {}", 
                amount, currency, description, orderId);
        
        return yooKassaService.createPayment(amount, currency, description, orderId, returnUrl)
                .map(ResponseEntity::ok)
                .onErrorResume(error -> {
                    log.error("Error creating payment via GET: {}", error.getMessage(), error);
                    return Mono.just(ResponseEntity.badRequest().build());
                });
    }
    
    @GetMapping("/yookassa/payment-status/{paymentId}")
    @ResponseBody
    public Mono<ResponseEntity<YooKassaPaymentResponse>> getPaymentStatus(@PathVariable String paymentId) {
        log.info("Received YooKassa payment status request for payment ID: {}", paymentId);
        
        return yooKassaService.getPaymentStatus(paymentId)
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.ok(YooKassaPaymentResponse.builder()
                        .id(paymentId)
                        .status("pending")
                        .amount(YooKassaPaymentResponse.Amount.builder()
                                .value("123.00")
                                .currency("RUB")
                                .build())
                        .description("Тестовый платеж")
                        .test(true)
                        .paid(false)
                        .refundable(false)
                        .build()));
    }
    
    @GetMapping("/yookassa/payment-page")
    public Mono<String> getPaymentPage(
            @RequestParam String amount,
            @RequestParam(defaultValue = "RUB") String currency,
            @RequestParam String description,
            Model model) {
        log.info("Received payment page request for amount: {} {}, description: {}", amount, currency, description);
        
        return yooKassaService.createPayment(amount, currency, description)
                .map(paymentResponse -> {
                    model.addAttribute("confirmationToken", paymentResponse.getConfirmation().getConfirmationToken());
                    model.addAttribute("paymentId", paymentResponse.getId());
                    model.addAttribute("amount", amount);
                    model.addAttribute("description", description);
                    model.addAttribute("returnUrl", "http://localhost:8087/payment-complete.html?paymentId=" + paymentResponse.getId());
                    return "payment-page";
                })
                .onErrorReturn("error");
    }
}
