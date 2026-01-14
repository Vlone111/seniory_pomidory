package com.marketplace.productservice.controller;

import com.marketplace.productservice.dto.request.ProductRequestDto;
import com.marketplace.productservice.dto.response.ProductResponseDto;
import com.marketplace.productservice.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductResponseDto> createProduct(
        @RequestHeader("X-User-Id") String userId,
        @Valid @RequestBody ProductRequestDto requestDto
    ) {
        UUID userUuid = UUID.fromString(userId);
        ProductResponseDto response = productService.createProduct(requestDto, userUuid);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDto> getProductById(@PathVariable UUID id) {
        ProductResponseDto response = productService.getProductById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<ProductResponseDto>> getProducts(
        @RequestParam(required = false) UUID shopId
    ) {
        List<ProductResponseDto> response;
        if (shopId != null) {
            response = productService.getProductsByShopId(shopId);
        } else {
            response = productService.getAllProducts();
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/shop/{shopId}/active")
    public ResponseEntity<List<ProductResponseDto>> getActiveProductsByShopId(@PathVariable UUID shopId) {
        List<ProductResponseDto> response = productService.getActiveProductsByShopId(shopId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<List<ProductResponseDto>> searchProducts(
        @RequestParam(required = false) UUID categoryId,
        @RequestParam(required = false) UUID brandId,
        @RequestParam(required = false) BigDecimal minPrice,
        @RequestParam(required = false) BigDecimal maxPrice,
        @RequestParam(required = false) String searchText
    ) {
        List<ProductResponseDto> response = productService.searchProducts(
            categoryId, brandId, minPrice, maxPrice, searchText
        );
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponseDto> updateProduct(
        @PathVariable UUID id,
        @RequestHeader("X-User-Id") String userId,
        @Valid @RequestBody ProductRequestDto requestDto
    ) {
        ProductResponseDto response = productService.updateProduct(id, requestDto);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable UUID id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
