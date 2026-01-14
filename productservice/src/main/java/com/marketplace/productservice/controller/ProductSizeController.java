package com.marketplace.productservice.controller;

import com.marketplace.productservice.dto.request.ProductSizeRequestDto;
import com.marketplace.productservice.dto.response.ProductSizeResponseDto;
import com.marketplace.productservice.service.ProductSizeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/product-sizes")
@RequiredArgsConstructor
public class ProductSizeController {

    private final ProductSizeService productSizeService;

    @PostMapping
    public ResponseEntity<ProductSizeResponseDto> createProductSize(
        @Valid @RequestBody ProductSizeRequestDto requestDto
    ) {
        ProductSizeResponseDto response = productSizeService.createProductSize(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductSizeResponseDto> getProductSizeById(@PathVariable UUID id) {
        ProductSizeResponseDto response = productSizeService.getProductSizeById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<ProductSizeResponseDto>> getProductSizesByProductId(@PathVariable UUID productId) {
        List<ProductSizeResponseDto> response = productSizeService.getProductSizesByProductId(productId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductSizeResponseDto> updateProductSize(
        @PathVariable UUID id,
        @Valid @RequestBody ProductSizeRequestDto requestDto
    ) {
        ProductSizeResponseDto response = productSizeService.updateProductSize(id, requestDto);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProductSize(@PathVariable UUID id) {
        productSizeService.deleteProductSize(id);
        return ResponseEntity.noContent().build();
    }
}
