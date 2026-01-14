package com.marketplace.productservice.controller;

import com.marketplace.productservice.dto.request.BrandRequestDto;
import com.marketplace.productservice.dto.response.BrandResponseDto;
import com.marketplace.productservice.service.BrandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/brands")
@RequiredArgsConstructor
public class BrandController {

    private final BrandService brandService;

    @PostMapping
    public ResponseEntity<BrandResponseDto> createBrand(@Valid @RequestBody BrandRequestDto requestDto) {
        BrandResponseDto response = brandService.createBrand(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BrandResponseDto> getBrandById(@PathVariable UUID id) {
        BrandResponseDto response = brandService.getBrandById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<BrandResponseDto>> getAllBrands() {
        List<BrandResponseDto> response = brandService.getAllBrands();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/shop/{shopId}")
    public ResponseEntity<List<BrandResponseDto>> getBrandsByShopId(@PathVariable UUID shopId) {
        List<BrandResponseDto> response = brandService.getBrandsByShopId(shopId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BrandResponseDto> updateBrand(
        @PathVariable UUID id,
        @Valid @RequestBody BrandRequestDto requestDto
    ) {
        BrandResponseDto response = brandService.updateBrand(id, requestDto);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBrand(@PathVariable UUID id) {
        brandService.deleteBrand(id);
        return ResponseEntity.noContent().build();
    }
}
